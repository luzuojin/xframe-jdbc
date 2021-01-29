package dev.xframe.jdbc.codec.transfer;

import java.lang.reflect.Field;

import dev.xframe.jdbc.codec.FieldCodec;

public class Importers {
    public static Importer of(Field field, final int columnIndex) {
        return of(field, columnIndex, null);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Importer of(Field field, final int columnIndex, final FieldCodec fc) {
        final long fieldOffset = Unsafe.getFieldOffset(field);
        if(fc != null) {
            return (obj, rs) -> {
                Unsafe.setObject(obj, fieldOffset, fc.decode(rs.getObject(columnIndex)));
            };
        }
        Class<?> type = field.getType();
        if(type == boolean.class) {
            return (obj, rs) -> {
                Unsafe.setBoolean(obj, fieldOffset, rs.getBoolean(columnIndex));
            };
        }
        if(type == byte.class) {
            return (obj, rs) -> {
                Unsafe.setByte(obj, fieldOffset, rs.getByte(columnIndex));
            };
        }
        if(type == short.class) {
            return (obj, rs) -> {
                Unsafe.setShort(obj, fieldOffset, rs.getShort(columnIndex));
            };
        }
        if(type == int.class) {
            return (obj, rs) -> {
                Unsafe.setInt(obj, fieldOffset, rs.getInt(columnIndex));
            };
        }
        if(type == float.class) {
            return (obj, rs) -> {
                Unsafe.setFloat(obj, fieldOffset, rs.getFloat(columnIndex));
            };
        }
        if(type == long.class) {
            return (obj, rs) -> {
                Unsafe.setLong(obj, fieldOffset, rs.getLong(columnIndex));
            };
        }
        if(type == double.class) {
            return (obj, rs) -> {
                Unsafe.setDouble(obj, fieldOffset, rs.getDouble(columnIndex));
            };
        }
        return (obj, rs) -> {
            Unsafe.setObject(obj, fieldOffset, rs.getObject(columnIndex));
        };
    }
}
