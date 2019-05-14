package dev.xframe.jdbc.sequal;

import java.util.Collection;
import java.util.List;

import dev.xframe.jdbc.PSSetter;

/**
 * 对应一个SQL
 * @author luzj
 * @param <T>
 */
public interface SQL<T> {
	
	public static enum Option {
		SELECT(0), INSERT(1), UPDATE(2), INSTUP(3), DELETE(4);
        public final int code;
        private Option(int code) {this.code = code;}
    }
	
	//fetch
	T fetchOne(PSSetter setter);
	List<T> fetchMany(PSSetter setter);
	
	//update(also insert,delete)
	boolean update(T value);
	boolean update(PSSetter setter);
	
	boolean updateBatch(T[] values);
	boolean updateBatch(Collection<T> values);
	
	//insert with incrment
	long insertAndIncrement(T value);
	
}
