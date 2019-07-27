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
import dev.xframe.jdbc.codec.FieldCodec;

@SuppressWarnings({"rawtypes","unchecked"})
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
		
		XField[] fields = columns.stream().map(c->new XField(c.jColumn.field, ftable.codecs.get(c.jColumn.name))).toArray(len->new XField[len]);
		return new FieldsRSParser<>(new SimpleTypeHandler<>(ftable.clazz, ftable.typeHandler), fields); 
	}
	
    static <T> TypePSSetter<T> buildSetter(FTable ftable, List<FColumn> columns) throws Exception {
    	if(useDynamicCode())
    		return DynamicCodes.makeSetter(ftable, columns);

    	XField[] fields = columns.stream().map(c->new XField(c.jColumn.field, ftable.codecs.get(c.jColumn.name))).toArray(len->new XField[len]);
    	return new FieldsTypePSSetter<>(fields);
    }
	
	static class SimpleTypeHandler<T> implements TypeHandler<T> {
		Constructor<T> constr;
		TypeHandler<T> setted;
		public SimpleTypeHandler(Class<?> clazz, TypeHandler<?> setted) {
			try {
				this.constr = (Constructor<T>) clazz.getDeclaredConstructor();
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
	
	static class XField {
		final Field field;
		final FieldCodec codec;
		public XField(Field field, FieldCodec codec) {
			this.field = field;
			this.codec = codec;
			this.field.setAccessible(true);
		}
		public Object getFrom(Object obj) throws Exception {
			Object f = field.get(obj);
			return codec == null ? f : codec.encode(f);
		}
		public void setTo(Object obj, Object f) throws Exception {
			field.set(obj, codec == null ? f : codec.decode(f));
		}
	}
	
	static class FieldsRSParser<T> implements RSParser<T> {
		final TypeHandler<T> handler;
		final XField[] fields;
		public FieldsRSParser(TypeHandler<T> handler, XField[] fields) {
			this.handler = handler;
			this.fields = fields;
		}
		@Override
		public T parse(ResultSet rs) throws Exception {
			T obj = handler.make(rs);
			for (int i = 0; i < fields.length; i++) {
				fields[i].setTo(obj, rs.getObject(i+1));
			}
			handler.apply(obj);
			return obj;
		}
	}
	
	static class FieldsTypePSSetter<T> implements TypePSSetter<T> {
		final XField[] fields;
		public FieldsTypePSSetter(XField[] fields) {
			this.fields = fields;
		}
		@Override
		public void set(PreparedStatement pstmt, T obj) throws Exception {
			for (int i = 0; i < fields.length; i++) {
				pstmt.setObject(i+1, fields[i].getFrom(obj));
			}
		}
	}

}
