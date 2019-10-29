package dev.xframe.jdbc.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.xframe.jdbc.builder.analyse.FColumn;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.sequal.NilSQL;
import dev.xframe.jdbc.sequal.SQL;
import dev.xframe.jdbc.sequal.SQL.Option;
import dev.xframe.jdbc.sequal.SQLFactory;


/**
 * 
 * 生成默认SQL
 * @author luzj
 * 
 */
public class SQLBuilder {
	
	private static String join(String delimiter, Stream<String> stream) {
		return String.join(delimiter, stream.collect(Collectors.toList()));
	}
    
    public static <T> SQL<T> buildInsertSQL(SQLFactory<T> factory, FTable ftable) throws Exception {
        return factory.newSQL(Option.INSERT, buildInsertSQLStr(ftable), ftable.batchLimit(ftable.columns.size()), CodecBuilder.buildSetter(ftable, ftable.columns), null);
    }

	private static String buildInsertSQLStr(FTable ftable) {
        String insertPre = join(",", ftable.columns.stream().map(f->f.dbColumn.quoteName()));
        String insertEnd = join(",", ftable.columns.stream().map(f->"?"));
        return "INSERT INTO " + ftable.tableName + " (" + insertPre + ") VALUES (" + insertEnd + ")";
	}
    
	public static <T> SQL<T> buildInstupSQL(SQLFactory<T> factory, FTable ftable) throws Exception {
        if(ftable.hasOnlyOneUniqueIndex() && ftable.hasColumnNotInUniqueIndex()) {
            String upt = join(",", ftable.columns.stream().filter(f->!ftable.primaryKeys.contains(f)).map(f->f.dbColumn.quoteName()+"=VALUES("+f.dbColumn.quoteName()+")"));
            String sql = buildInsertSQLStr(ftable) + " ON DUPLICATE KEY UPDATE " + upt;
            return factory.newSQL(Option.INSTUP, sql.toString(), ftable.batchLimit(ftable.columns.size()), CodecBuilder.buildSetter(ftable, ftable.columns), null);
        }
        return new NilSQL<>(String.format("Table[%s] unique index is empty or not only", ftable.tableName));
    }
    
    public static <T> SQL<T> buildUpdateSQL(SQLFactory<T> factory, FTable ftable, List<FColumn> whereColumns) throws Exception {
        if(!whereColumns.isEmpty()) {
            List<FColumn> fColumns = new ArrayList<FColumn>();

            String up = join(",", ftable.columns.stream().filter(f->!whereColumns.contains(f)).peek(f->fColumns.add(f)).map(c->c.dbColumn.quoteAssignName()));
            
            String we = join(" AND ", whereColumns.stream().peek(f->fColumns.add(f)).map(f->f.dbColumn.quoteAssignName()));
            
            String sql = "UPDATE " + ftable.tableName + " SET " + up + " WHERE " + we;
            
            return factory.newSQL(Option.UPDATE, sql, ftable.batchLimit(ftable.columns.size() + whereColumns.size()), CodecBuilder.buildSetter(ftable, fColumns), null);
        }
        return new NilSQL<>(String.format("Table[%s] update option where columns is empty", ftable.tableName));
    }
    
    public static <T> SQL<T> buildUpdateSQL(SQLFactory<T> factory, FTable ftable) throws Exception {
        return buildUpdateSQL(factory, ftable, ftable.primaryKeys);
    }
    
    public static <T> SQL<T> buildDeleteSQL(SQLFactory<T> factory, FTable ftable) throws Exception {
        return buildDeleteSQL(factory, ftable, ftable.primaryKeys);
    }
    
    public static <T> SQL<T> buildDeleteSQL(SQLFactory<T> factory, FTable ftable, List<FColumn> whereColumns) throws Exception {
    	return buildDeleteSQL(factory, ftable, whereColumns, SQLBuilder::simpleDeleteSQLBuilder);
    }
	private static String simpleDeleteSQLBuilder(FTable ftable, List<FColumn> whereColumns) {
		return "DELETE FROM " + ftable.tableName + " WHERE " + joinColumnsToCondStr(whereColumns);
	}
    public static <T> SQL<T> buildDeleteSQL(SQLFactory<T> factory, FTable ftable, List<FColumn> whereColumns, BiFunction<FTable, List<FColumn>, String> nativeSQLBuilder) throws Exception {
    	if(!whereColumns.isEmpty()) {
            return factory.newSQL(Option.DELETE, nativeSQLBuilder.apply(ftable, whereColumns), ftable.batchLimit(whereColumns.size()), CodecBuilder.buildSetter(ftable, whereColumns), null);
        }
        return new NilSQL<>(String.format("Table[%s] delete option where columns is empty", ftable.tableName));
    }

	private static String joinColumnsToCondStr(List<FColumn> columns) {
		return String.join(" AND ", columns.stream().map(f->f.dbColumn.quoteAssignName()).collect(Collectors.toList()));
	}
    
    @SuppressWarnings("unchecked")
    public static <T> SQL<T> buildQuerySQL(SQLFactory<T> factory, FTable ftable) throws Exception {
        return buildQuerySQL(factory, ftable, Collections.EMPTY_LIST);
    }
    
    public static <T> SQL<T> buildQryKeySQL(SQLFactory<T> factory, FTable ftable) throws Exception {
        return buildQuerySQL(factory, ftable, ftable.primaryKeys);
    }
    
    public static <T> SQL<T> buildQuerySQL(SQLFactory<T> factory, FTable ftable, List<FColumn> whereColumns) throws Exception {
    	return buildQuerySQL(factory, ftable, whereColumns, SQLBuilder::simpleQuerySQLBuilder);
    }
    private static String simpleQuerySQLBuilder(FTable ftable, List<FColumn> whereColumns) {
    	String selectColumns = join(",", ftable.columns.stream().map(f->f.dbColumn.quoteName()));
    	
    	StringBuilder sql = new StringBuilder("SELECT ").append(selectColumns).append(" FROM ").append(ftable.tableName);
    	
    	if(!whereColumns.isEmpty()) sql.append(" WHERE ").append(joinColumnsToCondStr(whereColumns));
    	return sql.toString();
    }
    
    public static <T> SQL<T> buildQuerySQL(SQLFactory<T> factory, FTable ftable, List<FColumn> whereColumns, BiFunction<FTable, List<FColumn>, String> nativeSQLBuilder) throws Exception {
    	return factory.newSQL(Option.SELECT, nativeSQLBuilder.apply(ftable, whereColumns), ftable.batchLimit(whereColumns.size()), null, CodecBuilder.buildParser(ftable, ftable.columns));
    }

}
