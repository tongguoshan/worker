package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.workerDispose.dao.AiZindexUserDao;
import ai.workerDispose.pojo.ClassifyTxt;
import ai.workerDispose.pojo.WriteClassifyCvs;
import ai.workerDispose.utils.ConversionTypeUtils;
import ai.workerDispose.utils.WriteIds;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DictClassify {


    static {
        ContextLoader.loadContext();
    }

    private static WriteIds writeIds = new WriteIds();

    private static AiZindexUserDao aiZindexUserDao = new AiZindexUserDao();

    /**
     * 关键词分类
     *
     * @param csvFilePath  初始路径
     * @param startId      起始id
     * @param batchLimit   批次大小
     * @return
     */
    public static String PickThorn(String csvFilePath, String startId, int batchLimit, String stopId) {
        String idto = "";//正在处理的id
        String content = "";
        int wordCount = 0; // 初始化词计数器
        String idto1 = "";//上一次处理的id


        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFilePath), StandardCharsets.UTF_8))) {
            String line;
            boolean found = true;
            StringBuilder batchContent = new StringBuilder();

            while ((line = br.readLine()) != null && found) {
                String[] values = line.split(",");
                if (values.length > 0) {
                    wordCount++;
                    String id = values[0].trim().replaceAll("^\"|\"$", "");
                    if (stopId.equals(id)) {
                        break;
                    }
                    if (id.equals(startId.trim())) {
                        found = false;
                    }
                }
            }
            System.out.println("从id为" + startId + "，第" + wordCount + "个开始分析");
            wordCount = 0;

            Map<String,String> idAndValueMap = new HashMap<>();
            while ((line = br.readLine()) != null && wordCount < batchLimit) {

                String[] values = line.split(",");
                if (values.length >= 2) {
                    String id = values[0].trim().replaceAll("^\"|\"$", ""); // 关键词在第二列
                    String keyword = values[1].trim().replaceAll("^\"|\"$", ""); // 关键词在第二列
                    if (!stopId.equals(id)) {
                        idAndValueMap.put(keyword,id);
                        //content += id+"."+keyword + ",";
                        content += keyword + ",";
                    } else {
                        break;
                    }
                    idto = id;
                }
                wordCount++;

                if (wordCount % batchLimit == 0) {
                    System.out.println("此时id为" + idto + "，分析到了第" + wordCount + "个");
                    if (idto1==null||idto1.equals("")){
                        idto1 = startId;
                    }

                    Map<String, String> map1 = new HashMap<>();
                    Map<String, String> map2 = new HashMap<>();
                    boolean flag = true;
                    while (flag){
                        try {
                            String keywords = analyse1(idto, content, stopId);
                            map1 = Workflows(keywords, idAndValueMap);
                            if (map1.size()>10){
                                flag = false;
                            }
                        }catch (Exception e){
                            System.out.println(e);
                        }
                    }

                    boolean flag1 = true;
                    while (flag1){
                        try {
                            String keywords1 = analyse(idto, content, stopId);
                            map2 = Workflows(keywords1, idAndValueMap);
                            if (map2.size()>10){
                                flag1 = false;
                            }
                        }catch (Exception e){
                            System.out.println(e);
                        }
                    }

                    Map<String, String> idToValueMap = ConversionTypeUtils.getIntersection(map1, map2);
                    //该方法适用于方案一
                    //Map<String,String> idToValueMap = ConversionTypeUtils.stringTomap(keywords);

                    WriteIds.writerClassifyCsv(csvFilePath,idToValueMap);

                    batchContent.setLength(0);
                    content = "";
                    wordCount = 0;
                    idto1 = idto;
                    idAndValueMap.clear();;
                }

            }
            // 处理最后一个批次（如果有剩余的词）
            if (!content.isEmpty()) {
                if (idto1==null||idto1.equals("")){
                    idto1 = startId;
                }
                System.out.println("处理最后一个批次，此时id为" + idto + "，分析到了第" + wordCount + "个");

                Map<String, String> map1 = new HashMap<>();
                Map<String, String> map2 = new HashMap<>();
                boolean flag = true;
                while (flag){
                    try {
                        String keywords = analyse1(idto, content, stopId);
                        map1 = Workflows(keywords, idAndValueMap);
                        if (map1.size()>10){
                            flag = false;
                        }
                    }catch (Exception e){
                        System.out.println(e);
                    }
                }

                boolean flag1 = true;
                while (flag1){
                    try {
                        String keywords1 = analyse(idto, content, stopId);
                        map2 = Workflows(keywords1, idAndValueMap);
                        if (map2.size()>10){
                            flag1 = false;
                        }
                    }catch (Exception e){
                        System.out.println(e);
                    }
                }

                Map<String, String> idToValueMap = ConversionTypeUtils.getIntersection(map1, map2);
                        //该方法适用于方案一
                // Map<String,String> idToValueMap = ConversionTypeUtils.stringTomap(keywords);
                WriteIds.writerClassifyCsv(csvFilePath,idToValueMap);

                batchContent.setLength(0);
                content = "";
                wordCount = 0;
                idto1 = idto;
                idAndValueMap.clear();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "任务完成！";
    }

    private static Map<String, String> Workflows(String keywords, Map<String, String> idAndValueMap) {
        String parse = ConversionTypeUtils.extractContentWithinBraces(keywords);
        if (parse != null && !parse.trim().isEmpty()){
            Map<String,String> Maps = ConversionTypeUtils.convertStringToMap(parse);
            Map<String,String> idToValueMap = ConversionTypeUtils.getKeyByClassId(Maps, idAndValueMap);
            return idToValueMap;
        }
        return new HashMap<>();
    }

    /**
     * 类型转换
     * @param keywords
     * @param nodeTxtList
     * @return
     */
    private static List<WriteClassifyCvs> WorkflowsPro(String keywords, List nodeTxtList) {
        List<WriteClassifyCvs> writeClassifyCvsList = new ArrayList<>();
        String parse = ConversionTypeUtils.extractContentWithinBraces(keywords);
        if (parse != null && !parse.trim().isEmpty()){
            Map<String,String> Maps = ConversionTypeUtils.convertStringToMap(parse);
            writeClassifyCvsList = ConversionTypeUtils.getKeyByClassId(Maps, nodeTxtList);
            return writeClassifyCvsList;
        }
        return writeClassifyCvsList;
    }

    /**
     * 智能处理
     *
     * @param idto 起始id
     * @param content
     * @return
     */
    private static String analyse1(String idto, String content,String stopId) {
        System.out.println("该处的id为：" + idto);
//        String[] promptHead = {"现有45类：\n" +
//                "“1.生物 2.人工制品  3.名字 4.行为 5.形象 6.种类 7.言语 8.参数 9.事件 10.程度 11.状态 \n" +
//                "12.关系 13.版本 14.医学 15.商务 16.介连词 17.软件 18.硬件 19.习俗 20.物理 21.味道 22.网站 \n" +
//                "23.企业 24.气候 25.团体  26.规定  27.院校 28.地点  29.娱乐  30.思想 \n" +
//                "31.食物 32.文艺 33.历史  34.宗教  35.颜色 36.符号  37.组织部位 38.功能 \n" +
//                "39.声音语气 40.时间 41.建筑 42.身份 43.化学 44.理论技法 45.自然物 ”\n" +
//                "要求：\n" +
//                "(1) 依据以上类别，将下面的name进行分类，type值即为该编号；\n" +
//                "(2) 如果不属于所列任何一个类别，就将该节点的类别值，标记为0;\n" +
//                "(3) 如果该词不仅不属于任何一个类别，且是错误的词语(生造词)，则将该节点的类别值，标记为-1;\n" +
//                "(4) 输出格式为：“{[id]=[type], [id]=[type]}” 例如：{4=1};\n" +
//                "请给下面的数据进行分类： \033[0;94m \n “"};
//        String[] promptTail = {"”\n \033[0m 注意：无需解释，无需其它提示词，只要输出“{[id]=[type], [id]=[type]}” 的数据即可。"};
                String[] promptHead = {"现有45类：\n" +
                "“生物,人工制品,名字,行为,形象,种类,言语,参数,事件,程度,状态 \n" +
                ",关系,版本,医学,商务,介连词,软件,硬件,习俗,物理,味道,网站 \n" +
                ",企业,气候,团体,规定,院校,地点,娱乐,思想 \n" +
                ",食物,文艺,历史,宗教,颜色,符号,组织部位,功能 \n" +
                ",声音语气,时间,建筑,身份,化学,理论技法,自然物”\n" +
                "要求：\n" +
                "(1) 依据以上类别，对下面的name进行分类，type的值即为该类型；\n" +
                "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就标记为“其它”;\n" +
                "(3) 如果该词不仅不属于任何一个类别，且是错误的词语(生造词)，则将该节点的type值，标记为“生造词”;\n" +
                "(4) 输出格式为：“{name属于type, name属于type}” 例如：{小鸡属于生物,麻辣香锅属于食物};\n" +
                "请给下面的数据进行分类： \033[0;94m \n “"};
        String[] promptTail = {"”\n \033[0m 注意：无需解释，无需其它提示词，只要输出“{name属于type, name属于type}” 的数据即可。"};

        String content1 = promptHead[0];
        content1 += content + promptTail[0];

        System.out.println(content1);

        //String result = content1; //chat(content1);
        String result = chat(content1,idto,stopId);
        System.out.println("千问大模型输出：" + result);
        return result;
    }

    private static String analyse(String idto, String content,String stopId) {
        System.out.println("该处的id为：" + idto);
        String[] promptHead = {"现有45类：\n" +
                "“生物,人工制品,名字,行为,形象,种类,言语,参数,事件,程度,状态 \n" +
                ",关系,版本,医学,商务,介连词,软件,硬件,习俗,物理,味道,网站 \n" +
                ",企业,气候,团体,规定,院校,地点,娱乐,思想 \n" +
                ",食物,文艺,历史,宗教,颜色,符号,组织部位,功能 \n" +
                ",声音语气,时间,建筑,身份,化学,理论技法,自然物”\n" +
                "要求：\n" +
                "(1) 依据以上类别，对下面的name进行分类，type的值即为该类型；\n" +
                "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就为“其它”;\n" +
                "(3) 如果该词不仅不属于任何一个类别，且是错误的词语(生造词)，则将该节点的type就为“生造词”;\n" +
                "(4) 输出格式为：“{name属于type, name属于type}” 例如：{小鸡属于生物,麻辣香锅属于食物};\n" +
                "请给下面的数据进行分类： \033[0;94m \n “"};
        String[] promptTail = {"”\n \033[0m 注意：无需解释，无需其它提示词，只要输出“{name属于type, name属于type}” 的数据即可。"};

        String content1 = promptHead[0];
        content1 += content + promptTail[0];

        System.out.println(content1);

        //String result = content1; //chat(content1);
        String result = chat1(content1,idto,stopId);
        System.out.println("kimi大模型输出：" + result);
        return result;
    }

    private static String analysePro(String content) {
        String[] promptHead = {"现有以下类型：\n" +
                "“生物,人工制品,名字,行为,形象,种类,言语,参数,事件,程度,状态 \n" +
                ",关系,版本,医学,商务,介连词,软件,硬件,习俗,物理,味道,网站 \n" +
                ",企业,气候,团体,规定,院校,地点,娱乐,思想 \n" +
                ",食物,文艺,历史,宗教,颜色,符号,组织部位,功能 \n" +
                ",声音语气,时间,建筑,身份,化学,理论技法,自然物," +
                "法律,数学,运动,地质,特征,其它,生造词\n”\n" +
                "要求：\n" +
                "(1) 依据以上类别，对下面的name进行分类，type的值即为该类型；\n" +
                "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就为“其它”;\n" +
                "(3) 如果该词不仅不属于任何一个类别，且是错误的词语(生造词)，则将该节点的type就为“生造词”;\n" +
                "(4) 输出格式为：“{name属于type, name属于type}” 例如：{小鸡属于生物,麻辣香锅属于食物};\n" +
                "请给下面的数据进行分类： \033[0;94m \n “"};
        String[] promptTail = {"”\n \033[0m 注意：无需解释，无需其它提示词，只要输出“{name属于type, name属于type}” 的数据即可。"};

        String content1 = promptHead[0];
        content1 += content + promptTail[0];

        System.out.println(content1);

        //String result = content1; //chat(content1);
        String result = chat1(content1,"0","0");
        System.out.println("kimi大模型输出：" + result);
        return result;
    }

    private static String analysePro1(String content) {
        String[] promptHead = {"现有以下类型：\n" +
                "“生物,人工制品,名字,行为,形象,种类,言语,参数,事件,程度,状态 \n" +
                ",关系,版本,医学,商务,介连词,软件,硬件,习俗,物理,味道,网站 \n" +
                ",企业,气候,团体,规定,院校,地点,娱乐,思想 \n" +
                ",食物,文艺,历史,宗教,颜色,符号,组织部位,功能 \n" +
                ",声音语气,时间,建筑,身份,化学,理论技法,自然物," +
                "法律,数学,运动,地质,特征,其它,生造词\n”\n" +
                "要求：\n" +
                "(1) 依据以上类别，对下面的name进行分类，type的值即为该类型；\n" +
                "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就为“其它”;\n" +
                "(3) 如果该词不仅不属于任何一个类别，且是错误的词语(生造词)，则将该节点的type就为“生造词”;\n" +
                "(4) 输出格式为：“{name属于type, name属于type}” 例如：{小鸡属于生物,麻辣香锅属于食物};\n" +
                "请给下面的数据进行分类： \033[0;94m \n “"};
        String[] promptTail = {"”\n \033[0m 注意：无需解释，无需其它提示词，只要输出“{name属于type, name属于type}” 的数据即可。"};

        String content1 = promptHead[0];
        content1 += content + promptTail[0];

        System.out.println(content1);

        //String result = content1; //chat(content1);
        String result = chat(content1,"0","0");
        System.out.println("通义千问大模型输出：" + result);
        return result;
    }

    public static  void categorizationOperations(String nids,String filePath){
        //String filePath = "D:\\OfFile\\test11111.csv";
        //String nids = "15674579,15674609,15674669,15674785,15674795,15674821,15674853,15675051,15675083,15675107,15675183,15675239,15675283";
        List<ClassifyTxt> nodeTxtList = aiZindexUserDao.getClassifyTxt(nids);
        String result = nodeTxtList.stream()
                .map(ClassifyTxt::getNode_text)
                .collect(Collectors.joining(","));
        System.out.println(result);

        List<WriteClassifyCvs> map1 = new ArrayList<>();
        List<WriteClassifyCvs> map2 = new ArrayList<>();
        boolean flag = true;
        while (flag){
            try {
                String keywords = analysePro(result);
                map1 = WorkflowsPro(keywords, nodeTxtList);
                if (map1.size()>=1){
                    flag = false;
                }
            }catch (Exception e){
                System.out.println(e);
            }
        }

        boolean flag1 = true;
        while (flag1){
            try {
                String keywords1 = analysePro1(result);
                map2 = WorkflowsPro(keywords1, nodeTxtList);
                if (map2.size()>=1){
                    flag1 = false;
                }
            }catch (Exception e){
                System.out.println(e);
            }
        }

        List<WriteClassifyCvs> finalMap = map2;
        List<WriteClassifyCvs> intersection = map1.stream()
                .filter(obj1 -> obj1.getId() != null &&
                        obj1.getType() != null)
                .filter(obj1 -> finalMap.stream()
                        .anyMatch(obj2 ->
                                obj2.getId() != null &&
                                        obj2.getType() != null &&
                                        obj1.getId().equals(obj2.getId()) &&
                                        obj1.getType().equals(obj2.getType())))
                .collect(Collectors.toList());

        writeIds.writeObjectsToCsv(intersection, filePath);

    }


    /**
     * 程序运行入口
     * @param args
     */
    public static void main(String[] args) {
        //开385876 停419268
        //demo1("403032" ,"419268");
    }

    /**
     * 根据传参subid来对节点分类
     */
    @Test
    public  void demo3(){
      String filePath = "D:\\OfFile\\test11111.csv";
      Integer batchLimit = 100;
      Integer snti = 74;
      Integer minNid = 1;
      Integer maxNid = 46513318;
      Integer jishu = 0;
        while (aiZindexUserDao.getClassifyTxtCount(snti,minNid,maxNid)){
            jishu++;
            List<ClassifyTxt> nodeTxtList = aiZindexUserDao.getClassifyTxt(batchLimit,snti,minNid,maxNid);
            System.out.println("第"+jishu+"次分类，当前第一条nid为："+minNid);
            minNid = nodeTxtList.get(nodeTxtList.size() - 1).getNid() + 1;
            System.out.println("第"+jishu+"次分类，当前最后一条nid为："+minNid);
            String result = nodeTxtList.stream()
                    .map(ClassifyTxt::getNode_text)
                    .collect(Collectors.joining(","));
            //System.out.println(result);

            List<WriteClassifyCvs> map1 = new ArrayList<>();
            List<WriteClassifyCvs> map2 = new ArrayList<>();
            boolean flag = true;
            while (flag){
                try {
                    String keywords = analysePro(result);
                    map1 = WorkflowsPro(keywords, nodeTxtList);
                    if (map1.size()>10){
                        flag = false;
                    }
                }catch (Exception e){
                    System.out.println(e);
                }
            }

            boolean flag1 = true;
            while (flag1){
                try {
                    String keywords1 = analysePro1(result);
                    map2 = WorkflowsPro(keywords1, nodeTxtList);
                    if (map2.size()>10){
                        flag1 = false;
                    }
                }catch (Exception e){
                    System.out.println(e);
                }
            }

            List<WriteClassifyCvs> finalMap = map2;
            List<WriteClassifyCvs> intersection = map1.stream()
                    .filter(obj1 -> obj1.getId() != null &&
                            obj1.getType() != null)
                    .filter(obj1 -> finalMap.stream()
                            .anyMatch(obj2 ->
                                    obj2.getId() != null &&
                                            obj2.getType() != null &&
                                            obj1.getId().equals(obj2.getId()) &&
                                            obj1.getType().equals(obj2.getType())))
                    .collect(Collectors.toList());

            writeIds.writeObjectsToCsv(intersection, filePath);
        }



    }

    @Test
    public  void demo2Start(){
        demo2("D:\\OfFile\\nid数据\\nid数据\\FAQ挖掘.csv", "41865854", 20, "31823386","D:\\OfFile\\分类后的数据(FAQ挖掘).csv");
    }

    private static String demo2(String csvFilePath, String startId, int batchLimit, String stopId,String filePath){
        String idto = "";//正在处理的id
        String content = "";
        int wordCount = 0; // 初始化词计数器
        String idto1 = "";//上一次处理的id


        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFilePath), StandardCharsets.UTF_8))) {
            String line;
            boolean found = true;
            StringBuilder batchContent = new StringBuilder();

            while ((line = br.readLine()) != null && found) {
                String[] values = line.split(",");
                if (values.length > 0) {
                    wordCount++;
                    String id = values[0].trim().replaceAll("^\"|\"$", "");
                    if (stopId.equals(id)) {
                        break;
                    }
                    if (id.equals(startId.trim())) {
                        found = false;
                    }
                }
            }
            System.out.println("从id为" + startId + "，第" + wordCount + "个开始分析");
            wordCount = 0;

            while ((line = br.readLine()) != null && wordCount < batchLimit) {

                String[] values = line.split(",");
                if (values.length >= 2) {
                    String id = values[0].trim().replaceAll("^\"|\"$", "");
                    if (!stopId.equals(id)) {
                        //content += id+"."+keyword + ",";
                        //content += keyword + ",";
                        content += id+",";
                    } else {
                        break;
                    }
                    idto = id;
                }
                wordCount++;

                if (wordCount % batchLimit == 0) {
                    System.out.println("此时id为" + idto + "，分析到了第" + wordCount + "个");
                    if (idto1==null||idto1.equals("")){
                        idto1 = startId;
                    }
                    System.out.println("这里读到的是"+content);
                    categorizationOperations(content,filePath);
                    batchContent.setLength(0);
                    content = "";
                    wordCount = 0;
                    idto1 = idto;
                }

            }
            // 处理最后一个批次（如果有剩余的词）
            if (!content.isEmpty()) {
                if (idto1==null||idto1.equals("")){
                    idto1 = startId;
                }
                System.out.println("这里读到的是"+content);
                categorizationOperations(content,filePath);

                batchContent.setLength(0);
                content = "";
                wordCount = 0;
                idto1 = idto;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return "任务完成！";
    }


    public static void demo1(String startId, String stopId){
        try {

            String words = PickThorn("D:\\OfFile\\ai_node_candidate_1031.csv", startId, 100, stopId);
            System.out.println(words);
        } catch (Exception e) {

        }
    }

    @Test
    public void ttt(){
        String input = "现有45类：\n" +
                "“生物,人工制品,名字,行为,形象,种类,言语,参数,事件,程度,状态 \n" +
                ",关系,版本,医学,商务,介连词,软件,硬件,习俗,物理,味道,网站 \n" +
                ",企业,气候,团体,规定,院校,地点,娱乐,思想 \n" +
                ",食物,文艺,历史,宗教,颜色,符号,组织部位,功能 \n" +
                ",声音语气,时间,建筑,身份,化学,理论技法,自然物”\n" +
                "要求：\n" +
                "(1) 依据以上类别，对下面的name进行分类，type的值即为该类型；\n" +
                "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就为“其它”;\n" +
                "(3) 如果该词不仅不属于任何一个类别，且是错误的词语(生造词)，则将该节点的type就为“生造词”;\n" +
                "(4) 输出格式为：“{name属于type, name属于type}” 例如：{小鸡属于生物,麻辣香锅属于食物};\n" +
                "请给下面的数据进行分类：  \n" +
                " “拏风,眔,短绌,四徯,霂雾,八牕,忧愤,髴兮,苛烈,嘈闲,荡飏,穹礴,邹媖,古郯,灿昀,诚惶,屡戒,偓体,视阈,渤澥,檄愈,浪蘂,继晷,婵,瑛,鸿毳,柳瀬,莋脚,隳肝,咯定,握杼,婸姬,梦婕,杷罗,贲临,挨踢,笹原,礑溪,広,濩泽,发愤,漏虀,若辱,枯涸,袒裎,楡,三襕,蒲缥,诩,镒韵,粪甾,献杵,成懴,大睄,祾,堑,锦厪,樯桅,煜,尕,浰,溧,嵋,簠,祯,琊,闺,俟奋,东矣,雩溪,农孵,防蔽,杵状,向阪,益溆,小嶋,铝业,撅坑,撅堑,沢木,黟山,煌熇,晶穂,黥武,含糗,雪浥,阪,雨浥,髌,讪牙,锍潋,埒才,扁鯺,美嵨,绋尘,辋穿,天瞾,吴垭,西赆,泺黄,”\n" +
                " 注意：完整的给每一个词分类，无需解释，无需其它提示词。";
        System.out.println(chat3(input));
    }


    public static String chat3(String content) {
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
        chatCompletionRequest.setModel("glm-3-turbo");
        //chatCompletionRequest.setModel("ERNIE-Speed-128K");
        //chatCompletionRequest.setModel("v3.5");

        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = null;
        String retry = null;
            try {
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
            }catch (Exception e){

        }
        return retry;
    }




    /**
     * 通义千问
     * @param content
     * @return
     */
    public static String chat(String content,String startId,String stopId) {
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
        //chatCompletionRequest.setModel("ERNIE-Speed-8K");
        //chatCompletionRequest.setModel("v3.5");
        //chatCompletionRequest.setModel("glm-3-turbo");
        //chatCompletionRequest.setModel("glm-4");
        chatCompletionRequest.setModel("qwen-plus");

        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = null;
        boolean isf = true;
        String retry = null;
        Integer count = 0;
        while (isf){
            try {
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                if (retry!=null){
                    isf = false;
                }
                count = 0;
            }catch (Exception e){
                chatCompletionRequest.setModel("moonshot-v1-128k");
                System.out.println("访问出错了，"+"起："+startId+"停："+stopId);
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                if (retry!=null){
                    isf = false;
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
    public static String chat1(String content,String startId,String stopId) {
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
        chatCompletionRequest.setModel("moonshot-v1-128k");

        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = null;
        boolean isf = true;
        String retry = null;
        Integer count = 0;
        while (isf){
            try {
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                if (retry!=null){
                    isf = false;
                }
                count = 0;
            }catch (Exception e){
                chatCompletionRequest.setModel("qwen-plus");
                System.out.println("访问出错了，"+"起："+startId+"停："+stopId);
                result = completionsService.completions(chatCompletionRequest);
                retry = result.getChoices().get(0).getMessage().getContent();
                if (retry!=null){
                    isf = false;
                }
            }
        }

        return retry;
    }

}
