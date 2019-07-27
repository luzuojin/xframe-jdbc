package dev.xframe.jdbc;

import java.sql.ResultSet;


/**
 * ResultSet parser
 * @author luzj
 * @param <T>
 */
public interface RSParser<T> {
	
	T parse(ResultSet rs) throws Exception;
	
}
