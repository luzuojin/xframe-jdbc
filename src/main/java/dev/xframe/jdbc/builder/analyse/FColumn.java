package dev.xframe.jdbc.builder.analyse;

/**
 * 
 * analyze final column
 * @author luzj
 *
 */
public class FColumn {
    public DBColumn dbColumn;
    public JColumn jColumn;
	public FColumn(DBColumn dbColumn, JColumn jColumn) {
		this.dbColumn = dbColumn;
		this.jColumn = jColumn;
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((dbColumn == null) ? 0 : dbColumn.hashCode());
		result = prime * result
				+ ((jColumn == null) ? 0 : jColumn.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FColumn other = (FColumn) obj;
		if (dbColumn == null) {
			if (other.dbColumn != null)
				return false;
		} else if (!dbColumn.equals(other.dbColumn))
			return false;
		if (jColumn == null) {
			if (other.jColumn != null)
				return false;
		} else if (!jColumn.equals(other.jColumn))
			return false;
		return true;
	}
}