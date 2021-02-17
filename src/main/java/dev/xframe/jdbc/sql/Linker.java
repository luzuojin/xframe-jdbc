package dev.xframe.jdbc.sql;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Linker extends XTypeSQL {
	
	Where where;
	
	String link;
	
	List<Condition> conditions = new LinkedList<>();
	
	public Linker(String link, Where where) {
		super(where.sql);
		this.where = where;
		this.link = link;
	}
	
	public Linker AND() {
		return where.AND();
	}
	
	public Linker OR() {
		return where.OR();
	}

	public Linker EQ(String... columns) {
		Arrays.stream(columns).forEach(column->conditions.add(new Condition("=", column, "?")));
		return this;
	}
	public Linker NEQ(String... columns) {
		Arrays.stream(columns).forEach(column->conditions.add(new Condition("<>", column, "?")));
		return this;
	}
	public Linker LESS(String... columns) {
		Arrays.stream(columns).forEach(column->conditions.add(new Condition("<", column, "?")));
		return this;
	}
	public Linker LESS_EQ(String... columns) {
		Arrays.stream(columns).forEach(column->conditions.add(new Condition("<=", column, "?")));
		return this;
	}
	public Linker OVER(String... columns) {
		Arrays.stream(columns).forEach(column->conditions.add(new Condition(">", column, "?")));
		return this;
	}
	public Linker OVER_EQ(String... columns) {
		Arrays.stream(columns).forEach(column->conditions.add(new Condition(">=", column, "?")));
		return this;
	}
	
	public Linker LIKE(String... columns) {
	    Arrays.stream(columns).forEach(column->conditions.add(new Condition("LIKE", column, "?")));
	    return this;
	}
	
	public List<String> placeHolderColumns() {
		return conditions.stream().filter(c->!c.isPlaceholder()).map(Condition::getColumn).collect(Collectors.toList());
	}
	
	public String toString() {
		return link + " (" + String.join(" AND ", conditions.stream().map(Condition::toString).collect(Collectors.toList())) + ")";
	}
	
}
