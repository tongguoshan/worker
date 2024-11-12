package ai.workerDispose.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class CsvGenerator {
    public static void main(String[] args) {
//        String inputFilePath = "E:\\file\\TempFiles\\nid数据\\小信智能体.csv";
//        String inputFilePath = "E:\\file\\TempFiles\\nid数据\\股票智能体.csv";
//        String inputFilePath = "E:\\file\\TempFiles\\nid数据\\FAQ挖掘.csv";
        String inputFilePath = "E:\\file\\TempFiles\\nid数据\\小信智能体.csv";

        // 自动生成 outputFilePath，将文件名改为 output_ + 原文件名
        Path inputPath = Paths.get(inputFilePath);
        String baseName = inputPath.getFileName().toString().replace(".csv", "");
        String outputFilePath = inputPath.getParent().resolve("output_" + baseName + ".csv").toString();

        List<String[]> entries = new ArrayList<>();

        // 使用 GB2312 编码读取原始 CSV 文件
        System.out.println("开始读取文件: " + inputFilePath);
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFilePath), "GB2312"))) {
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println("读取行: " + line);  // 打印每一行内容
                String[] parts = line.split(",");  // 使用逗号分隔
                if (parts.length == 2) {
                    entries.add(new String[]{parts[0].trim(), parts[1].trim()});  // 存储 nid 和 Parent
                }
            }
            System.out.println("文件读取完成，读取到 " + entries.size() + " 行数据。");
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // 使用 UTF-8 编码写入新的 CSV 文件，包含 edge_id, Parent, Child, Parent_id, Child_id
        System.out.println("开始生成并写入新的 CSV 文件: " + outputFilePath);
        int totalEdges = entries.size() * (entries.size() - 1);
        int progressStep = Math.max(totalEdges / 10, 1); // 每10%提示一次进度
        int edgeCount = 0;

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFilePath), StandardCharsets.UTF_8))) {
            int edgeId = 1;
            bw.write("edge_id,Parent,Child,Parent_id,Child_id\n");  // 写入表头

            // 生成唯一边
            for (int i = 0; i < entries.size(); i++) {
                String parentId = entries.get(i)[0];
                String parent = entries.get(i)[1];
                for (int j = 0; j < entries.size(); j++) {
                    if (i != j) {
                        String childId = entries.get(j)[0];
                        String child = entries.get(j)[1];
                        bw.write(edgeId + "," + parent + "," + child + "," + parentId + "," + childId + "\n");
                        edgeId++;
                        edgeCount++;

                        // 进度提示
                        if (edgeCount % progressStep == 0) {
                            int progress = (edgeCount * 100) / totalEdges;
                            System.out.println("进度: " + progress + "% (" + edgeCount + "/" + totalEdges + ")");
                        }
                    }
                }
            }
            System.out.println("CSV 文件生成完成，路径为：" + outputFilePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
