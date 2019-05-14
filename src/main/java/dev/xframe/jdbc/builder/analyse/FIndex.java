package dev.xframe.jdbc.builder.analyse;

import java.util.ArrayList;
import java.util.List;

public class FIndex {
    
    public String keyNmae;
    public boolean nonUnique;
    public List<FColumn> columns;
    public FIndex(String keyName, boolean nonUnique) {
        this.keyNmae = keyName;
        this.nonUnique = nonUnique;
        this.columns = new ArrayList<FColumn>();
    }

}
