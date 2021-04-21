package dev.xframe.jdbc.codec.transfer;

import java.sql.PreparedStatement;

@FunctionalInterface
public interface Exporter {
    
    void exports(Object obj, PreparedStatement pstmt) throws Exception;

}
