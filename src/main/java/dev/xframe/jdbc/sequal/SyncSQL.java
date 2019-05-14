package dev.xframe.jdbc.sequal;

import java.util.Collection;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypePSSetter;

public class SyncSQL<T> extends AbtSQL<T> {
    
    public SyncSQL(Option option, String sql, JdbcTemplate jdbcTemplate, int batchLimit, TypePSSetter<T> setter, RSParser<T> parser) {
        super(option, sql, jdbcTemplate, batchLimit, setter, parser);
    }
    
    //update(also insert,delete)
    public boolean update(T value) {
       return _update0(value);
    }
    public boolean update(PSSetter setter) {
        return _update1(setter);
    }
    
    public boolean updateBatch(T[] values) {
        return _updateBatch0(values);
    }
    public boolean updateBatch(Collection<T> values) {
        return _updateBatch0(values);
    }
    
}
