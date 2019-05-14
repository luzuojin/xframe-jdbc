package dev.xframe.jdbc.sql;

public class Grouping {
	
	String[] columns;

	public Grouping(String[] columns) {
		this.columns = columns;
	}

	@Override
	public String toString() {
		return "GROUP BY " + String.join(",", columns);
	}
	
}
