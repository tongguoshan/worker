package ai.workerDispose.utils;

import ai.workerDispose.pojo.WriteCvs;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class WriteIds {
    public static String do1(String keywords,String csvFilePath,String csvFilePath1) {
        String result = "写入失败！";
        try {
            List<WriteCvs> keywordList = findIdsByKeywords(keywords, csvFilePath);
            result =  writerCsv(csvFilePath1, keywordList);
        } catch (IOException e) {
            System.out.println(e);
        }
        return result;
    }

    public static String writerCsv(String csvFilePath, List<WriteCvs> keywordList){
        //new InputStreamReader(new FileInputStream(csvFilePath), Charset.forName("GBK"))
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFilePath,true))) {
            if (!hasHeader(csvFilePath)) {
                bw.write("id");
                bw.write(",");
                bw.write("name");
                bw.write(",");
                bw.write("comments");
//                bw.write(",");
//                bw.write("dimension");
                bw.newLine();
            }
            // 写入每个关键词对象的数据
            for (WriteCvs keywordObject : keywordList) {
                //System.out.println(keywordObject.getId());
                bw.write(keywordObject.getId());
                bw.write(",");
                bw.write(keywordObject.getName());
                bw.write(",");
                bw.write(keywordObject.getComments());
//                bw.write(",");
//                bw.write(keywordObject.getDimension());
                bw.newLine(); // 换行，开始写入下一个关键词对象
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "写入成功！";
    }


    private static List<WriteCvs> findIdsByKeywords(String keywords, String csvFilePath) throws IOException {
        List<String> ids = new ArrayList<>();
        List<WriteCvs> keywordList = new ArrayList<>();
        String[] keywordArray = keywords.split(",");

        Set<String> keywordSet = new HashSet<>(Arrays.asList(keywordArray));

        keywordArray = keywordSet.toArray(new String[0]);

        //try (BufferedReader br = new BufferedReader(new FileReader(csvFilePath))) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(csvFilePath), Charset.forName("UTF-8")))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(","); // 假设CSV是用逗号分隔的
                if (values.length >= 2) { // 确保有足够的列
                    String id = values[0].trim(); // 假设ID在第一列
                    String keyword = values[1].trim().replaceAll("^\"|\"$", "");
                    String comments ="";
                    if (values.length > 2){
                        comments = values[2].trim().replaceAll("^\"|\"$", "");
                    }
                    //String dimension = values[3].trim().replaceAll("^\"|\"$", "");
                    //System.out.println("keyword: " + keyword);

                    for (String k : keywordArray) {
                        //System.out.println("比对：k: " + k);
                        //空格转下划线
                        String newkeyword = keyword.replaceAll("\\s", "_");
                        String newk = k.trim().replaceAll("\\s", "_");

                        if (newkeyword.equals(newk)) {
                            ids.add(id);
                            WriteCvs writeCvs = new WriteCvs();
                            writeCvs.setId(id);
                            writeCvs.setName(k.trim());
                            writeCvs.setComments(comments.trim());
//                            writeCvs.setDimension(dimension.trim());
                            keywordList.add(writeCvs);
                            break; // 如果找到了匹配的关键词，跳出循环
                        }
                    }
                }
            }
        }

        return keywordList;
    }
    private static boolean hasHeader(String filePath) {
        boolean hasHeader = false;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), Charset.forName("UTF-8")))) {
            //try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line = br.readLine();
            if (line != null && !line.isEmpty()) {
                String[] columns = line.split(",");
                if (columns.length == 3) {
                    hasHeader = true;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return hasHeader;
    }
    private static void writeIdsToFile(List<String> ids, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            for (String id : ids) {
                writer.write(id.replaceAll("^\"|\"$", "") + ","); // 写入ID，并添加换行符
            }
        }
    }

    public static String readFileToString(String filePath) throws IOException {
        StringBuilder fileContentBuilder = new StringBuilder();
        BufferedReader reader = null;

        try {
            //new FileReader(filePath)
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), Charset.forName("UTF-8")));
            String line;
            while ((line = reader.readLine()) != null) {
                fileContentBuilder.append(line);
                fileContentBuilder.append(System.lineSeparator()); // 添加换行符
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }

        return fileContentBuilder.toString();
    }

    public static void main(String[] args) {
        System.out.println("Hello world!");
    }
}
