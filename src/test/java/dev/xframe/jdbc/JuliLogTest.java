package dev.xframe.jdbc;

import org.apache.juli.logging.LogFactory;
import org.junit.Assert;
import org.junit.Test;

import dev.xframe.jdbc.datasource.tomcatjdbc.Slf4jLog;

public class JuliLogTest {
	
	@Test
	public void test() {
		Assert.assertEquals(LogFactory.getLog("JuliLogTest").getClass(), Slf4jLog.class);
	}

}
