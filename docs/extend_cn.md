# 自由扩展

您可以根据您的需求参考此文档，对Lag[i] (联基) 进行自由扩展，从而契合您的业务需求。

## 模型扩展

如果您想扩展Lag[i] (联基) 适配其它大模型，您可以参考以下步骤：

### 1. 适配yml

**1.在models下中新增配置**

```yaml
models:
    - name: your-model-name
      type: your-model-type
      enable: true # 这个标志决定了后端服务是否启用。“true”表示启用。
      drivers: # 该模型需要多个models时加上drviers进行扩展。
        - model: model-version
          driver: ai.llm.adapter.impl.Yonr-Adapter #对应的实现类。
          oss: oos-name # 对应的OSS配置名称。
      api_key: your-api-key 
      secret_key: your-secret-key 
      access_key_secret: your-access-key-secret
      api_address: your-aip-address # 如为私有化部署的大模型，需要指定模型服务的Api地址。

functions:
   chat:
    - backend: your-model-name
      model: model-version
      enable: true
      stream: true
      priority: 0

```

示列：

```yaml
models:
  backends:
    - name: ernie
      type: Baidu
      enable: true
      drivers:
        - model: ERNIE-Speed-128K,ERNIE-Bot-turbo,ERNIE-4.0-8K,ERNIE-3.5-8K-0205,ERNIE-3.5-4K-0205, ERNIE-3.5-8K-1222
          driver: ai.llm.adapter.impl.ErnieAdapter
        - model: Fuyu-8B,Stable-Diffusion-XL
          driver: ai.image.adapter.impl.BaiduImageAdapter
        - model: translate
          driver: ai.translate.adapter.impl.BaiduTranslateAdapter
          app_id: your-api-key
          security_key: your-security-key
        - model: enhance
          driver: ai.image.adapter.impl.BaiduAiImageAdapter
          api_key: your-api-key
          secret_key: 9your-sercet-key
        - model: aiVideo
          driver: ai.video.adapter.impl.BaiduVideoAdapter
          api_key: your-api-key
          secret_key: your-sercet-key
      app_id: your-app-id
      api_key: your-api-key
      secret_key: your-sercet-key
      
functions:
  chat:
    - backend: ernie
      model: ERNIE-Speed-128K
      enable: true
      stream: true
      priority: 10

```

### 2.适配方法

>1.扩展chat:   

在lagi-core模块下的ai>llm>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现ILlmAdapter接口。

```java
@LLM(modelName = { "Yonr-version"})
public class YonrAdapter extends ModelService implements ILlmAdapter {
    /**
     * API调用
     * @param chatCompletionRequest
     * @return
     */
    @Override
    public ChatCompletionResult completions(ChatCompletionRequest chatCompletionRequest) {
        return null;
    }

    /**
     * 流式调用
     * @param chatCompletionRequest
     * @return
     */
    @Override
    public ChatCompletionResult streamCompletions(ChatCompletionRequest chatCompletionRequest) {
        return null;
    }
}

```

示列：

