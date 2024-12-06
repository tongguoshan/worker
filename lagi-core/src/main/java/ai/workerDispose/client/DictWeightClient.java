package ai.workerDispose.client;

import ai.workerDispose.service.DictWeightProcessing;


public class DictWeightClient {
    // page range is 1 to 7000000
    private static final int START_PAGE = 1;
    private static final int END_PAGE = 5;
    private static final int PAGE_SIZE = 1;

    private static final DictWeightProcessing dictWeightProcessing = new DictWeightProcessing();

    public static void main(String[] args) {
        dictWeightProcessing.dictWeightProcess(START_PAGE, END_PAGE, PAGE_SIZE);

//        String filepath = "E:/Desktop/nid数据/1.csv";
//        dictWeightProcessing.dictWeightProcess(filepath);
    }
}
