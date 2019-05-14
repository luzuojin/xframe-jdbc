package dev.xframe.jdbc.builder.analyse;

import java.util.ArrayList;
import java.util.List;

public class DBIndex {
    
    public String keyNmae;
    public boolean nonUnique;
    public List<DBColumn> columns;
    public DBIndex(String keyName, boolean nonUnique) {
        this.columns = new ArrayList<DBColumn>();
        this.keyNmae = keyName;
        this.nonUnique = nonUnique;
    }

}