```java
@LLM(modelNames = {"your_model1,your_model2"})
public class DoubaoAdapter extends ModelService implements ILlmAdapter {
    @Override
    public ChatCompletionResult completions(ChatCompletionRequest request) {
        ArkService service = ArkService.builder().apiKey(apiKey).baseUrl("https://ark.cn-beijing.volces.com/api/v3/").build();
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content("你是豆包人工智能助手").build();
        ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(request.getMessages().get(0).getContent()).build();
        messages.add(systemMessage);
        messages.add(userMessage);

        com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest chatCompletionRequest = com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest.builder()
                .model(getModel())
                .messages(messages)
                .build();
        com.volcengine.ark.runtime.model.completion.chat.ChatCompletionResult chatCompletion = service.createChatCompletion(chatCompletionRequest);

        ChatCompletionResult result = new ChatCompletionResult();
        BeanUtil.copyProperties(chatCompletion, result);
        return result;
    }

    @Override
    public Observable<ChatCompletionResult> streamCompletions(ChatCompletionRequest request) {
        ArkService service = ArkService.builder().apiKey(apiKey).baseUrl("https://ark.cn-beijing.volces.com/api/v3/").build();
        List<ChatMessage> messages = new ArrayList<>();
        ChatMessage systemMessage = ChatMessage.builder().role(ChatMessageRole.SYSTEM).content("你是豆包人工智能助手").build();
        ChatMessage userMessage = ChatMessage.builder().role(ChatMessageRole.USER).content(request.getMessages().get(0).getContent()).build();
        messages.add(systemMessage);
        messages.add(userMessage);

        com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest streamChatCompletionRequest = com.volcengine.ark.runtime.model.completion.chat.ChatCompletionRequest.builder()
                .model(getModel())
                .messages(messages)
                .build();

        Flowable<ChatCompletionChunk> flowable = service.streamChatCompletion(streamChatCompletionRequest);
        Observable<ChatCompletionResult> iterable = Observable.create(observableEmitter -> {
            try {
                flowable.blockingForEach(chatCompletionChunk -> {
                    System.out.println(JSONUtil.toJsonStr(chatCompletionChunk));
                    if (chatCompletionChunk.getChoices().size() > 0) {
                        observableEmitter.onNext(convertResponse(chatCompletionChunk));
                        if (chatCompletionChunk.getChoices().get(0).getMessage().getContent().equals("")) {
                            service.shutdownExecutor();
                        }
                    }
                });
            } catch (Exception e) {
                System.out.println(e);
            }
            observableEmitter.onComplete();
        });
        return iterable;
    }
}
```

>2.扩展speech2text:

在lagi-core模块下的ai>audio>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现IAudioAdapter接口。

```java
@ASR(company = "your-company-name", modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements IAudioAdapter {
    @Override
    public String asr(String audioFilePath) {
        return null;
    }
 }
```

示列：

```java
@ASR(company = "alibaba", modelNames = "asr")
public class YonrAdapter extends ModelService implements IAudioAdapter {
    @Override
    public String asr(String audioFilePath) {
        AlibabaAsrService asrService = new AlibabaAsrService(
                getAppKey(),
                getAccessKeyId(),
                getAccessKeySecret()
        );
        return gson.fromJson(asrService.asr(audio),AsrResult .class);
}
```

>3.扩展text2speech:

在lagi-core模块下的ai>audio>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现IAudioAdapter接口。

```java
@TTS(company = "your-company-name", modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements IAudioAdapter {
    @Override
    public String tts(String audioFilePath) {
        return null;
    }
 }
```

示列：

```java
@TTS(company = "alibaba", modelNames = "tts")
public class YonrAdapter extends ModelService implements IAudioAdapter {
    @Override
    public String tts(String audioFilePath) {
        AlibabaTtsService ttsService = new AlibabaTtsService(
                getAppKey(),
                getAccessKeyId(),
                getAccessKeySecret()
        );
        param.setSample_rate(16000);
        param.setFormat("wav");
        Request request = ttsService.getRequest(param);
        TTSResult result = new TTSResult();
        try {
            OkHttpClient client = new OkHttpClient();
            okhttp3.Response response = client.newCall(request).execute();
            String contentType = response.header("Content-Type");
            if ("audio/mpeg".equals(contentType)) {
                String tempDir = System.getProperty("java.io.tmpdir");
                String tempFile = tempDir + FileUploadUtil.generateRandomFileName("wav");
                File audio = new File(tempFile);
                FileOutputStream fout = new FileOutputStream(audio);
                fout.write(response.body().bytes());
                fout.close();
                String url = universalOSS.upload("tts/" + audio.getName(), audio);
                audio.delete();
                result.setStatus(LagiGlobal.TTS_STATUS_SUCCESS);
                result.setResult(url);
            } else {
                String errorMessage = response.body().string();
                result = gson.fromJson(errorMessage, TTSResult.class);
            }
            response.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return result;
    }
 }
```

>4.扩展text2image:

在lagi-core模块下的ai>image>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现IImageGenerationAdapter接口。

```java
@ImgGen(modelNames = "Yonr-modelNames")
public class SparkImageAdapter extends ModelService implements IImageGenerationAdapter {
    @Override
    public ImageGenerationResult generations(ImageGenerationRequest request) {
        return null;
    }
}
```

示列：

