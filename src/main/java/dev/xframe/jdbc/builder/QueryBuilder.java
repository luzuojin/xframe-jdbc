package dev.xframe.jdbc.builder;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import dev.xframe.jdbc.JdbcEnviron;
import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.TypeFactory;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.TypeQuery;
import dev.xframe.jdbc.builder.analyse.Analyzer;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.codec.FieldCodec;
import dev.xframe.jdbc.codec.FieldCodecSet;
import dev.xframe.jdbc.datasource.DBIdent;
import dev.xframe.jdbc.partition.PartitionStrategy;
import dev.xframe.jdbc.partition.PartitioningQuery;
import dev.xframe.jdbc.sequal.SQL;
import dev.xframe.jdbc.sequal.SQLFactory;
import dev.xframe.jdbc.sql.TypeSQL;
import dev.xframe.utils.XCaught;

public class QueryBuilder<T> {
	
	protected Class<T> type;
	
	protected DBIdent dbKey;
	
	protected String tableName;
	
	protected TypeFactory<T> typeFactory;
	protected TypeHandler<T> typeHandler;
	
	//field name or class name
	protected FieldCodecSet.Customized fieldCodecs = new FieldCodecSet.Customized();
	//column name to field name
	protected FieldMapping.Customized fieldMapping = new FieldMapping.Customized();

	protected int asyncModel = -1;
	
	protected int upsertUsage = -1;
	
	protected int ignore = 0;
	
	protected TypeSQL[] tsqls = new TypeSQL[0];
	
	//for partition
	protected PartitionStrategy partitionStrategy;
	protected Function<T, String> partitionNameFunc;
	protected BiFunction<String, String, String> partitionTableNameFunc;
	
	public QueryBuilder(Class<T> clazz) {
		this.type = clazz;
	}
	
	public QueryBuilder<T> setTable(DBIdent dbKey, String tableName) {
		this.dbKey = dbKey;
		this.tableName = tableName;
		return this;
	}
	
	public QueryBuilder<T> setTypeFactory(TypeFactory<T> typeFactory) {
	    this.typeFactory = typeFactory;
	    return this;
	}
	public QueryBuilder<T> setTypeHandler(TypeHandler<T> typeHandler) {
		this.typeHandler = typeHandler;
		return this;
	}
	
	public QueryBuilder<T> setMapping(String fieldName, String columnName) {
	    this.fieldMapping.set(fieldName, columnName);
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
	
	public QueryBuilder<T> setPartitionalFunc(Function<T, String> partitionNameFunc) {
	    this.partitionNameFunc = partitionNameFunc;
	    return this;
	}
	public QueryBuilder<T> setPartitionalStrategy(PartitionStrategy partitionStrategy) {
	    this.partitionStrategy = partitionStrategy;
	    return this;
	}
	public QueryBuilder<T> setPartitionalTableFunc(BiFunction<String, String, String> partitionTableNameFunc) {
	    this.partitionTableNameFunc = partitionTableNameFunc;
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
			FieldCodecSet fcSet = new FieldCodecSet.Chained(fieldCodecs, JdbcEnviron.getFieldCodecSet());
			
            FTable ftable = Analyzer.analyze(type, tableName, jdbcTemplate, fcSet, FieldMapping.mixed(fieldMapping));
            ftable.setTypeFactory(typeFactory);
			ftable.setTypeHandler(typeHandler);
			
			SQLFactory<T> factory = new SQLFactory<>(isAsyncModel(), jdbcTemplate);
			
			TypeQuery<T> query = isPartitional() ? makePartitioningQuery() : new TypeQuery<>();
			TypeQuery.Setter.setTable(query, tableName);
			
			SQL<T> upsert = SQLBuilder.buildUpsertSQL(factory, ftable);
			if((ignore & (1 << 0)) == 0)
			    TypeQuery.Setter.setInsert(query, (getUpsertUsage() & 1) > 0 ? upsert : SQLBuilder.buildInsertSQL(factory, ftable));
			if((ignore & (1 << 1)) == 0)
			    TypeQuery.Setter.setUpsert(query, upsert);
			if((ignore & (1 << 2)) == 0)
			    TypeQuery.Setter.setUpdate(query, (getUpsertUsage() & 2) > 1 ? upsert : SQLBuilder.buildUpdateSQL(factory, ftable));
			if((ignore & (1 << 3)) == 0)
			    TypeQuery.Setter.setDelete(query, SQLBuilder.buildDeleteSQL(factory, ftable));
			if((ignore & (1 << 4)) == 0)
			    TypeQuery.Setter.setQryKey(query, SQLBuilder.buildQryKeySQL(factory, ftable));
			if((ignore & (1 << 5)) == 0)
			    TypeQuery.Setter.setQryAll(query, SQLBuilder.buildQuerySQL(factory, ftable));
			
			@SuppressWarnings("unchecked")
			SQL<T>[] xtsqls = new SQL[tsqls.length];
			for (int i = 0; i < this.tsqls.length; i++) {
				if(tsqls[i] != null) {
					xtsqls[i] = tsqls[i].buildSQL(factory, ftable);
				}
			}
			TypeQuery.Setter.setTSqls(query, xtsqls);
			
			return query;
		} catch (Throwable e) {
		    throw XCaught.throwException(e);
		}
	}

    private TypeQuery<T> makePartitioningQuery() {
        PartitionStrategy strategy = orElse(partitionStrategy, JdbcEnviron.getPartitionStrategy());
        BiFunction<String, String, String> tFunc = strategy == PartitionStrategy.Builtin ?
                PartitionStrategy.BuiltinTableNameFunc :
                orElse(partitionTableNameFunc, JdbcEnviron.getPartitionTableNameFunc());
        return new PartitioningQuery<>(partitionNameFunc, tFunc, strategy == PartitionStrategy.Builtin);
    }

    private boolean isPartitional() {
        return partitionNameFunc != null;
    }
	private boolean isAsyncModel() {
		return JdbcEnviron.isAsyncModel() && (asyncModel == 1 || asyncModel == -1);
	}
	private <X> X orElse(X x, X d) {
        return x == null ? d : x;
    }

}
