package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.embedding.Embeddings;
import ai.embedding.impl.QwenEmbeddings;
import ai.llm.service.CompletionsService;
import ai.medusa.pojo.InstructionData;
import ai.medusa.pojo.InstructionPairRequest;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.vector.VectorCacheLoader;
import ai.vector.VectorStoreService;
import ai.vector.impl.ChromaVectorStore;
import ai.vector.pojo.IndexRecord;
import ai.vector.pojo.QueryCondition;
import ai.vector.pojo.UpsertRecord;
import ai.workerDispose.dao.AiZindexUserDao;
import ai.workerDispose.pojo.IndexDictValues;
import ai.workerDispose.pojo.Node;
import ai.workerDispose.pojo.NodeValue;
import ai.workerDispose.pojo.WeightObj;
import ai.workerDispose.utils.KimiThread;
import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DictionaryProcessing {

    static {
        ContextLoader.loadContext();
    }
    private static  AiZindexUserDao aiZindexUserDao = new AiZindexUserDao();

    private Embeddings ef = new QwenEmbeddings(ContextLoader.configuration.getFunctions().getEmbedding().get(0));

    private ChromaVectorStore vectorStore = new ChromaVectorStore(ContextLoader.configuration.getStores().getVectors().get(0),ef);

    private final VectorStoreService vectorStoreService = new VectorStoreService();

    private long lastExecutionTime = 0;
    private final Object lock = new Object();
    private static KimiThread rateLimiter = new KimiThread();

    /**
     * 查询相似度
     * @param
     * @return
     */
    public List<IndexRecord> query(IndexDictValues dictValue){
        int n = 32;
        Map<String, String> where = new HashMap<>();
        List<IndexRecord> list1 = new ArrayList<>();
        while (n>0) {
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setN(n);
            queryCondition.setText(dictValue.getPlainText());
            queryCondition.setWhere(where);
            list1 =  vectorStore.query(queryCondition, "dict");
            list1 = list1.stream()
                    .filter(record -> record.getDistance() < 0.5)
                    .collect(Collectors.toList());
            if (list1.size() == n){
                n = n *2 ;
            }else {
                n =-1;
            }
        }
        return list1;
    }

    /**
     * 批量新增入向量数据库
     * @param indexDictValuesList
     */
    public void add(List<IndexDictValues> indexDictValuesList){
        synchronized (lock) {
            long now = System.currentTimeMillis();
            if (now - lastExecutionTime < 30) { // 1秒内不允许再次执行
                try {
                    lock.wait(30 - (now - lastExecutionTime)); // 等待剩余时间
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        InstructionPairRequest instructionPairRequest = new InstructionPairRequest();
        instructionPairRequest.setCategory("dict");

        for (IndexDictValues indexDictValues : indexDictValuesList) {
            InstructionData instructionData = new InstructionData();
            List<String> list =new ArrayList<>();
            List<Node> nidlist = new ArrayList<>();
            if (list.size() == 0){
                nidlist = indexDictValues.getNodes();
            }
            for (Node node : nidlist) {
                list.add(node.getNode());
            }
            instructionData.setInstruction(list);
            instructionData.setOutput(indexDictValues.getPlainText());
            instructionPairRequest.setData(Arrays.asList(instructionData));

        long timestamp = Instant.now().toEpochMilli();
            List<InstructionData> instructionDataList = instructionPairRequest.getData();
            String category = instructionPairRequest.getCategory();
            String level = Optional.ofNullable(instructionPairRequest.getLevel()).orElse("user");
            Map<String, String> qaMap = new HashMap<>();
            for (InstructionData data : instructionDataList) {
                String output = data.getOutput().trim();
                List<String> list1 = data.getInstruction();
                for (int i = 0; i < list1.size(); i++) {
                    String instruction = list1.get(i).trim();
                    qaMap.put(instruction, output);
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("category", category);
                    metadata.put("level", level);
                    metadata.put("nid", nidlist.get(i).getNid().toString());
                    metadata.put("did", indexDictValues.getDid().toString());
                    metadata.put("plainText", indexDictValues.getPlainText());
                    metadata.put("seq", Long.toString(timestamp));
                    List<UpsertRecord> upsertRecords = new ArrayList<>();
                    upsertRecords.add(UpsertRecord.newBuilder()
                            .withMetadata(metadata)
                            .withDocument(instruction)
                            .build());
                    upsertRecords.add(UpsertRecord.newBuilder()
                            .withMetadata(new HashMap<>(metadata))
                            .withDocument(output)
                            .build());
                    String s = instruction.replaceAll("\n","");
                    VectorCacheLoader.put2L2(s, timestamp, output);
                    vectorStoreService.upsertCustomVectors(upsertRecords, category, true);
                }
                for (String instruction : data.getInstruction()) {

                }
            }
        }
        System.out.println("新增成功！");
            lastExecutionTime = System.currentTimeMillis();
            lock.notifyAll(); // 唤醒所有等待的线程
        }
    }

    /**
     * 单线程新增向量数据
     */
    @Test
    public void text1(){
//从第10页到第20页，（包括10和20）
        for (int i = 1; i <= 100; i++) {
            List<NodeValue> nodeValues = aiZindexUserDao.getNodeValue(i,10);
            System.out.println(nodeValues.size());
            List<IndexDictValues> indexNodeValuesList = new ArrayList<>();
            nodeValues.stream().forEach(nv -> {
                IndexDictValues obj = null;
                for (IndexDictValues indexNodeValues : indexNodeValuesList) {
                    if (indexNodeValues.getDid().equals(nv.getDid())) {
                        obj = indexNodeValues;
                        break;
                    }
                }

                if (obj != null){
                    Node node = new Node();
                    BeanUtil.copyProperties(nv,node);
                    List<Node> dodeList = obj.getNodes();
                    dodeList.add(node);
                    obj.setNodes(dodeList);
                } else {
                    IndexDictValues indexNodeValues = new IndexDictValues();
                    BeanUtil.copyProperties(nv,indexNodeValues);
                    Node node = new Node();
                    BeanUtil.copyProperties(nv,node);
                    indexNodeValues.setNodes(Lists.newArrayList(node));
                    indexNodeValuesList.add(indexNodeValues);
                }

            });
            indexNodeValuesList.forEach(indexNodeValues -> add(Lists.newArrayList(indexNodeValues)));
            //add(indexNodeValuesList);
        }
    }

    /**
     * 线程池，多线程新增向量数据
     */
    @Test
    public void test3() {
        final int NUM_PAGES = 10; // 最后页数
        final int START_PAGE = 1; // 起始页数
        int PAGE_SIZE = 10; // 每页大小
        ExecutorService executor = Executors.newFixedThreadPool(10); // 创建一个固定大小的线程池
        for (int i = START_PAGE; i <= NUM_PAGES; i++) {
            final int page = i;
            executor.submit(() -> {
                try {
                    List<NodeValue> nodeValues = aiZindexUserDao.getNodeValue(page, PAGE_SIZE);
                    //System.out.println("Page " + page + " size: " + nodeValues.size());
                    List<IndexDictValues> indexDictValuesList = new ArrayList<>();
                    nodeValues.forEach(nv -> {
                        IndexDictValues obj = indexDictValuesList.stream()
                                .filter(indexNodeValues -> indexNodeValues.getDid().equals(nv.getDid()))
                                .findFirst()
                                .orElse(null);

                        if (obj != null){
                            Node node = new Node();
                            BeanUtil.copyProperties(nv,node);
                            List<Node> dodeList = obj.getNodes();
                            dodeList.add(node);
                            obj.setNodes(dodeList);
                        } else {
                            IndexDictValues indexNodeValues = new IndexDictValues();
                            BeanUtil.copyProperties(nv,indexNodeValues);
                            Node node = new Node();
                            BeanUtil.copyProperties(nv,node);
                            indexNodeValues.setNodes(Lists.newArrayList(node));
                            indexDictValuesList.add(indexNodeValues);
                        }
                    });
                    //synchronized () {
                    add(indexDictValuesList);
                    //}
                    System.out.println("执行完毕！！！");
                }catch (Exception e){
                    System.out.println("线程执行出错！！！"+e);
                }

            });
        }

        executor.shutdown(); // 关闭线程池，等待所有任务完成
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow(); // 超时后取消当前执行的任务
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            System.out.println("报错了！！！！！！");
        }
    }

    /**
     * 修改权重---正式修改
     */
    @Test
    public void test4(){
        //jj代表节点的起始页数
        for (int jj = 1; jj <= 100; jj++) {
            DictionaryProcessing dictionaryProcessing = new DictionaryProcessing();
            List<NodeValue> nodeValues = aiZindexUserDao.getNodeValue(jj,10);
            List<IndexDictValues> indexDictValuesList = new ArrayList<>();
            nodeValues.stream().forEach(nv -> {
                IndexDictValues obj = null;
                for (IndexDictValues indexDictValues : indexDictValuesList) {
                    if (indexDictValues.getDid().equals(nv.getDid())) {
                        obj = indexDictValues;
                        break;
                    }
                }

                if (obj != null){
                    Node node = new Node();
                    BeanUtil.copyProperties(nv,node);
                    List<Node> nodeList = obj.getNodes();
                    nodeList.add(node);
                    obj.setNodes(nodeList);
                } else {
                    IndexDictValues indexNodeValues = new IndexDictValues();
                    BeanUtil.copyProperties(nv,indexNodeValues);
                    Node node = new Node();
                    BeanUtil.copyProperties(nv,node);
                    indexNodeValues.setNodes(Lists.newArrayList(node));
                    indexDictValuesList.add(indexNodeValues);
                }

            });

            for (IndexDictValues indexDictValues : indexDictValuesList) {
                //System.out.println(indexNodeValues);
                List<IndexRecord> list1 = dictionaryProcessing.query(indexDictValues);
                List<Node> nodeList  = indexRecordTONode(list1,indexDictValues.getDid());
                if (nodeList!=null&&nodeList.size()>0){
                    List<Node> nodes = fieldRating(indexDictValues.getPlainText(),removeDuplicates(nodeList));
                    nodes = removeDuplicates(nodes);
                    for (Node node1 : nodes){
                        System.out.println("传入nid"+indexDictValues.getDid()+"\n 传入did"+node1.getNid()+"权重："+node1.getWeight());
                        aiZindexUserDao.updateWeight(node1.getNid(),indexDictValues.getDid(),node1.getWeight());
                    }
                }
            }
        }
    }

    public static List<Node> removeDuplicates(List<Node> dictList) {
        Set<Node> set = new HashSet<>(dictList);
        return new ArrayList<>(set);
    }

    /**
     * 将list1转成List<Dict>
     * @param list1
     * @return
     */
    private static List<Node> indexRecordTONode(List<IndexRecord> list1,Integer did) {
        List<Node> dictList =  Collections.synchronizedList(new ArrayList<>());
        for (IndexRecord indexRecord : list1) {
            String nid = (String) indexRecord.getMetadata().get("nid");
            //String nid = (String) indexRecord.getMetadata().get("nid");
            Node dict = new Node();
            WeightObj weightObj = aiZindexUserDao.selectWeight(Integer.parseInt(nid),did);
            //System.out.println(weightObj);
            dict.setNid(Integer.parseInt(nid));
            dict.setNode(indexRecord.getDocument());
            dict.setWeight(weightObj.getWeight());
            dictList.add(dict);
        }
        return dictList;
    }

    /**
     * 智能计算权重
     * @return
     */
    public static List<Node> fieldRating(String PlainTextValue,List<Node> nodeList){
        //String result = chat("请用10条解释一下：“"+nodeValue+"”是什么意思，(只要这10条，不用其它任何提示词，每个句子必须都要包含“"+nodeValue+"”字样)");
        //String result = chat("请帮我用“"+nodeValue+"”从九个角度来造九个句子，要求：每个句子必须都要包含“"+nodeValue+"”字样，而且要包括它的定义，每个句子都要体现该词的不同角度的正交性，（只要这九个句子，不用其它任何提示语句）");
//        String result = chat("请帮我用“"+PlainTextValue+"”从九个角度来造九个句子，要求：每个句子必须都要包含“"+PlainTextValue+"”字样，而且每个句子使用该词时结构要独立，不能有歧义，信息也要独立.其中第一个句子要解释该词的定义," +
//                "第二个句子要说明该词所属的类，第三个句子要说明该词涉及的行业，第四个句子从事实陈述的角度造句，" +
//                "第五个从该词比喻的角度造句，第六个从该词情感表达角度造句，第七个从借代的角度造句。" +
//                "（只要这九个句子，不用其它任何提示语句）");
        String result = "请帮我从九个不同的角度构造包含“"+PlainTextValue+"”的句子，确保每个句子都从不同的方面呈现“"+PlainTextValue+"”的使用场景。要求如下：\n" +
                "1. 第一个句子解释“"+PlainTextValue+"”的定义。\n" +
                "2. 第二个句子说明“"+PlainTextValue+"”所属的类。\n" +
                "3. 第三个句子描述“"+PlainTextValue+"”涉及的行业。\n" +
                "4. 第四个句子从事实陈述的角度来造句" +
                "5. 第五个从该词比喻的角度造句" +
                "6. 第六个从该词情感表达角度造句" +
                "6. 第七个从借代的角度造句" +
                "7. 每个句子必须包含“"+PlainTextValue+"”字样。\n" +
                "8. 每个句子应从不同的角度呈现“"+PlainTextValue+"”的特性，不重复内容，不使用相同的文本结构和套路。每个句子最好都有自己独立的上下文。\n" +
                "生成结果中仅需要这九个句子，无需额外的提示或说明。";
        result = chat(result);

        synchronized (nodeList) {
            Iterator<Node> iterator = nodeList.iterator();
            while (iterator.hasNext()) {
                Node node = iterator.next();
                //System.out.println("相同"+node.getNode()+PlainTextValue);
                //两个一样就直接返回
                if (node.getNode().equals(PlainTextValue)) {
                    //System.out.println("相同");
                    Double weight = 0.0;
                    if (node.getWeight() != null) {
                        weight = 1.0 + node.getWeight();
                    } else {
                        weight = 1.0;
                    }
                    node.setWeight(weight);
                    System.out.println(weight);
                    continue;
                }
                System.out.println("第一次回答的内容：" + result);
                String modifiedText = replaceIgnoreCase(result, PlainTextValue, node.getNode())+"\n 10.有些时候“"+PlainTextValue+"”等同于“"+node.getNode()+"”。";

                //String contxt = "在下面的句子中：\n"+modifiedText + "\n有几个理解是正确的。(只需要告诉我“0个”或“1个”或“2个”或“3个”或“4个”或“5个”或“6个”或“7个”或“8个”，不要输出多余的字。)。";只需要，不要输出多余的字。
                //String contxt = "在下面的句子中：\n"+modifiedText + "\n有几个句子，词语：“"+dict.getPlainText()+"”是使用恰当的。(告诉我几个是恰当的。)。";//“0个”或“1个”或“2个”或“3个”或“4个”或“5个”或“6个”或“7个”或“8个”或“9个”或“10个”是正确的
                String contxt = "在下面的句子中：\n"+modifiedText + "\n有几个句子的：“"+node.getNode()+"”是使用比较恰当的，并且它的表意和解释都是的正确的。" +
                        "(请先告诉我有“0个”或“1个”或“2个”或“3个”或“4个”或“5个”或“6个”或“7个”或“8个”或“9个”或“10个”是正确的，再告诉我原因，字数控制在100字以内，例如：有1个是正确的)。";

                System.out.println("第二次提问的内容: " + contxt);
                String result1 =rateLimiter.execute(contxt);
                //String result1 = chat1(contxt);

                Integer num = getNumber(result1);

                Integer number = num>=10?10:num;
                //Integer number = 1;
                Double weight = 0.0;
                if (node.getWeight() != null) {
                    weight = number * 0.1 + node.getWeight();
                    BigDecimal bd = new BigDecimal(weight);
                    weight = bd.setScale(1, RoundingMode.HALF_UP).doubleValue();;
                    System.out.println("最终的权重：" + weight);
                } else {
                    weight = number * 0.1 ;
                    BigDecimal bd = new BigDecimal(weight);
                    weight = bd.setScale(1, RoundingMode.HALF_UP).doubleValue();;
                    System.out.println("最终的权重：" + weight);
                }
                if (weight <= 0) {
                    iterator.remove();
                } else {
                    node.setWeight(weight);
                }
            }
        }
        return nodeList;
    }

    /**
     * 智能计算权重-gaiban
     * @param nodeValue
     * @param dictList
     * @return
     */
    public static List<Node> fieldRatingGaiban(String nodeValue,List<Node> dictList){
        synchronized (dictList) {
            Iterator<Node> iterator = dictList.iterator();
            while (iterator.hasNext()) {
                Node dict = iterator.next();

                //两个一样就直接返回
                if (dict.getNode().equals(nodeValue)) {
                    Double weight = 0.0;
                    if (dict.getWeight() != null) {
                        weight = 1.0 + dict.getWeight();
                    } else {
                        weight = 1.0;
                    }
                    dict.setWeight(weight);
                    continue;
                }
                //String modifiedText = replaceIgnoreCase(result, nodeValue, dict.getPlainText());
                //System.out.println("造的句子：" + result);
                //String contxt = "在下面的句子中：\n"+modifiedText + "\n有几个理解是正确的。(只需要告诉我“0个”或“1个”或“2个”或“3个”或“4个”或“5个”或“6个”或“7个”或“8个”，不要输出多余的字。)。";
                //String contxt = "在下面的句子中：\n"+modifiedText + "\n有几个句子，词语：“"+dict.getPlainText()+"”是使用恰当的。(告诉我几个是恰当的。)。";
                //只需要告诉我“0分”或“0分”或“0分”或“0分”或“0分”或“0分”或“0分”或“0分”或“0分”，
                String biozhun = "评分标准为：" +
                        "0分 - 无关：两个词或概念之间没有任何联系，它们在不同的领域或语境中完全独立。\n" +
                        "1分 - 极弱相关：两个词或概念之间几乎不相关，但在非常特定或偶然的情况下可能会有微弱的联系。\n" +
                        "2分 - 弱相关：两个词或概念之间存在非常模糊的联系，通常不会在同一语境或讨论中同时出现。\n" +
                        "3分 - 轻度相关：两个词或概念之间有一定的联系，但在功能和用途上仍有显著差异。\n" +
                        "4分 - 中等相关：两个词或概念之间有明显的联系，它们在某些方面共享相似的特征或用途。\n" +
                        "5分 - 中等到高度相关：两个词或概念之间的联系较强，它们经常在同一语境或讨论中一起出现，但并不完全等同。\n" +
                        "6分 - 高度相关：两个词或概念之间有非常紧密的联系，它们在很多情况下可以互换使用，但仍有一些细微的差别。\n" +
                        "7分 - 非常高度相关：两个词或概念几乎可以视为同义词，它们在大多数情况下可以互换，只在非常特定的情境下有所不同。\n" +
                        "8分 - 完全相关：两个词或概念完全相同，在任何语境或讨论中都可以互换使用，没有差别。";
                String contxt = biozhun+"满分为8分，你会给“"+nodeValue + "”和“"+dict.getNode()+"”相关度打几分。(告我相关度打几分，，就行不要输出多余的字。)。";
                System.out.println("第二次提问的内容: " + contxt);
                String result1 =rateLimiter.execute(contxt);
                //String result1 = chat1(contxt);

                Integer num = getNumber(result1);

                Integer number = num>=8?8:num;
                //Integer number = 1;
                Double weight = 0.0;
                if (dict.getWeight() != null) {
                    weight = (number * 5.0 / 8.0) * 0.2 + dict.getWeight();
                    System.out.println("最终的权重：" + weight);
                } else {
                    weight = (number * 5.0 / 8.0) * 0.2;
                    System.out.println("最终的权重：" + weight);
                }
                if (weight <= 0) {
                    iterator.remove();
                } else {
                    dict.setWeight(weight);
                }
            }
        }
        return dictList;
    }

    /**
     * 通过结果获取正确个数
     * @param result1
     * @return
     */
    @NotNull
    private static Integer getNumber(String result1) {
        String numberPattern = "(\\d+)个";
        Pattern pattern = Pattern.compile(numberPattern);
        Matcher matcher = pattern.matcher(result1);
        Integer number = 0;
        if (matcher.find()) {
            String numberStr = matcher.group(1);
            number = Integer.parseInt(numberStr);
            System.out.println("提取的数字是: " + number);
        } else {
            System.out.println("没有找到数字");
        }
        System.out.println("最终反馈的结果：" + result1);
        return number;
    }

    /**
     * 忽视大小写的替换，并将大写的字符串替换为小写
     * @param original
     * @param target
     * @param replacement
     * @return
     */
    public static String replaceIgnoreCase(String original, String target, String replacement) {
        // 使用 Pattern 和 Matcher 进行不区分大小写的替换
        String replaced = Pattern.compile(target, Pattern.CASE_INSENSITIVE)
                .matcher(original)
                .replaceAll(replacement);
        // 将替换后的字符串转换为小写
        return replaced.toLowerCase();
    }

    @Test
    public void sss(){
        //
        System.out.println(chat("请帮我用“-y”造八个最常用的句子(只要这八个句子，不用其它任何提示词,不用翻译成中文，每个句子都要包含“-y”字样)？"));
    }

    /**
     * 文心一言
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
        //chatCompletionRequest.setModel("moonshot-v1-8k");
        // Create an instance of CompletionsService
        //chatCompletionRequest.setModel("ERNIE-Speed-128K");
        chatCompletionRequest.setModel("ERNIE-Speed-8K");
        //chatCompletionRequest.setModel("v3.5");
        //chatCompletionRequest.setModel("glm-3-turbo");
        //chatCompletionRequest.setModel("glm-4");
        //chatCompletionRequest.setModel("qwen-plus");

        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = null;
        boolean isf = true;
        String retry = null;
        while (isf){
            try {
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                if (retry!=null){
                    isf = false;
                }
            }catch (Exception e){
                isf = false;
                try {
                    Thread.sleep(1000);
                    System.out.println("访问出错了");
                }catch (Exception e1){
                    System.out.println("等待都失败了！");
                    int i = 1/0;
                }
            }
        }

        return retry;
    }

    /**
     * kimi
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
        //chatCompletionRequest.setModel("v3.5");
        //chatCompletionRequest.setModel("v1.1");
        //chatCompletionRequest.setModel("glm-3-turbo");
        //chatCompletionRequest.setModel("moonshot-v1-8k");
        chatCompletionRequest.setModel("ERNIE-Speed-8K");
        //chatCompletionRequest.setModel("glm-4v");
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
                    Thread.sleep(1000);
                    System.out.println("访问出错了");
                }catch (Exception e1){
                    System.out.println("等待都失败了！");
                    int i = 1/0;
                }
            }
        }

        return retry;
    }
    public static void main(String[] args) {

        List<NodeValue> nodeValues = aiZindexUserDao.getNodeValue(1,10);
        System.out.println(nodeValues);

//        List<Dict> dictList = new ArrayList<>();
//        Dict dict = new Dict();
//        dict.setDid(1);
//        dict.setPlainText("FIELD");
//        dictList.add(dict);
//        Dict dict1 = new Dict();
//        dict1.setDid(2);
//        dict1.setPlainText("田");
//        dictList.add(dict1);
//        List<Dict> list = fieldRating("FIELD", dictList);
//        for (Dict d : list){
//            System.out.println(d);
//        }
//            Integer number = 0;
//            double weight = 0.0;
//            weight = (number * 5.0 / 8.0) * 0.2 ;
//            System.out.println("最终的权重：" + weight);
    }
}
