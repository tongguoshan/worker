System homepage display name, this setting specifies the name that will be displayed on the system homepage, which is "Lagi".

```yaml
system_title: Lagi
```

Model configuration

```yaml
# This section defines the model configuration used by the middleware.
models:
  # Model configuration in the case of single driver model.
  - name: chatgpt  # The name of the backend service.
    type: OpenAI  # Type is the company, in this case OpenAI.
    enable: false # This flag determines whether the backend service is enabled or not. true: indicates that the function is enabled.
    model: gpt-3.5-turbo,gpt-4-turbo # list of models supported by the driver
    driver: ai.llm.adapter.impl.GPTAdapter # model driver
    api_key: your-api-key # API key
  # The model supports multi-driver configuration
  - name: landing
    type: Landing
    enable: false
    drivers: # Multi-driver configuration.
      - model: turing,qa,tree,proxy # List of driver support functions
        driver: ai.llm.adapter.impl.LandingAdapter # driver address
      - model: image # List of driver supported features
        driver: ai.image.adapter.impl.LandingImageAdapter # driver address
        oss: landing # Name of the storage object service
      - model: landing-tts,landing-asr
        driver: ai.audio.adapter.impl.LandingAudioAdapter
      - model: video
        driver: ai.video.adapter.impl.LandingVideoAdapter
        api_key: your-api-key # specifies api key for the driver
    api_key:  your-api-key  # the public api key of drivers

```

Storage function configuration

```yaml
# This section defines the storage device configuration used by the middleware.
stores:
  # This part is the configuration of the vector database
  vectors: # Vector database configuration list
    - name: chroma # Vector database name
      driver: ai.vector.impl.ChromaVectorStore # Vector database driver
      default_category: default # Parameters used in vector database queries
      similarity_top_k: 10
      similarity_cutoff: 0.5
      parent_depth: 1
      child_depth: 1
      url: http://localhost:8000 # the url address of the Vector database
  # This section describes how to configure the object storage service
  oss:
    - name: landing # Name of the storage object service
      driver: ai.oss.impl.LandingOSS  # The driver class that stores the object service
      bucket_name: lagi # The bucket name used to store the object
      enable: true # Whether to enable the storage object service

    - name: alibaba
      driver: ai.oss.impl.AlibabaOSS
      access_key_id: your-access-key-id #  the access key id  used by the third-party storage object service
      access_key_secret: your-access-key-secret # the access key secret  used by the third-party storage object service
      bucket_name: ai-service-oss
      enable: true

  # This part is the configuration of Elasticsearch
  bigdata:
    - name: elasticsearch # The name of the full-text search
      driver: ai.bigdata.impl.ElasticsearchAdapter
      host: localhost # IP address
      port: 9200 # Port number
      enable: false # Whether it is turned on
  # This section is the configuration of the retrieval enhancement build service
  rag:
    vector: chroma # The name of the vector database used by the service
    fulltext: elasticsearch # Full-text search (optional, if this configuration is filled in, this configuration will be enabled)
    graph: landing # Graph retrieval (optional, if this configuration is filled in, this configuration will be enabled)
    enable: true # Enable or not
    priority: 10 # Priority for this function，When the priority is greater than the model, the prompt in default is returned if the context is not found
    default: "Please give prompt more precisely" # If the context is not found, the cue is returned
  # This section is the configuration of Medusa's Accelerated Inference Service
  medusa:
    enable: true # Enable or not
    algorithm: hash # Algorithm used
```

Middleware function configuration

```yaml
# Functional configuration used by large models
functions:
  # embedding service configuration
  embedding:
    - backend: qwen
      type: Qwen
      api_key: your-api-key
  
  # The configuration list of chat dialog and text generation functions
  chat:
    - backend: chatgpt # The name of the model configuration used by the backend
      model: gpt-4-turbo # Model name
      enable: true # Enable or not
      stream: true # Whether to use stream
      priority: 200 #  priority for this function

    - backend: chatglm # The name of the model configuration used by the backend
      model: glm-3-turbo
      enable: false
      stream: false
      priority: 10
  
  # Translation configuration list
  translate:
    - backend: ernie # The name of the model configuration used by the backend
      model: translate
      enable: false
      priority: 10
  
  # Voice to text configuration list
  speech2text:
    - backend: qwen # The name of the model configuration used by the backend
      model: asr
      enable: true
      priority: 10
  
  # Text-to-voice configuration list
  text2speech:
    - backend: landing # The name of the model configuration used by the backend
      model: tts
      enable: true
      priority: 10
  
  # Sound clone configuration list
  speech2clone:
    - backend: doubao # The name of the model configuration used by the backend
      model: openspeech
      enable: true
      priority: 10
      others: your-speak-id

  # Text image configuration list
  text2image:
    - backend: spark # The name of the model configuration used by the backend
      model: tti
      enable: true
      priority: 10
    - backend: ernie
      model: Stable-Diffusion-XL
      enable: true
      priority: 5
  # Configuration list of the image text generation
  image2text:
    - backend: ernie # The name of the model configuration used by the backend
      model: Fuyu-8B
      enable: true
      priority: 10
  # Image enhancement configuration list
  image2enhance:
    - backend: ernie # The name of the model configuration used by the backend
      model: enhance
      enable: true
      priority: 10
  # Text generated video configuration list
  text2video:
    - backend: landing # The name of the model configuration used by the backend
      model: video
      enable: true
      priority: 10
  # image generated video configuration list
  image2video:
    - backend: qwen # The name of the model configuration used by the backend
      model: vision
      enable: true
      priority: 10
  # 视频追踪功能配置列表
  video2track:
    - backend: landing # The name of the model configuration used by the backend
      model: video
      enable: true
      priority: 10
  # 视屏增强功能配置列表
  video2enhance:
    - backend: qwen # The name of the model configuration used by the backend
      model: vision
      enable: true
      priority: 10

```

Agent configuration

```yaml

# This section represents the agent configuration supported by the model
agents:
  - name: qq # Name of the agent
    api_key: your-api-key # api key used by the agent
    driver: ai.agent.social.QQAgent # Agent driver

  - name: wechat
    api_key: your-api-key
    driver: ai.agent.social.WechatAgent

  - name: ding
    api_key: your-api-key
    driver: ai.agent.social.DingAgent

# This section represents the workers configuration supported by the model
workers:
  - name: qq-robot # worker name
    agent: qq # Name of the agent
    worker: ai.worker.social.RobotWorker # Worker driver

  - name: wechat-robot
    agent: wechat
    worker: ai.worker.social.RobotWorker

  - name: ding-robot
    agent: ding
    worker: ai.worker.social.RobotWorker

```
