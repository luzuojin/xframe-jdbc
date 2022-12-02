package dev.xframe.jdbc.codec.transfer;

import dev.xframe.jdbc.codec.FieldCodec;

import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.function.Function;

public class FieldRSGetters {
    public static FieldRSGetter of(FieldInvokder field, final int columnIndex) {
        return of(field, columnIndex, null);
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static FieldRSGetter of(FieldInvokder field, final int columnIndex, final FieldCodec fc) {
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
        //兼容 java.util.time
        if(type == LocalDateTime.class) {
            return (obj, rs) -> {
                field.set(obj, map(rs.getTimestamp(columnIndex), Timestamp::toLocalDateTime));
            };
        }
        if(type == LocalDate.class) {
            return (obj, rs) -> {
                field.set(obj, map(rs.getDate(columnIndex), Date::toLocalDate));
            };
        }
        if(type == LocalTime.class) {
            return (obj, rs) -> {
                field.set(obj, map(rs.getTime(columnIndex), Time::toLocalTime));
            };
        }
        return (obj, rs) -> {
            field.set(obj, rs.getObject(columnIndex));
        };
    }

    static <T, R> R map(T t, Function<T, R> func) {
        return t == null ? null : func.apply(t);
    }
}
