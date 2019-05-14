package dev.xframe.jdbc.sequal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface SQLTask extends Runnable {
    
    static final Logger logger = LoggerFactory.getLogger(SQLTask.class);
    
    public default void run() {
        try {
            exec();
        } catch (Throwable t) {
            logger.error("Run async sql task: ", t);
        }
    }
    
    public void exec();

}
