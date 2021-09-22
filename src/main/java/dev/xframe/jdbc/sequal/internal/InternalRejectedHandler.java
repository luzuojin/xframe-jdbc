package dev.xframe.jdbc.sequal.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FunctionalInterface
public interface InternalRejectedHandler {
    
    public void rejected(Runnable task, SingleThreadExecutor executor);

    
    Logger logger = LoggerFactory.getLogger(InternalRejectedHandler.class);
    
    static void onRejected(Runnable task) {
        logger.warn("SQLTask queue overflow...");
        //direct run(ensure data saved)
        task.run();
    }

}
