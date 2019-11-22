package dev.xframe.jdbc.sequal.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class InternalThreadFactory implements ThreadFactory {
    
    private ThreadGroup group;
    private AtomicInteger next;
    
    public InternalThreadFactory() {
        this.group = new ThreadGroup("jdbc-async");
        this.next = new AtomicInteger();
    }
    
    public Thread newThread(Runnable r) {
        Thread t = new Thread(group, r, group.getName() + "-" + next.incrementAndGet());
        if (t.isDaemon())
            t.setDaemon(false);
        if (t.getPriority() != Thread.NORM_PRIORITY)
            t.setPriority(Thread.NORM_PRIORITY);
        return t;
    }
    
}