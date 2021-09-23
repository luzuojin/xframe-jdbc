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
	
	public TypeQuery<T> partition(String pName) {
	    return this;
	}
	
	protected String table;
	
	protected SQL<T> qryall;
	protected SQL<T> getQryall() {
	    return qryall;
	}
	public List<T> fetchAll() {
		return getQryall().fetchMany(PSSetter.NONE);
	}
	
	protected SQL<T> qrykey;
	protected SQL<T> getQryKey() {
	    return qrykey;
	}
	public T fetchOne(Object key) {
		return getQryKey().fetchOne(PSSetter.of(key));
	}
	public T fetchOne(PSSetter key) {
	    return getQryKey().fetchOne(key);
	}
	
	protected SQL<T> insert;
	protected SQL<T> getInsert() {
	    return insert;
	}
	public boolean insert(T obj) {
		return getInsert().update(obj);
	}
	public boolean insertBatch(Collection<T> objs) {
        return getInsert().updateBatch(objs);
    }
	public boolean insertBatch(T[] objs) {
	    return getInsert().updateBatch(objs);
	}
	public long insertAndIncrement(T obj) {
        return getInsert().insertAndIncrement(obj);
    }
	
	protected SQL<T> upsert;
	protected SQL<T> getUpsert() {
	    return upsert;
	}
    public boolean upsert(T obj) {
        return getUpsert().update(obj);
    }
    public boolean upsertBatch(T[] objs) {
        return getUpsert().updateBatch(objs);
    }
    public boolean upsertBatch(Collection<T> objs) {
        return getUpsert().updateBatch(objs);
    }
	
    protected SQL<T> update;
    protected SQL<T> getUpdate() {
        return update;
    }
	public boolean update(T obj) {
	    return getUpdate().update(obj);
	}
	public boolean updateBatch(T[] objs) {
	    return getUpdate().updateBatch(objs);
	}
	public boolean updateBatch(Collection<T> objs) {
	    return getUpdate().updateBatch(objs);
	}
	
	protected SQL<T> delete;
	protected SQL<T> getDelete() {
	    return delete;
	}
	public boolean delete(T obj) {
	    return getDelete().update(obj);
	}
	public boolean delete(PSSetter setter) {
	    return getDelete().update(setter);
	}
	
	protected SQL<T>[] tsqls;
	public SQL<T> getSQL(int index) {
		return tsqls[index];
	}
	
	public static class Setter {
	    public static <T> void setTable(TypeQuery<T> query, String table) {
	        query.table = table;
	    }
		public static <T> void setQryAll(TypeQuery<T> query, SQL<T> sql) {
			query.qryall = sql;
		}
		public static <T> void setQryKey(TypeQuery<T> query, SQL<T> sql) {
			query.qrykey = sql;
		}
		public static <T> void setInsert(TypeQuery<T> query, SQL<T> sql) {
			query.insert = sql;
		}
		public static <T> void setUpsert(TypeQuery<T> query, SQL<T> sql) {
			query.upsert = sql;
		}
		public static <T> void setUpdate(TypeQuery<T> query, SQL<T> sql) {
			query.update = sql;
		}
		public static <T> void setDelete(TypeQuery<T> query, SQL<T> sql) {
			query.delete = sql;
		}
		public static <T> void setTSqls(TypeQuery<T> query, SQL<T>[] tsqls) {
			query.tsqls = tsqls;
		}
	}
	public static class Getter {
	    public static <T> String getTable(TypeQuery<T> query) {
            return query.table;
        }
        public static <T> SQL<T> getQryAll(TypeQuery<T> query) {
            return query.qryall;
        }
        public static <T> SQL<T> getQryKey(TypeQuery<T> query) {
            return query.qrykey;
        }
        public static <T> SQL<T> getInsert(TypeQuery<T> query) {
            return query.insert;
        }
        public static <T> SQL<T> getUpsert(TypeQuery<T> query) {
            return query.upsert;
        }
        public static <T> SQL<T> getUpdate(TypeQuery<T> query) {
            return query.update;
        }
        public static <T> SQL<T> getDelete(TypeQuery<T> query) {
            return query.delete;
        }
        public static <T> SQL<T>[] getTSqls(TypeQuery<T> query) {
            return query.tsqls;
        }
    }
	
}
