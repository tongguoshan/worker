系统首页展示名称，这个设置指定了将在系统首页显示的名称，这里是“Lagi”。

```yaml
system_title: Lagi
```

模型配置

```yaml
# 这部分定义了中间件使用的模型配置。
models:
  # 单驱动模型情况下模型的配置。
  - name: chatgpt  # 后端服务的名称。
    type: OpenAI  # 所属公司，例如这里是OpenAI
    enable: false # 这个标志决定了后端服务是否启用。“true”表示已启用。
    model: gpt-3.5-turbo,gpt-4-turbo # 驱动支持的模型列表
    driver: ai.llm.adapter.impl.GPTAdapter # 模型驱动
    api_key: your-api-key # API密钥
  # 模型支持多驱动的配置
  - name: landing
    type: Landing
    enable: false
    drivers: # 多驱动配置.
      - model: turing,qa,tree,proxy # 驱动支持功能列表
        driver: ai.llm.adapter.impl.LandingAdapter # 驱动地址
      - model: image # 驱动支持功能列表
        driver: ai.image.adapter.impl.LandingImageAdapter # 驱动地址
        oss: landing # 用到的存储对象服务的名称
      - model: landing-tts,landing-asr
        driver: ai.audio.adapter.impl.LandingAudioAdapter 
      - model: video
        driver: ai.video.adapter.impl.LandingVideoAdapter 
        api_key: your-api-key # 驱动指定api_key
    api_key:  your-api-key # 驱动公用的api_key

```

存储功能配置

```yaml
# 这部分定义了中间件用到的存储设备配置。
stores:
  # 这部分是向量数据库的配置
  vectors: # 向量数据库配置列表
    - name: chroma # 向量数据库名称
      driver: ai.vector.impl.ChromaVectorStore # 向量数据库驱动
      default_category: default # 向量数据库存储的分类
      similarity_top_k: 10 # 向量数据库查询时使用的参数
      similarity_cutoff: 0.5
      parent_depth: 1
      child_depth: 1
      url: http://localhost:8000 # 向量数据库的存储配置
  # 这部分是对象存储服务的配置
  oss:
    - name: landing # 存储对象服务的名称
      driver: ai.oss.impl.LandingOSS  # 存储对象服务的驱动类
      bucket_name: lagi # 存储对象的 bucket_name
      enable: true # 是否开启该存储对象服务

    - name: alibaba
      driver: ai.oss.impl.AlibabaOSS
      access_key_id: your-access-key-id # 第三方存储对象的服务用到的 access key id
      access_key_secret: your-access-key-secret # 第三方存储对象的服务用到的 access key secret 
      bucket_name: ai-service-oss
      enable: true

  # 这部分是elasticsearch的配置
  bigdata:
    - name: elasticsearch # 全文检索名称
      driver: ai.bigdata.impl.ElasticsearchAdapter
      host: localhost # 全文检索的elasticsearch地址
      port: 9200 # 全文检索的elasticsearch的端口号
      enable: false # 是否开启
  # 这部分是检索增强生成服务的配置
  rag:
      vector: chroma # 服务用到的向量数据库的名称
      fulltext: elasticsearch # 全文检索（可选填，如填写该配置，则开启该配置，不开启，直接注释即可）
      graph: landing # 图检索（可选填，如填写该配置，则开启该配置，不开启，直接注释即可）
      enable: true # 是否开启
      priority: 10 # 优先级，当该优先级大于模型时,则匹配不到上下文就只返回default中提示语
      default: "Please give prompt more precisely" # 如未匹配到上下文，则返回该提示语
  # 这部分是美杜莎的加速推理服务的配置
  medusa:
    enable: true # 是否开启
    algorithm: hash # 使用的算法
```

中间件功能配置

```yaml
# 大模型使用的功能配置
functions:
  # embedding 服务配置
  embedding:
    - backend: qwen
      type: Qwen
      api_key: your-api-key
  
  # 聊天对话、文本生成功能的配置列表
  chat:
    - backend: chatgpt # 后端使用的模型配置的名称
      model: gpt-4-turbo # 模型名
      enable: true # 是否开启
      stream: true # 是否使用流
      priority: 200 # 优先级

    - backend: chatglm
      model: glm-3-turbo
      enable: false
      stream: false
      priority: 10
  
  # 翻译功能的配置列表
  translate:
    - backend: ernie # 后端使用的模型配置的名称
      model: translate # 模型名
      enable: false # 是否开启
      priority: 10 # 优先级
  
  # 语音转文字功能配置列表
  speech2text:
    - backend: qwen  # 后端使用的模型配置的名称
      model: asr
      enable: true
      priority: 10
  
  # 文字转语音功能配置列表
  text2speech:
    - backend: landing # 后端使用的模型配置的名称
      model: tts
      enable: true
      priority: 10
  
  # 声音克隆功能配置列表
  speech2clone:
    - backend: doubao # 后端使用的模型配置的名称
      model: openspeech
      enable: true
      priority: 10
      others: your-speak-id

  # 文字生成图片功能配置列表
  text2image:
    - backend: spark # 后端使用的模型配置的名称
      model: tti
      enable: true
      priority: 10
    - backend: ernie
      model: Stable-Diffusion-XL
      enable: true
      priority: 5
  # 图片生成文字功能配置列表
  image2text:
    - backend: ernie # 后端使用的模型配置的名称
      model: Fuyu-8B
      enable: true
      priority: 10
  # 图片增强功能配置列表
  image2enhance:
    - backend: ernie # 后端使用的模型配置的名称
      model: enhance
      enable: true
      priority: 10
  # 文本生成视频功能配置列表
  text2video:
    - backend: landing # 后端使用的模型配置的名称
      model: video
      enable: true
      priority: 10
  # 图片生成视频功能配置列表
  image2video:
    - backend: qwen # 后端使用的模型配置的名称
      model: vision
      enable: true
      priority: 10
  # 视频追踪功能配置列表
  video2track:
    - backend: landing # 后端使用的模型配置的名称
      model: video
      enable: true
      priority: 10
  # 视屏增强功能配置列表
  video2enhance:
    - backend: qwen # 后端使用的模型配置的名称
      model: vision
      enable: true
      priority: 10

```

智能体配置

```yaml

# 这部分表示模型支持的智能体配置
agents:
  - name: qq # 智能体的名称
    api_key: your-api-key # 智能体用到的 api key
    driver: ai.agent.social.QQAgent # 智能体驱动

  - name: wechat
    api_key: your-api-key
    driver: ai.agent.social.WechatAgent

  - name: ding
    api_key: your-api-key
    driver: ai.agent.social.DingAgent

# 这部分表示智能体实际作业的配置
workers:
  - name: qq-robot # 作业名称
    agent: qq # 工作的智能体名称
    worker: ai.worker.social.RobotWorker # 作业驱动

  - name: wechat-robot
    agent: wechat
    worker: ai.worker.social.RobotWorker

  - name: ding-robot
    agent: ding
    worker: ai.worker.social.RobotWorker

```



