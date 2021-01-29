package dev.xframe.jdbc.codec.transfer;

import java.sql.ResultSet;
import java.sql.SQLException;

//从RS转移至obj
@FunctionalInterface
public interface Importer {
    public void imports(Object obj, ResultSet rs) throws SQLException;
}
