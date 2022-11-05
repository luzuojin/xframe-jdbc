package dev.xframe.jdbc.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Parameter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypeFactory;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.codec.transfer.FieldPSSetter;
import dev.xframe.jdbc.codec.transfer.FieldPSSetters;
import dev.xframe.jdbc.codec.transfer.FieldInvokder;
import dev.xframe.jdbc.codec.transfer.FieldRSGetter;
import dev.xframe.jdbc.codec.transfer.FieldRSGetters;
import dev.xframe.utils.XLambda;
import dev.xframe.utils.XReflection;

@SuppressWarnings({"unchecked"})
public class CodecFactory {
    
    public static <T> RSParser<T> newParser(Class<?> cls, TypeFactory<T> tFactory, TypeHandler<T> tHandler, FieldCodecSet fcSet, List<Field> columns) throws Exception {
        if(XReflection.getConstructor(cls) != null) {//empty paramters constructor
            FieldRSGetter[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeImporter(fcSet, columns.get(i), i+1)).toArray(FieldRSGetter[]::new);
            return new FieldsRSParser<>(tFactory == null ? new DefaultFactory<>(cls) : tFactory, tHandler, fields); 
        } else {
            //暂时只匹配参数数量一致的构造函数
            Constructor<?> constructor = Arrays.stream(cls.getConstructors())
                    .filter(c->c.getParameterCount()==columns.size())
                    .findAny().orElseThrow(()->new IllegalArgumentException(String.format("Columns[%s] matched constructor not found", columns.toString())));
            Parameter[] parameters = constructor.getParameters();
            Map<String, Integer> argsIndexMap = IntStream.range(0, parameters.length).boxed().collect(Collectors.toMap(i->parameters[i].getName(), i->i));
            FieldRSGetter[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeImporter(new FieldInvokder.ArrayBased(columns.get(i).getType(), argsIndexMap.get(columns.get(i).getName())), fcSet, columns.get(i), i+1)).toArray(FieldRSGetter[]::new);
            return new FieldsRSParser<>(new ArraydFactory<>(cls, constructor.getParameterTypes()), tHandler, fields);
        }
	}
    
    public static <T> TypePSSetter<T> newSetter(FieldCodecSet fcSet, List<Field> columns) throws Exception {
        FieldPSSetter[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeExporter(fcSet, columns.get(i), i+1)).toArray(FieldPSSetter[]::new);
        return new FieldsTypePSSetter<>(fields);
    }
	
    static FieldRSGetter makeImporter(FieldCodecSet fcSet, Field c, int columnIndex) {
        return makeImporter(new FieldInvokder.PojoBased(c), fcSet, c, columnIndex);
    }
    static FieldRSGetter makeImporter(FieldInvokder field, FieldCodecSet fcSet, Field c, int columnIndex) {
        return FieldRSGetters.of(field, columnIndex, fcSet.get(c));
    }
    static FieldPSSetter makeExporter(FieldCodecSet fcSet, Field c, int paramIndex) {
        return FieldPSSetters.of(new FieldInvokder.PojoBased(c), paramIndex, fcSet.get(c));
    }
    
    static class DefaultFactory<T> implements TypeFactory<T> {
        private Supplier<T> factory;
        public DefaultFactory(Class<?> cls) {
            this.factory = XLambda.createByConstructor(cls);
        }
        public Object make(ResultSet rs) throws Exception {
            return factory.get();
        }
    }
    
    //使用构造赋值所有属性
    static class ArraydFactory<T> implements TypeFactory<T> {
        private int argsLen;
        private Constructor<T> factory;
        public ArraydFactory(Class<?> cls, Class<?>[] argTypes) {
            this.argsLen = argTypes.length;
            this.factory = (Constructor<T>) XReflection.getConstructor(cls, argTypes);
        }
        public Object make(ResultSet rs) throws Exception {
            return new Object[argsLen];
        }
        public T resolve(Object obj) throws Exception {
            return factory.newInstance((Object[]) obj);
        }
    }
    
	static class FieldsRSParser<T> implements RSParser<T> {
	    final TypeFactory<T> factory;
		final TypeHandler<T> handler;
		final FieldRSGetter[] fields;
		public FieldsRSParser(TypeFactory<T> factory, TypeHandler<T> handler, FieldRSGetter[] fields) {
		    this.factory = factory;
			this.handler = handler;
			this.fields = fields;
		}
		public T parse(ResultSet rs) throws Exception {
			Object o = factory.make(rs);
			for (FieldRSGetter field : fields) {
                field.apply(o, rs);
            }
			T t = factory.resolve(o);
			if(handler != null) 
			    handler.apply(t);
			return t;
		}
	}
	
	static class FieldsTypePSSetter<T> implements TypePSSetter<T> {
		final FieldPSSetter[] fields;
		public FieldsTypePSSetter(FieldPSSetter[] fields) {
			this.fields = fields;
		}
		public void set(PreparedStatement pstmt, T obj) throws Exception {
		    for (FieldPSSetter field : fields) {
                field.apply(obj, pstmt);
            }
		}
	}

}
