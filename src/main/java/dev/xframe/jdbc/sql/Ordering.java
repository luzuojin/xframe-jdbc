package dev.xframe.jdbc.sql;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Ordering {
	
	String suffix;
	String[] columns;

	public Ordering(String suffix, String[] columns) {
		this.suffix = suffix;
		this.columns = columns;
	}
	
	Ordering next;

	@Override
	public String toString() {
		List<Ordering> orderings = new ArrayList<>();
		Ordering tmp = this;
		do {
			orderings.add(tmp);
		} while((tmp = tmp.next) != null);
		
		return "ORDER BY " + String.join(",", orderings.stream().map(Ordering::toString0).collect(Collectors.toList()));
	}

	private String toString0() {
		return String.join(",", columns) + " " + suffix;
	}
	
	
}
