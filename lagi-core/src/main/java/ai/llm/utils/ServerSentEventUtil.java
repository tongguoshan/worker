package ai.llm.utils;

import ai.annotation.LLM;
import ai.common.ModelService;
import ai.common.exception.RRException;
import ai.common.utils.ObservableList;
import ai.openai.pojo.ChatCompletionResult;
import okhttp3.*;
import okhttp3.sse.EventSource;
import okhttp3.sse.EventSourceListener;
import okhttp3.sse.EventSources;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class ServerSentEventUtil {


    private static final Logger log = LoggerFactory.getLogger(ServerSentEventUtil.class);

    public static ObservableList<ChatCompletionResult> streamCompletions(String json, String apiUrl, String apiKey, Function<String, ChatCompletionResult> func, ModelService modelService) {
        return streamCompletions(json, apiUrl, apiKey, new HashMap<>(), func, modelService);
    }
    public static ObservableList<ChatCompletionResult> streamCompletions(String json, String apiUrl, String apiKey, Map<String,String> addHeader , Function<String, ChatCompletionResult> func, ModelService modelService) {
        OkHttpClient client = new OkHttpClient.Builder()
//                .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 7890)))
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        Request.Builder requestBuilder = new Request.Builder();
        if (apiKey != null) {
            requestBuilder.header("Authorization", "Bearer " + apiKey);
        }
        for (Map.Entry<String, String> entry : addHeader.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }
        Request request = requestBuilder.url(apiUrl)
                .header("Accept", "text/event-stream")
                .post(RequestBody.create(json, MediaType.parse("application/json")))
                .build();

        EventSource.Factory factory = EventSources.createFactory(client);
        ObservableList<ChatCompletionResult> result = new ObservableList<>();

        factory.newEventSource(request, new EventSourceListener() {
            @Override
            public void onOpen(@NotNull EventSource eventSource, @NotNull Response response) {
            }

            @Override
            public void onEvent(@NotNull EventSource eventSource, @Nullable String id, @Nullable String type, @NotNull String data) {
                ChatCompletionResult chatCompletionResult = func.apply(data);
                if (chatCompletionResult != null) {
                    result.add(chatCompletionResult);
                }
            }

            @Override
            public void onFailure(@NotNull EventSource eventSource, @Nullable Throwable t, @Nullable Response response) {
                log.error("model request failed response : {}", response);
                LLM annotation = modelService.getClass().getAnnotation(LLM.class);
                 Arrays.stream(annotation.modelNames()).forEach(
                    model->{
                        CacheManager.put(model, Boolean.FALSE);
                    }
                );
                if(t != null) {
                    log.error("model request failed error {}", t.getMessage());
                }
                closeConnection(eventSource);
            }

            @Override
            public void onClosed(@NotNull EventSource eventSource) {
                closeConnection(eventSource);
            }

            private void closeConnection(EventSource eventSource) {
                result.onComplete();
                eventSource.cancel();
                client.dispatcher().executorService().shutdown();
            }
        });
        return result;
    }
}
