package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;
import ai.workerDispose.dao.AiZindexUserDao;
import ai.workerDispose.pojo.ClassifyTxt;
import ai.workerDispose.pojo.General;
import ai.workerDispose.pojo.General2;
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
                        if (map1.size()>=10){
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
                        if (map2.size()>=10){
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
                "(5) “大括号只能出现一个” ;\n" +
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
                "(5) “大括号只能出现一个” ;\n" +
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
                "(5) “大括号只能出现一个” ;\n" +
                "请给完整的下面的数据进行分类： \033[0;94m \n “"};
        String[] promptTail = {"”\n \033[0m 注意：无需解释，无需其它提示词，只要完整的输出“{name属于type, name属于type}” 的数据即可。"};

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
                "(5) “大括号只能出现一个” ;\n" +
                "请完整的给下面的数据进行分类： \033[0;94m \n “"};
        String[] promptTail = {"”\n \033[0m 注意：无需解释，无需其它提示词，只要完整的输出“{name属于type, name属于type}” 的数据即可。"};

        String content1 = promptHead[0];
        content1 += content + promptTail[0];

        System.out.println(content1);

        //String result = content1; //chat(content1);
        String result = chat(content1,"0","0");
        System.out.println("通义千问大模型输出：" + result);
        return result;
    }

    private static  Integer cccc =0;
    @Test
    public void tttt(){
        String nids ="32048105, 22233426, 29967306, 44462754, 41036916, 34567173, 28148129, 16661747, 35717168, 19394353, 41044581, 34211350, 27963317, 28941734, 39681663, 30842354, 17417507, 22889842, 45412507, 17433939, 30789558, 30829490, 28128708, 28332508, 41218565, 24121103, 29966774, 30101832, 31333210, 24760291, 18917821, 38830285, 30046548, 41649892, 19362183, 30291310, 16207441, 42938920, 27971843, 16230492, 30972275, 23236558, 21620216, 16217669, 46448758, 27999000, 15678549, 17722831, 29356926, 20761578, 17419223, 16219167, 26825886, 20754483, 39204677, 16217056, 21080153, 33918749, 19402289, 16214508, 22670405, 42953106, 28389525, 28902557, 32667351, 16217053, 39747445, 22435428, 22638698, 16217545, 30094987, 19432545, 31362704, 17428561, 27925703, 29147847, 29386316, 34820994, 16197432, 16765227, 39580559, 29079149, 29967376, 23415993, 44675957, 16217386, 18591430, 16217367, 36881875, 28343383, 39675322, 41146270, 16336136, 30040630, 39246249";

        String nids1= "17807628, 33661442, 16216305, 30220746, 31181269, 41151101, 27146630, 20764966, 30321108, 37356065, 41366123, 39238216, 45036209, 34400787, 41493094, 17416492, 22303089, 30036463, 15675593, 29934062, 28383640, 29884899, 16330965, 40673859, 44747417, 42754742, 40886864, 41357896, 19464552, 24889658, 27277794, 30957979, 35246684, 20885880, 28043757, 15681703, 17418561, 39797270, 28223972, 30886300, 22051101, 38969887, 21197069, 24086846, 29966744, 27228620, 28256764, 30099870, 21434652, 20744547, 31109564, 42699489, 28019141, 31979926, 29966768, 36178514, 36670126, 34661021, 22276572, 15675519, 20906413, 27349300, 34955904, 31025512, 20734351, 35019429, 30984556, 17648059, 28154222, 30327045, 24721835, 20771302, 31275267, 30677310, 34552574, 42758781, 31940882, 40016540, 41597818, 23857255, 38980465, 32921822, 28197019, 16232886, 41626424, 22767627, 28133618, 20742238, 20818009, 22548541, 21688375, 30683222, 34139020, 20775098, 28053536, 30503006, 35470226, 34820994, 17494172, 30920783, 39629765, 16214377, 33840027, 16200021, 17416363, 33964965, 27187258, 28237834, 31053948, 20752532, 33452119, 16214364, 36146068, 39697285, 35832824, 44675914, 33948643, 22668153, 19086082, 30036984, 27954154, 21668635, 38639642, 28091383, 15706757, 33201046, 16224920, 20812618, 21263155, 40824857, 45338827, 27988944, 32830351, 29936565, 41797885, 32197482, 19454895, 18942898, 29995868, 44034110, 24519645, 28941074, 29373292, 40591557, 18588596, 19430296, 16210501, 29967228, 41830600, 17419185, 22207466, 34070717, 41998396, 28355343, 27378528, 18467805, 30866195, 40583331, 44222570, 18918377, 19368942, 20765687, 43192387, 23641017, 34191579, 21891992, 28392296, 31017786, 42653805, 16218654, 44871745, 39246008, 44752976, 18738147, 45869499, 29891265, 29991622, 30325468, 40804729, 42953106, 30921463, 16763868, 27060927, 22219386, 16214983, 30989032, 42105264, 20822653, 32766643, 28080875, 21504513, 43373023, 31085193, 17463913, 42936806, 30323390, 40976882, 37894598, 29768272, 43680003, 30327396, 31033971, 33905064, 21635839, 18784933, 44298603, 29784591, 45070694, 31351315, 30321182, 43479367, 16216867, 21115543, 28142200, 20902633, 30327321, 41099693, 30321198, 22125235, 44032374, 44632387, 21981858, 30169633, 20818639, 29995569, 30462510, 20821307, 30842332, 28280227, 19567908, 42061470, 43790012, 42524334, 26707365, 40441425, 46456456, 40388133, 20585833, 20870500, 28302835, 24517908, 42065635, 19430732, 30238122, 39306814, 22517053, 35743306, 18269543, 42907389, 28190170, 41625091, 28024274, 29963698, 17896812, 43148816, 21628355, 30319943, 20770231, 24292862, 30324033, 16217179, 16204871, 29926778, 35751573, 41621199, 21370366, 21519858, 22369771, 30328076, 38465166, 27225432, 28243303, 20774384, 30897435, 30997781, 21228944, 28151158, 28028238, 28052805, 34191098, 37584590, 30096700, 22199720, 21634122, 22305884, 31835373, 23243886, 31921384, 32451835, 16217568, 28179642, 20870182, 20818983, 20765726, 20818965, 21560417, 28132486, 44781444, 33133773, 21392496, 39735071, 17945688, 16205245, 36968294, 16215457, 29967519, 16197038, 19012677, 30321830, 18078873, 16213370, 24737003, 34750336, 39006155, 40130553, 40052675, 38254581, 30235673, 39323526, 34555883, 28361802, 28243887, 21026633, 27996090, 18951995, 27948980, 30232557, 44881034, 16215770, 28178307, 31274978, 28194798, 43889881, 32628660, 30322562, 36613216, 44928251, 42773717, 16215725, 27987905, 32745354, 33563769, 27572190, 30310320, 21338051, 30367556, 17442712, 38970565, 30943041, 18038668, 30803831, 40496297, 29017967, 30850827, 20926422, 27463519, 28278609, 29910839, 44481625, 30324430, 31055579, 18726430, 19416608, 28425913, 23955042, 22140488, 30322389, 28038792, 34796859, 27979398, 21413498, 16218058, 41975263, 19390029, 24518170, 30820024, 30035644, 30891680, 29896369, 30842543, 38950188, 29967944, 39669205, 28218915, 43296025, 30320193, 29210146, 28063245, 44461327, 41063873, 31667803, 21933813, 26514984, 21945999, 21845636, 44461421, 44320113, 32038406, 30037560, 24759936, 21552823, 33107462";

        //FAQ
        String nids2= "\"41865854, 30323151, 17418511, 28027308, 30220746, 32475617, 35609105, 30296542, 30878146, 27146630, 29966801, 35191347, 30779878, 38810223, 27263467, 21952802, 17830293, 46367301, 40948474, 33524080, 43647509, 28455205, 34656904, 43727363, 28352811, 39727809, 22026739, 16345181, 19403149, 27953410, 16230492, 30972275, 21336552, 28311809, 19984865, 30320918, 30898493, 33970920, 34656997, 40215199, 19468759, 43256723, 41644919, 29444304, 40334198, 41575267, 33968925, 32968918, 17600554, 31852719, 16218553, 28063994, 29935775, 19400816, 15718795, 42505190, 15724958, 22538456, 21172450, 19421324, 22397072, 23249063, 29956108, 28346486, 20816119, 44931939, 21426324, 29956123, 29759507, 30003230, 30732291, 31557691, 32944155, 30326841, 24580979, 17785608, 18938666, 15676131, 15751932, 20730665, 28517285, 16448237, 30008275, 40810612, 24705894, 28720040, 20081419, 16224973, 30854038, 20820842, 38639642, 18176842, 28312528, 44419264, 43778299, 28166952, 31046481, 28003120, 16216680, 15759957, 16233042, 20816791, 17714100, 29059939, 16202247, 19401688, 19462716, 43704716, 42953106, 39676227, 19417610, 30460667, 30569214, 22305401, 21545597, 26763949, 27218599, 33835325, 28332684, 29909640, 30325390, 38822192, 20761176, 28334807, 28330714, 19419718, 16214918, 38955517, 30478873, 44298603, 20740849, 33968597, 19981035, 28940895, 44761409, 30169633, 16216834, 42593924, 31202803, 16217287, 26690976, 29967856, 22850840, 29963662, 30039451, 40367665, 45905642, 19459440, 41184809, 33893979, 24141103, 32198068, 40302087, 20753759, 42845948, 21108021, 28061140, 34313843, 17896812, 24292862, 28155168, 23807433, 44461676, 31046935, 43462210, 24552878, 29512991, 18429383, 15688707, 32662815, 30328121, 21853620, 22078891, 20813251, 36429604, 24167522, 26594435, 40499057, 29967585, 44871553, 30479614, 24564795, 32810130, 19393623, 34949018, 24921325, 19168438, 16215394, 20769962, 30196823, 42229540, 23651530, 38219749, 22037757, 40816606, 30929978, 34082809, 15740169, 17417460, 29955124, 40176754, 31049666, 44881034, 40070235, 16621246, 34221133, 32364475, 30101394, 44461311, 44461304, 28008394, 17440635, 28186584, 31280988, 28344102, 38970565, 19392447, 21295076, 32307030, 20760573, 28299108, 27979640, 19275774, 28188535, 31301380, 30850827, 36418804, 30105387, 33866982, 34522349, 30320418, 17416184, 40707221, 40357017, 20760105, 39238976, 30314199, 39865724, 16486374, 18935300, 27803279, 27965069, 15675359, 35190057, 21614202, 27960979, 34411831, 45421963, 30891752, 43808221, 16246718, 40985876, 39085356, 19848875, 20117173, 18882216, 21427916, 17796764, 30035550, 30742089, 19416704, 40285658, 16219955, 44717415, 17575641, 30219793, 18919111, 16111389, 38481315, 30094909, 31823386";

        //汇率智能体
        String nids3= "28307886,16216821,41591934,20888377,24668528,16231166,28252582,20813623,28135847,31536618,29366734,17769246,19395380,28941740,17577750,17418517,41072236,27963317,28941734,19435785,27947400,27255738,20823319,22744432,27954565,19448089,34788899,38399613,30323191,42051259,20919556,20816131,40484905,18918753,18801499,22861589,39797270,31727022,20759415,40694304,27570165,29855637,40927780,31420809,19451263,22290700,21495582,20870500,16217258,18432836,20871008,30996359,30036395,33720421,28199366,28295109,19419483,20816203,31282597,28024274,30321074,15681681,41535230,28315450,31496046,16201336,21123533,29933400,17014674,30075743,28613923,36005042,38859463,28130101,33823900,24707041,39470787,28003120,40868593,26332464,28648720,16711255,28363545,28127495,35489463,40021193,19430789,32518493,35019429,21370366,22175215,29367679,20813310,36185325,41412785,30305543,41498807,21405078,46308371,20823022,20835816,21273491,39749257,27929458,33926879,19444213,27769721,20818396,20812254,19455436,42607200,16201244,20744151,28350812,34089701,40215178,28299098,31418661,33784067,41255285,27052190,30325450,33367800,21177419,37284662,24638567,21642334,20883496,41136997,38127454,30451406,19406391,33952550,22290037,28258445,36680461,27060927,34013476,33905952,28185223,29934305,19467781,20775953,21042285,22217825,33026244,33833779,29991664,21642353,39734629,44044702,33956677,19411554,40849700,41102648,24196640,30934666,21195287,21588508,32574631,33226912,29672601,31154310,31685251,21747754,20742238,39457592,20818009,23264262,30961852,33338009,33996131,15735702,20750426,28337368,19454018,16332181,24707095,28172509,28054237,27990745,24767498,30992555,24088091,16197519,19457104,41379084,41441539,38950188,29070907,40293863,28197930,20816058,44237103,33595280,28088381,24737003,22811336,36010416,28076091,29901906,16214377,31601276,33878938,16197463,32059464,20816025,31053948,17494188,44101383,28212230,21021420,24161500,34883003,20822671,33915319,19466397,21635839,44111201,15720232,27525740,21845639,34032084,16216867,41441197,30320153,39697285,40111550,32350769,20885214,26145903,19409100,29175897,34659319,19396815,35029500,44461900,20903637,34549219,20752589,31547923,19436763,32406555,21327548,30465065,15709973,42891636";

        String filePath = "D:\\OfFile\\nid数据\\test小信的.csv";
        String filePath1 = "D:\\OfFile\\nid数据\\test股票的.csv";
        String filePath2 = "D:\\OfFile\\nid数据\\testFAQ的.csv";
        String filePath3 = "D:\\OfFile\\nid数据\\汇率智能体的分类.csv";

        categorizationOperations(nids3,filePath3);
    }

    /**
     *ai_node_candidate分类的方法
     */
    @Test
    public void qianyi_ai_node_candidate(){
      String filePath = "D:\\OfFile\\test11111.csv";
      Integer batchLimit = 20;
      Integer minNodeId = 1;
      Integer maxNodeId = 419268;
      Integer jishu = 0;
      while (aiZindexUserDao.getQianyiAiNodeCandidateCount(minNodeId,maxNodeId)) {
          jishu++;
        List<General2> list = aiZindexUserDao.getQianyiAiNodeCandidate(batchLimit,minNodeId,maxNodeId);
          //System.out.println("第" + jishu + "次分类，当前第一条nid为：" + minNodeId);
          minNodeId = list.get(list.size() - 1).getNodeId() + 1;
          System.out.println("第" + jishu + "次分类，当前最后一条nid为：" + maxNodeId);
          String result = list.stream()
                  .map(general -> String.valueOf(general.getNid()))
                  .collect(Collectors.joining(","));
          categorizationOperations(result,filePath);
      }
    }

    public static  void categorizationOperations(String nids,String filePath){
        //String filePath = "D:\\OfFile\\test11111.csv";
        //String nids = "15674579,15674609,15674669,15674785,15674795,15674821,15674853,15675051,15675083,15675107,15675183,15675239,15675283";
        Integer chunkSize = 50;
        String[] nidsArray = ConversionTypeUtils.splitStringByChunks(nids, chunkSize);
        Integer jishu = nidsArray.length;
        for (String nidStr : nidsArray) {
            jishu-=10;
            List<ClassifyTxt> nodeTxtList = aiZindexUserDao.getClassifyTxt(nidStr);
            String result = nodeTxtList.stream()
                    .map(ClassifyTxt::getNode_text)
                    .collect(Collectors.joining(","));
            System.out.println(cccc+=nodeTxtList.size());

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

            List<WriteClassifyCvs> intersection = map2;
            List<WriteClassifyCvs> list2 = map1;

            for (WriteClassifyCvs c1 : intersection) {
                for (WriteClassifyCvs c2 : list2) {
                    if (c1.getId().equals(c2.getId())){
                        if (c1.getType()!=null&& c2.getType()!=null){
                            //type有数据
                            if (!c1.getType().equals(c2.getType())){
                                //type不同
                                c1.setType("0");
                            }
                        }else {
                            //type没有数据
                            c1.setType("0");
                        }

                    }
                }
            }

            writeIds.writeObjectsToCsv(intersection, filePath);
        }
    }
