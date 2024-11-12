package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import com.google.common.collect.Lists;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CsvExpandForOneChat {

    static {
        ContextLoader.loadContext();
    }

    // 配置参数
    private static int batchSize = 100;  // 每批次处理的记录数量
    private static String inputCsvPath = "E:\\file\\TempFiles\\test.csv";  // 输入CSV路径

    // 调用大模型API
    public static String chatFor910B(String content) {
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setTemperature(0.8);
        chatCompletionRequest.setMax_tokens(4096);
        chatCompletionRequest.setCategory("default");
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(content);
        chatCompletionRequest.setMessages(Lists.newArrayList(message));
        chatCompletionRequest.setStream(false);
        chatCompletionRequest.setModel("qwen-plus");
        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = completionsService.completions(chatCompletionRequest);
        return result.getChoices().get(0).getMessage().getContent();
    }

    // 获取CSV文件的起始和结束edge_id
    public static int[] getEdgeIdRange(String inputCsvPath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(inputCsvPath), StandardCharsets.UTF_8);
        List<Integer> edgeIds = lines.stream()
                .skip(1)
                .map(line -> Integer.parseInt(line.split(",")[0]))
                .collect(Collectors.toList());

        int startEdgeId = edgeIds.stream().min(Integer::compare).orElse(1);
        int endEdgeId = edgeIds.stream().max(Integer::compare).orElse(startEdgeId);
        return new int[]{startEdgeId, endEdgeId};
    }

    // 处理CSV文件
    public static void processCsv(String inputCsvPath, int startEdgeId, int endEdgeId) throws IOException {
        Path inputPath = Paths.get(inputCsvPath);
        String baseName = inputPath.getFileName().toString().replace(".csv", "");
        Path parentDir = inputPath.getParent();
        String outputCsvPath = parentDir.resolve(baseName + "_output.csv").toString();

        List<String> lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
        String header = lines.get(0) + ",relation";
        List<String[]> rows = lines.stream()
                .skip(1)
                .map(line -> line.split(","))
                .collect(Collectors.toList());

        List<String[]> processedRows = new ArrayList<>();
        processedRows.add(header.split(","));

        // 根据批次大小，处理CSV文件
        for (int i = startEdgeId - 1; i < endEdgeId && i < rows.size(); i += batchSize) {
            int currentEdgeId = Integer.parseInt(rows.get(i)[0]);

            try {
                List<String> batch = new ArrayList<>();
                // 准备当前批次的数据
                for (int j = i; j < Math.min(i + batchSize, rows.size()); j++) {
                    String[] row = rows.get(j);
                    batch.add("'" + row[1] + "' '" + row[2] + "'");
                }

                // 构建内容并调用API进行分类
                String content = "现有38种type：\n" +
                        "因果关系、动及关系、总分关系、比较关系、隶属关系、主形关系、组互关系、表征关系、含有关系、命名关系、位置关系、组团关系、症解关系、并列关系、承接关系、转折关系、选择关系、假设关系、让步关系、递进关系、条件关系、目的关系、指代关系、虚语关系、构成关系、层级关系、来源关系、用途关系、类别关系、活动关系、描述关系、身份关系、影响关系、传承关系、时间关系、状态关系、评价关系、协作关系。\n" +
                        "要求：\n" +
                        "(1) 依据以上类别，对下面的每组词进行分类，每对之间用分号隔开。type的值即为该类型；\n" +
                        "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就为“其它关系”; \n" +
                        "(3) 输出格式为：“{词A和词B属于type, 词A和词B属于type}” 例如：{新银和贷款属于目的关系, 新银和资产负债属于系统关系};\n" +
                        "请给下面的数据进行分类： \n" +
                        String.join(";", batch) + ";\n" +
                        "注意：完整的给每组词分类，无需解释，无需其它提示词。";

                String response = chatFor910B(content);  // 获取API返回的响应

                List<String> resultList = parseResponse(response);  // 解析响应结果

                // 处理批次结果
                for (int j = 0; j < batch.size(); j++) {
                    if (i + j >= rows.size()) break;

                    String[] row = rows.get(i + j);
                    String relation = resultList.size() > j ? resultList.get(j) : "0";  // Default to "0"
                    String[] newRow = Arrays.copyOf(row, row.length + 1);
                    newRow[row.length] = relation;
                    processedRows.add(newRow);

                    currentEdgeId = Integer.parseInt(row[0]);
                }

                // 计算当前进度
                double progress = Math.min(((i + batchSize) * 100.0) / (endEdgeId), 100.0);
                System.out.printf("当前进度: %.2f%%, 当前处理到 edge_id: %d%n", progress, currentEdgeId);
            } catch (Exception e) {
                System.err.printf("Error encountered at edge_id range [%d, %d]. Retrying...%n", currentEdgeId, endEdgeId);
                e.printStackTrace();
                i -= batchSize;  // 错误时回滚并重试
            }
        }

        // 写入处理后的CSV文件
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputCsvPath), StandardCharsets.UTF_8)) {
            for (String[] row : processedRows) {
                writer.write(String.join(",", row));
                writer.newLine();
            }
            System.out.println("csv处理完成！");
        }
    }

    // 解析API返回的响应
    private static List<String> parseResponse(String response) {
        List<String> relationList = new ArrayList<>();

        if (response == null || response.trim().isEmpty()) {
            return relationList;
        }

        String[] answers = response.replace("{", "").replace("}", "").split(", ");
        for (String answer : answers) {
            String[] splitAnswer = answer.split("属于");
            if (splitAnswer.length == 2) {
                String relation = splitAnswer[1].trim();
                relation = relation.equals("其它关系") ? "0" : relation;  // Change "其它关系" to "0"
                relationList.add(relation);
            } else {
                System.out.println("Problematic Answer: " + answer);
                relationList.add("0");  // Default to "0" if parsing fails
            }
        }
        return relationList;
    }

    public static void main(String[] args) {
        try {
            int[] edgeIdRange = getEdgeIdRange(inputCsvPath);
            int startEdgeId = edgeIdRange[0];
            int endEdgeId = edgeIdRange[1];

            processCsv(inputCsvPath, startEdgeId, endEdgeId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 设置批次大小
    public static void setBatchSize(int batchSize) {
        CsvExpandForOneChat.batchSize = batchSize;
    }

    // 设置输入CSV文件路径
    public static void setInputCsvPath(String inputCsvPath) {
        CsvExpandForOneChat.inputCsvPath = inputCsvPath;
    }
}
