package dev.xframe.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.xframe.jdbc.tools.WrappedRS;
import dev.xframe.utils.XCaught;

public class JdbcTemplate {
	
	private static final String EXECUTE_ERROR_TEMPLATE = "Execute sql[%s] error, cause:%s";
    private static final String CONNECT_CLOSE = "Close connection error";
    
    private static final Logger logger = LoggerFactory.getLogger(JdbcTemplate.class);

    public final DataSource dataSource;

    
    /**
	 * 防止某些异常代码出现 在Debug环境中使用WrappedResultSet代替
	 */
    public static JdbcTemplate of(DataSource source) {
        if(logger.isDebugEnabled()) {
            return new JdbcTemplate(source) {
                protected ResultSet wrap(ResultSet rs) {
                    return new WrappedRS(rs);
                }
            };
        }
        return new JdbcTemplate(source);
    }
    
	protected ResultSet wrap(ResultSet rs) {
		return rs;
	}
    public JdbcTemplate(DataSource dataSource) {
    	this.dataSource = dataSource;
    }

    /**
     * 不处理one/many由RSParser自行处理
     * @param sql
     * @param setter
     * @param parser
     * @return
     */
    public <T> T fetch(String sql, PSSetter setter, RSParser<T> parser) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            
            if(setter != null)
                setter.set(pstmt);
            
