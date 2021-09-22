package dev.xframe.jdbc.sequal.internal;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import dev.xframe.utils.XProperties;

public class InternalExecutorFactory {
    
    public static ExecutorService newExecutor() { //single thread executor
        return XProperties.getAsBool("xframe.jdbc.executor.internal", false) ? newInternalExecutor() : newThreadPoolExecutor();
    }

    private static ThreadPoolExecutor newThreadPoolExecutor() {
        return new ThreadPoolExecutor(
                1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(XProperties.getAsInt("xframe.jdbc.queue.capacity", 16_0000)),
                new InternalThreadFactory(),
                (r, e) -> InternalRejectedHandler.onRejected(r));
    }

    private static SingleThreadExecutor newInternalExecutor() {
        return new SingleThreadExecutor(
                new InternalThreadFactory(),
                new MpscLinkedBlockingQueue<>(),
                (r, e) -> InternalRejectedHandler.onRejected(r));
    }

	public static BlockingQueue<Runnable> getQueue(ExecutorService exec) {
		return (exec instanceof SingleThreadExecutor) ? ((SingleThreadExecutor) exec).getQueue() : ((ThreadPoolExecutor) exec).getQueue();
	}

}
