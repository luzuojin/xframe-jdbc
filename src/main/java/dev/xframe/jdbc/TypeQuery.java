package dev.xframe.jdbc;

import java.util.Collection;
import java.util.List;

import dev.xframe.jdbc.builder.QueryBuilder;
import dev.xframe.jdbc.sequal.SQL;

/**
 * Query database by java Type
 * @author luzj
 */
public class TypeQuery<T> {

	public static <X> QueryBuilder<X> newBuilder(Class<X> clazz) {
		return new QueryBuilder<>(clazz);
	}
	
	private SQL<T> qryall;
	public List<T> fetchAll() {
		return qryall.fetchMany(PSSetter.NONE);
	}
	
	private SQL<T> qrykey;
	public T fetchOne(Object key) {
		return qrykey.fetchOne(PSSetter.of(key));
	}
	public T fetchOne(PSSetter key) {
	    return qrykey.fetchOne(key);
	}
	
	private SQL<T> insert;
	public boolean insert(T obj) {
		return insert.update(obj);
	}
	public boolean insertBatch(Collection<T> objs) {
        return insert.updateBatch(objs);
    }
	public boolean insertBatch(T[] objs) {
	    return insert.updateBatch(objs);
	}
	
	public long insertAndIncrement(T obj) {
        return insert.insertAndIncrement(obj);
    }
	
	private SQL<T> upsert;
    public boolean upsert(T obj) {
        return upsert.update(obj);
    }
    public boolean upsertBatch(T[] objs) {
        return upsert.updateBatch(objs);
    }
    public boolean upsertBatch(Collection<T> objs) {
        return upsert.updateBatch(objs);
    }
	
	private SQL<T> update;
	public boolean update(T obj) {
	    return update.update(obj);
	}
	public boolean updateBatch(T[] objs) {
	    return update.updateBatch(objs);
	}
	public boolean updateBatch(Collection<T> objs) {
	    return update.updateBatch(objs);
	}
	
	private SQL<T> delete;
	public boolean delete(T obj) {
	    return delete.update(obj);
	}
	public boolean delete(PSSetter setter) {
	    return delete.update(setter);
	}
	
	private SQL<T>[] tsqls;
	public SQL<T> getSQL(int index) {
		return tsqls[index];
	}
	
	public static class SQLSetter<T> {
		public SQLSetter<T> setQryAll(TypeQuery<T> query, SQL<T> sql) {
			query.qryall = sql;
			return this;
		}
		public SQLSetter<T> setQryKey(TypeQuery<T> query, SQL<T> sql) {
			query.qrykey = sql;
			return this;
		}
		public SQLSetter<T> setInsert(TypeQuery<T> query, SQL<T> sql) {
			query.insert = sql;
			return this;
		}
		public SQLSetter<T> setUpsert(TypeQuery<T> query, SQL<T> sql) {
			query.upsert = sql;
			return this;
		}
		public SQLSetter<T> setUpdate(TypeQuery<T> query, SQL<T> sql) {
			query.update = sql;
			return this;
		}
		public SQLSetter<T> setDelete(TypeQuery<T> query, SQL<T> sql) {
			query.delete = sql;
			return this;
		}
		public SQLSetter<T> setTSqls(TypeQuery<T> query, SQL<T>[] tsqls) {
			query.tsqls = tsqls;
			return this;
		}
	}
	
}
