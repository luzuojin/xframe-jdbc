package dev.xframe.jdbc.builder;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.builder.analyse.FColumn;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.builder.javassist.DynamicCodes;

public class CodecBuilder {
	
	static boolean useDynamicCode() {
		try {
			Class.forName("javassist.ClassPool");
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	static <T> RSParser<T> buildParser(FTable ftable, List<FColumn> columns) throws Exception {
		if(useDynamicCode())
			return DynamicCodes.makeParser(ftable, columns);
		
		Field[] fields = columns.stream().map(c->c.jColumn.field).toArray(len->new Field[len]);
		return new FieldsRSParser<>(new SimpleTypeHandler<>(ftable.clazz, ftable.typeHandler), fields); 
	}
	
    static <T> TypePSSetter<T> buildSetter(FTable ftable, List<FColumn> columns) throws Exception {
    	if(useDynamicCode())
    		return DynamicCodes.makeSetter(ftable, columns);

		Field[] fields = columns.stream().map(c->c.jColumn.field).toArray(len->new Field[len]);
    	return new FieldsTypePSSetter<>(fields);
    }
	
	static class SimpleTypeHandler<T> implements TypeHandler<T> {
		Constructor<T> constr;
		TypeHandler<T> setted;
		@SuppressWarnings("unchecked")
		public SimpleTypeHandler(Class<?> clazz, TypeHandler<?> setted) {
			try {
				this.constr = (Constructor<T>) clazz.getConstructor();
				this.setted = (TypeHandler<T>) setted;
				this.constr.setAccessible(true);
			} catch (Exception e) {
				throw new IllegalArgumentException(e);
			}
		}
		public T make(ResultSet rs) throws Exception {
			T obj = null;
			if(setted != null) {
				obj = setted.make(rs);
			}
			if(obj == null) {
				obj = constr.newInstance();
			}
			return obj;
		}
		public void apply(T t) {
			if(setted != null) setted.apply(t);
		}
	}
	
	static class FieldsRSParser<T> implements RSParser<T> {
		final TypeHandler<T> handler;
		final Field[] fields;
		public FieldsRSParser(TypeHandler<T> handler, Field[] fields) {
			this.handler = handler;
			this.fields = fields;
		}
		@Override
		public T parse(ResultSet rs) throws Exception {
			T obj = handler.make(rs);
			for (int i = 0; i < fields.length; i++) {
				fields[i].set(obj, rs.getObject(i+1));
			}
			handler.apply(obj);
			return obj;
		}
	}
	
	static class FieldsTypePSSetter<T> implements TypePSSetter<T> {
		final Field[] fields;
		public FieldsTypePSSetter(Field[] fields) {
			this.fields = fields;
		}
		@Override
		public void set(PreparedStatement pstmt, T obj) throws Exception {
			for (int i = 0; i < fields.length; i++) {
				pstmt.setObject(i+1, fields[i].get(obj));
			}
		}
	}

}
