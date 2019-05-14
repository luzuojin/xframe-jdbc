package dev.xframe.jdbc.sequal;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.JdbcEnviron;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypePSSetter;

/**
 * 查询还是同步
 * 异步保存
 * @author luzj
 * @param <T>
 */
public class AsyncSQL<T> extends AbtSQL<T> {
    
    final SQLExecutor exec;
    
    public AsyncSQL(Option option, String sql, JdbcTemplate jdbcTemplate, int batchLimit, TypePSSetter<T> setter, RSParser<T> parser) {
        super(option, sql, jdbcTemplate, batchLimit, setter, parser);
        this.exec = JdbcEnviron.getExecutor();
    }
    
    //update(also insert, delete)
    public boolean update(final T value) {
        if(_updatable(value)) {
            execute(new SingleValUpdateTask<>(this, value));
            return true;
        }
        return false;
    }
    public boolean update(final PSSetter setter) {
        execute(()->_update1(setter));
        return true;
    }
    
    
    public boolean updateBatch(final T[] values) {
        execute(()->_updateBatch0(values));
        return true;
    }
    public boolean updateBatch(final Collection<T> values) {
        List<T> vs = new ArrayList<>(values);
        execute(()->_updateBatch0(vs));
        return true;
    }
    
    private void execute(SQLTask task) {
        if(exec.isShutdown()) {
            task.run();
            return;
        }
        try {
            exec.execute(task);
        } catch (Throwable e) {
            task.run();
        }
    }
    
    static final class SingleValUpdateTask<T> implements SQLTask {
        final AbtSQL<T> sql;
        final T value;
        public SingleValUpdateTask(AbtSQL<T> sql, T value) {
            this.sql = sql;
            this.value = value;
        }
        public void exec() {
            sql._update0(value);
        }
        @Override
        public final int hashCode() {
            return System.identityHashCode(value);
        }
    }
    
}
