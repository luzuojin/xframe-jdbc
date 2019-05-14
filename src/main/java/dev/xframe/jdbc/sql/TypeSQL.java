package dev.xframe.jdbc.sql;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.xframe.jdbc.builder.SQLBuilder;
import dev.xframe.jdbc.builder.analyse.FColumn;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.sequal.SQL;
import dev.xframe.jdbc.sequal.SQL.Option;
import dev.xframe.jdbc.sequal.SQLFactory;

public class TypeSQL {
	
	Where where;
	Option option;
	Grouping grouping;
	Limiting limiting;
	Ordering ordering;
	
	public static Linker where() {
		return new Where(new TypeSQL()).NIL();
	}
	
	void setOrdering(Ordering ordering) {
		if(this.ordering == null) {
			this.ordering = ordering;
		} else {
			Ordering tmp = this.ordering;
			while(tmp.next != null) tmp = tmp.next;
			tmp.next = ordering;
		}
	}
	
	public <T> SQL<T> buildSQL(SQLFactory<T> factory, FTable ftable) throws Exception {
		List<FColumn> whereColumns = where.placeHolderColumns().stream().map(c->ftable.columnMap.get(c.toLowerCase())).collect(Collectors.toList());
		switch (option) {
		case SELECT:
			return SQLBuilder.buildQuerySQL(factory, ftable, whereColumns, this::buildQueryNativeSQL);
		case DELETE:
			return SQLBuilder.buildDeleteSQL(factory, ftable, whereColumns, this::buildDeleteNativeSQL);
		default:
			throw new UnsupportedOperationException(String.valueOf(option));
		}
	}
	
	private String buildQueryNativeSQL(FTable ftable, List<FColumn> whereColumns) {
		return queryToString("SELECT " + columnsToSelectStr(ftable.columns) + " FROM " + ftable.tableName);
	}

	private String columnsToSelectStr(List<FColumn> columns) {
		return String.join(",", columns.stream().map(f->f.dbColumn.quoteName()).collect(Collectors.toList()));
	}

	private String buildDeleteNativeSQL(FTable ftable, List<FColumn> whereColumns) {
		return "DELETE FROM " + ftable.tableName + " " + toStringOr(where);
	}

	@Override
	public String toString() {
		return option == Option.DELETE ? deleteToString() : queryToString();
	}

	private String queryToString() {
		return queryToString("SELECT * FROM {table}");
	}
	
	private String queryToString(String prefix) {
		return String.join(" ", Arrays.asList(prefix, toStringOr(where), toStringOr(grouping), toStringOr(ordering), toStringOr(limiting)).stream().filter(s->!s.isEmpty()).collect(Collectors.toList()));
	}

	private String deleteToString() {
		return "DELETE FROM {table} " + toStringOr(where);
	}

	private String toStringOr(Object obj) {
		return obj == null ? "" : obj.toString();
	}
	
}