```java
@ImgGen(modelNames = "tti")
public class SparkImageAdapter extends ModelService implements IImageGenerationAdapter {
    @Override
    public ImageGenerationResult generations(ImageGenerationRequest request) {
        try {
            String authUrl = getAuthUrl(apiUrl, apiKey, secretKey);
            SparkGenImgRequest sparkGenImgRequest = convert2SparkGenImageRequest(request);
            String post = doPostJson(authUrl, null, JSONUtil.toJsonStr(sparkGenImgRequest));
            SparkGenImgResponse bean = JSONUtil.toBean(post, SparkGenImgResponse.class);
            return  convert2ImageGenerationResult(bean);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return null;
    }
}
```

>5.扩展image2text:

在lagi-core模块下的ai>image>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现IImage2TextAdapter接口。

```java
@Img2Text(modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements IImage2TextAdapter {
    @Override
    public ImageToTextResponse toText(FileRequest param) {
        return null;
    }
}
```

示列：

```java
@Img2Text(modelNames = "Fuyu-8B")
public class YonrAdapter extends ModelService implements IImage2TextAdapter {
    @Override
    public ImageToTextResponse toText(FileRequest param) {
        try {
            Image2TextRequest image2TextRequest = convertImage2TextRequest(param);
            Image2TextResponse image2TextResponse = buildQianfan().image2Text(image2TextRequest);
            return ImageToTextResponse.success(image2TextResponse.getResult());
        } catch (Exception e) {
            logger.error("error", e);
        }
        return ImageToTextResponse.error();
    }
}
```

>6.扩展image2enhance:

在lagi-core模块下的ai>image>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现ImageEnhanceAdapter接口。

```java
@ImgEnhance(modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements ImageEnhanceAdapter {
    @Override
    public ImageEnhanceResult enhance(ImageEnhanceRequest imageEnhanceRequest) {
        return null;
    }
}
```

示列：

```java
@ImgEnhance(modelNames = "enhance")
public class YonrAdapter extends ModelService implements ImageEnhanceAdapter {
    @Override
    public ImageEnhanceResult enhance(ImageEnhanceRequest imageEnhanceRequest) {
        String image = null;
        String url = "https://aip.baidubce.com/rest/2.0/image-process/v1/image_definition_enhance";
        try {
            byte[] imgData = ImageUtil.getFileStream(imageEnhanceRequest.getImageUrl());
            String imgStr = Base64Util.encode(imgData);
            String imgParam = URLEncoder.encode(imgStr, "UTF-8");
            String param = "image=" + imgParam;
            String accessToken = getAccessToken();
            String result = BaiduHttpUtil.post(url, accessToken, param);
            image = JSONUtil.parse(result).getByPath("image", String.class);
        } catch (Exception e) {
            log.error("error", e);
        }
        return ImageEnhanceResult.builder().type("base64").data(image).build();
    }
}
```

>7.扩展text2video:

在lagi-core模块下的ai>video>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现Text2VideoAdapter接口。

```java
@Text2Video(modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements Text2VideoAdapter {
    @Override
    public VideoJobResponse toVideo(ImageGenerationRequest request) {
        return null;
    }
}
```

示列：

```java
@Text2Video(modelNames = "video")
public class YonrAdapter extends ModelService implements Text2VideoAdapter {
    @Override
    public VideoJobResponse toVideo(ImageGenerationRequest request) {
        ImageGenerationResult generations = generations(request);
        if(generations != null) {
            String url = generations.getData().get(0).getUrl();
            return VideoJobResponse.builder().data(url).build();
        }
        return null;
    }
}
```

>8.扩展image2video:

在lagi-core模块下的ai>video>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现Image2VideoAdapter接口。

```java
@Img2Video(modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements Image2VideoAdapter {
    @Override
    public VideoJobResponse image2Video(VideoGeneratorRequest videoGeneratorRequest) {
        return null;
    }
}
```

示列：

