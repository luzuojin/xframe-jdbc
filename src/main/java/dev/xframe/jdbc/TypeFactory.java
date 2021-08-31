package dev.xframe.jdbc;

import java.sql.ResultSet;

@FunctionalInterface
public interface TypeFactory<T> {
    
    /**create instance*/
    Object make(ResultSet rs) throws Exception;
    
    @SuppressWarnings("unchecked")
    default T resolve(Object obj) throws Exception {
        return (T) obj;
    }

}
