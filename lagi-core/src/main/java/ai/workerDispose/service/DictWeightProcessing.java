package ai.workerDispose.service;

import ai.common.client.AiServiceCall;
import ai.common.client.AiServiceInfo;
import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.utils.OkHttpUtil;
import ai.vector.VectorStoreService;
import ai.vector.pojo.IndexRecord;
import ai.vector.pojo.QueryCondition;
import ai.workerDispose.dao.AiZindexUserDao;
import ai.workerDispose.pojo.*;
import cn.hutool.core.bean.BeanUtil;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DictWeightProcessing {
    static {
        ContextLoader.loadContext();
    }

    private final AiZindexUserDao aiZindexUserDao = new AiZindexUserDao();
    private final VectorStoreService vectorStoreService = new VectorStoreService();
    private final Gson gson = new Gson();
    private static final AiServiceCall packageCall = new AiServiceCall();

    private final String datePattern = "yyyy-MM-dd HH:mm:ss.SSS";
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(datePattern);

    private static final String VECTOR_QUERY_URL = "https://lagi.saasai.top/v1/vector/query";
    private static final String CATEGORY = "dict";
    private static final double QUERY_SIMILARITY = 0.15;
    private static final String DICT_WEIGHT_PROGRESS_FILE = "DictWeightProgress.json";

    private String getDictWeightProgressFile() {
        return System.getProperty("user.home") + "/" + DICT_WEIGHT_PROGRESS_FILE;
    }

    private void saveToJsonFile(DictWeightProgress progress, String filePath) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create(); // 美化 JSON 输出
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(progress, writer);
            System.out.println("对象已成功保存到文件：" + filePath);
        } catch (IOException e) {
            System.err.println("写入 JSON 文件时出错：" + e.getMessage());
        }
    }

    private DictWeightProgress readFromJsonFile(String filePath) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, DictWeightProgress.class);
        } catch (IOException e) {
            System.err.println("读取 JSON 文件时出错：" + e.getMessage());
            return null;
        }
    }

    public void dictWeightProcess(int startPage, int endPage, int pageSize) {
        DictWeightProgress progress = readFromJsonFile(getDictWeightProgressFile());
        if (progress == null) {
            progress = new DictWeightProgress();
            progress.setStartPage(startPage);
            progress.setEndPage(endPage);
            progress.setProcessedPage(startPage);
            saveToJsonFile(progress, getDictWeightProgressFile());
        }
        int i = progress.getProcessedPage();
        while (i <= endPage) {
            System.out.println("\n\nCurrent page is " + i + ", time is " + simpleDateFormat.format(new Date()));
            try {
                dictWeightProcess(pageSize, i);
            } catch (Exception e) {
                e.printStackTrace();
                sleep(1000 * 30);
                continue;
            }
            progress.setProcessedPage(i);
            saveToJsonFile(progress, getDictWeightProgressFile());
            i++;
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void dictWeightProcess(int pageSize, int i) {
        List<DictValue> dictList = aiZindexUserDao.getDictList(i, pageSize);
        dictWeightProcess(dictList);
    }

    public void dictWeightProcess(String filepath) {
        List<DictValue> dictValueList = getDictValueFromFilePath(filepath);
        for (int i = 0; i <= dictValueList.size(); i++) {
            System.out.println("\n\nCurrent index is " + i + ", time is " + simpleDateFormat.format(new Date()));
            try {
                dictWeightProcess(Collections.singletonList(dictValueList.get(i)));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private List<DictValue> getDictValueFromFilePath(String filepath) {
        List<DictValue> dictValueList = new ArrayList<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filepath));
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                dictValueList.add(new DictValue(Integer.parseInt(values[0]), values[1]));
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dictValueList;
    }

    public void dictWeightProcess(List<DictValue> dictList) {
        List<IndexDictValues> indexDictValuesList = new ArrayList<>();
        dictList.forEach(dict -> {
            IndexDictValues obj = null;
            for (IndexDictValues indexDictValues : indexDictValuesList) {
                if (indexDictValues.getDid().equals(dict.getDid())) {
                    obj = indexDictValues;
                    break;
                }
            }
            if (obj != null) {
                Node node = new Node();
                BeanUtil.copyProperties(dict, node);
                List<Node> nodeList = obj.getNodes();
                nodeList.add(node);
                obj.setNodes(nodeList);
            } else {
                IndexDictValues indexNodeValues = new IndexDictValues();
                BeanUtil.copyProperties(dict, indexNodeValues);
                Node node = new Node();
                BeanUtil.copyProperties(dict, node);
                indexNodeValues.setNodes(Lists.newArrayList(node));
                indexDictValuesList.add(indexNodeValues);
            }
        });

        for (IndexDictValues indexDictValues : indexDictValuesList) {
            long start = System.currentTimeMillis();
            List<IndexRecord> nodeQueryList = query(indexDictValues);
            long end = System.currentTimeMillis();
            System.out.println("node query size is " + nodeQueryList.size() + ", time is " + (end - start) + "ms");
            List<Node> nodeList = indexRecordTONode(nodeQueryList, indexDictValues.getDid());
            if (!nodeList.isEmpty()) {
                List<Node> nodes = fieldRating(indexDictValues.getPlainText(), removeDuplicates(nodeList));
                nodes = removeDuplicates(nodes);
                for (Node node : nodes) {
                    System.out.println("did=" + indexDictValues.getDid() + " uid=" + node.getNid() + " weight=" + node.getWeight());
                    updateWeight(node.getNid(), indexDictValues.getDid(), node.getWeight());
                }
            }
        }
    }

    private List<IndexRecord> query(IndexDictValues dictValue) {
        return queryLocal(dictValue);

    }

    private List<IndexRecord> queryLocal(IndexDictValues dictValue) {
        int n = 32;
        Map<String, String> where = new HashMap<>();
        List<IndexRecord> list1 = new ArrayList<>();
        while (n > 0) {
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setN(n);
            queryCondition.setText(dictValue.getPlainText());
            queryCondition.setWhere(where);
            list1 = vectorStoreService.query(queryCondition, CATEGORY);
            list1 = list1.stream()
                    .filter(record -> record.getDistance() < QUERY_SIMILARITY)
                    .collect(Collectors.toList());
            if (list1.size() == n) {
                n = n * 2;
            } else {
                n = -1;
            }
        }
        return list1;
    }


    private List<IndexRecord> queryRemote(IndexDictValues dictValue) throws IOException {
        int n = 32;
        Map<String, String> where = new HashMap<>();
        List<IndexRecord> list = new ArrayList<>();
        while (n > 0) {
            QueryCondition queryCondition = new QueryCondition();
            queryCondition.setN(n);
            queryCondition.setText(dictValue.getPlainText());
            queryCondition.setWhere(where);
            queryCondition.setCategory(CATEGORY);
            String json = OkHttpUtil.post(VECTOR_QUERY_URL, gson.toJson(queryCondition));
            VectorQueryResponse vectorQueryResponse = gson.fromJson(json, VectorQueryResponse.class);

            if (vectorQueryResponse.getStatus().equals("success")) {
                list = vectorQueryResponse.getData();
            } else {
                n = -1;
            }
            list = list.stream()
                    .filter(record -> record.getDistance() < QUERY_SIMILARITY)
                    .collect(Collectors.toList());
            if (list.size() == n) {
                n = n * 2;
            } else {
                n = -1;
            }
        }
        return list;
    }

    private List<Node> removeDuplicates(List<Node> dictList) {
        Set<Node> set = new HashSet<>(dictList);
        return new ArrayList<>(set);
    }

    /**
     * 将list1转成List<Dict>
     *
     * @param list1
     * @return
     */
    private List<Node> indexRecordTONode(List<IndexRecord> list1, Integer did) {
        List<Node> dictList = Collections.synchronizedList(new ArrayList<>());
        for (IndexRecord indexRecord : list1) {
            String nid = (String) indexRecord.getMetadata().get("nid");
            Node dict = new Node();
//            WeightObj weightObj = aiZindexUserDao.selectWeight(Integer.parseInt(nid), did);
            dict.setNid(Integer.parseInt(nid));
            dict.setNode(indexRecord.getDocument());
//            dict.setWeight(weightObj.getWeight());
            dictList.add(dict);
        }
        return dictList;
    }

    private List<Node> fieldRating(String PlainTextValue, List<Node> nodeList) {
        String result = "请帮我从九个不同的角度构造包含“" + PlainTextValue + "”的句子，确保每个句子都从不同的方面呈现“" + PlainTextValue + "”的使用场景。要求如下：\n" +
                "1. 第一个句子解释“" + PlainTextValue + "”的定义。\n" +
                "2. 第二个句子说明“" + PlainTextValue + "”所属的类。\n" +
                "3. 第三个句子描述“" + PlainTextValue + "”涉及的行业。\n" +
                "4. 第四个句子从事实陈述的角度来造句" +
                "5. 第五个从该词比喻的角度造句" +
                "6. 第六个从该词情感表达角度造句" +
                "7. 第七个从借代的角度造句" +
                "8. 每个句子必须包含“" + PlainTextValue + "”字样。\n" +
                "9. 每个句子应从不同的角度呈现“" + PlainTextValue + "”的特性，不重复内容，不使用相同的文本结构和套路。每个句子最好都有自己独立的上下文。\n" +
                "生成结果中仅需要这九个句子，无需额外的提示或说明。";
        result = chat(result);

        Iterator<Node> iterator = nodeList.iterator();
        while (iterator.hasNext()) {
            Node node = iterator.next();
            //System.out.println("相同"+node.getNode()+PlainTextValue);
            //两个一样就直接返回
            if (node.getNode().equals(PlainTextValue)) {
                //System.out.println("相同");
                Double weight;
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
            String modifiedText = replaceIgnoreCase(result, PlainTextValue, node.getNode()) + "\n10.有些时候“" + PlainTextValue + "”等同于“" + node.getNode() + "”。";

            String prompt = "-----------------\n" + modifiedText + "\n-----------------\n" +
                    "以上提供的几个句子之中，有几个句子中的“" + node.getNode() + "”是使用比较恰当的，并且它的表意和解释都是的正确的。" +
                    "回答只需要数量，使用阿拉伯数字，返回结果示例：0个、1个、2个、3个、4个、5个、6个、7个、8个、9个和10个。";

            System.out.println("第二次提问的内容: " + prompt);
            String result1 = chat(prompt);
            Integer num = getNumber(result1);

            int number = num >= 10 ? 10 : num;
            double weight;
            if (node.getWeight() != null) {
                weight = number * 0.1 + node.getWeight();
                BigDecimal bd = new BigDecimal(weight);
                weight = bd.setScale(1, RoundingMode.HALF_UP).doubleValue();
                System.out.println("最终的权重：" + weight);
            } else {
                weight = number * 0.1;
                BigDecimal bd = new BigDecimal(weight);
                weight = bd.setScale(1, RoundingMode.HALF_UP).doubleValue();
                System.out.println("最终的权重：" + weight);
            }
            if (weight <= 0) {
                iterator.remove();
            } else {
                node.setWeight(weight);
            }
        }
        return nodeList;
    }

    /**
     * 通过结果获取正确个数
     *
     * @param result1
     * @return
     */
    private static Integer getNumber(String result1) {
        String numberPattern = "(\\d+)个";
        Pattern pattern = Pattern.compile(numberPattern);
        Matcher matcher = pattern.matcher(result1);
        int number = 0;
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
     *
     * @param original
     * @param target
     * @param replacement
     * @return
     */
    private static String replaceIgnoreCase(String original, String target, String replacement) {
        // 使用 Pattern 和 Matcher 进行不区分大小写的替换
        String replaced = Pattern.compile(target, Pattern.CASE_INSENSITIVE)
                .matcher(original)
                .replaceAll(replacement);
        // 将替换后的字符串转换为小写
        return replaced.toLowerCase();
    }

    private String chat(String content) {
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setTemperature(0.8);
        chatCompletionRequest.setMax_tokens(1024);
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(content);
        chatCompletionRequest.setMessages(Lists.newArrayList(message));
        chatCompletionRequest.setStream(false);
        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result;
        boolean isf = true;
        String retry = null;
        while (isf) {
            try {
                long start = System.currentTimeMillis();
                result = completionsService.completions(chatCompletionRequest);
                long end = System.currentTimeMillis();
                System.out.println("LLM completion time：" + (end - start) + "ms");
                if (result != null && result.getChoices() != null && result.getChoices().size() > 0) {
                    retry = result.getChoices().get(0).getMessage().getContent();
                    if (retry != null) {
                        isf = false;
                    }
                }
            } catch (Exception e) {
                try {
                    Thread.sleep(1000);
                    System.out.println("访问出错了");
                } catch (Exception e1) {
                    System.out.println("等待都失败了！");
                }
            }
        }

        return retry;
    }

    private boolean updateWeight(Integer nid, Integer did, Double weight) {
        DictZIndexEntity dictZIndexEntity = new DictZIndexEntity();
        dictZIndexEntity.setUid(nid);
        dictZIndexEntity.setDid(did);
        dictZIndexEntity.setWeight(weight);
        Object[] params = {gson.toJson(dictZIndexEntity)};
        String returnStr = packageCall.callWS(AiServiceInfo.GWSUniUrl, "updateOrAddDictIndexWeight", params)[0];
        return returnStr != null && returnStr.equals("success");
    }
}
