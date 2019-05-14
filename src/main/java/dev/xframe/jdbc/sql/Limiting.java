package dev.xframe.jdbc.sql;

public class Limiting {
	
	int start;
	int count;

	public Limiting(int start, int count) {
		this.start = start;
		this.count = count;
	}

	@Override
	public String toString() {
		return "LIMIT " + start + ", " + count;
	}
	
}
