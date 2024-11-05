package ai.workerDispose.utils;

import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConversionTypeUtils {

    /*
     * 将字符串id对id转换为Map
     */
    public static Map<String,String> stringTomap(String input){

        Map<String, String> map = new HashMap<>();
        input = input.substring(1, input.length() - 1);
        Pattern pattern = Pattern.compile("(\\d+)=(\\d+)");
        Matcher matcher = pattern.matcher(input);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            map.put(key, value);
        }

        System.out.println(map);
        return map;
    }

    public static String extractContentWithinBraces(String input) {
        input = input.replaceAll("\\s", "");
        input = input.replace("，", ",");
        if (input == null || !input.contains("{") || !input.contains("}")) {
            return null;
        }
        int startIndex = input.indexOf("{");
        int endIndex = input.indexOf("}", startIndex);
        return input.substring(startIndex + 1, endIndex);
    }

    public static Map<String, String> convertStringToMap(String input) {
        Map<String, String> map = new HashMap<>();
        String[] pairs = input.split(",");
        for (String pair : pairs) {
            String[] keyValue = pair.split("属于");
            if (keyValue.length > 1){
                map.put(keyValue[0], keyValue[1]);
            }
        }
        return map;
    }

    public static Map<String, String> getKeyByClassId(Map<String, String> map,Map<String, String> idANDValueMap) {

        Map<String,String> result = new HashMap<>();
        Map<String, String> categoryMap = new HashMap<>();
        categoryMap.put("生物", "1");
        categoryMap.put("人工制品","2");
        categoryMap.put("名字", "3");
        categoryMap.put("行为", "4");
        categoryMap.put("形象", "5");
        categoryMap.put("种类", "6");
        categoryMap.put("言语", "7");
        categoryMap.put("参数", "8");
        categoryMap.put("事件", "9");
        categoryMap.put("程度", "10");
        categoryMap.put("状态", "11");
        categoryMap.put("关系", "12");
        categoryMap.put("版本", "13");
        categoryMap.put("医学", "14");
        categoryMap.put("商务", "15");
        categoryMap.put("介连词", "16");
        categoryMap.put("软件", "17");
        categoryMap.put("硬件", "18");
        categoryMap.put("习俗", "19");
        categoryMap.put("物理", "20");
        categoryMap.put("味道", "21");
        categoryMap.put("网站", "22");
        categoryMap.put("企业", "23");
        categoryMap.put("气候", "24");
        categoryMap.put("团体", "25");
        categoryMap.put("规定", "26");
        categoryMap.put("院校", "27");
        categoryMap.put("地点", "28");
        categoryMap.put("娱乐", "29");
        categoryMap.put("思想", "30");
        categoryMap.put("食物", "31");
        categoryMap.put("文艺", "32");
        categoryMap.put("历史", "33");
        categoryMap.put("宗教", "34");
        categoryMap.put("颜色", "35");
        categoryMap.put("符号", "36");
        categoryMap.put("组织部位", "37");
        categoryMap.put("功能", "38");
        categoryMap.put("声音语气", "39");
        categoryMap.put("时间", "40");
        categoryMap.put("建筑", "41");
        categoryMap.put("身份", "42");
        categoryMap.put("化学", "43");
        categoryMap.put("理论技法", "44");
        categoryMap.put("自然物", "45");
        categoryMap.put("其它", "0");
        categoryMap.put("生造词", "-2");
        for (String key : map.keySet()) {
            String value = categoryMap.get(map.get(key.trim()));
            String key1 =  idANDValueMap.get(key.trim());
            if (value != null && !value.trim().isEmpty()
               && key1 != null&& !key1.trim().isEmpty()){
                result.put(key1,value);
            }
        }
        System.out.println("转换后的结果："+result);
        return result;
    }

    public static <K, V> Map<K, V> getIntersection(Map<K, V> map1, Map<K, V> map2) {
        Map<K, V> intersection = new HashMap<>();
        for (Map.Entry<K, V> entry : map1.entrySet()) {
            K key = entry.getKey();
            V value = entry.getValue();
            if (map2.containsKey(key) && value.equals(map2.get(key))) {
                intersection.put(key, value);
            }
        }
        System.out.println("交集：" + intersection);
        return intersection;
    }


    public static void main(String[] args) {
//        String input = "{2292=3, 2325=3, 2330=6, 2383=5, 2390=11, 2422=5, 2515=5, 2586=4}";
//        ConversionTypeUtils ctu=new ConversionTypeUtils();
//        ctu.stringTomap(input);
        String input = "{隶属属于关系, 经纪属于商务, 区号属于参数, 区域属于地点, 标准属于规定, 二级属于参数, 售后属于商务, 官方属于身份, 特殊属于参数, 小组属于团体, 体制属于规定, 分支属于关系, 价值属于参数, 专业属于参数, 不良属于状态, 反应属于行为, 高等属于参数, 配套属于参数, 景观属于自然物, 简体属于人工制品, 清晰属于参数, 体系属于规定, 环境属于自然物, 最佳属于参数, 湖泊属于自然物, 无线属于物理, 直属属于关系, 职务属于身份, 少数属于参数, 辖区属于地点, 市场属于商务, 名胜属于地点, 古迹属于历史, 中心属于地点, 天干属于自然物, 高速属于物理, 常用属于参数, 性质属于参数, 现状属于状态, 不锈属于人工制品, 重要属于参数, 残疾属于状态, 限界属于参数, 闽南属于地点, 微量属于参数, 普通属于参数, 住宅属于地点, 智能属于技术, 快捷属于参数, 省级属于规定, 感光属于物理, 风光属于自然物, 部分属于参数, 实用属于参数, 场所属于地点, 高级属于参数, 导电属于物理, 公共属于物理, 社区属于地点, 奇偶属于数学, 相关属于关系, 技能属于行为, 耐热属于物理, 强度属于物理, 导热属于物理, 公益属于行为, 乐属于娱乐, 习惯属于行为, 权威属于行为, 水星属于自然物, 附属属于关系, 医院属于医学, 手动属于行为, 汉化属于人工制品, 异界属于虚构, 大陆属于地点, 虚拟属于人工制品, 官渡属于历史, 神圣属于宗教, 邵阳属于地点, 热血属于行为, 南山属于地点, 外用属于行为, 湖南属于地点, 宝鸡属于地点, 防盗属于物理, 家常属于食物, 赣州属于地点, 南方属于地理, 河县属于地点, 有限属于规定, 街道属于地点, 免费属于参数, 北京属于地点, 威尔士属于地点, 鄂州属于地点, 第八属于参数, 都市属于地点, 苏丹属于地点, 强壮属于状态} \n" +
                "请注意，\"异界\"被归类为虚构，因为根据提供的类别列表中没有更合适的分类。如果您有其他特定的分类需求，请告知。";
        String content = extractContentWithinBraces(input);
        System.out.println(content);
        Map<String, String> map = convertStringToMap(content);
        System.out.println(map);

    }
    @Test
    public void test(){
        String a = "现有45类：\n" +
                "“1.生物 2.人工制品  3.名字 4.行为 5.形象 6.种类 7.言语 8.参数 9.事件 10.程度 11.状态 \n" +
                "12.关系 13.版本 14.医学 15.商务 16.介连词 17.软件 18.硬件 19.习俗 20.物理 21.味道 22.网站 \n" +
                "23.企业 24.气候 25.团体  26.规定  27.院校 28.地点  29.娱乐  30.思想 \n" +
                "31.食物 32.文艺 33.历史  34.宗教  35.颜色 36.符号  37.组织部位 38.功能 \n" +
                "39.声音语气 40.时间 41.建筑 42.身份 43.化学 44.理论技法 45.自然物 ”\n" ;
        System.out.println(a);
    }

}
