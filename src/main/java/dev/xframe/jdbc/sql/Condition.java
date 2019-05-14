package dev.xframe.jdbc.sql;

public class Condition {
	
	String assign;
	String column;
	String value;

	public Condition(String assign, String column, String value) {
		this.assign = assign;
		this.column = column;
		this.value = value;
	}

	public boolean isPlaceholder() {
		return "?".equals(value);
	}

	public String getColumn() {
		return column;
	}

	@Override
	public String toString() {
		return String.format("`%s`%s%s", column, assign, value);
	}
	
}
