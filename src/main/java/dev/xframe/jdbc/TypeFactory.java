package dev.xframe.jdbc;

import java.sql.ResultSet;

@FunctionalInterface
public interface TypeFactory<T> {
    
    /**create instance*/
    T make(ResultSet rs) throws Exception ;

}
