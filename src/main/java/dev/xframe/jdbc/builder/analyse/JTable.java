package dev.xframe.jdbc.builder.analyse;

import java.util.HashMap;
import java.util.Map;

/**
 * java table
 * @author luzj
 *
 */
public class JTable {
    public Class<?> clazz;
	//DB_COLUMN_NAME <> JAVA_FIELD_COLUMN
    public Map<String, JColumn> columns = new HashMap<String, JColumn>();
	
	//MethodNameLowerCase -- MethodName
    public Map<String, String> getter = new HashMap<String, String>();
    //MethodNameLowerCase -- MethodName
    public Map<String, String> setter = new HashMap<String, String>();
    
	public JTable(Class<?> clazz) {
	    this.clazz = clazz;
    }
	
	public void addJColumn(String dbColumnName, JColumn jColumn) {
	    columns.put(dbColumnName.toLowerCase(), jColumn);
	}
	public JColumn getJColumn(String dbColumnName) {
	    return columns.get(dbColumnName.toLowerCase());
	}
	
}