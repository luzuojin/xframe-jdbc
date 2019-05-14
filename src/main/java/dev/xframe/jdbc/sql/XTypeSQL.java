package dev.xframe.jdbc.sql;

import dev.xframe.jdbc.sequal.SQL.Option;

public class XTypeSQL {
	protected TypeSQL sql;
	protected XTypeSQL(TypeSQL sql) {
		this.sql = sql;
	}
	public XTypeSQL groupby(String... columns) {
		sql.grouping = new Grouping(columns);
		return this;
	}
	public XTypeSQL orderbyDesc(String... columns) {
		sql.setOrdering(new Ordering("DESC", columns));
		return this;
	}
	public XTypeSQL orderbyAsc(String... columns) {
		sql.setOrdering(new Ordering("ASC", columns));
		return this;
	}
	public XTypeSQL limit(int start, int count) {
		sql.limiting = new Limiting(start, count);
		return this;
	}
	
	public TypeSQL select() {
		sql.option = Option.SELECT;
		return sql;
	}
	public TypeSQL delete() {
		sql.option = Option.DELETE;
		return sql;
	}
	
}