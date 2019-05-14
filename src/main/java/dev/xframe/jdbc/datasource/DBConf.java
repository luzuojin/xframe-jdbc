package dev.xframe.jdbc.datasource;


/**
 * 编码使用utf8
 * @author luzj
 */
public class DBConf {
	
    public final String host;
	public final    int port;
	public final String name;//dbname
	public final String user;
	public final String pass;
	
	public DBConf(String host, int port, String name, String user, String pass) {
		this(host, port, name, user, pass, default_min_conn, default_max_conn);
	}
	
	public final int minconn;
	public final int maxconn;
	
	public DBConf(String host, int port, String name, String user, String pass, int minconn, int maxconn) {
	    this.host = host;
	    this.port = port;
	    this.name = name;
	    this.user = user;
	    this.pass = pass;
	    
	    this.minconn = minconn;
	    this.maxconn = maxconn;
	}
	
	private static final int default_max_conn = 1;
    private static final int default_min_conn = 2;

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((host == null) ? 0 : host.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + port;
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
        DBConf other = (DBConf) obj;
        if (host == null) {
            if (other.host != null)
                return false;
        } else if (!host.equals(other.host))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (port != other.port)
            return false;
        return true;
    }
	
}
