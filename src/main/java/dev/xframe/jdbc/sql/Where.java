package dev.xframe.jdbc.sql;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Where extends XTypeSQL {

	List<Linker> linkers = new LinkedList<>();

	public Where(TypeSQL sql) {
		super(sql);
		sql.where = this;
	}
	
	Linker append(Linker linker) {
		this.linkers.add(linker);
		return linker;
	}
	
	public Linker AND() {
		return append(new Linker("AND", this));
	}

	public Linker OR() {
		return append(new Linker("OR", this));
	}

	List<String> placeHolderColumns() {
		List<String> x = new ArrayList<>();
		linkers.forEach(l->x.addAll(l.placeHolderColumns()));
		return x;
	}

	Linker NIL() {
		return append(new Linker("", this));
	}

	@Override
	public String toString() {
		return "WHERE" + String.join(" ", linkers.stream().map(Linker::toString).collect(Collectors.toList()));
	}

}
