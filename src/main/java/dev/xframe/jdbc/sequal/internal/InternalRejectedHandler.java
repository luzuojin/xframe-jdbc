package dev.xframe.jdbc.sequal.internal;


@FunctionalInterface
public interface InternalRejectedHandler {
    
    public void rejected(Runnable task, SingleThreadExecutor executor);

}
