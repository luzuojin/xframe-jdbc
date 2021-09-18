package dev.xframe.jdbc.builder.analyse;

/**
 * java table
 * @author luzj
 *
 */
public class JTable {
    
    Class<?> clazz;
	//key: field name
    CaseIgnoredMap<JColumn> columns = new CaseIgnoredMap<>();
	
	public JTable(Class<?> clazz) {
	    this.clazz = clazz;
    }

	public void addJColumn(JColumn jColumn) {
	    columns.put(jColumn.name, jColumn);
	}

	public JColumn getJColumn(String jColumnName) {
	    return columns.get(jColumnName);
	}

}