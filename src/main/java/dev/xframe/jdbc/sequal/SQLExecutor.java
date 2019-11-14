package dev.xframe.jdbc.sequal;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import java.util.function.IntFunction;

public class SQLExecutor {
    
    final IntFunction<ExecutorService> chooser;
    
    final ExecutorService[] executors;
    
    volatile boolean isRunning;
    
    public SQLExecutor(int nThreads) {
        this.chooser = newChooser(nThreads);
        this.executors = newExecutors(nThreads);
        this.isRunning = true;
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

	private ThreadPoolExecutor newExecutor() {
		return new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, newTaskQueue(), newThreadFactory(), newRejector());
	}

	private BlockingQueue<Runnable> newTaskQueue() {
		return Boolean.parseBoolean(System.getProperty("xframe.jdbc.mpsc.usage", "true")) ? new MpscLinkedBlockingQueue<>() : new LinkedBlockingQueue<>();
	}
	
	private ThreadFactory newThreadFactory() {
		return new SimpleThreadFactory();
	}
	
	private RejectedExecutionHandler newRejector() {
		return (r, e) -> r.run();
	}

    public void execute(SQLTask task) {
        if(isRunning) {
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

    public synchronized void shutdown() {
    	if(isRunning) {
    		isRunning = false;
    		for (ExecutorService exec : executors) {
    			exec.shutdown();
    		}
    	}
    }
    
    public boolean isShutdown() {
        return !isRunning;
    }
    
    static class SimpleThreadFactory implements ThreadFactory {
        private ThreadGroup group;
        private AtomicInteger number;
        public SimpleThreadFactory() {
            this.group = new ThreadGroup("jdbc-async");
            this.number = new AtomicInteger();
        }
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, group.getName() + "-" + number.getAndIncrement(), 0);
            if (t.isDaemon())
                t.setDaemon(false);
            if (t.getPriority() != Thread.NORM_PRIORITY)
                t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    }
    
    /**
     * multi producer single consumer blocking queue
     * @author luzj
     * @param <E>
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static class MpscLinkedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {
        
        static final AtomicReferenceFieldUpdater</*-obsessive-*/MpscNode, MpscNode> nextUpdater = AtomicReferenceFieldUpdater.newUpdater(MpscNode.class, MpscNode.class, "next");
        static final AtomicReferenceFieldUpdater<MpscLinkedBlockingQueue, MpscNode> headUpdater = AtomicReferenceFieldUpdater.newUpdater(MpscLinkedBlockingQueue.class, MpscNode.class, "head");
        static final AtomicReferenceFieldUpdater<MpscLinkedBlockingQueue, MpscNode> tailUpdater = AtomicReferenceFieldUpdater.newUpdater(MpscLinkedBlockingQueue.class, MpscNode.class, "tail");
        
        static class MpscNode<E> {
            E value;
            volatile MpscNode<E> next;
            public MpscNode(E value) {
                this.value = value;
            }
            E clear() {
                E o = this.value;
                this.value = null;
                return o;
            }
        }
        
        volatile MpscNode<E> head;
        volatile MpscNode<E> tail;
        
        public MpscLinkedBlockingQueue() {
            MpscNode<E> val = new MpscNode<>(null);
            setHead(val);
            setTail(val);
        }
        
        MpscNode<E> setHead(MpscNode<E> val) {
            return headUpdater.getAndSet(this, val);
        }
        MpscNode<E> getHead() {
            return headUpdater.get(this);
        }
        
        MpscNode<E> setTail(MpscNode<E> val) {
            return tailUpdater.getAndSet(this, val);
        }
        
        boolean updateTail(MpscNode<E> expect, MpscNode<E> update) {
            return tailUpdater.compareAndSet(this, expect, update);
        }
        
        MpscNode<E> getTail() {
            return tailUpdater.get(this);
        }
        
        void setNext(MpscNode<E> prev, MpscNode<E> next) {
            nextUpdater.lazySet(prev, next);
        }
        
        void delNext(MpscNode<E> node) {
            setNext(node, node);//防止node.next存活时node不会释放, 设置node是因为next==null有其他的意义
        }
        
        MpscNode<E> spinGetNext(MpscNode<E> node) {
            MpscNode<E> next;
            while ((next = node.next) == null) {
                // spin, wait for lazy set complete
            }
            return next;
        }
        
        //@see setNext
        MpscNode<E> actualGetNext(MpscNode<E> node) {
            MpscNode<E> next = node.next;
            if (next == null && node != getTail()){//next为空 但node不等于tail时表明next正在被set
                next = spinGetNext(node);
            }
            return next;
        }
        
        @Override
        public boolean offer(E e) {
            assert e != null;
            final MpscNode<E> next = new MpscNode<>(e);
            final MpscNode<E> prev = setTail(next);
            setNext(prev, next);
            if(prev.value == null)//head
                singal();
            return true;
        }
        
        @Override
        public E poll() {
            MpscNode<E> head = getHead();
            MpscNode<E> next = actualGetNext(head);
            if(next != null) {
                return poll0(head, next);
            }
            return null;
        }

        E poll0(MpscNode<E> head, MpscNode<E> node) {
            E o = node.clear();//polled node
            delNext(head);     //avoid gc
            setHead(node);     //next-->head
            return o;
        }

        @Override
        public E peek() {
            MpscNode<E> head = getHead();
            MpscNode<E> next = actualGetNext(head);
            if(next != null) {
                return next.value;
            }
            return null;
        }

        @Override
        public void put(E e) throws InterruptedException {
            offer(e);
        }

        @Override
        public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
            return offer(e);
        }

