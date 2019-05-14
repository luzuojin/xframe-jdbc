package dev.xframe.jdbc.sequal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.TypeFilter;

public abstract class AbtSQL<T> implements SQL<T> {

	protected final Option option;
    protected final String sql;
    protected final JdbcTemplate jdbcTemplate;
    
    protected final TypePSSetter<T> setter;
    protected final RSParser<T> parser;
    
    protected final int batchLimit;

    public AbtSQL(Option option, String sql, JdbcTemplate jdbcTemplate, int batchLimit, TypePSSetter<T> setter, RSParser<T> parser) {
    	this.option = option;
        this.sql = sql;
        this.jdbcTemplate = jdbcTemplate;
        this.batchLimit = batchLimit;
        this.setter = setter;
        this.parser = parser;
    }
    
    public TypePSSetter<T> getSetter() {
		return setter;
	}

	public RSParser<T> getParser() {
		return parser;
	}
	
	public String getSql() {
		return sql;
	}
	
    //fetch
    public T fetchOne(PSSetter setter) {
        return jdbcTemplate.fetchOne(sql, setter, parser);
    }
    public List<T> fetchMany(PSSetter setter) {
        return jdbcTemplate.fetchMany(sql, setter, parser);
    }
    
	@Override
    public long insertAndIncrement(T value) {
        return jdbcTemplate.fetchIncrement(sql, setter, value);
    }

    @Override
    public String toString() {
        return sql;
    }
    
    
    /*-----------------exec---------------*/
    
    protected boolean _updatable(T value) {
        if(value instanceof TypeFilter) {
            TypeFilter val = (TypeFilter) value;
            return val.updatable(option.code);
        }
        return true;
    }
    
    protected boolean _update0(T value) {
        if(value instanceof TypeFilter) {
            TypeFilter val = (TypeFilter) value;
            if(val.updatable(option.code)) {
                int option = val.update(this.option.code);
                int ret = jdbcTemplate.updateAndReturnCause(sql, setter, value);
                if(ret == 0) {
                    val.commit(option);
                    return true;
                } else {
                    val.cancel(option, ret);
                }
            }
            return false;
        } else {
            return jdbcTemplate.update(sql, setter, value);
        }
    }
    
    protected boolean _update1(PSSetter setter) {
    	return jdbcTemplate.update(sql, setter);
    }
    
    protected boolean _updateBatch0(List<T> vals) {
        int size = vals.size();
        int start = 0;
        while(size > 0 && start < size) {
            int end = Math.min(size, start+batchLimit);
            jdbcTemplate.updateBatch(sql, setter, vals.subList(start, end));
            start = end;
        }
        return true;
    }
    
    protected boolean _updateBatch0(T[] values) {
        return _updateBatch0(_filterAndCommit(values));
    }
    
    protected boolean _updateBatch0(Collection<T> values) {
        return _updateBatch0(_filterAndCommit(values));
    }
    
    protected List<T> _filterAndCommit(Collection<T> values) {
        List<T> ret = new ArrayList<>(values.size());
        for (T t : values) {
            if(t instanceof TypeFilter) {
                TypeFilter val = (TypeFilter) t;
                if(!val.updatable(option.code)) continue;
                val.commit(val.update(option.code));
            }
            ret.add(t);
        }
        return ret;
    }

    protected List<T> _filterAndCommit(T[] values) {
    	List<T> ret = new ArrayList<>(values.length);
        for (T t : values) {
            if(t instanceof TypeFilter) {
                TypeFilter val = (TypeFilter) t;
                if(!val.updatable(option.code)) continue;
                val.commit(val.update(option.code));
            }
            ret.add(t);
        }
        return ret;
    }
    
    protected boolean _updateBatch1(List<PSSetter> setters) {
        int size = setters.size();
        int start = 0;
        while(size > 0 && start < size) {
            int end = Math.min(size, start+batchLimit);
            jdbcTemplate.updateBatch(sql, setters.subList(start, end));
            start = end;
        }
        return true;
    }
    
    protected boolean _updateBatch1(Collection<PSSetter> setters) {
    	return (setters.size() > batchLimit) ? _updateBatch1(new ArrayList<>(setters)) : jdbcTemplate.updateBatch(sql, setters);
    }

    protected boolean _updateBatch1(PSSetter[] setters) {
    	return (setters.length > batchLimit) ? _updateBatch1(Arrays.asList(setters)) : jdbcTemplate.updateBatch(sql, setters);
    }
    
}