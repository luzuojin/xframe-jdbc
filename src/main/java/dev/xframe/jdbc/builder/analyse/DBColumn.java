package dev.xframe.jdbc.builder.analyse;

/**
 * db column
 * @author luzj
 *
 */
public class DBColumn {
    public String name;
    public int index;
    public int type;
    
	public String quoteName() {
	    return "`" + name + "`";
	}
	public String quoteAssignName() {
		return "`" + name + "`=?";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + index;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + type;
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
		DBColumn other = (DBColumn) obj;
		if (index != other.index)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (type != other.type)
			return false;
		return true;
	}
	public int pkSEQ;
	public DBColumn withPKSEQ(int seq) {
	    this.pkSEQ = seq;
	    return this;
	}
}