package dev.xframe.jdbc.datasource;

public class DBSource {
	
	final String user;
	
	final String password;
	
	final String driver;
	
	final String url;
	
	final int minconn;
	
	final int maxconn;
	
	public DBSource(String user, String password, String driver, String url, int minconn, int maxconn) {
		this.user = user;
		this.password = password;
		this.driver = driver;
		this.url = url;
		this.minconn = minconn;
		this.maxconn = maxconn;
	}
	
}
