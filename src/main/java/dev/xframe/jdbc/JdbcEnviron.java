package dev.xframe.jdbc;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.sql.DataSource;

import dev.xframe.jdbc.codec.Delimiters;
import dev.xframe.jdbc.codec.FieldCodec;
import dev.xframe.jdbc.codec.FieldCodecs;
import dev.xframe.jdbc.sequal.SQLExecutor;

public class JdbcEnviron {
	
	static int asyncThreads;
	
	static Map<Object, JdbcTemplate> jdbcTemplates = new HashMap<>();
	
	static int instupUsage;
	
	static Function<String, String> fieldMapper = Function.identity();
	
	static SQLExecutor executor;

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
		public EnvironConfigurator setDatasource(Object dbKey, DataSource dataSource) {
			jdbcTemplates.put(dbKey, new JdbcTemplate(dataSource));
			return this;
		}
		
		/**
		 * @param usage: 0 self, 1 insert, 2 update
		 */
		public EnvironConfigurator setInstupUsage(boolean insert, boolean update) {
			instupUsage += (insert ? 1 : 0);
			instupUsage += (update ? 2 : 0);
			return this;
		}
		
		/**
		 * global column name & field name mapping
		 * @param mapper
		 */
		public EnvironConfigurator setFieldMapper(Function<String, String> mapper) {
			fieldMapper = mapper;
			return this;
		}
		
		/**
		 * global field val & column val mapping
		 * @param type
		 * @param codec
		 */
		public EnvironConfigurator setFieldCodec(Class<?> type, FieldCodec<?, ?> codec) {
			FieldCodecs.addDefault(type, codec);
			return this;
		}
		public EnvironConfigurator setFieldCodec(Predicate<Class<?>> p, Function<Field, FieldCodec<?, ?>> fc) {
			FieldCodecs.addDefault(p, fc);
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

	public static JdbcTemplate getJdbcTemplate(Object dbKey) {
		return jdbcTemplates.get(dbKey);
	}

	public static int getInstupUsage() {
		return instupUsage;
	}

}
