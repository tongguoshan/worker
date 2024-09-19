package ai.llm.utils;

import ai.common.pojo.IndexSearchData;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.utils.LagiGlobal;
import ai.vector.VectorStoreService;
import com.google.gson.Gson;

import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class CompletionUtil {
    private static final Gson gson = new Gson();
    private static final VectorStoreService vectorStoreService = new VectorStoreService();


    private static final int MAX_INPUT = 4096;
//    private static final int MAX_INPUT = 1024;

    public static ChatCompletionResult getDummyCompletion() {
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        String currentDatetime = dateFormat.format(new Date());
        String message = currentDatetime + " " + UUID.randomUUID();
        String json = "{\"created\":1719495617,\"choices\":[{\"index\":0,\"message\":{\"role\":\"assistant\"," +
                "\"content\":\"" + message + "\"},\"finish_reason\":\"stop\"}],\"usage\":{\"prompt_tokens\":38," +
                "\"completion_tokens\":8,\"total_tokens\":46}}\n";
        return gson.fromJson(json, ChatCompletionResult.class);
    }

    public static void populateContext(ChatCompletionResult result, List<IndexSearchData> indexSearchDataList, String context) {
        if (result != null && !result.getChoices().isEmpty()
                && indexSearchDataList != null && !indexSearchDataList.isEmpty()) {
            IndexSearchData indexData = indexSearchDataList.get(0);
            List<String> imageList = vectorStoreService.getImageFiles(indexData);
            for (int i = 0; i < result.getChoices().size(); i++) {
                ChatMessage message = result.getChoices().get(i).getMessage();
                message.setContext(context);
                if (!(indexData.getFilename() != null && indexData.getFilename().size() == 1
                        && indexData.getFilename().get(0).isEmpty())) {
                    message.setFilename(indexData.getFilename());
                }
                message.setFilepath(indexData.getFilepath());
                message.setImageList(imageList);
            }
        }
    }

    public static String truncate(String context) {
        return truncate(context, MAX_INPUT);
    }

    public static String truncate(String context, int maxLength) {
        if(context == null) {
            return "";
        }
        if(context.length() <= maxLength) {
            return context;
        }
        return context.substring(0, maxLength);
    }

    public static List<ChatMessage> truncateChatMessages(List<ChatMessage> chatMessages) {
        return truncateChatMessages(chatMessages, MAX_INPUT);
    }

    public static List<ChatMessage> truncateChatMessages(List<ChatMessage> chatMessages, int maxLength) {
        if(chatMessages != null && !chatMessages.isEmpty()) {
            ChatMessage systemChatMessage = null;
            if (chatMessages.get(0).getRole().equals(LagiGlobal.LLM_ROLE_SYSTEM)) {
                systemChatMessage = chatMessages.get(0);
            }
            ChatMessage lastQuestion = chatMessages.get(chatMessages.size() - 1);
            int userMaxLength = maxLength;
            if (systemChatMessage != null) {
                userMaxLength = maxLength - systemChatMessage.getContent().length();
            }
            lastQuestion.setContent(truncate(lastQuestion.getContent(), userMaxLength));
            int length = lastQuestion.getContent().length();
            int lastIndex = chatMessages.size() - 1;
            for(int i = chatMessages.size() - 2; i >= 0; i--) {
                ChatMessage chatMessage = chatMessages.get(i);
                length += chatMessage.getContent().length();
                if(length > userMaxLength) {
                    break;
                }
                if(chatMessage.getRole().equals(LagiGlobal.LLM_ROLE_USER)) {
                    lastIndex = i;
                }
            }
            chatMessages = chatMessages.subList(lastIndex, chatMessages.size());
            if (systemChatMessage != null) {
                chatMessages.add(0, systemChatMessage);
            }
        }
        return chatMessages;
    }


    public static void main(String[] args) {
        ChatCompletionResult result = getDummyCompletion();
        System.out.println(result.getChoices().get(0).getMessage().getContent());
    }
}
