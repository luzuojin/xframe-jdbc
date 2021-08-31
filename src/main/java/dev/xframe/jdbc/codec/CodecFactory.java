package dev.xframe.jdbc.codec;

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypeFactory;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.builder.analyse.FCodecSet;
import dev.xframe.jdbc.builder.analyse.FColumn;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.builder.analyse.JColumn;
import dev.xframe.jdbc.codec.transfer.Exporter;
import dev.xframe.jdbc.codec.transfer.Exporters;
import dev.xframe.jdbc.codec.transfer.FieldWrap;
import dev.xframe.jdbc.codec.transfer.Importer;
import dev.xframe.jdbc.codec.transfer.Importers;
import dev.xframe.utils.XCaught;
import dev.xframe.utils.XReflection;

@SuppressWarnings({"unchecked"})
public class CodecFactory {
    
    public static <T> RSParser<T> newParser(FTable ftable, List<FColumn> columns) throws Exception {
        return newParser(ftable.clazz, ftable.typeFactory(), ftable.typeHandler(), ftable.codecs, columns.stream().map(c->c.jColumn).collect(Collectors.toList()));
    }
    public static <T> RSParser<T> newParser(Class<?> cls, TypeFactory<T> tFactory, TypeHandler<T> tHandler, FCodecSet fcSet, List<JColumn> columns) throws Exception {
        if(XReflection.getConstructor(cls) != null) {
            Importer[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeImporter(fcSet, columns.get(i), i+1)).toArray(Importer[]::new);
            return new FieldsRSParser<>(tFactory == null ? new DefaultFactory<>(cls) : tFactory, tHandler, fields); 
        } else {
            Constructor<?> constructor = getConstructorByParamsCount(cls, columns.size());
            if(constructor == null)
                throw new IllegalArgumentException("Constructor parameters don`t matched columns");
            Parameter[] parameters = constructor.getParameters();
            Map<String, Integer> argsIndexMap = IntStream.range(0, parameters.length).mapToObj(Integer::valueOf).collect(Collectors.toMap(i->parameters[i].getName(), i->i));
            Importer[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeImporter(new FieldWrap.ArrayBased(columns.get(i).field.getType(), argsIndexMap.get(columns.get(i).name)), fcSet, columns.get(i), i+1)).toArray(Importer[]::new);
            return new FieldsRSParser<>(new ArraydFactory<>(cls, constructor.getParameterTypes()), tHandler, fields);
        }
	}
    private static Constructor<?> getConstructorByParamsCount(Class<?> cls, int paramsCount) {
        Constructor<?>[] constructors = cls.getConstructors();
        for (Constructor<?> constructor : constructors) {
            if(constructor.getParameters().length == paramsCount) {
                return constructor;
            }
        }
        return null;
    }

    public static <T> TypePSSetter<T> newSetter(FTable ftable, List<FColumn> columns) throws Exception {
        return newSetter(ftable.codecs, columns.stream().map(c->c.jColumn).collect(Collectors.toList()));
    }
    public static <T> TypePSSetter<T> newSetter(FCodecSet fcSet, List<JColumn> columns) throws Exception {
        Exporter[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeExporter(fcSet, columns.get(i), i+1)).toArray(Exporter[]::new);
        return new FieldsTypePSSetter<>(fields);
    }
	
    static Importer makeImporter(FCodecSet fcSet, JColumn c, int columnIndex) {
        return makeImporter(new FieldWrap.PojoBased(c.field), fcSet, c, columnIndex);
    }
    static Importer makeImporter(FieldWrap field, FCodecSet fcSet, JColumn c, int columnIndex) {
        return Importers.of(field, columnIndex, fcSet.get(c.name));
    }
    static Exporter makeExporter(FCodecSet fcSet, JColumn c, int paramIndex) {
        return Exporters.of(new FieldWrap.PojoBased(c.field), paramIndex, fcSet.get(c.name));
    }
    
    static class DefaultFactory<T> implements TypeFactory<T> {
        private Constructor<T> factory;
        public DefaultFactory(Class<?> cls) {
            try {
                this.factory = (Constructor<T>) XReflection.getConstructor(cls);
            } catch (Throwable e) {
                throw XCaught.throwException(e);
            }
        }
        public Object make(ResultSet rs) throws Exception {
            return factory.newInstance();
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
		final Importer[] fields;
		public FieldsRSParser(TypeFactory<T> factory, TypeHandler<T> handler, Importer[] fields) {
		    this.factory = factory;
			this.handler = handler;
			this.fields = fields;
		}
		public T parse(ResultSet rs) throws Exception {
			Object o = factory.make(rs);
			for (Importer field : fields) {
                field.imports(o, rs);
            }
			T t = factory.resolve(o);
			if(handler != null) 
			    handler.apply(t);
			return t;
		}
	}
	
	static class FieldsTypePSSetter<T> implements TypePSSetter<T> {
		final Exporter[] fields;
		public FieldsTypePSSetter(Exporter[] fields) {
			this.fields = fields;
		}
		public void set(PreparedStatement pstmt, T obj) throws Exception {
		    for (Exporter field : fields) {
                field.exports(obj, pstmt);
            }
		}
	}

}
