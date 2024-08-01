package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.llm.utils.CacheManager;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.workerDispose.utils.WriteIds;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class Dispose {

    static {
        //initialize Profiles
        ContextLoader.loadContext();
    }
    /**
     * 第一步
     */
    @Test
    public void test1() {
        String csvFilePath = "D:/关键词ids/ai_dict_user_1.csv"; // 输入文件路径
        String csvFilePath1 = "D:/关键词ids/输出的词/output.csv"; // 输出CSV文件的路径
        int startAtWord = 2173; // 从第n个词开始
        int batchLimit = 1000; // 设置每个批次的最大词数
        String result = PickThorn(csvFilePath,startAtWord,batchLimit,csvFilePath1);
        System.out.println(result);
    }
    /**
     * 第二步
     */
    @Test
    public void test2() {
        String csvFilePath = "C:/Users/ruiqing.luo/Desktop/ai_dict_segment_chinese_0711.csv"; // 输入文件路径
        String csvFilePath1 = "D:/关键词ids/output.csv"; // 输出CSV文件的路径
        int startAtWord = 542043; // 从第n个词开始
        int batchLimit = 300; // 设置每个批次的最大词数
        String result = PickThorn(csvFilePath,startAtWord,batchLimit,csvFilePath1);

        System.out.println(result);
    }

    public static String PickThorn(String csvFilePath,int startAtWord,int batchLimit,String csvFilePath1) {

        //String content = "请帮我分析一下这些词中，请把不正确的词和人名完整的挑出来,有错字的词汇,没有意义的词汇,包括人名，要求：全部挑出来，不用解释。相关词为:“";
        String content = "";
        int pici =0;

        int wordCount = 0; // 初始化词计数器

        String idto = "";

        //try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFilePath), Charset.forName("UTF-8")))) {

            String line;

            StringBuilder batchContent = new StringBuilder(); // 用于存储每个批次的词

            //while ((line = br.readLine()) != null && wordCount < startAtWord) {
            while ((line = br.readLine()) != null && wordCount < startAtWord) {
                String[] values = line.split(","); // 假设CSV是用逗号分隔的
                if (values.length >= 2) { // 确保有足够的列
                    wordCount++; // 增加词计数器
                }
            }

            System.out.println("从第"+wordCount+"个开始分析");
            wordCount = 0;

            while ((line = br.readLine()) != null && wordCount < batchLimit) {
                String[] values = line.split(","); // 假设CSV是用逗号分隔的
                if (values.length >= 2) { // 确保有足够的列
                    String id = values[0].trim(); // 假设ID在第一列
                    String keyword = values[1].trim().replaceAll("^\"|\"$", ""); // 关键词在第二列
                    content += keyword+",";

                    idto = id;
                }

                wordCount++; // 增加词计数器

                 //当达到批次的词数限制时
                if (wordCount % batchLimit == 0) {
                    // 执行分析逻辑，例如打印或处理batchContent
                    System.out.println("分析从第" + (wordCount - batchLimit*pici + 1) + ":"+batchContent.toString()+"个开始的批次");
                    String keywords = analyse(startAtWord,  wordCount,  idto,  content);
                    String result = WriteIds.do1(keywords,csvFilePath,csvFilePath1);
                    //String result = "";
                    System.out.println(result);
                    // 重置批次内容，准备下一个批次
                    // 等待30秒
                    try {
                        Thread.sleep(30 * 1000);
                        batchContent.setLength(0);
                        content = "";
                        wordCount = 0;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

            }

            // 处理最后一个批次（如果有剩余的词）
            if (batchContent.length() > 0) {
                System.out.println("Analyzing last batch starting at word " + (wordCount - batchContent.toString().split(",").length + 1) + ":");
                System.out.println(batchContent.toString());
                String keywords = analyse(startAtWord,  wordCount,  idto,  content);
                String result = WriteIds.do1(keywords,csvFilePath,csvFilePath1);
                //String result = "";
                System.out.println(result);
                // 重置批次内容，准备下一个批次
                batchContent.setLength(0);
                content = "";
            }

        }catch (Exception e){
            e.printStackTrace();
        }

        return "任务完成！";
    }

    private static String analyse(int startAtWord, int wordCount, String idto, String content) {
        System.out.println("该处的id为："+ idto +"");
        //请帮我分析一下这些词中，请把不正确的词和没有意义的词汇全部挑出来，不用解释。相关词为:“
        String content1 = "请把不常见的词语挑出来 “";

        System.out.println("第一道工序："+content);

        content1 += chat2("请帮我分析一下这些词中，请把没有意义的词汇，生造词，全部挑出来，不用解释。"+content)+
        //content1 += content+
                "” \n 再用英文的逗号进行拼接给我，只用返回这些词汇就行，不用返回其它的提示词，不用反回注意事项,没有就不返回";
        System.out.println("第二道工序："+content1);

//        String request = chat(content1);

//        String content2 = "请帮我把没有实际意义的词汇挑出来。相关字符为:“";
//
//        content2 += request+
//                "” \n 请把无意义的词挑出来，并用,进行拼接给我，只用返回这些词汇就行,不用返回其它的提示词，不用反回注意事项";
//
//        System.out.println("第三道工序："+content2);
//        String result = chat1(content2);
        String result =chat(content1);
        //request.substring(0, request.length() - 1);
        System.out.println("最终送分析的数据："+result);
        return result;
    }

    /**
     * kimi
     * @param content
     * @return
     */
    public static String chat(String content) {
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
        // Create an instance of CompletionsService
        CompletionsService completionsService = new CompletionsService();
        CacheManager.put("moonshot-v1-128k", Boolean.TRUE);
        // Call the completions method to process the chat completion request
        ChatCompletionResult result = completionsService.completions(chatCompletionRequest);
        // Print the content of the first completion choice
       // System.out.println("outcome:" + result.getChoices().get(0).getMessage().getContent());
        return result.getChoices().get(0).getMessage().getContent();
    }

    /**
     * 智谱轻言
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
        // Create an instance of CompletionsService
        CompletionsService completionsService = new CompletionsService();
        CacheManager.put("glm-4v", Boolean.TRUE);

        ChatCompletionResult result = completionsService.completions(chatCompletionRequest);

        return result.getChoices().get(0).getMessage().getContent();
    }

    /**
     * 文心一言
     * @param content
     * @return
     */
    public static String chat2(String content) {
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
        // Create an instance of CompletionsService
        CompletionsService completionsService = new CompletionsService();
        CacheManager.put("ERNIE-Speed-128K", Boolean.TRUE);

        // Call the completions method to process the chat completion request
        ChatCompletionResult result = completionsService.completions(chatCompletionRequest);
                //ernieAdapter.completions(chatCompletionRequest);

        return result.getChoices().get(0).getMessage().getContent();
    }
    @Test
    public void test11(){
        System.out.println(chat2("你是那个公司开发的！"));
    }
}
