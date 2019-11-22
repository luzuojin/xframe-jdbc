package dev.xframe.jdbc.sequal;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

import dev.xframe.jdbc.sequal.internal.InternalExecutorFactory;

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
        	array[i] = InternalExecutorFactory.newExecutor();
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
    		for (ExecutorService exec : executors) {
    			exec.shutdown();
    		}
    	}
    }
    
    public boolean isShutdown() {
        return isRunning.get();
    }
    
}
