package ai.medusa;

public class PromptCacheConstant {
    public static final int POOL_INITIAL = 1;
    public static final int POOL_DIVERSIFIED = 2;
    public static final int POOL_CACHE_PUT = 3;
    public static final int PRODUCER_THREADS = 1;
    public static final int CONSUMER_THREADS = 1;
    public static final int TOTAL_THREAD_COUNT = 1;
    public static final int PRODUCER_LIMIT = 1;
    public static final int POOL_CACHE_SIZE = 10000;
    public static final int COMPLETION_CACHE_SIZE = 10000;

    public static final String DIVERSIFY_PROMPT = "以下是用户当前的问题，请预测该用户接下来可能会问什么其它问题？只返回问题内容本身：\n";
}
