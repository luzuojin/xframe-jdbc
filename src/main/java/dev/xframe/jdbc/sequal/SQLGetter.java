package dev.xframe.jdbc.sequal;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.sequal.SQL.Option;

public class SQLGetter {
    
    public static <T> Option getOption(AbtSQL<T> sql) {
        return sql.option;
    }
    public static <T> String getSql(AbtSQL<T> sql) {
        return sql.sql;
    }
    public static <T> JdbcTemplate getJdbcTemplate(AbtSQL<T> sql) {
        return sql.jdbcTemplate;
    }
    public static <T> TypePSSetter<T> getSetter(AbtSQL<T> sql) {
        return sql.setter;
    }
    public static <T> RSParser<T> getParser(AbtSQL<T> sql) {
        return sql.parser;
    }
    public static <T> int getBatchLimit(AbtSQL<T> sql) {
        return sql.batchLimit;
    }
    public static <T> SQLExecutor getExec(AsyncSQL<T> sql) {
        return sql.exec;
    }
    public static <T> String getCause(NilSQL<T> sql) {
        return sql.cause;
    }
    
}
