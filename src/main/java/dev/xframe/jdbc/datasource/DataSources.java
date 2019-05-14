package dev.xframe.jdbc.datasource;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;

public class DataSources {
	
	public static DataSource tomcatJdbc(DBConf conf) {
		PoolProperties p = new PoolProperties();
        p.setUrl(getUrl(conf));
        p.setUsername(conf.user);
        p.setPassword(conf.pass);
        p.setJmxEnabled(true);
        p.setDriverClassName("com.mysql.jdbc.Driver");
        p.setValidationQuery("SELECT 1;");
        p.setTestWhileIdle(true);
        p.setTestOnBorrow(false);
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(conf.maxconn);
        p.setInitialSize(conf.minconn);
        p.setMinIdle(conf.minconn);
        p.setMaxIdle(conf.maxconn);
        p.setMaxWait(10000);
        p.setRemoveAbandonedTimeout(60);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setLogAbandoned(true);
        p.setRemoveAbandoned(true);
        p.setJdbcInterceptors("ConnectionState;StatementFinalizer;StatementCache(max=1024)");
//        p.setJdbcInterceptors("ConnectionState;StatementFinalizer;StatementCache(max=50);SlowQueryReport;SlowQueryReportJmx");
		return new org.apache.tomcat.jdbc.pool.DataSource(p);
	}
	
	static String URL_TEMPLATE = "jdbc:mysql://%s:%s/%s?characterEncoding=utf-8&autoReconnect=true&failOverReadOnly=false&useServerPrepStmts=true&rewriteBatchedStatements=true";
    public static String getUrl(DBConf conf) {
        return String.format(URL_TEMPLATE, conf.host, conf.port, conf.name);
    }
    
}
