package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.workerDispose.pojo.General;
import ai.workerDispose.pojo.Generator;
import ai.workerDispose.utils.ConversionTypeUtils;
import com.google.common.collect.Lists;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CsvGenerator {

    static {
        ContextLoader.loadContext();
    }

    private static ConversionTypeUtils conversionTypeUtils = new ConversionTypeUtils();

    public static void main(String[] args) {
//        String inputFilePath = "E:\\file\\TempFiles\\nid数据\\小信智能体.csv";
//        String inputFilePath = "D:\\OfFile\\nid数据\\nid数据\\小信智能体.csv";
        String inputFilePath = "E:\\file\\TempFiles\\nid数据\\FAQ挖掘.csv";
        Integer pici = 50;

        // 自动生成 outputFilePath，将文件名改为 output_ + 原文件名
        Path inputPath = Paths.get(inputFilePath);
        String baseName = inputPath.getFileName().toString().replace(".csv", "");
        String outputFilePath = inputPath.getParent().resolve("output_" + baseName + ".csv").toString();

        List<String[]> entries = new ArrayList<>();
        Map<String, String> nidMap = new HashMap<>();

        System.out.println("开始读取文件: " + inputFilePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath), "GB2312"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");  // 使用逗号分隔
                if (parts.length >= 2) {
                    entries.add(new String[]{parts[0].trim(), parts[1].trim()});  // 存储 nid 和 Parent
                    nidMap.put(parts[1].trim(), parts[0].trim());
                }
            }
            System.out.println("文件读取完成，读取到 " + entries.size() + " 行数据。");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        List<General> responseList = new ArrayList<>();
        System.out.println("entries.size(): " + entries.size());
        for (int i = entries.size()-1; i >= 992; i--) {
            String guanjianci = "";
            Integer num = entries.size();
            Integer num1 = 0;
            for (int j = 0; j < entries.size(); j++) {
                num1++;
                if (j != i) {
                    guanjianci += entries.get(j)[1] + ",";
                    if (num1 >= pici) {
                        String response = "在以下词中，与“" + entries.get(i)[1] + "”有关系的词有那些。\n“" + guanjianci;
                        response += "” \n 要求：" +
                                "1.完整的输出有关系的词。\n " +
                                "2.输出格式为：每个词之间用逗号拼接，并用“{}”将所有有关的词括起来。例如：{有关的词1,有关的词2,有关的词3} \n" +
                                "3.输出的词中不能是生造词 \n" +
                                "4.在回答中“{}”有且只能出现一次 \n";
                        System.out.println("提问：" + response);
                        String reuslt = chat1(response);
                        System.out.println("处理前：" + reuslt);
                        reuslt = conversionTypeUtils.extractContentWithinBraces(reuslt);
                        String[] pairs = new String[0];
                        if (reuslt != null && reuslt.contains(",")) {
                            pairs = reuslt.split(",");
                            for (String pair : pairs) {
                                if (pair != null && !pair.equals("")) {
                                    responseList.add(new General(entries.get(i)[1], pair));
                                }
                            }
                        }
                        List<Generator> generatorList = new ArrayList<>();
                        for (General general : responseList) {
                            if (nidMap.get(general.getName()) != null && nidMap.get(general.getValue()) != null) {
                                generatorList.add(new Generator(nidMap.get(general.getName()), nidMap.get(general.getValue()), general.getName(), general.getValue()));
                            }
                        }
                        responseList.clear();
                        writeCvs(outputFilePath, generatorList);
                        System.out.println("写入文件：" + generatorList);

                        num -= pici;
                        num1 = 0;
                        guanjianci = "";
                    }
                }
            }
            String response = "在以下词中，与“" + entries.get(i)[1] + "”有直接或间接关系的词有那些。\n“" + guanjianci;
            response += "” \n 要求：" +
                    "1.完整的输出有关系的词。\n " +
                    "2.输出格式为：每个词之间用逗号拼接，并用“{}”将所有有关的词括起来。例如：{有关的词1,有关的词2,有关的词3} \n" +
                    "3.输出的词中不能是生造词 \n" +
                    "4.在回答中“{}”有且只能出现一次 \n";

            if (num >= 0) {
                System.out.println("最后一个提问：" + response);
                String reuslt = chat1(response);
                String[] pairs = new String[0];
                if (reuslt != null && reuslt.contains(",")) {
                    pairs = reuslt.split(",");
                    for (String pair : pairs) {
                        if (pair != null && !pair.equals("")) {
                            responseList.add(new General(entries.get(i)[1], pair));
                        }
                    }
                }
                List<Generator> generatorList = new ArrayList<>();
                for (General general : responseList) {
                    if (nidMap.get(general.getName()) != null && nidMap.get(general.getValue()) != null) {
                        generatorList.add(new Generator(nidMap.get(general.getName()), nidMap.get(general.getValue()), general.getName(), general.getValue()));
                    }
                }
                responseList.clear();
                writeCvs(outputFilePath, generatorList);
            }


        }

    }

    private static void writeCvs(String outputFilePath, List<Generator> generatorList) {
        //       boolean hasHeader = Files.exists(Paths.get(outputFilePath));
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath, true), StandardCharsets.UTF_8))) {
            int edgeId = 1;
//            if (hasHeader){
//                bw.write("edge_id,Parent,Child,Parent_id,Child_id\n");  // 写入表头
//            }

            for (Generator generator : generatorList) {
                bw.write(edgeId + "," + generator.getParent() + "," + generator.getChild() + "," + generator.getParentId() + "," + generator.getChildId() + "\n");
                edgeId++;
            }
            System.out.println("写入完成，共写入 " + edgeId + " 条数据。");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * kimi
     *
     * @param content
     * @return
     */
    public static String chat1(String content) {
        //mock request
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setTemperature(0.8);
        chatCompletionRequest.setMax_tokens(1024);
        chatCompletionRequest.setCategory("default");
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(content);
        chatCompletionRequest.setMessages(Lists.newArrayList(message));
        // Set the stream parameter to false
        chatCompletionRequest.setStream(false);
        chatCompletionRequest.setModel("moonshot-v1-128k");

        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = null;
        boolean isf = true;
        String retry = null;
        while (isf) {
            try {
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                if (retry != null) {
                    isf = false;
                }
            } catch (Exception e) {
                //chatCompletionRequest.setModel("qwen-plus");
                chatCompletionRequest.setModel("qwen-plus");
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                if (retry != null) {
                    isf = false;
                }
            }
        }

        return retry;
    }

}