```java
@Img2Video(modelNames = "video")
public class YonrAdapter extends ModelService implements Image2VideoAdapter {
    @Override
    public VideoJobResponse image2Video(VideoGeneratorRequest videoGeneratorRequest) {
        Client client = createClient();
        GenerateVideoRequest generateVideoRequest = convert2GenerateVideoRequest(videoGeneratorRequest);
        try {
            GenerateVideoResponse generateVideoResponse = client.generateVideo(generateVideoRequest);
            String requestId = generateVideoResponse.getBody().getRequestId();
            if(requestId != null) {
                return wait2Result(requestId);
            }
            return VideoJobResponse.builder().jobId(generateVideoResponse.getBody().getRequestId()).build();
        } catch (TeaException error) {
            log.error(error.getMessage());
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            log.error(error.getMessage());
        }
        return null;
    }
}
```

>9.扩展video2track:

在lagi-core模块下的ai>video>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现Video2trackAdapter接口。

```java
@VideoTrack(modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements Video2trackAdapter {
    @Override
    public VideoJobResponse track(String videoUrl) {
        return null;
    }
}
```

示列：

```java
@VideoTrack(modelNames = "video")
public class YonrAdapter extends ModelService implements Video2trackAdapter {
    @Override
    public VideoJobResponse track(String videoUrl) {
        File file = new File(videoUrl);
        String url = universalOSS.upload("mmtracking/" + file.getName(), file);
        MotInferenceRequest request = new MotInferenceRequest();
        request.setVideoUrl(url);
        Object[] params = { gson.toJson(request) };
        String[] result = call.callWS(AiServiceInfo.WSVdoUrl, "motInference", params);
        Response response = gson.fromJson(result[0], Response.class);
        if(response != null) {
            return VideoJobResponse.builder().data(response.getData()).build();
        }
        return null;
    }
}
```

>10.扩展video2enhance:

在lagi-core模块下的ai>video>adapter>impl目录下新建YonrAdapter.java继承ModelService类并实现Video2EnhanceAdapter接口。

```java
@VideoEnhance(modelNames = "Yonr-modelNames")
public class YonrAdapter extends ModelService implements Video2EnhanceAdapter {
    @Override
    public VideoJobResponse enhance(VideoEnhanceRequest videoEnhanceRequest) {
        return null;
    }
}
```

示列：

```java
@VideoEnhance(modelNames = "vision")
public class YonrAdapter extends ModelService implements Video2EnhanceAdapter {
    @Override
    public VideoJobResponse enhance(VideoEnhanceRequest videoEnhanceRequest) {
        Client client = createClient();
        EnhanceVideoQualityRequest enhanceVideoQualityRequest =convert2EnhanceVideoQualityRequest(videoEnhanceRequest);
        try {
            EnhanceVideoQualityResponse enhanceVideoQualityResponse = client.enhanceVideoQuality(enhanceVideoQualityRequest);
            return wait2Result(enhanceVideoQualityResponse.getBody().getRequestId());
        } catch (TeaException error) {
            log.error(error.getMessage());
        } catch (Exception _error) {
            TeaException error = new TeaException(_error.getMessage(), _error);
            log.error(error.getMessage());
        }
        return null;
    }
}
```

## 数据库扩展

如果您想将 Lag[i]（联基）扩展适配到其他向量数据库，可以按照以下步骤操作：

### 1. 适配yml

**1.在stores下中新增配置**

```yaml
stores:
  vectors:
    - name: vector_name
      driver: ai.vector.impl.Yonr_VectorStore 
      default_category: default
      similarity_top_k: 10
      similarity_cutoff: 0.5
      parent_depth: 1
      child_depth: 1
      url: yonr_url

  rag:
    vector: vector_name
    fulltext: elasticsearch
    graph: landing
    enable: true
    priority: 10
    default: "Please give prompt more precisely"

```

示列：

```yaml
stores:
  vectors:
    - name: chroma
      driver: ai.vector.impl.ChromaVectorStore
      default_category: chaoyang
      similarity_top_k: 10
      similarity_cutoff: 0.5
      parent_depth: 1
      child_depth: 1
      url: http://127.0.0.1:8000

  rag:
    vector: chroma
    fulltext: elasticsearch
    graph: landing
    enable: true
    priority: 10
    default: "Please give prompt more precisely"

```

### 2.适配方法

在lagi-core模块下的ai>vector>impl目录下新建YonrVectorStore.java继承BaseVectorStore类并重写VectorStore的所有方法即可。

