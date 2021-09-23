package dev.xframe.jdbc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.sql.DataSource;

import dev.xframe.jdbc.builder.FieldMapping;
import dev.xframe.jdbc.codec.Delimiters;
import dev.xframe.jdbc.codec.FieldCodec;
import dev.xframe.jdbc.codec.FieldCodecSet;
import dev.xframe.jdbc.codec.provides.ArrayCodec;
import dev.xframe.jdbc.codec.provides.EnumCodec;
import dev.xframe.jdbc.codec.provides.ListSetCodec;
import dev.xframe.jdbc.datasource.DBIdent;
import dev.xframe.jdbc.partition.PartitionStrategy;
import dev.xframe.jdbc.sequal.SQLExecutor;

public class JdbcEnviron {
	
	static int asyncThreads;
	
	static Map<DBIdent, JdbcTemplate> jdbcTemplates = new HashMap<>();
	
	static int upsertUsage;
	
	//默认支持: Enum(@Codec),Array,List,Set
	static FieldCodecSet.Typed fcSet = new FieldCodecSet.Typed()
                                	        .add(Class::isEnum, EnumCodec::build)
                                	        .add(ArrayCodec::isArray, ArrayCodec::build)
                                	        .add(ListSetCodec::isListOrSet, ListSetCodec::build);
	
	static FieldMapping fieldMapping =  c -> c;
	
	static SQLExecutor executor;
	
	//for partition
	static PartitionStrategy partitionStrategy = PartitionStrategy.Simulate;
	//default: tableName_partitionName
	static BiFunction<String, String, String> partitionTableNameFunc = (t, p) -> String.format("%s_%s", t, p);
	
	public static EnvironConfigurator getConfigurator() {
		return new EnvironConfigurator();
	}
	
	public static class EnvironConfigurator {
		/**
		 * save data with async model
		 * @param nthreads
		 */
		public EnvironConfigurator setAsyncThreads(int nthreads) {
			asyncThreads = nthreads;
			return this;
		}
		
		/**
		 * @param delimiters 
		 * @see @Codec
		 */
		public EnvironConfigurator setDelimiters(char major, char minor) {
			Delimiters.setDefaults(major, minor);
			return this;
		}
		
		/**
		 * @param key: mark difference datasources
		 * @param dataSource
		 */
		public EnvironConfigurator setDatasource(DBIdent ident, DataSource dataSource) {
			jdbcTemplates.put(ident, JdbcTemplate.of(dataSource));
			return this;
		}
		
		/**
		 * @param usage: 0 self, 1 insert, 2 update
		 */
		public EnvironConfigurator setUpsertUsage(boolean insert, boolean update) {
			upsertUsage += (insert ? 1 : 0);
			upsertUsage += (update ? 2 : 0);
			return this;
		}
		
		/**
		 * global column name & field name mapping
		 * @param mapping
		 */
		public EnvironConfigurator setFieldMapping(FieldMapping mapping) {
			fieldMapping = mapping;
			return this;
		}
		
		/**
		 * global field val & column val mapping
		 * @param type
		 * @param fc
		 */
		public EnvironConfigurator setFieldCodec(Class<?> type, FieldCodec<?, ?> fc) {
		    fcSet.add(type, f->fc);
			return this;
		}
		public EnvironConfigurator setFieldCodec(Predicate<Class<?>> p, Function<Field, FieldCodec<?, ?>> fc) {
			fcSet.add(p, fc);
			return this;
		}
		
		/**
		 * parition strategy builtin or simulate
		 * @param strategy
		 */
		public EnvironConfigurator setPartitionStrategy(PartitionStrategy strategy) {
		    partitionStrategy = strategy;
		    return this;
		}
		/**
		 * @param strategy
		 * @param tableNameFunc (simulate partition table name via basic table name and partition name)
		 */
		public EnvironConfigurator setPartitionTableFunc(BiFunction<String, String, String> tableNameFunc) {
		    partitionTableNameFunc = tableNameFunc;
		    return this;
		}
	}

	public static synchronized SQLExecutor getExecutor() {
		if(asyncThreads > 0 && executor == null) {
			executor = new SQLExecutor(asyncThreads);
		}
		return executor;
	}

	public static boolean isAsyncModel() {
		return asyncThreads > 0;
	}

	public static JdbcTemplate getJdbcTemplate(DBIdent ident) {
		return jdbcTemplates.get(ident);
	}

	public static int getUpsertUsage() {
		return upsertUsage;
	}

	public static FieldCodecSet getFieldCodecSet() {
	    return fcSet;
	}
	
	public static FieldMapping getFieldMapping() {
	    return fieldMapping;
	}
	
	public static PartitionStrategy getPartitionStrategy() {
	    return partitionStrategy;
	}
	
	public static BiFunction<String, String, String> getPartitionTableNameFunc() {
	    return partitionTableNameFunc;
	}
}
