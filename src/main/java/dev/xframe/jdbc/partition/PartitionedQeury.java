package dev.xframe.jdbc.partition;

import java.util.function.BiFunction;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.TypeQuery;
import dev.xframe.jdbc.sequal.AsyncSQL;
import dev.xframe.jdbc.sequal.NilSQL;
import dev.xframe.jdbc.sequal.SQL;
import dev.xframe.jdbc.sequal.SQLGetter;
import dev.xframe.jdbc.sequal.SyncSQL;

public class PartitionedQeury<T> extends TypeQuery<T> {

    final TypeQuery<T> origin;
    final String pName;
    //partition table name function
    final BiFunction<String, String, String> tFunc;
    //partition table exists(builtin or created by other sys)
    volatile boolean tableExists;

    public PartitionedQeury(TypeQuery<T> origin, String pName, BiFunction<String, String, String> tFunc, boolean tableExists) {
        this.table = TypeQuery.Getter.getTable(origin);
        this.origin = origin;
        this.pName = pName;
        this.tFunc = tFunc;
        this.tableExists = tableExists;
    }
    
    @Override
    public TypeQuery<T> partition(String pName) {
        return origin.partition(pName);
    }

    private void ensurePartitionedTableExists(JdbcTemplate jdbc) {
        if(tableExists)
            return;
        //table not exists
        synchronized (this) {
            if(!tableExists) {
                String pTable = makePartitionTableName();
                //check table exists
                if(!jdbc.fetch(String.format("SHOW TABLES LIKE '%s';", pTable) , PSSetter.NONE, r -> r.next())) {
                    //create simulate partition table
                    if(!jdbc.execute(String.format("CREATE TABLE `%s` LIKE `%s`;", pTable, table))) {
                        throw new IllegalStateException(String.format("Create table %s faield", pTable));
                    }
                }
                tableExists = true;
            }
        }
    }
    private String makePartitionTableName() {
        return tFunc.apply(table, pName);
    }
    private String makePartitionNativeSQL(String sql) {    //table_parition
        return sql.replace(table, makePartitionTableName());
    }
    //copy sql to partitioned sql
    private SQL<T> makePartitionSQL(SQL<T> origin) {
        if(origin instanceof AsyncSQL) {
            AsyncSQL<T> src = (AsyncSQL<T>) origin;
            ensurePartitionedTableExists(SQLGetter.getJdbcTemplate(src));
            return new AsyncSQL<>(SQLGetter.getOption(src), makePartitionNativeSQL(SQLGetter.getSql(src)), SQLGetter.getJdbcTemplate(src), SQLGetter.getBatchLimit(src), SQLGetter.getSetter(src), SQLGetter.getParser(src));
        }
        if(origin instanceof SyncSQL) {
            SyncSQL<T> src = (SyncSQL<T>) origin;
            ensurePartitionedTableExists(SQLGetter.getJdbcTemplate(src));
            return new SyncSQL<>(SQLGetter.getOption(src), makePartitionNativeSQL(SQLGetter.getSql(src)), SQLGetter.getJdbcTemplate(src), SQLGetter.getBatchLimit(src), SQLGetter.getSetter(src), SQLGetter.getParser(src));
        }
        if(origin instanceof NilSQL) {
            return origin;
        }
        throw new IllegalArgumentException(String.format("Unsupported SQL type: %s", origin.getClass()));
    }

    @Override
    protected SQL<T> getQryall() {
        return qryall == null ? (qryall = makePartitionSQL(TypeQuery.Getter.getQryAll(origin))) : qryall;
    }

    @Override
    protected SQL<T> getQryKey() {
        return qrykey == null ? (qrykey = makePartitionSQL(TypeQuery.Getter.getQryKey(origin))) : qrykey;
    }
    @Override
    protected SQL<T> getInsert() {
        return insert == null ? (insert = makePartitionSQL(TypeQuery.Getter.getInsert(origin))) : insert;
    }
    @Override
    protected SQL<T> getUpsert() {
        return upsert == null ? (upsert = makePartitionSQL(TypeQuery.Getter.getUpsert(origin))) : upsert;
    }
    @Override
    protected SQL<T> getUpdate() {
        return update == null ? (update = makePartitionSQL(TypeQuery.Getter.getUpdate(origin))) : update;
    }
    @Override
    protected SQL<T> getDelete() {
        return delete == null ? (delete = makePartitionSQL(TypeQuery.Getter.getDelete(origin))) : delete;
    }
    @Override
    public SQL<T> getSQL(int index) {
        return (tsqls == null ? (tsqls = makePartitionTSqls(TypeQuery.Getter.getTSqls(origin))) : tsqls)[index];
    }
    private  SQL<T>[] makePartitionTSqls(SQL<T>[] src) {
        if(src != null) {
            @SuppressWarnings("unchecked")
            SQL<T>[] dst = new SQL[src.length];
            for (int i = 0; i < src.length; i++) {
                dst[i] = makePartitionSQL(src[i]);
            }
            return dst;
        }
        return null;
    }
    
}
