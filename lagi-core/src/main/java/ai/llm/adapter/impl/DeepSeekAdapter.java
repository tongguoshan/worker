package ai.llm.adapter.impl;

import ai.annotation.LLM;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.utils.qa.ChatCompletionUtil;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@LLM(modelNames = {"deepseek-chat"})
@Slf4j
public class DeepSeekAdapter extends OpenAIStandardAdapter {
    @Override
    public String getApiAddress() {
        if (apiAddress == null) {
            apiAddress = "https://api.deepseek.com/chat/completions";
        }
        return apiAddress;
    }

    @Override
    public ChatCompletionResult completions(ChatCompletionRequest chatCompletionRequest) {
        ChatCompletionResult result = super.completions(chatCompletionRequest);
        log.info("response: {}", ChatCompletionUtil.getFirstAnswer(result));
        return result;
    }
}
