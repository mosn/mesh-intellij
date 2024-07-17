package io.mosn.coder.compiler;

import java.util.List;
import java.util.concurrent.*;

public abstract class AbstractCompiler {


    /**
     * compile or packet plugin task pool
     */
    final static ThreadPoolExecutor pool = new ThreadPoolExecutor(2
            , Runtime.getRuntime().availableProcessors() + 1
            , 60
            , TimeUnit.SECONDS, new LinkedBlockingQueue<>(6));


    public static void clearTask() {
        BlockingQueue<Runnable> tasks = new LinkedBlockingQueue<>(pool.getQueue());
        for (Runnable task : tasks) {
            pool.remove(task);
        }
    }

}