            rs = pstmt.executeQuery();
            return parser.parse(rs);//不判断RS.next(), 由parser自己控制
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            throw XCaught.throwException(e);
        } finally {
            close(conn, pstmt, rs);
            watch(first, second, sql);
        }
    }
    
    public <T> T fetchOne(String sql, PSSetter setter, RSParser<T> parser) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            if(setter != null)
                setter.set(pstmt);
            
            rs = pstmt.executeQuery();
            if(rs.next()) {
                return parser.parse(wrap(rs));
            }
            return null;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            throw XCaught.throwException(e);
        } finally {
            close(conn, pstmt, rs);
            watch(first, second, sql);
        }
    }
    
    /**
     * where子句中只有一个参数
     * @param sql
     * @param condParam (where子句中的参数)
     * @param parser
     * @return
     */
    public <T> T fetchOne(String sql, Object condParam, RSParser<T> parser) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            pstmt.setObject(1, condParam);
            
            rs = pstmt.executeQuery();
            if(rs.next()) {
                return parser.parse(wrap(rs));
            }
            return null;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            throw XCaught.throwException(e);
        } finally {
            close(conn, pstmt, rs);
            watch(first, second, sql);
        }
    }
    
    public <T> List<T> fetchMany(String sql, PSSetter setter, RSParser<T> parser) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            if(setter != null)
                setter.set(pstmt);
            
            rs = pstmt.executeQuery();
            List<T> ret = new ArrayList<>();
            while(rs.next()) {
                ret.add(parser.parse(wrap(rs)));
            }
            return ret;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            throw XCaught.throwException(e);
        } finally {
            close(conn, pstmt, rs);
            watch(first, second, sql);
        }
    }
    
    public boolean update(String sql, PSSetter setter) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            if(setter != null)
                setter.set(pstmt);
            
            return pstmt.executeUpdate() > -1;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        } finally {
            close(conn, pstmt);
            watch(first, second, sql);
        }
        return false;
    }
    
    public <T> boolean update(String sql, TypePSSetter<T> setter, T value) {
        return updateAndReturnCause(sql, setter, value) == 0;
    }
    
    public <T> int updateAndReturnCause(String sql, TypePSSetter<T> setter, T value) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            if(setter != null)
                setter.set(pstmt, value);
            
            return pstmt.executeUpdate() > -1 ? 0 : -1;
        } catch (SQLException e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            return e.getErrorCode();
        } catch (Exception e) {
        	logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        	return -2;
        } finally {
            close(conn, pstmt);
            watch(first, second, sql);
        }
    }

    public <T> long fetchIncrement(String sql, TypePSSetter<T> setter, T value) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if(setter != null)
                setter.set(pstmt, value);
            
            pstmt.execute();
            
            rs = pstmt.getGeneratedKeys();
            if(rs.next()){
                return rs.getLong(1);
            }
            return -1;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            throw XCaught.throwException(e);
        } finally {
            close(conn, pstmt, rs);
            watch(first, second, sql);
        }
    }
    
    public long fetchIncrement(String sql, PSSetter setter) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            if(setter != null)
                setter.set(pstmt);
            
            pstmt.execute();
            
            rs = pstmt.getGeneratedKeys();
            if(rs.next()){
                return rs.getLong(1);
            }
            return -1;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            throw XCaught.throwException(e);
        } finally {
            close(conn, pstmt, rs);
            watch(first, second, sql);
        }
    }
    
    public long fetchIncrement(String sql) {
        return fetchIncrement(sql, PSSetter.NONE);
    }

    public long fetchLong(String sql, PSSetter setter) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            setter.set(pstmt);
            
            rs = pstmt.executeQuery();
            if(rs.next()) {
                return rs.getLong(1);
            }
            return -1;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
            throw XCaught.throwException(e);
        } finally {
        	close(conn, pstmt, rs);
            watch(first, second, sql);
        }
    }

    public boolean updateBatch(String sql, PSSetter... setters) {
        if(setters == null || setters.length == 0) return false;
        
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            for (PSSetter setter : setters) {
            	setter.set(pstmt);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        } finally {
        	close(conn, pstmt);
            watch(first, second, sql);
        }
        return false;
    }
    
    public boolean updateBatch(String sql, Collection<PSSetter> setters) {
        if(setters == null || setters.isEmpty()) return false;
        
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            for (PSSetter setter : setters) {
                setter.set(pstmt);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        } finally {
            close(conn, pstmt);
            watch(first, second, sql);
        }
        return false;
    }
    
    public <T> boolean updateBatch(String sql, TypePSSetter<T> setter, T[] values) {
        if(values == null || values.length == 0) return false;
        
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            for (T value : values) {
            	setter.set(pstmt, value);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        } finally {
        	close(conn, pstmt);
            watch(first, second, sql);
        }
        return false;
    }
    
    public <T> boolean updateBatch(String sql, TypePSSetter<T> setter, Collection<T> values) {
        if(values == null || values.isEmpty()) return false;
        
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            for (T value : values) {
                setter.set(pstmt, value);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        } finally {
            close(conn, pstmt);
            watch(first, second, sql);
        }
        return false;
    }
    
    public boolean callProcedure(String procedureName) {
        long first = System.currentTimeMillis(), second = 0;
        String sql = "{call  " + procedureName + "()}";
        Connection conn = null;
        PreparedStatement pstmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            pstmt = conn.prepareStatement(sql);
            return pstmt.execute();
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        } finally {
            close(conn, pstmt);
            watch(first, second, sql);
        }
        return false;
    }
    
    private void close(Connection conn, PreparedStatement pstmt, ResultSet rs) {
    	try {
            if (rs != null) {
                rs.close();
            }
        } catch (Exception e) {
            logger.error(CONNECT_CLOSE, e);
        }
    	
    	close(conn, pstmt);
	}
    
	private void close(Connection conn, PreparedStatement pstmt) {
		try {
            if (pstmt != null) {
                pstmt.clearParameters();
                pstmt.close();
            }
        } catch (Exception e) {
            logger.error(CONNECT_CLOSE, e);
        }
		
		close(conn);
	}

	private void close(Connection conn, Statement stmt) {
		try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (Exception e) {
            logger.error(CONNECT_CLOSE, e);
        }
		close(conn);
	}

    private void close(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (Exception e) {
            logger.error(CONNECT_CLOSE, e);
        }
    }
    
    private void watch(long first, long second, String sql) {
        long end = System.currentTimeMillis();      
        long spendTime = end - first;
        if (spendTime > 1000) {
            logger.warn(String.format("Execute sql[%s] slow, used:%sms, pool:%sms, exec:%sms", sql, spendTime, second - first, end - second));
        }
    }
    
    public boolean execute(String sql) {
        long first = System.currentTimeMillis(), second = 0;
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = dataSource.getConnection();
            second = System.currentTimeMillis();
            
            stmt = conn.createStatement();
            int ret = stmt.executeUpdate(sql);
            return ret > -1;
        } catch (Exception e) {
            logger.error(String.format(EXECUTE_ERROR_TEMPLATE, sql, e), e);
        } finally {
            close(conn, stmt);
            watch(first, second, sql);
        }
        return false;
    }
    
    //transactional
    public boolean executeBatch(List<String> sqls) {
        if(sqls == null || sqls.isEmpty()) return false;
        
        Connection conn = null;
        Statement stmt = null;
        boolean success = true;
        try {
            conn = dataSource.getConnection();
            conn.setAutoCommit(false);
            
            stmt = conn.createStatement();
            for (String sql : sqls) {
                stmt.addBatch(sql);
            }
            int[] rets = stmt.executeBatch();
            for (int ret : rets) {
                if (ret < 0) {
                    success = false;
                    break;
                }
            }
        } catch (Exception e) {
            success = false;
        } finally {
            if(conn != null) {
                try {
                    if(success) {
                        conn.commit();
                    } else {
                        conn.rollback();
                    }
                    conn.setAutoCommit(true);
                } catch (Exception ex){
                    //ignore
                }
            }
            close(conn, stmt);
        }
        return success;
    }
}
