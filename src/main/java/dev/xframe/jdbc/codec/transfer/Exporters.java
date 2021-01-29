package dev.xframe.jdbc.codec.transfer;

import java.lang.reflect.Field;

import dev.xframe.jdbc.codec.FieldCodec;

public class Exporters {
    public static Exporter of(Field field, final int paramIndex) {
        return of(field, paramIndex, null);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Exporter of(Field field, final int paramIndex, final FieldCodec fc) {
        final long fieldOffset = Unsafe.getFieldOffset(field);
        if(fc != null) {
            return (obj, pstmt) -> {
                pstmt.setObject(paramIndex, fc.encode(Unsafe.getObject(obj, fieldOffset)));
            };
        }
        Class<?> type = field.getType();
        if(type == boolean.class) {
            return (obj, pstmt) -> {
                pstmt.setBoolean(paramIndex, Unsafe.getBoolean(obj, fieldOffset));
            };
        }
        if(type == byte.class) {
            return (obj, pstmt) -> {
                pstmt.setByte(paramIndex, Unsafe.getByte(obj, fieldOffset));
            };
        }
        if(type == short.class) {
            return (obj, pstmt) -> {
                pstmt.setShort(paramIndex, Unsafe.getShort(obj, fieldOffset));                  
            };
        }
        if(type == int.class) {
            return (obj, pstmt) -> {
                pstmt.setInt(paramIndex, Unsafe.getInt(obj, fieldOffset));                  
            };
        }
        if(type == float.class) {
            return (obj, pstmt) -> {
                pstmt.setFloat(paramIndex, Unsafe.getFloat(obj, fieldOffset));                  
            };
        }
        if(type == long.class) {
            return (obj, pstmt) -> {
                pstmt.setLong(paramIndex, Unsafe.getLong(obj, fieldOffset));                  
            };
        }
        if(type == double.class) {
            return (obj, pstmt) -> {
                pstmt.setDouble(paramIndex, Unsafe.getDouble(obj, fieldOffset));
            };
        }
        return (obj, pstmt) -> {
            pstmt.setObject(paramIndex, Unsafe.getObject(obj, fieldOffset));
        };
    }
}
