package dev.xframe.jdbc.codec.transfer;

import java.sql.PreparedStatement;

/**
 * get from Object
 * set to PreparedStatement
 */
@FunctionalInterface
public interface FieldPSSetter {
    void apply(Object obj, PreparedStatement pstmt) throws Exception;
}