        Thread singleWaiter;
        
        void singal() {
            LockSupport.unpark(singleWaiter);
        }
        
        @Override
        public E take() throws InterruptedException {
            interruptedCheck();
            while(getHead().next == null) {
                singleWaiter = Thread.currentThread();
                LockSupport.park(this);
                interruptedCheck();
            }
            return poll();
        }

        @Override
        public E poll(long timeout, TimeUnit unit) throws InterruptedException {
            interruptedCheck();
            long nanos = unit.toNanos(timeout);
            long deadline = System.nanoTime() + nanos;
            while(nanos > 0 && getHead().next == null) {
                singleWaiter = Thread.currentThread();
                LockSupport.parkNanos(this, nanos);
                nanos = deadline - System.nanoTime();
                interruptedCheck();
            }
            return poll();
        }
        
        void interruptedCheck() throws InterruptedException {
            if(Thread.interrupted()) throw new InterruptedException();
        }
        
        @Override
        public boolean isEmpty() {
            return getHead() == getTail();
        }

        @Override
        public int size() {
            MpscNode<E> head = getHead();
            MpscNode<E> tail = getTail();
            int size = 0;
            while (head != tail && head != null && size < Integer.MAX_VALUE) {
                MpscNode<E> next = head.next;
                if (next == head){
                    return size;
                }
                head = next;
                size ++;
            }
            return size;
        }
        
        @Override
        public int drainTo(Collection<? super E> c, int maxElements) {
            MpscNode<E> head = getHead();
            MpscNode<E> tail = getTail();
            int size = 0;
            while (head != tail && head != null && size < maxElements) {
                MpscNode<E> next = head.next;
                if (next == head){
                    return size;
                }
                c.add(poll0(head, next));
                head = next;
                size ++;
            }
            return size;
        }
        
        @Override
        public int drainTo(Collection<? super E> c) {
           return drainTo(c, Integer.MAX_VALUE);
        }

        public boolean remove(Object o) {
            if(null == o) return false;

            MpscNode<E> prev = getHead();
            MpscNode<E> curr = actualGetNext(prev);
            while (curr != null) {
                if (o.equals(curr.value)) {
                    MpscNode<E> next = actualGetNext(curr);
                    if (next != null){//middle
                        setNext(prev, next);
                    } else {//tail
                        setNext(prev, next);
                        if (!updateTail(curr, prev)) {
                            next = spinGetNext(curr);
                            setNext(prev, next);
                        }
                    }
                    
                    curr.clear();
                    delNext(curr);// Avoid GC
                    return true;
                }
                prev = curr;
                curr = actualGetNext(curr);
            }
            return false;
        }
        
        @Override
        public int remainingCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public Iterator<E> iterator() {
            throw new UnsupportedOperationException();
        }
    }
    
}
