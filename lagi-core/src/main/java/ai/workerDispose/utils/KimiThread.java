package ai.workerDispose.utils;

import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import com.google.common.collect.Lists;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class KimiThread {
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private long lastExecutionTime = 0;

    public String execute(String content) {
        String retry = null;
        lock.lock();
        try {
            long currentTime = System.currentTimeMillis();
            // 检查上次执行时间
            if (currentTime - lastExecutionTime < 3000) {
                // 等待直到过去了一秒
                condition.await(3000 - (currentTime - lastExecutionTime), TimeUnit.MILLISECONDS);
            }
            // 更新上次执行时间
            lastExecutionTime = System.currentTimeMillis();
            // 执行你的逻辑
            //System.out.println("Method executed at: " + lastExecutionTime);
            retry = chat(content);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // 恢复中断状态
        } finally {
            lock.unlock();
        }
        return retry;
    }

    /**
     * kimi
     * @param content
     * @return
     */
    public static synchronized String chat(String content) {
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
        //chatCompletionRequest.setModel("v3.5");
        //chatCompletionRequest.setModel("v1.1");
        //chatCompletionRequest.setModel("glm-3-turbo");
        //chatCompletionRequest.setModel("moonshot-v1-8k");
        //chatCompletionRequest.setModel("ERNIE-Speed-128K");
        //chatCompletionRequest.setModel("glm-4");
        //chatCompletionRequest.setModel("ERNIE-Speed-8K");
        //chatCompletionRequest.setModel("glm-3-turbo");
        chatCompletionRequest.setModel("qwen-turbo");
        //chatCompletionRequest.setModel("qwen-plus");
        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = null;
        boolean isf = true;
        String retry = null;
        while (isf){
            try {
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                isf = false;
            }catch (Exception e){
                try {
                    Thread.sleep(3000);
                    System.out.println("访问出错了");
                }catch (Exception e1){
                    System.out.println("等待都失败了！");
                    int i = 1/0;
                }
                System.out.println("访问出错了");
            }
        }

        return retry;
    }

}
