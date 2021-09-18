package dev.xframe.jdbc.builder.analyse;

import java.util.ArrayList;
import java.util.List;

/**
 * db table
 * @author luzj
 *
 */
public class DBTable {
	String tableName;
	List<DBColumn> primaryKeys;
	List<DBIndex> uniqueIndexes;
	List<DBIndex> indexes;
	
	CaseIgnoredMap<DBColumn> columns;
	
	public DBTable(String name) {
		this.tableName = name;
		this.primaryKeys = new ArrayList<>();
		this.uniqueIndexes = new ArrayList<>();
		this.indexes = new ArrayList<>();
		this.columns = new CaseIgnoredMap<>();
	}
	public void addDBIndex(String keyName, boolean nonUnique, DBColumn column) {
	    if(!nonUnique) {
	        addDBIndex0(uniqueIndexes, keyName, nonUnique, column);
	    }
	    addDBIndex0(indexes, keyName, nonUnique, column);
	}
    protected void addDBIndex0(List<DBIndex> uniqueIndexes, String keyName, boolean nonUnique, DBColumn column) {
        DBIndex idx = null;
	    for (DBIndex dbIndex : uniqueIndexes) {
            if(dbIndex.keyNmae.equals(keyName)) {
                idx = dbIndex;
                break;
            }
        }
	    if(idx == null) {
	        idx = new DBIndex(keyName, nonUnique);
	        uniqueIndexes.add(idx);
	    }

	    idx.columns.add(column);
    }
	
	public DBColumn getDBColumn(String columnName) {
	    return columns.get(columnName);
	}
	
	public void addDBColumn(DBColumn dbColumn) {
	    this.columns.put(dbColumn.name, dbColumn);
	}
	
}