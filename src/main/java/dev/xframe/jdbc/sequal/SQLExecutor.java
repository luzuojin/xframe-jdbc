package dev.xframe.jdbc.sequal;

import static dev.xframe.jdbc.sequal.internal.InternalExecutorFactory.newExecutor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.IntFunction;

public class SQLExecutor {
    
    private AtomicBoolean isRunning;
    private ExecutorService[] executors;
    private IntFunction<ExecutorService> chooser;
    
    public SQLExecutor(int nThreads) {
        this.chooser = newChooser(nThreads);
        this.executors = newExecutors(nThreads);
        this.isRunning = new AtomicBoolean(true);
    }

    public int setThreadsCount(int nThreads) {
        int exLen = executors.length;
        if(isRunning.get() && nThreads > exLen) {
            ExecutorService[] dest = new ExecutorService[nThreads];
            System.arraycopy(executors, 0, dest, 0, exLen);
            //grow executors
            for (int i = exLen; i < dest.length; i++) {
                dest[i] = newExecutor();
            }
            //setup
            this.executors = dest;
            this.chooser = newChooser(nThreads);
        }
        return exLen;
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
    //确保所有任务都处理完成
    public void shutdown() {
    	if(isRunning.compareAndSet(true, false)) {
    		for (ExecutorService exec : executors) {
    			exec.shutdown();
    		}
    		awaitTermination();
    	}
    }
    private void awaitTermination() {
        for (ExecutorService exec : executors) {
            awaitTermination0(exec);
        }
    }
    private void awaitTermination0(ExecutorService exec) {
        try {
            boolean terminated = false;
            do {
                terminated = exec.awaitTermination(1, TimeUnit.MINUTES);
            } while(!terminated);
        } catch (InterruptedException e) {//retry
            if(!exec.isTerminated()) {
                awaitTermination0(exec);
            }
        }
    }
    
    public boolean isShutdown() {
        return !isRunning.get();
    }
    
}
