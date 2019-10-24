package dev.xframe.jdbc.datasource;

import javax.sql.DataSource;

import org.apache.tomcat.jdbc.pool.PoolProperties;

public class DataSources {
	
	public static DataSource tomcatJdbc(DBConf conf) {
		return tomcatJdbc(conf.toMysqlSource());
	}

	public static DataSource tomcatJdbc(DBSource source) {
		PoolProperties p = new PoolProperties();
        p.setUrl(source.url);
        p.setUsername(source.user);
        p.setPassword(source.password);
        p.setJmxEnabled(true);
        p.setDriverClassName(source.driver);
        p.setValidationQuery("SELECT 1;");
        p.setTestWhileIdle(true);
        p.setTestOnBorrow(false);
        p.setTestOnReturn(false);
        p.setValidationInterval(30000);
        p.setTimeBetweenEvictionRunsMillis(30000);
        p.setMaxActive(source.maxconn);
        p.setInitialSize(source.minconn);
        p.setMinIdle(source.minconn);
        p.setMaxIdle(source.maxconn);
        p.setMaxWait(10000);
        p.setRemoveAbandonedTimeout(60);
        p.setMinEvictableIdleTimeMillis(30000);
        p.setLogAbandoned(true);
        p.setRemoveAbandoned(true);
        p.setJdbcInterceptors("ConnectionState;StatementFinalizer;StatementCache(max=1024)");
//        p.setJdbcInterceptors("ConnectionState;StatementFinalizer;StatementCache(max=50);SlowQueryReport;SlowQueryReportJmx");
		return new org.apache.tomcat.jdbc.pool.DataSource(p);
	}
	
    
}
