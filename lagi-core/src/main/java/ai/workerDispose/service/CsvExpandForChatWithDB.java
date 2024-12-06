package ai.workerDispose.service;

import ai.config.ContextLoader;
import ai.llm.service.CompletionsService;
import ai.openai.pojo.ChatCompletionRequest;
import ai.openai.pojo.ChatCompletionResult;
import ai.openai.pojo.ChatMessage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class CsvExpandForChatWithDB {

    // 数据库连接配置
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/ai?useSSL=false&useUnicode=true&characterEncoding=UTF-8&zeroDateTimeBehavior=convertToNull";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "root";

    // 获取数据库连接
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    static {
        //initialize Profiles
        ContextLoader.loadContext();
    }

    private static final Map<String, String> defaultAliasMap = new HashMap<>();

    static {
        defaultAliasMap.put("1", "cause_symptom");
        defaultAliasMap.put("2", "entity_state");
        defaultAliasMap.put("3", "entity_types");
        defaultAliasMap.put("4", "general_relation");
        defaultAliasMap.put("5", "total_hierarchy");
        defaultAliasMap.put("6", "target_appearance");
        defaultAliasMap.put("7", "group_interaction");
        defaultAliasMap.put("8", "object_representation");
        defaultAliasMap.put("9", "element_relation");
        defaultAliasMap.put("10", "command_argument");
        defaultAliasMap.put("11", "target_location");
        defaultAliasMap.put("12", "command_output");
        defaultAliasMap.put("13", "symptom_action");
        defaultAliasMap.put("14", "coordinating_relation");
        defaultAliasMap.put("15", "carry_on");
        defaultAliasMap.put("16", "opposing_link");
        defaultAliasMap.put("17", "choice_option");
        defaultAliasMap.put("18", "conditional_case");
        defaultAliasMap.put("19", "concessive_condition");
        defaultAliasMap.put("20", "cumulative_relation");
        defaultAliasMap.put("21", "required_condition");
        defaultAliasMap.put("22", "intended_action");
        defaultAliasMap.put("23", "reference_target");
        defaultAliasMap.put("24", "metaphorical_expression");
        defaultAliasMap.put("25", "whole_part");
        defaultAliasMap.put("26", "hierarchical_level");
        defaultAliasMap.put("27", "origin_outcome");
        defaultAliasMap.put("28", "practical_function");
        defaultAliasMap.put("29", "type_instance");
        defaultAliasMap.put("30", "task_process");
        defaultAliasMap.put("31", "item_description");
        defaultAliasMap.put("32", "entity_role");
        defaultAliasMap.put("33", "impact_target");
        defaultAliasMap.put("34", "cultural_heritage");
        defaultAliasMap.put("35", "temporal_event");
        defaultAliasMap.put("36", "condition_change");
        defaultAliasMap.put("37", "assessment_subject");
        defaultAliasMap.put("38", "joint_effort");
    }

    // 获取分页查询数据（改进：基于主键的分页）
    public static List<String[]> fetchData(int lastEdgeId, int limit) throws SQLException {
        String query = "SELECT edge_id, parent_uid, child_uid, comments FROM ai_aspect_candidates WHERE edge_id > ? LIMIT ?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, lastEdgeId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            List<String[]> rows = new ArrayList<>();
            while (rs.next()) {
                String edgeId = rs.getString("edge_id");
                String parentUid = rs.getString("parent_uid");
                String childUid = rs.getString("child_uid");
                String comments = rs.getString("comments");
                rows.add(new String[]{edgeId, parentUid, childUid, comments});
            }
            return rows;
        }
    }

    // 生成查询内容
    public static String generateQueryContent(List<String[]> batch) {
        List<String> pairs = batch.stream()
                .map(row -> row[3].split("->"))
                .map(parts -> String.format("'%s' '%s'", parts[0], parts[1]))
                .collect(Collectors.toList());
        return "现有如下type：\n" +
                "因果关系、动及关系、总分关系、比较关系、隶属关系、主形关系、组互关系、表征关系、含有关系、命名关系、位置关系、组团关系、症解关系、并列关系、承接关系、转折关系、选择关系、假设关系、让步关系、递进关系、条件关系、目的关系、指代关系、虚语关系、构成关系、层级关系、来源关系、用途关系、类别关系、活动关系、描述关系、身份关系、影响关系、传承关系、时间关系、状态关系、评价关系、协作关系。\n" +
                "要求：\n" +
                "(1) 依据以上类别，对下面的每组词进行分类，每对之间用分号隔开。type的值即为该类型；\n" +
                "(2) 如果不属于所列任何一个类别，就将该节点的类别值，type就为“其它关系”; \n" +
                "(3) 输出格式为：“{词A和词B属于type; 词A和词B属于type}” 例如：{立体声和立体属于命名关系; 立体声和声属于表征关系; 免费版和免费属于目的关系; 免费版和版属于组成关系; 股份制和股份属于命名关系; 股份制和制属于构成关系; 许可证和许可属于目的关系; 许可证和证属于组成关系; 战国策和战国属于命名关系; 战国策和策属于构成关系;}\n" +
                "请给下面的数据进行分类： \n" + String.join(";", pairs) + ";\n" +
                "注意：完整的给每组词分类，无需解释，无需其它提示词。";
    }

    // 调用大模型API
    public static String chatFor910B(String content) {
        ChatCompletionRequest chatCompletionRequest = new ChatCompletionRequest();
        chatCompletionRequest.setTemperature(0.8);
        chatCompletionRequest.setMax_tokens(1024);
        chatCompletionRequest.setCategory("default");
        chatCompletionRequest.setModel("Baichuan-13b");
        ChatMessage message = new ChatMessage();
        message.setRole("user");
        message.setContent(content);
        chatCompletionRequest.setMessages(Collections.singletonList(message));
        chatCompletionRequest.setStream(false);
        CompletionsService completionsService = new CompletionsService();
        ChatCompletionResult result = completionsService.completions(chatCompletionRequest);
        return result.getChoices().get(0).getMessage().getContent();
    }

    // 解析模型响应
    public static List<String> parseResponse(String response) {
        List<String> relationList = new ArrayList<>();
        if (response == null || response.trim().isEmpty()) {
            return relationList;
        }

        // 去掉所有的大括号
        response = response.replace("{", "").replace("}", "");

        // 处理每条记录，每条记录的关系对可能通过分号或逗号分隔
        String[] records = response.split(";");

        for (String record : records) {
            record = record.trim();
            if (record.isEmpty()) {
                continue;  // 跳过空的记录
            }

            // 将一条记录按逗号分割，得到多个关系对
            String[] relations = record.split(",");

            for (String relation : relations) {
                relation = relation.trim();
                if (relation.isEmpty()) {
                    continue;  // 跳过空的关系对
                }

                // 处理每个关系对，如果是以“属于”分隔
                String[] splitAnswer = relation.split("属于");

                // 确保解析出词对和关系类型
                if (splitAnswer.length == 2) {
                    String relationType = splitAnswer[1].trim();
                    // 如果是“其它关系”，将其转换为 "0"
                    relationList.add(relationType.equals("其它关系") ? "0" : relationType);
                } else {
                    relationList.add("0");  // 解析出错时添加 "0"
                }
            }
        }
        return relationList;
    }

    // 获取Alias值
    public static String getAlias(String relId) {
        return defaultAliasMap.getOrDefault(relId, "未知");
    }

    // 写入到CSV文件（改进：控制写入频率，避免过多的写入操作）
    public static void writeCsv(String outputCsvPath, List<String[]> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputCsvPath), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            for (String[] row : rows) {
                writer.write(String.join(",", row));
                writer.newLine();

                // 打印写入的行数据
                System.out.println("写入的数据行: " + String.join(",", row));
            }
        }
    }

    // 检查CSV文件是否存在
    public static boolean doesCsvFileExist(String csvFilePath) {
        File file = new File(csvFilePath);
        return file.exists() && file.isFile();
    }

    // 从CSV文件中读取已存在的edge_id
    public static Set<String> readExistingEdgeIds(String csvFilePath) throws IOException {
        Set<String> existingEdgeIds = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(csvFilePath), StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",");
                if (columns.length > 0) {
                    existingEdgeIds.add(columns[0]);  // 假设edge_id是每行的第一个字段
                }
            }
        }
        return existingEdgeIds;
    }

    // 处理分页查询数据并生成CSV（改进：如果CSV文件存在，则进行过滤）
    // 处理分页查询数据并生成CSV（改进：如果CSV文件存在，则进行过滤）
    public static void processData(int pageSize) {
        String outputCsvPath = "ai_aspect_candidates.csv";
        int offset = 0;
        int lastEdgeId = 0;  // 改为使用主键分页
        boolean isComplete = false;
        int totalRowsWritten = 0;

        try {
            // 检查CSV文件是否存在，如果存在，则读取已有的edge_id
            Set<String> existingEdgeIds = new HashSet<>();
            if (doesCsvFileExist(outputCsvPath)) {
                existingEdgeIds = readExistingEdgeIds(outputCsvPath);
                System.out.println("CSV file exists, loading existing edge_ids...");
            } else {
                System.out.println("CSV file does not exist, skipping edge_id filtering...");
            }

            // 将existingEdgeIds声明为final，以确保它不会被修改
            final Set<String> finalExistingEdgeIds = existingEdgeIds;

            while (!isComplete) {
                System.out.printf("Processing page with offset: %d%n", offset);

                // 获取当前页的数据
                List<String[]> data = fetchData(lastEdgeId, pageSize);

                if (data.isEmpty()) {
                    isComplete = true;
                    continue;
                }

                // 如果CSV文件存在，则过滤掉已存在的edge_id
                List<String[]> filteredData = data.stream()
                        .filter(row -> !finalExistingEdgeIds.contains(row[0]))  // 过滤掉已存在的edge_id
                        .collect(Collectors.toList());

                if (filteredData.isEmpty()) {
                    // 如果当前页面所有的edge_id都已存在，则跳过该页，直接查询下一页
                    System.out.println("All records in this page already exist in the CSV. Skipping this page...");
                    lastEdgeId = Integer.parseInt(data.get(data.size() - 1)[0]);  // 更新lastEdgeId继续分页
                    continue;
                }

                // 为每批数据生成模型查询内容
                String content = generateQueryContent(filteredData);
                System.out.println("Query content for model:\n" + content);

                // 调用大模型并打印回复
                String response = chatFor910B(content);
                System.out.println("Model response:\n" + response);

                // 解析模型的响应
                List<String> relations = parseResponse(response);
                System.out.println("relations = " + relations);
                List<String[]> outputRows = new ArrayList<>();

                // 从模型响应中提取关系，逐个查询数据库中的 rel_id
                for (int i = 0; i < filteredData.size(); i++) {
                    String[] row = filteredData.get(i);
                    String relationChinese = relations.get(i);  // 获取每一行对应的关系
                    String relId = "0";  // 默认值为 "0"
                    String alias = "";  // 默认Alias为""

                    try (Connection conn = getConnection()) {
                        // 查询当前 relationChinese 的 rel_id
                        String query = "SELECT rel_id FROM ai_meta_relation WHERE relation_chinese = ?";
                        try (PreparedStatement stmt = conn.prepareStatement(query)) {
                            stmt.setString(1, relationChinese);
                            ResultSet rs = stmt.executeQuery();
                            if (rs.next()) {
                                relId = rs.getString("rel_id");  // 获取对应的 rel_id
                                alias = getAlias(relId);  // 根据rel_id获取Alias
                            }
                        }
                    } catch (SQLException e) {
                        // 如果查询过程中出现异常，输出日志，并继续处理
                        System.err.println("Error querying rel_id for relation: " + relationChinese);
                        e.printStackTrace();
                    }

                    // 将 rel_id 和 alias 填入当前行
                    String[] newRow = Arrays.copyOf(row, row.length + 2);  // 多出两个字段：rel_id 和 alias
                    newRow[row.length] = relId;  // 将 rel_id 加入到倒数第二列
                    newRow[row.length + 1] = alias;  // 将 alias 加入到最后一列
                    outputRows.add(newRow);  // 添加到输出列表
                }

                // 写入到CSV文件
                writeCsv(outputCsvPath, outputRows);
                totalRowsWritten += outputRows.size();

                System.out.printf("Wrote %d rows to CSV file. Total rows written so far: %d%n", outputRows.size(), totalRowsWritten);

                // 更新 lastEdgeId 为当前批次的最后一条数据的 edge_id
                lastEdgeId = Integer.parseInt(filteredData.get(filteredData.size() - 1)[0]);

            }

            System.out.println("CSV文件生成完毕，写入总行数: " + totalRowsWritten);

        } catch (SQLException | IOException e) {
            // 增强异常捕获，输出详细的错误日志
            System.err.println("Error occurred during processing: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        int pageSize = 10;  // 每次查询10条数据
        processData(pageSize);
    }
}
