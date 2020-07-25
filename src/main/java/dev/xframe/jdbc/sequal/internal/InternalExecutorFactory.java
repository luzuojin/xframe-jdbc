package dev.xframe.jdbc.sequal.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class InternalExecutorFactory {
    
    public static ExecutorService newExecutor() {
        return Boolean.parseBoolean(System.getProperty("xframe.internal.executor.usage", "false")) ? newInternalExecutor() : newThreadPoolExecutor();
    }

    private static ThreadPoolExecutor newThreadPoolExecutor() {
        return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), new InternalThreadFactory(), (r, e) -> r.run());
    }

    private static SingleThreadExecutor newInternalExecutor() {
        return new SingleThreadExecutor(new InternalThreadFactory(), new MpscLinkedBlockingQueue<>(), (r, e) -> r.run());
    }

	public static BlockingQueue<Runnable> getQueue(ExecutorService exec) {
		return (exec instanceof SingleThreadExecutor) ? ((SingleThreadExecutor) exec).getQueue() : ((ThreadPoolExecutor) exec).getQueue();
	}

}
