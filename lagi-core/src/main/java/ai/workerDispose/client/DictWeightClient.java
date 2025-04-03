package ai.workerDispose.client;

import ai.workerDispose.dao.AiZindexUserDao;
import ai.workerDispose.pojo.DictValue;
import ai.workerDispose.service.DictWeightProcessing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DictWeightClient {
    private static final Logger logger = LoggerFactory.getLogger(DictWeightClient.class);

    // page range is 1 to 7000000
    private static final int START_PAGE = 1;
    private static final int END_PAGE = 5;
    private static final int PAGE_SIZE = 1;
    private static final int THREAD_POOL_SIZE = 3;

    private static final DictWeightProcessing dictWeightProcessing = new DictWeightProcessing();
    private static final AiZindexUserDao aiZindexUserDao = new AiZindexUserDao();

    private static final ThreadPoolExecutor executor = new ThreadPoolExecutor(
            THREAD_POOL_SIZE,
            THREAD_POOL_SIZE,
            60,
            TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(THREAD_POOL_SIZE * 1000)
    );

    private static final String DICT_WEIGHT_PROGRESS_FILE = System.getProperty("user.home") + "/dict_weight_progress.txt";
    private static final Set<DictValue> dictValueSet;

    static {
        dictValueSet = loadDictValueFromFile();
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    }

    public static void main(String[] args) {
//        dictWeightProcessFromDb();

        String filepath = "E:/Desktop/CLUD与MMLU/did/Dict-CLUD与MMLU_1.txt";
        dictWeightProcessFromFile(filepath);
    }

    private static void dictWeightProcessFromDb() {
        for (int i = START_PAGE; i < END_PAGE; i++) {
            List<DictValue> dictList = aiZindexUserDao.getDictList(i, PAGE_SIZE);
            for (DictValue dictValue : dictList) {
                executor.submit(() -> asyncDictWeightProcess(dictValue));
            }
        }
    }

    public static void dictWeightProcessFromFile(String filepath) {
        List<DictValue> dictValueList = getDictValueFromFilePath(filepath);
        for (int i = 0; i < dictValueList.size(); i++) {
            DictValue dictValue = dictValueList.get(i);
            executor.submit(() -> asyncDictWeightProcess(dictValue));
        }
    }

    private static void asyncDictWeightProcess(DictValue dictValue) {
        if (dictValueSet.contains(dictValue)) {
            logger.info("Dict value already processed: {}", dictValue);
            return;
        }
        List<DictValue> dictList = new ArrayList<>();
        dictList.add(dictValue);
        logger.info("Processing dict value: {}", dictValue);
        long startTime = System.currentTimeMillis();
        while (true) {
            try {
                dictWeightProcessing.dictWeightProcess(dictList);
                break;
            } catch (Exception e) {
                logger.error("Error processing dict value: {}, error: {}", dictValue, e.getMessage());
                sleep(30 * 1000);
            }
        }
        long endTime = System.currentTimeMillis();
        long timeUsed = (endTime - startTime) / 1000;
        logger.info("Processing dict value completed: {}, and it took {} seconds", dictValue, timeUsed);
        writeDictValueToFile(dictList);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    private static List<DictValue> getDictValueFromFilePath(String filepath) {
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
            logger.error("Error reading file: {}", e.getMessage());
        }
        return dictValueList;
    }

    private static Set<DictValue> loadDictValueFromFile() {
        Set<DictValue> dictSet = new HashSet<>();
        File file = new File(DICT_WEIGHT_PROGRESS_FILE);
        if (!file.exists()) {
            return dictSet;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                DictValue dictValue = new DictValue(Integer.parseInt(parts[0]), parts[1]);
                dictSet.add(dictValue);
            }
        } catch (IOException e) {
            logger.error("Error reading dict weight progress file: {}", e.getMessage());
        }

        return dictSet;
    }

    private static synchronized void writeDictValueToFile(List<DictValue> dictList) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(DICT_WEIGHT_PROGRESS_FILE, true))) {
            for (DictValue dictValue : dictList) {
                writer.write(dictValue.getDid() + "," + dictValue.getPlainText());
                writer.newLine();
            }
        } catch (IOException e) {
            logger.error("Error writing dict weight progress file: {}", e.getMessage());
        }
    }
}
