package ai.workerDispose.client;

import ai.workerDispose.service.DictWeightProcessing;

public class DictWeightClient {
    // page range is 1 to 7000000
    private static final int START_PAGE = 1;
    private static final int END_PAGE = 5;
    private static final int PAGE_SIZE = 1;

    public static void main(String[] args) {
        new DictWeightProcessing().dictWeightProcess(START_PAGE, END_PAGE, PAGE_SIZE);
    }
}
