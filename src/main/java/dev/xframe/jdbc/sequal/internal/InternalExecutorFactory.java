package dev.xframe.jdbc.sequal.internal;

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
    
    
    public static void main(String[] args) {
        ExecutorService exec = newInternalExecutor();
        for (int i = 0; i < 500; i++) {
            final String j = String.valueOf(i);
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(j + "\t" + Thread.currentThread().getName());
                }
            });
        }
        exec.shutdownNow();
        for (int i = 500; i < 1000; i++) {
            final String j = String.valueOf(i);
            exec.execute(new Runnable() {
                @Override
                public void run() {
                    System.out.println(j + "\t" + Thread.currentThread().getName());
                }
            });
        }
    }

}
