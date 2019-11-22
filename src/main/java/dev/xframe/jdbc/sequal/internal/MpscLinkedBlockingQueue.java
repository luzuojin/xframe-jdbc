package dev.xframe.jdbc.sequal.internal;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

class MpscNode<E> {
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<MpscNode, MpscNode> nextUpdater = AtomicReferenceFieldUpdater.newUpdater(MpscNode.class, MpscNode.class, "next");
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
    void setNext(MpscNode<E> next) {
        nextUpdater.lazySet(this, next);
    }
}

abstract class MpscLinkedBlockingQueuePad1<E> extends AbstractQueue<E> implements BlockingQueue<E> {
    long p10, p11, p12, p13, p14, p15, p16, p17, p18, p19, p1a, p1b, p1c, p1d, p1e, p1f;
}
abstract class MpscLinkedBlockingQueueHeadRef<E> extends MpscLinkedBlockingQueuePad1<E> {
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<MpscLinkedBlockingQueueHeadRef, MpscNode> headUpdater = AtomicReferenceFieldUpdater.newUpdater(MpscLinkedBlockingQueueHeadRef.class, MpscNode.class, "head");
    volatile MpscNode<E> head;
}
abstract class MpscLinkedBlockingQueuePad2<E> extends MpscLinkedBlockingQueueHeadRef<E> {
    long p20, p21, p22, p23, p24, p25, p26, p27, p28, p29, p2a, p2b, p2c, p2d, p2e, p2f;
}
abstract class MpscLinkedBlockingQueueTailRef<E> extends MpscLinkedBlockingQueuePad2<E> {
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<MpscLinkedBlockingQueueTailRef, MpscNode> tailUpdater = AtomicReferenceFieldUpdater.newUpdater(MpscLinkedBlockingQueueTailRef.class, MpscNode.class, "tail");
    volatile MpscNode<E> tail;
}
abstract class MpscLinkedBlockingQueuePad3<E> extends MpscLinkedBlockingQueueTailRef<E> {
    long p30, p31, p32, p33, p34, p35, p36, p37, p38, p39, p3a, p3b, p3c, p3d, p3e, p3f;
}

/**
 * multi producer single consumer blocking queue
 * 不支持shutdownNow, drainTo/remove不支持多线程模式
 * @author luzj
 * @param <E>
 */
@SuppressWarnings("unchecked") 
public class MpscLinkedBlockingQueue<E> extends MpscLinkedBlockingQueuePad3<E> {
    @SuppressWarnings("rawtypes")
    static final AtomicReferenceFieldUpdater<MpscLinkedBlockingQueue, Thread> waitUpdater = AtomicReferenceFieldUpdater.newUpdater(MpscLinkedBlockingQueue.class, Thread.class, "waiting");
    
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
    MpscNode<E> getTail() {
        return tailUpdater.get(this);
    }
    boolean updateTail(MpscNode<E> expect, MpscNode<E> update) {
        return tailUpdater.compareAndSet(this, expect, update);
    }
    
    void setNext(MpscNode<E> prev, MpscNode<E> next) {
        prev.setNext(next);
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
        if(!offer(e)) {
            if(Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("Interrupted while waiting offer data");
            }
        }
    }

    @Override
    public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    volatile int barrier;
    volatile Thread waiting;
    void singal() {
        barrier = 1;//保证singal线程获得的waiting是最新值
        LockSupport.unpark(waiting);
    }
    
    @Override
    public E take() throws InterruptedException {
        E e = poll();
        if(e != null) return e;
        
        Thread t = Thread.currentThread();
        waiting = t;
        while((e = poll()) == null) {
            LockSupport.park(this);
            if(t.isInterrupted())
                throw new InterruptedException("Interrupted while waiting poll data");
        }
        waitUpdater.lazySet(this, t);
        return e;
    }

    @Override
    public E poll(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public boolean isEmpty() {
        return actualGetNext(getHead()) == null;
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
        int s = 0;
        E e;
        while(s < maxElements && (e = poll()) != null) {
            c.add(e);
            s ++;
        }
        return s;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        throw new UnsupportedOperationException();
    }
}