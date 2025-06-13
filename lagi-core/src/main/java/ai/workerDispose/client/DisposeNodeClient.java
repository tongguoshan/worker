package ai.workerDispose.client;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.utils.OkHttpUtil;
import ai.workerDispose.dao.NodeDao;
import ai.workerDispose.pojo.UniNode;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DisposeNodeClient {
    private static final Logger logger = LoggerFactory.getLogger(DisposeNodeClient.class);

    private static final int START_INDEX = 0;
    private static final int END_INDEX = 70000;
    private static final int PAGE_SIZE = 10000;
    private static final int THREAD_POOL_SIZE = 10;
    private static final NodeDao nodeDao;
    private static final String BASE_URL = "http://127.0.0.1:8090";
//    private static final String BASE_URL = "https://kgraph.landingbj.com";

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(THREAD_POOL_SIZE * 1000)
    );

    private static final String NODE_WEIGHT_PROGRESS_FILE = System.getProperty("user.home") + "/node_dispose_progress.txt";
    private static final Set<UniNode> UniNodeSet;

    static {
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ContextLoader.loadContext();
        UniNodeSet = loadNodeFromFile();
        nodeDao = new NodeDao();
    }

    public static void main(String[] args) {
        nodeWeightDisposeFromDb();
        logger.info("Processing completed.");
    }

    private static void nodeWeightDisposeFromDb() {
        int offset = START_INDEX;
        while (offset < END_INDEX) {
            int limit = Math.min(PAGE_SIZE, END_INDEX - offset + 1);
            List<UniNode> nodeList = nodeDao.getNodeNotInZIndex(offset, limit);
            for (UniNode UniNode : nodeList) {
                executor.submit(() -> asyncNodeDispose(UniNode));
            }
            logger.info("Processed offset: {}", offset);
            offset += limit;
        }
    }

    private static void asyncNodeDispose(UniNode uniNode) {
        if (UniNodeSet.contains(uniNode)) {
            logger.info("Node value already processed: {}", uniNode);
            return;
        }
        if (uniNode.getComments().length() == 1 || isEnglishText(uniNode.getComments())) {
            return;
        }
        logger.info("node {}", uniNode);
        String prompt = String.format(getPromptTemplate(), uniNode.getComments());
        String model1 = "ERNIE-Speed-128K";
        String model2 = "deepseek-chat";
        String content1 = callLlm(prompt, model1);
        String content2 = callLlm(prompt, model2);
//        String content1 = "否";
//        String content2 = "否";
        if (content1 == null || content2 == null) {
            return;
        }
        boolean deleteFlag = false;
        if (content1.contains("否") && content2.contains("否")) {
//            try {
//                deleteNode(uniNode.getNid());
//            } catch (IOException e) {
//                logger.error("Error deleting dict value: {}", e.getMessage());
//                return;
//            }
            deleteFlag = true;
            logger.info("Delete dict value: {}", uniNode.getComments());
        }
        writeNodeToFile(Collections.singletonList(uniNode), deleteFlag);
    }

    private static boolean isEnglishText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        return text.matches("^[a-zA-Z0-9\\s.,;:!?'\"()\\[\\]{}\\-_]+$");
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


    private static synchronized void deleteNode(Integer id) throws IOException {
        if (id == null) {
            return;
        }
        String idStr = String.valueOf(id);
        String url = BASE_URL + "/admin/deleteVertices";
        Map<String, String> params = new HashMap<>();
        params.put("id", idStr);
        OkHttpUtil.get(url, params);
    }

    private static Set<UniNode> loadNodeFromFile() {
        Set<UniNode> nodeSet = new HashSet<>();
        File file = new File(NODE_WEIGHT_PROGRESS_FILE);
        if (!file.exists()) {
            return nodeSet;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    continue;
                }
                UniNode UniNode = new UniNode(Integer.parseInt(parts[0]), parts[1]);
                nodeSet.add(UniNode);
            }
        } catch (IOException e) {
            logger.error("Error reading node progress file: {}", e.getMessage());
        }
        return nodeSet;
    }

    private static synchronized void writeNodeToFile(List<UniNode> nodeList, boolean deleteFlag) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(NODE_WEIGHT_PROGRESS_FILE, true))) {
            for (UniNode uniNode : nodeList) {
                writer.write(uniNode.getNid() + "," + uniNode.getComments() + (deleteFlag ? ",deleted" : ",remained"));
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("Error writing node progress file: {}", e.getMessage());
        }
    }
}