@Test
    public void  sss(){
    String parse = ConversionTypeUtils.extractContentWithinBraces("{挂失属于行为,条件三属于规定,交易记录属于事件,证明材料属于人工制品,业务办理属于行为,新银属于企业,银行账户属于种类,能力的属于状态,的方式属于介连词,首先要明确属于思想}");
    if (parse != null && !parse.trim().isEmpty()){
        Map<String,String> Maps = ConversionTypeUtils.convertStringToMap(parse);

        List<ClassifyTxt> nodeTxtList = new ArrayList<>();
        nodeTxtList.add(new ClassifyTxt(16661747, 187129, 187129, "挂失", "挂失", 75));
        nodeTxtList.add(new ClassifyTxt(19394353, 960371, 960371, "条件三", "条件三", 75));
        nodeTxtList.add(new ClassifyTxt(22233426, 1881214, 1881214, "交易记录", "交易记录", 75));
        nodeTxtList.add(new ClassifyTxt(28148129, 3369944, 3369944, "证明材料", "证明材料", 75));
        nodeTxtList.add(new ClassifyTxt(29967306, 3943212, 3943212, "业务办理", "业务办理", 75));
        nodeTxtList.add(new ClassifyTxt(32048105, 4503564, 4503564, "新银", "新银", 75));
        nodeTxtList.add(new ClassifyTxt(34567173, 5291153, 5291153, "银行账户", "银行账户", 75));
        nodeTxtList.add(new ClassifyTxt(35717168, 5641964, 5641964, "能力的", "能力的", 75));
        nodeTxtList.add(new ClassifyTxt(41036916, 7133903, 7133903, "的方式", "的方式", 75));
        nodeTxtList.add(new ClassifyTxt(44462754, 8083470, 8083470, "首先要明确", "首先要明确", 75));
        System.out.println(ConversionTypeUtils.getKeyByClassId(Maps, nodeTxtList).size());
        //return writeClassifyCvsList;
    }
    //System.out.println(parse);
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
                    .map(obj1 -> {
                        // 如果 map1 中的 obj1 与 map2 中有匹配的对象，合并它们，否则保留 obj1
                        return finalMap.stream()
                                .filter(obj2 -> obj2.getId() != null &&
                                        obj2.getType() != null &&
                                        obj1.getId() != null &&
                                        obj1.getType() != null &&
                                        obj1.getId().equals(obj2.getId()) &&
                                        obj1.getType().equals(obj2.getType()))
                                .findFirst()
                                .orElse(obj1); // 如果没有找到匹配的元素，返回原始 obj1
                    })
                    .collect(Collectors.toList());

            writeIds.writeObjectsToCsv(intersection, filePath);
        }



    }

    @Test
    public  void demo2Start(){
        demo2("D:\\OfFile\\nid数据\\nid数据\\FAQ挖掘.csv", "41865854", 10, "31823386","D:\\OfFile\\分类后的数据(FAQ挖掘22).csv");
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
                "(5) “大括号只能出现一个” ;\n" +
                "请给下面的数据进行分类：  \n" +
                " “拏风,眔,短绌,四徯,霂雾,八牕,忧愤,髴兮,苛烈,嘈闲,荡飏,穹礴,邹媖,古郯,灿昀,诚惶,屡戒,偓体,视阈,渤澥,檄愈,浪蘂,继晷,婵,瑛,鸿毳,柳瀬,莋脚,隳肝,咯定,握杼,婸姬,梦婕,杷罗,贲临,挨踢,笹原,礑溪,広,濩泽,发愤,漏虀,若辱,枯涸,袒裎,楡,三襕,蒲缥,诩,镒韵,粪甾,献杵,成懴,大睄,祾,堑,锦厪,樯桅,煜,尕,浰,溧,嵋,簠,祯,琊,闺,俟奋,东矣,雩溪,农孵,防蔽,杵状,向阪,益溆,小嶋,铝业,撅坑,撅堑,沢木,黟山,煌熇,晶穂,黥武,含糗,雪浥,阪,雨浥,髌,讪牙,锍潋,埒才,扁鯺,美嵨,绋尘,辋穿,天瞾,吴垭,西赆,泺黄,”\n" +
                " 注意：完整的给每一个词分类，无需解释，无需其它提示词。";
        System.out.println(chat3(input));
    }

//用来测试是否可行的---
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
        //chatCompletionRequest.setModel("Qwen-14b");
        chatCompletionRequest.setModel("Baichuan-13b");
        //chatCompletionRequest.setModel("glm-3-turbo");
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
        chatCompletionRequest.setModel("Qwen-14b");
        //chatCompletionRequest.setModel("moonshot-v1-8k");
        // Create an instance of CompletionsService
        //chatCompletionRequest.setModel("ERNIE-Speed-128K");
        //chatCompletionRequest.setModel("ERNIE-Speed-8K");
        //chatCompletionRequest.setModel("v3.5");
        //chatCompletionRequest.setModel("glm-3-turbo");
        //chatCompletionRequest.setModel("glm-4");
        //chatCompletionRequest.setModel("qwen-plus");

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
                chatCompletionRequest.setModel("Baichuan-13b");
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
     * 百川
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
        //chatCompletionRequest.setModel("moonshot-v1-128k");
        chatCompletionRequest.setModel("Baichuan-13b");
        //chatCompletionRequest.setModel("Qwen-14b");


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
                //chatCompletionRequest.setModel("qwen-plus");
                chatCompletionRequest.setModel("Qwen-14b");
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
