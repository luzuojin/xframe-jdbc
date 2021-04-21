package dev.xframe.jdbc.codec.transfer;

import java.lang.reflect.Field;

import dev.xframe.jdbc.codec.FieldCodec;
import dev.xframe.jdbc.codec.InternalReflection;

public class Importers {
    public static Importer of(Field field, final int columnIndex) {
        return of(field, columnIndex, null);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Importer of(Field field, final int columnIndex, final FieldCodec fc) {
    	InternalReflection.setAccessible(field);
        if(fc != null) {
            return (obj, rs) -> {
                field.set(obj, fc.decode(rs.getObject(columnIndex)));
            };
        }
        Class<?> type = field.getType();
        if(type == boolean.class) {
            return (obj, rs) -> {
                field.setBoolean(obj, rs.getBoolean(columnIndex));
            };
        }
        if(type == byte.class) {
            return (obj, rs) -> {
                field.setByte(obj, rs.getByte(columnIndex));
            };
        }
        if(type == short.class) {
            return (obj, rs) -> {
                field.setShort(obj, rs.getShort(columnIndex));
            };
        }
        if(type == int.class) {
            return (obj, rs) -> {
                field.setInt(obj, rs.getInt(columnIndex));
            };
        }
        if(type == float.class) {
            return (obj, rs) -> {
                field.setFloat(obj, rs.getFloat(columnIndex));
            };
        }
        if(type == long.class) {
            return (obj, rs) -> {
                field.setLong(obj, rs.getLong(columnIndex));
            };
        }
        if(type == double.class) {
            return (obj, rs) -> {
                field.setDouble(obj, rs.getDouble(columnIndex));
            };
        }
        return (obj, rs) -> {
            field.set(obj, rs.getObject(columnIndex));
        };
    }
}