> 分别重写以下方法

```java

void upsert(List<UpsertRecord> upsertRecords);

void upsert(List<UpsertRecord> upsertRecords, String category);

List<IndexRecord> query(QueryCondition queryCondition);

List<IndexRecord> query(QueryCondition queryCondition, String category);

List<IndexRecord> fetch(List<String> ids);

List<IndexRecord> fetch(List<String> ids, String category);

void delete(List<String> ids);

void delete(List<String> ids, String category);

void deleteWhere(List<Map<String, String>> where);

void deleteWhere(List<Map<String, String>> whereList, String category);

void deleteCollection(String category);

```

示列：

1.重写插入或更新数据方法: upsert();

```java
 public void upsert(List<UpsertRecord> upsertRecords) {
        upsert(upsertRecords, this.config.getDefaultCategory());
    }

    public void upsert(List<UpsertRecord> upsertRecords, String category) {
        List<String> documents = new ArrayList<>();
        List<Map<String, String>> metadatas = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (UpsertRecord upsertRecord : upsertRecords) {
            documents.add(upsertRecord.getDocument());
            metadatas.add(upsertRecord.getMetadata());
            ids.add(upsertRecord.getId());
        }
        List<List<Float>> embeddings = this.embeddingFunction.createEmbedding(documents);
        Collection collection = getCollection(category);
        try {
            collection.upsert(embeddings, metadatas, documents, ids);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
```

2.重写查询方法：query();

```java
public List<IndexRecord> query(QueryCondition queryCondition) {
        return query(queryCondition, this.config.getDefaultCategory());
    }

    public List<IndexRecord> query(QueryCondition queryCondition, String category) {
        List<IndexRecord> result = new ArrayList<>();
        Collection collection = getCollection(category);
        Collection.GetResult gr;
        if (queryCondition.getText() == null) {
            try {
                gr = collection.get(null, queryCondition.getWhere(), null);
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
            return getIndexRecords(result, gr);
        }
        List<String> queryTexts = Collections.singletonList(queryCondition.getText());
        Integer n = queryCondition.getN();
        Map<String, String> where = queryCondition.getWhere();
        Collection.QueryResponse qr = null;
        try {
            qr = collection.query(queryTexts, n, where, null, null);
        } catch (ApiException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < qr.getDocuments().size(); i++) {
            for (int j = 0; j < qr.getDocuments().get(i).size(); j++) {
                IndexRecord indexRecord = IndexRecord.newBuilder()
                        .withDocument(qr.getDocuments().get(i).get(j))
                        .withId(qr.getIds().get(i).get(j))
                        .withMetadata(qr.getMetadatas().get(i).get(j))
                        .withDistance(qr.getDistances().get(i).get(j))
                        .build();
                result.add(indexRecord);
            }
        }
        return result;
    }

```

3.重写获取方法：fetch();

```java
    public List<IndexRecord> fetch(List<String> ids) {
        return fetch(ids, this.config.getDefaultCategory());
    }

    public List<IndexRecord> fetch(List<String> ids, String category) {
        List<IndexRecord> result = new ArrayList<>();
        Collection.GetResult gr;
        Collection collection = getCollection(category);
        try {
            gr = collection.get(ids, null, null);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
        return getIndexRecords(result, gr);
    }
``` 

4.重写删除方法：delete();

```java
    public void delete(List<String> ids) {
        this.delete(ids, this.config.getDefaultCategory());
    }

    public void delete(List<String> ids, String category) {
        Collection collection = getCollection(category);
        try {
            collection.deleteWithIds(ids);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
```

5.重写条件删除数据方法：deleteWhere();

```java
    public void deleteWhere(List<Map<String, String>> whereList) {
        deleteWhere(whereList, this.config.getDefaultCategory());
    }
    
    public void deleteWhere(List<Map<String, String>> whereList, String category) {
        Collection collection = getCollection(category);
        try {
            for (Map<String, String> where : whereList) {
                collection.deleteWhere(where);
            }
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
```


6.重写删除集合方法：deleteCollection();

```java
    public void deleteCollection(String category) {
        try {
            client.deleteCollection(category);
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
```