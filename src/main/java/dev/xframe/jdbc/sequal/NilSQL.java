package dev.xframe.jdbc.sequal;

import java.util.Collection;
import java.util.List;

import dev.xframe.jdbc.PSSetter;

public class NilSQL<T> implements SQL<T> {
	
	final String cause;
	
	public NilSQL(String cause) {
		this.cause = cause;
	}
	
	@Override
	public T fetchOne(PSSetter setter) {
		throw new UnsupportedOperationException(cause);
	}

	@Override
	public List<T> fetchMany(PSSetter setter) {
		throw new UnsupportedOperationException(cause);
	}

	@Override
	public boolean update(T value) {
		throw new UnsupportedOperationException(cause);
	}

	@Override
	public boolean update(PSSetter setter) {
		throw new UnsupportedOperationException(cause);
	}

	@Override
	public boolean updateBatch(T[] values) {
		throw new UnsupportedOperationException(cause);
	}

	@Override
	public boolean updateBatch(Collection<T> values) {
		throw new UnsupportedOperationException(cause);
	}

	@Override
	public long insertAndIncrement(T value) {
		throw new UnsupportedOperationException(cause);
	}

}
