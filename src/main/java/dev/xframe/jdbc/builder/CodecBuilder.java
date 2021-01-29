package dev.xframe.jdbc.builder;

import java.lang.reflect.Constructor;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;
import java.util.stream.IntStream;

import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypeFactory;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.builder.analyse.FColumn;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.codec.transfer.Exporter;
import dev.xframe.jdbc.codec.transfer.Exporters;
import dev.xframe.jdbc.codec.transfer.Importer;
import dev.xframe.jdbc.codec.transfer.Importers;

@SuppressWarnings({"unchecked"})
public class CodecBuilder {
	
	static <T> RSParser<T> buildParser(FTable ftable, List<FColumn> columns) throws Exception {
		Importer[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeImporter(ftable, columns.get(i), i+1)).toArray(Importer[]::new);
		return new FieldsRSParser<>(castFactory(ftable.typeFactory, ftable.clazz), castHandler(ftable.typeHandler), fields); 
	}
	
    static <T> TypeFactory<T> castFactory(TypeFactory<?> typeFactory, Class<?> clazz) {
        if(typeFactory == null) {
            return new DefTypeFactory<>(clazz);
        }
        return (TypeFactory<T>) typeFactory;
    }

    static <T> TypeHandler<T> castHandler(TypeHandler<?> typeHandler) {
        return (TypeHandler<T>) typeHandler;
    }

    static <T> TypePSSetter<T> buildSetter(FTable ftable, List<FColumn> columns) throws Exception {
    	Exporter[] fields = IntStream.range(0, columns.size()).mapToObj(i->makeExporter(ftable, columns.get(i), i+1)).toArray(Exporter[]::new);
    	return new FieldsTypePSSetter<>(fields);
    }
	
    static Importer makeImporter(FTable t, FColumn c, int columnIndex) {
        return Importers.of(c.jColumn.field, columnIndex, t.codecs.get(c.jColumn.name));
    }
    static Exporter makeExporter(FTable t, FColumn c, int paramIndex) {
        return Exporters.of(c.jColumn.field, paramIndex, t.codecs.get(c.jColumn.name));
    }
    
    static class DefTypeFactory<T> implements TypeFactory<T> {
        private Constructor<T> factory;
        public DefTypeFactory(final Class<?> clazz) {
            try {
                Constructor<?> c = clazz.getConstructor();
                c.setAccessible(true);
                this.factory = (Constructor<T>) c;
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
        public T make(ResultSet rs) throws Exception {
            return factory.newInstance();
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
			T obj = factory.make(rs);
			for (Importer field : fields) {
                field.imports(obj, rs);
            }
			if(handler != null) 
			    handler.apply(obj);
			return obj;
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
