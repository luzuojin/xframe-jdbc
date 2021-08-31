package dev.xframe.jdbc.codec.transfer;

import dev.xframe.jdbc.codec.FieldCodec;

public class Exporters {
    public static Exporter of(FieldWrap field, final int paramIndex) {
        return of(field, paramIndex, null);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static Exporter of(FieldWrap field, final int paramIndex, final FieldCodec fc) {
        if(fc != null) {
            return (obj, pstmt) -> {
                pstmt.setObject(paramIndex, fc.encode(field.get(obj)));
            };
        }
        Class<?> type = field.getType();
        if(type == boolean.class) {
            return (obj, pstmt) -> {
                pstmt.setBoolean(paramIndex, field.getBoolean(obj));
            };
        }
        if(type == byte.class) {
            return (obj, pstmt) -> {
                pstmt.setByte(paramIndex, field.getByte(obj));
            };
        }
        if(type == short.class) {
            return (obj, pstmt) -> {
                pstmt.setShort(paramIndex, field.getShort(obj));                  
            };
        }
        if(type == int.class) {
            return (obj, pstmt) -> {
                pstmt.setInt(paramIndex, field.getInt(obj));                  
            };
        }
        if(type == float.class) {
            return (obj, pstmt) -> {
                pstmt.setFloat(paramIndex, field.getFloat(obj));                  
            };
        }
        if(type == long.class) {
            return (obj, pstmt) -> {
                pstmt.setLong(paramIndex, field.getLong(obj));                  
            };
        }
        if(type == double.class) {
            return (obj, pstmt) -> {
                pstmt.setDouble(paramIndex, field.getDouble(obj));
            };
        }
        return (obj, pstmt) -> {
            pstmt.setObject(paramIndex, field.get(obj));
        };
    }
}
