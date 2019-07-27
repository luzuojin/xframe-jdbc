package dev.xframe.jdbc;

import java.sql.PreparedStatement;

/**
 * PreparedStatement setter with obj
 * @author luzj
 */
public interface TypePSSetter<T> {

	void set(PreparedStatement pstmt, T obj) throws Exception;
	
}
