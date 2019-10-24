package dev.xframe.jdbc.datasource;

public interface DBSource {
	
	String user();
	
	String password();
	
	String driver();
	
	String url();
	
	default int minconn() {
		return 1;
	}
	default int maxconn() {
		return 1;
	}
	
}
