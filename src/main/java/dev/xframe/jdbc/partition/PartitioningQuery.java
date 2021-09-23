package dev.xframe.jdbc.partition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import dev.xframe.jdbc.TypeQuery;

public class PartitioningQuery<T> extends TypeQuery<T> {

    //partition name function
    private Function<T, String> pFunc;
    //partition table name function
    private BiFunction<String, String, String> tFunc;
    //partition table exists(builtin or created by other sys)
    private boolean tableExists;
    //cache partitioned TypeQuery
    private Map<String, TypeQuery<T>> pCached = new HashMap<>();
    
    public PartitioningQuery(Function<T, String> pFunc, BiFunction<String, String, String> tFunc, boolean tableExists) {
        this.pFunc = pFunc;
        this.tFunc = tFunc;
        this.tableExists = tableExists;
    }
    
    @Override
    public TypeQuery<T> partition(String pName) {
        TypeQuery<T> query = pCached.get(pName);
        if(query == null) {
            query = new PartitionedQeury<>(this, pName, tFunc, tableExists);
            pCached.put(pName, query);
        }
        return query;
    }
    
    private TypeQuery<T> getPartitionedQeury(T obj) {
        return partition(pFunc.apply(obj));
    }
    
    private Map<TypeQuery<T>, List<T>> groupByPartition(Collection<T> objs) {
        Map<TypeQuery<T>, List<T>> map = new HashMap<>();
        for (T obj : objs) {
            TypeQuery<T> key = getPartitionedQeury(obj);
            List<T> val = map.computeIfAbsent(key, k -> new ArrayList<>());
            val.add(obj);
        }
        return map;
    }

    @Override
    public boolean insert(T obj) {
        return getPartitionedQeury(obj).insert(obj);
    }
    @Override
    public long insertAndIncrement(T obj) {
        return getPartitionedQeury(obj).insertAndIncrement(obj);
    }
    @Override
    public boolean insertBatch(Collection<T> objs) {
        boolean r = true;
        for (Map.Entry<TypeQuery<T>, List<T>> entry : groupByPartition(objs).entrySet()) {
            r &= entry.getKey().insertBatch(entry.getValue());
        }
        return r;
    }
    @Override
    public boolean insertBatch(T[] objs) {
        boolean r = true;
        for (Map.Entry<TypeQuery<T>, List<T>> entry : groupByPartition(Arrays.asList(objs)).entrySet()) {
            r &= entry.getKey().insertBatch(entry.getValue());
        }
        return r;
    }
    
    @Override
    public boolean upsert(T obj) {
        return getPartitionedQeury(obj).upsert(obj);
    }
    @Override
    public boolean upsertBatch(T[] objs) {
        boolean r = true;
        for (Map.Entry<TypeQuery<T>, List<T>> entry : groupByPartition(Arrays.asList(objs)).entrySet()) {
            r &= entry.getKey().upsertBatch(entry.getValue());
        }
        return r;
    }
    @Override
    public boolean upsertBatch(Collection<T> objs) {
        boolean r = true;
        for (Map.Entry<TypeQuery<T>, List<T>> entry : groupByPartition(objs).entrySet()) {
            r &= entry.getKey().upsertBatch(entry.getValue());
        }
        return r;
    }
    
    @Override
    public boolean update(T obj) {
        return getPartitionedQeury(obj).update(obj);
    }
    @Override
    public boolean updateBatch(T[] objs) {
        boolean r = true;
        for (Map.Entry<TypeQuery<T>, List<T>> entry : groupByPartition(Arrays.asList(objs)).entrySet()) {
            r &= entry.getKey().updateBatch(entry.getValue());
        }
        return r;
    }
    @Override
    public boolean updateBatch(Collection<T> objs) {
        boolean r = true;
        for (Map.Entry<TypeQuery<T>, List<T>> entry : groupByPartition(objs).entrySet()) {
            r &= entry.getKey().updateBatch(entry.getValue());
        }
        return r;
    }
    
    @Override
    public boolean delete(T obj) {
        return getPartitionedQeury(obj).delete(obj);
    }
    
}
