package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import com.google.common.collect.Lists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;


public class CsvExpandForChat {

    static {
        ContextLoader.loadContext();
    }

    // 配置参数
    private static int batchSize = 5;
    //    private static String inputCsvPath = "E:\\file\\TempFiles\\nid数据\\output_股票智能体.csv";  // 输入CSV路径
    private static String inputCsvPath = "E:\\file\\TempFiles\\nid数据\\output_FAQ挖掘.csv";  // 输入CSV路径

    // 关系对应ID的映射
    private static final Map<String, Integer> relationMap = new HashMap<>();
    private static final Map<String, String> relationFormatMap = new HashMap<>();

    static {
        // 关系映射和格式映射的初始化，这部分和你原代码一样
        relationMap.put("因果关系", 1);
        relationMap.put("动及关系", 2);
        relationMap.put("总分关系", 3);
        relationMap.put("比较关系", 4);
        relationMap.put("隶属关系", 5);
        relationMap.put("主形关系", 6);
        relationMap.put("组互关系", 7);
        relationMap.put("表征关系", 8);
        relationMap.put("含有关系", 9);
        relationMap.put("命名关系", 10);
        relationMap.put("位置关系", 11);
        relationMap.put("组团关系", 12);
        relationMap.put("症解关系", 13);
        relationMap.put("并列关系", 14);
        relationMap.put("承接关系", 15);
        relationMap.put("转折关系", 16);
        relationMap.put("选择关系", 17);
        relationMap.put("假设关系", 18);
        relationMap.put("让步关系", 19);
        relationMap.put("递进关系", 20);
        relationMap.put("条件关系", 21);
        relationMap.put("目的关系", 22);
        relationMap.put("指代关系", 23);
        relationMap.put("虚语关系", 24);
        relationMap.put("构成关系", 25);
        relationMap.put("层级关系", 26);
        relationMap.put("来源关系", 27);
        relationMap.put("用途关系", 28);
        relationMap.put("类别关系", 29);
        relationMap.put("活动关系", 30);
        relationMap.put("描述关系", 31);
        relationMap.put("身份关系", 32);
        relationMap.put("影响关系", 33);
        relationMap.put("传承关系", 34);
        relationMap.put("时间关系", 35);
        relationMap.put("状态关系", 36);
        relationMap.put("评价关系", 37);
        relationMap.put("协作关系", 38);

        // 关系格式表
        relationFormatMap.put("因果关系", "Cause-->Result");
        relationFormatMap.put("动及关系", "Action-->Target");
        relationFormatMap.put("总分关系", "Assembly-->Part");
        relationFormatMap.put("比较关系", "L-->Compare-->R");
        relationFormatMap.put("隶属关系", "Totality-->SubType");
        relationFormatMap.put("主形关系", "Target-->Form");
        relationFormatMap.put("组互关系", "Master<--Target-->Master");
        relationFormatMap.put("表征关系", "Symptom-->Eigens");
        relationFormatMap.put("含有关系", "Target-->Content");
        relationFormatMap.put("命名关系", "Target-->Naming");
        relationFormatMap.put("位置关系", "Target-->Location");
        relationFormatMap.put("组团关系", "Suite-->Unit");
        relationFormatMap.put("症解关系", "Symptom-->Solution");
        relationFormatMap.put("并列关系", "coordinating-->coordinating");
        relationFormatMap.put("承接关系", "Carry-->On");
        relationFormatMap.put("转折关系", "Whereas-->Conj");
        relationFormatMap.put("选择关系", "Or-->Or");
        relationFormatMap.put("假设关系", "If-->Else");
        relationFormatMap.put("让步关系", "Even-->If");
        relationFormatMap.put("递进关系", "In-->Addition");
        relationFormatMap.put("条件关系", "If-->Onlyif");
        relationFormatMap.put("目的关系", "Purpose-->Action");
        relationFormatMap.put("指代关系", "Refer-->To");
        relationFormatMap.put("虚语关系", "Imaginary--Language");
        relationFormatMap.put("构成关系", "Constitution-->Component");
        relationFormatMap.put("层级关系", "Level-->Stage");
        relationFormatMap.put("来源关系", "Source-->Result");
        relationFormatMap.put("用途关系", "Usage-->Function");
        relationFormatMap.put("类别关系", "Category-->Instance");
        relationFormatMap.put("活动关系", "Activity-->Process");
        relationFormatMap.put("描述关系", "Object-->Description");
        relationFormatMap.put("身份关系", "Identity-->Role");
        relationFormatMap.put("影响关系", "Impact-->On");
        relationFormatMap.put("传承关系", "Heritage-->Tradition");
        relationFormatMap.put("时间关系", "Time-->Event");
        relationFormatMap.put("状态关系", "State-->Change");
        relationFormatMap.put("评价关系", "Evaluation-->Of");
        relationFormatMap.put("协作关系", "Collaborate-->With");
    }

