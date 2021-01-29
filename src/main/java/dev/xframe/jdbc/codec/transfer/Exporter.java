package dev.xframe.jdbc.codec.transfer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface Exporter {
    
    void exports(Object obj, PreparedStatement pstmt) throws SQLException;

}
