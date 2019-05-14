package dev.xframe.jdbc;

import dev.xframe.jdbc.sequal.SQL;

/**
 * 提供给SQL.Update时判断更新状态
 * @author luzj
 */
public interface TypeFilter {
    
    public static final int DUPLICATE_KEY_ERROR = 1062;
    
    /**
     * @see SQL.Option
     * @param option
     * @return
     */
    boolean updatable(int option);
    
    /**
     * @see SQL.Option
     * @return option
     */
	int update(int option);
    
    /**
     * 更新成功
     * @param option (update() return option)
     */
    void commit(int option);
    
    /**
     * 更新失败
     * @param option (update() return option)
     */
    void cancel(int option, int cause);

}
