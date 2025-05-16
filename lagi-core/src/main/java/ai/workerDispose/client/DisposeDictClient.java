package ai.workerDispose.client;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.utils.OkHttpUtil;
import ai.workerDispose.dao.AiZindexUserDao;
import ai.workerDispose.pojo.DictValue;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DisposeDictClient {
    private static final Logger logger = LoggerFactory.getLogger(DisposeDictClient.class);

    private static final int START_INDEX = 0;
    private static final int END_INDEX = 7000000;
    private static final int PAGE_SIZE = 10000;
    private static final int THREAD_POOL_SIZE = 1;
    private static final AiZindexUserDao aiZindexUserDao;
//    private static final String BASE_URL = "http://127.0.0.1:8090";
    private static final String BASE_URL = "https://kgraph.landingbj.com";

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(THREAD_POOL_SIZE * 1000)
    );

    static {
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ContextLoader.loadContext();
        aiZindexUserDao = new AiZindexUserDao();
    }

    public static void main(String[] args) {
        dictWeightDisposeFromDb();
        logger.info("Processing completed.");
    }

    private static void dictWeightDisposeFromDb() {
        int offset = START_INDEX;
        while (offset < END_INDEX) {
            int limit = Math.min(PAGE_SIZE, END_INDEX - offset + 1);
            List<DictValue> dictList = aiZindexUserDao.getDictList(offset, limit);
            for (DictValue dictValue : dictList) {
                executor.submit(() -> asyncDictWeightDispose(dictValue));
            }
            logger.info("Processed offset: {}", offset);
            offset += limit;
        }
    }

    private static void asyncDictWeightDispose(DictValue dictValue) {
        if (dictValue.getPlainText().length() == 1 || isEnglishText(dictValue.getPlainText())) {
            return;
        }
        String prompt = String.format(getPromptTemplate(), dictValue.getPlainText());
        String tableName;
        try {
            tableName = aiZindexUserDao.getDictTableName(dictValue.getTableIndex());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        logger.info("{}, {}", dictValue, tableName);
        String model1 = "ERNIE-Speed-128K";
        String model2 = "deepseek-chat";
        String content1 = callLlm(prompt, model1);
        String content2 = callLlm(prompt, model2);
        if (content1 == null || content2 == null) {
            return;
        }
        if (content1.contains("否") && content2.contains("否")) {
            logger.info("Delete dict value: {}", dictValue.getPlainText());
            try {
                deleteDict(tableName, dictValue.getSubId());
            } catch (IOException e) {
                logger.error("Error deleting dict value: {}", e.getMessage());
            }
        }
    }

    public static boolean isEnglishText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("^[a-zA-Z\\s.,;:!?'\"()\\[\\]{}\\-_]+$");
    }

    public static String getPromptTemplate() {
        return "## 任务\n"
                + "评估提供的字符串是否代表有效的中文词汇、短语或句子，仅返回\"是\"或\"否\"。\n\n"
                + "## 输入\n"
                + "%s\n\n"
                + "## 输出格式\n"
                + "仅返回：\n"
                + "- 如果输入是有效的中文词汇、短语或句子，返回\"是\"\n"
                + "- 如果输入不是有效的中文词汇、短语或句子，返回\"否\"\n\n"
                + "## 注意\n"
                + "- 不要提供任何解释或额外信息\n"
                + "- 不要包含分析过程\n"
                + "- 答案必须只包含一个字：\"是\"或\"否\"";
    }

    private static String callLlm(String content, String model) {
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setTemperature(0.8);
        chatCompletionRequest.setMax_tokens(1024);
        chatCompletionRequest.setModel(model);
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(content);
        chatCompletionRequest.setMessages(Lists.newArrayList(message));
        chatCompletionRequest.setStream(false);
        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result;
        String finalContent = null;
        result = completionsService.completions(chatCompletionRequest);
        if (result != null && result.getChoices() != null && !result.getChoices().isEmpty()) {
            finalContent = result.getChoices().get(0).getMessage().getContent();
        }
        return finalContent;
    }


    private static void deleteDict(String srcTableName, Integer id) throws IOException {
        if (id == null) {
            return;
        }
        String idStr = String.valueOf(id);
        String url = BASE_URL + "/admin/deleteFromSubDict";
        Map<String, String> params = new HashMap<>();
        params.put("srcTableName", srcTableName);
        params.put("id", idStr);
        OkHttpUtil.get(url, params);
    }
}
