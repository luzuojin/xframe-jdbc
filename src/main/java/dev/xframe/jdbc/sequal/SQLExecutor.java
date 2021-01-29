package dev.xframe.jdbc.sequal;

import static dev.xframe.jdbc.sequal.internal.InternalExecutorFactory.newExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;

public class SQLExecutor {
    
    final IntFunction<ExecutorService> chooser;
    
    final ExecutorService[] executors;
    
    final AtomicBoolean isRunning;
    
    public SQLExecutor(int nThreads) {
        this.chooser = newChooser(nThreads);
        this.executors = newExecutors(nThreads);
        this.isRunning = new AtomicBoolean(true);
    }

	private IntFunction<ExecutorService> newChooser(int nThreads) {
		if((nThreads & -nThreads) == nThreads) {//pow of 2
			final int basis = nThreads - 1;
			return hashCode -> executors[hashCode & basis];
		}
		return hashCode -> executors[hashCode % nThreads];
	}
	
	private ExecutorService[] newExecutors(int nThreads) {
		ExecutorService[] array = new ExecutorService[nThreads];
        for (int i = 0; i < nThreads; i++) {
        	array[i] = newExecutor();
        }
		return array;
	}

    public void execute(SQLTask task) {
        if(isRunning.get()) {
            ExecutorService exec = getExecutor(task.hashCode());
            if(exec.isShutdown()) {
                task.run();
            } else {
                exec.execute(task);
            }
        } else {
            task.run();
        }
    }
    
    private ExecutorService getExecutor(int hashCode) {
        return chooser.apply(hashCode);
    }

    public void shutdown() {
    	if(isRunning.compareAndSet(true, false)) {
    		Thread w = Thread.currentThread();
    		AtomicInteger c = new AtomicInteger(executors.length);
    		Runnable t = completionTask(w, c);
    		for (ExecutorService exec : executors) {
    		    exec.execute(t);//exec latest task
    			exec.shutdown();
    		}
    		LockSupport.park(this);//waiting all tasks completion
    	}
    }
	private Runnable completionTask(Thread w, AtomicInteger c) {
		return () -> {
			if (c.decrementAndGet() == 0) LockSupport.unpark(w);
		};
	}
    
    public boolean isShutdown() {
        return !isRunning.get();
    }
    
}
