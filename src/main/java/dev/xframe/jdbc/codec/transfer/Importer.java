package dev.xframe.jdbc.codec.transfer;

import java.sql.ResultSet;

//从RS转移至obj
@FunctionalInterface
public interface Importer {
    public void imports(Object obj, ResultSet rs) throws Exception;
}
