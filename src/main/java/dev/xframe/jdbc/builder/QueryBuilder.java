package dev.xframe.jdbc.builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import dev.xframe.jdbc.JdbcEnviron;
import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.TypeQuery;
import dev.xframe.jdbc.TypeQuery.SQLSetter;
import dev.xframe.jdbc.builder.analyse.Analyzer;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.codec.FieldCodec;
import dev.xframe.jdbc.codec.FieldCodecs;
import dev.xframe.jdbc.datasource.DBIdent;
import dev.xframe.jdbc.sequal.SQL;
import dev.xframe.jdbc.sequal.SQLFactory;
import dev.xframe.jdbc.sql.TypeSQL;

public class QueryBuilder<T> {
	
	private Class<T> type;
	
	private DBIdent dbKey;
	
	private String tableName;
	
	private TypeHandler<T> typeHandler;
	
	//field name or class name
	private FieldCodecs fieldCodecs = new FieldCodecs();
	
	private Map<String, String> fieldMappings = new HashMap<>();

	private int asyncModel = -1;
	
	private int upsertUsage = -1;
	
	private int ignore = 0;
	
	private TypeSQL[] tsqls = new TypeSQL[0];

	public QueryBuilder(Class<T> clazz) {
		this.type = clazz;
	}
	
	public QueryBuilder<T> setTable(DBIdent dbKey, String tableName) {
		this.dbKey = dbKey;
		this.tableName = tableName;
		return this;
	}
	
	public QueryBuilder<T> setTypeHandler(TypeHandler<T> typeHandler) {
		this.typeHandler = typeHandler;
		return this;
	}
	
	public QueryBuilder<T> setMapping(String fieldName, String columnName) {
	    this.fieldMappings.put(fieldName, columnName);
	    return this;
	}
	
	public QueryBuilder<T> setFieldCodec(String fieldName, FieldCodec<?, ?> fieldCodec) {
		this.fieldCodecs.add(fieldName, fieldCodec);
		return this;
	}
	public QueryBuilder<T> setFieldCodec(Class<?> fieldType, FieldCodec<?, ?> fieldCodec) {
		this.fieldCodecs.add(fieldType, fieldCodec);
		return this;
	}
	public QueryBuilder<T> setFieldCodec(Function<T, Object> getter, FieldCodec<?, ?> fieldCodec) {
        this.fieldCodecs.add(getter, fieldCodec);
        return this;
    }
	public QueryBuilder<T> setFieldCodec(Function<T, Object> getter, int delimiters) {
	    this.fieldCodecs.add(getter, delimiters);
	    return this;
	}
	
	public QueryBuilder<T> setSQL(int index, TypeSQL tsql) {
		int nl = Math.max(index, tsqls.length) + 1;
        tsqls = Arrays.copyOf(tsqls, nl);
        tsqls[index] = tsql;
		return this;
	}
	
	public QueryBuilder<T> setAsyncModel(boolean asyncModel) {
		this.asyncModel = asyncModel ? 1 : 0;
		return this;
	}
	
	/**
	 * 1 << 0	insert
	 * 1 << 1	upsert
	 * 1 << 2	update
	 * 1 << 3	delete
	 * 1 << 4	qrykey
	 * 1 << 5	qryall
	 * 
	 * 0xFF		ingoreall
	 * @param ignore
	 * @return
	 */
	public QueryBuilder<T> setIgnore(int ignore) {
		this.ignore = ignore;
		return this;
	}
	
	public QueryBuilder<T> setUpsertUsage(boolean insert, boolean update) {
		this.upsertUsage = 0;
		this.upsertUsage += (insert ? 1 : 0);
		this.upsertUsage += (update ? 2 : 0);
		return this;
	}
	
	private int getUpsertUsage() {
		return this.upsertUsage == -1 ? JdbcEnviron.getUpsertUsage() : this.upsertUsage;
	}
	
	public TypeQuery<T> build() {
		try {
		    if(dbKey == null) throw new IllegalArgumentException("dbKey is empty");
		    if(tableName == null) throw new IllegalArgumentException("tableName is empty");
		    
			JdbcTemplate jdbcTemplate = JdbcEnviron.getJdbcTemplate(dbKey);
			
			FTable ftable = Analyzer.analyze(type, tableName, jdbcTemplate, fieldMappings, fieldCodecs);
			ftable.setTypeHandler(typeHandler);
			
			TypeQuery<T> query = new TypeQuery<>();
			SQLSetter<T> setter = new SQLSetter<>();
			
			SQLFactory<T> factory = new SQLFactory<>(isAsyncModel(), jdbcTemplate);
			
			SQL<T> upsert = SQLBuilder.buildUpsertSQL(factory, ftable);
			if((ignore & (1 << 0)) == 0)
				setter.setInsert(query, (getUpsertUsage() & 1) > 0 ? upsert : SQLBuilder.buildInsertSQL(factory, ftable));
			if((ignore & (1 << 1)) == 0)
				setter.setUpsert(query, upsert);
			if((ignore & (1 << 2)) == 0)
				setter.setUpdate(query, (getUpsertUsage() & 2) > 1 ? upsert : SQLBuilder.buildUpdateSQL(factory, ftable));
			if((ignore & (1 << 3)) == 0)
				setter.setDelete(query, SQLBuilder.buildDeleteSQL(factory, ftable));
			if((ignore & (1 << 4)) == 0)
				setter.setQryKey(query, SQLBuilder.buildQryKeySQL(factory, ftable));
			if((ignore & (1 << 5)) == 0)
				setter.setQryAll(query, SQLBuilder.buildQuerySQL(factory, ftable));
			
			@SuppressWarnings("unchecked")
			SQL<T>[] xtsqls = new SQL[tsqls.length];
			for (int i = 0; i < this.tsqls.length; i++) {
				if(tsqls[i] != null) {
					xtsqls[i] = tsqls[i].buildSQL(factory, ftable);
				}
			}
			setter.setTSqls(query, xtsqls);
			
			return query;
		} catch (Throwable e) {QueryBuilder.throwException0(e);}
		return null;
	}
	
    @SuppressWarnings("unchecked")
    static <E extends Throwable> void throwException0(Throwable e) throws E {
        throw (E) e;
    }

	private boolean isAsyncModel() {
		return JdbcEnviron.isAsyncModel() && (asyncModel == 1 || asyncModel == -1);
	}

}
