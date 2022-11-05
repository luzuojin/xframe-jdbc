package dev.xframe.jdbc.codec.transfer;

import java.sql.ResultSet;

/**
 * get from ResultSet
 * set to Object
 */
@FunctionalInterface
public interface FieldRSGetter {
    void apply(Object obj, ResultSet rs) throws Exception;
}