    // 获取last_processed_edge_id.txt的路径
    private static Path getLastProcessedEdgeIdPath() {
        Path inputPath = Paths.get(inputCsvPath);
        Path parentDir = inputPath.getParent();
        return parentDir.resolve("last_processed_edge_id.txt");
    }

    // 读取上次处理的edge_id
    private static int readLastProcessedEdgeId() {
        Path path = getLastProcessedEdgeIdPath();
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String content = reader.readLine().trim();
                return Integer.parseInt(content);
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
        return 1; // 默认从1开始
    }

    // 保存当前处理的edge_id
    private static void saveLastProcessedEdgeId(int edgeId) {
        Path path = getLastProcessedEdgeIdPath();
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(String.valueOf(edgeId));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    // 生成关系描述
    public static String generateRelationDesc(String parent, String child, String relation) {
        // 检查特殊的虚语关系
        if ("虚语关系".equals(relation)) {
            return "-";  // 将虚语关系格式化为"新银--或其"这种格式
        }

        String format = relationFormatMap.getOrDefault(relation, "->");
        if (format.contains("<--") && format.contains("-->")) {
            return "<-->";
        } else if (format.contains("-->")) {
            return "->";
        }
        return format;
    }

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


    // 在处理CSV时，使用追加模式打开文件


    public static void processCsv(String inputCsvPath, int startEdgeId, int endEdgeId) throws IOException {
        Path inputPath = Paths.get(inputCsvPath);
        String baseName = inputPath.getFileName().toString().replace(".csv", "");
        Path parentDir = inputPath.getParent();
        Path outputCsvPath = parentDir.resolve(baseName + "_output.csv");

        // 确保输出文件目录存在，不存在则创建
        if (Files.notExists(parentDir)) {
            Files.createDirectories(parentDir);  // 创建目录
        }

        // 打印路径调试
        System.out.println("Output file path: " + outputCsvPath);

        List<String> lines = Files.readAllLines(inputPath, StandardCharsets.UTF_8);
        String header = lines.get(0) + ",relation_id,direction,child_attribute,child_weight,comments,desc";  // 更新表头顺序

        try (BufferedWriter writer = Files.newBufferedWriter(outputCsvPath, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            writer.write(header);
            writer.newLine();

            List<String[]> rows = lines.stream()
                    .skip(1)
                    .map(line -> line.split(","))
                    .collect(Collectors.toList());

            int totalWritten = 0;

            for (int i = startEdgeId - 1; i < endEdgeId && i < rows.size(); i += batchSize) {
                int currentEdgeId = Integer.parseInt(rows.get(i)[0]);

                try {
                    List<String> batch = new ArrayList<>();
                    for (int j = i; j < Math.min(i + batchSize, rows.size()); j++) {
                        String[] row = rows.get(j);
                        batch.add("'" + row[1] + "' '" + row[2] + "'"); // 打包数据
                    }

                    String content = "现有如下type：\n" +
                            "因果关系、动及关系、总分关系、比较关系、隶属关系、主形关系、组互关系、表征关系、含有关系、命名关系、位置关系、组团关系、症解关系、并列关系、承接关系、转折关系、选择关系、假设关系、让步关系、递进关系、条件关系、目的关系、指代关系、虚语关系、构成关系、层级关系、来源关系、用途关系、类别关系、活动关系、描述关系、身份关系、影响关系、传承关系、时间关系、状态关系、评价关系、协作关系。\n" +
                            "要求：\n" +
                            "(1) 依据以上类别，对下面的每组词进行分类，每对之间用分号隔开。type的值即为该类型；\n" +
                            "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就为“其它关系”; \n" +
                            "(3) 输出格式为：“{词A和词B属于type, 词A和词B属于type}” 例如：{新银和贷款属于目的关系, 新银和资产负债属于系统关系};\n" +
                            "请给下面的数据进行分类： \n" +
                            String.join(";", batch) + ";\n" +
                            "注意：完整的给每组词分类，无需解释，无需其它提示词。";

                    System.out.println("content = " + content);

                    String response = chatFor910B(content);
                    System.out.println("response = " + response);

                    List<String> resultList = parseResponse(response);
                    System.out.println("resultList = " + resultList);

                    for (int j = 0; j < batch.size(); j++) {
                        if (i + j >= rows.size()) break;

                        String[] row = rows.get(i + j);
                        String relation = resultList.size() > j ? resultList.get(j) : "0";
                        Integer relationId = relationMap.getOrDefault(relation, 0);
                        String relationDesc = generateRelationDesc(row[1], row[2], relation);

                        // 新增列
                        String[] newRow = Arrays.copyOf(row, row.length + 6);
                        newRow[row.length] = String.valueOf(relationId);
                        newRow[row.length + 1] = relationDesc;
                        newRow[row.length + 2] = "";  // child_attribute 为空
                        newRow[row.length + 3] = "1"; // child_weight 固定为 1
                        newRow[row.length + 4] = row[1] + relationDesc + row[2];  // comments
                        newRow[row.length + 5] = row[1] + relationDesc + row[2];  // desc

                        // 实时写入到文件
                        writer.write(String.join(",", newRow));
                        writer.newLine();  // 换行
                        writer.flush();  // 强制将缓冲区内容写入文件

                        totalWritten++;  // 累加写入的条数
                    }

                    double progress = Math.min(((i + batchSize) * 100.0) / (endEdgeId), 100.0);
                    System.out.printf("当前进度: %.2f%%, 当前处理到 edge_id: %d%n", progress, currentEdgeId);

                    // 保存当前的 edge_id
                    saveLastProcessedEdgeId(currentEdgeId);

                    // 提示当前写入数据条数
                    System.out.printf("当前已写入 %d 条数据。\n", totalWritten);

                } catch (Exception e) {
                    System.err.printf("Error encountered at edge_id range [%d, %d]. Retrying...%n", currentEdgeId, endEdgeId);
                    e.printStackTrace();
                    i -= batchSize; // 出现异常时回退处理
                }
            }

            // 处理结束后，输出总共写入的条数
            System.out.printf("CSV处理完成，共写入了 %d 条数据！%n", totalWritten);

        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
            e.printStackTrace();
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
                relation = relation.equals("其它关系") ? "0" : relation;
                relationList.add(relation);
            } else {
                System.out.println("Problematic Answer: " + answer);
                relationList.add("0");
            }
        }
        return relationList;
    }

    public static void main(String[] args) {
        try {
            int[] edgeIdRange = getEdgeIdRange(inputCsvPath);
            int startEdgeId = edgeIdRange[0];
            int endEdgeId = edgeIdRange[1];

            // 读取上次处理的edge_id
            startEdgeId = readLastProcessedEdgeId();
            processCsv(inputCsvPath, startEdgeId, endEdgeId);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
