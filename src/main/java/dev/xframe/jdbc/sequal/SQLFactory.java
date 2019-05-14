package dev.xframe.jdbc.sequal;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.sequal.SQL.Option;

public class SQLFactory<T> {
	
	private boolean asyncModel;
	private JdbcTemplate jdbcTemplate;
	
	public SQLFactory(boolean asyncModel, JdbcTemplate jdbcTemplate) {
		this.asyncModel = asyncModel;
		this.jdbcTemplate = jdbcTemplate;
	}

	public AbtSQL<T> newSQL(Option option, String sql, int batchLimit, TypePSSetter<T> setter, RSParser<T> parser) {
		if(asyncModel) {
			return new AsyncSQL<T>(option, sql, jdbcTemplate, batchLimit, setter, parser);
		}
		return new SyncSQL<T>(option, sql, jdbcTemplate, batchLimit, setter, parser);
	}
	
}
