package dev.xframe.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * PreparedStatement setter
 * @author luzj
 */
@FunctionalInterface
public interface PSSetter {
	
	void set(PreparedStatement pstmt) throws Exception;
	
	static PSSetter of(Object param) {
		return new SimpleParamPSSetter(param);
	}
	
	static PSSetter of(Object param1, Object param2) {
		return new DoubleParamPSSetter(param1, param2);
	}
	
	static PSSetter of(Object... params) {
		return new ArrayParamPSSetter(params);
	}
	
	class SimpleParamPSSetter implements PSSetter {
		Object param;
		public SimpleParamPSSetter(Object param) {
			this.param = param;
		}
		@Override
		public void set(PreparedStatement pstmt) throws SQLException {
			pstmt.setObject(1, param);
		}
	}
	class DoubleParamPSSetter implements PSSetter {
		Object firstParam;
		Object secondParam;
		public DoubleParamPSSetter(Object firstParam, Object secondParam) {
			this.firstParam = firstParam;
			this.secondParam = secondParam;
		}
		@Override
		public void set(PreparedStatement pstmt) throws SQLException {
			pstmt.setObject(1, firstParam);
			pstmt.setObject(2, secondParam);
		}
	}
	class ArrayParamPSSetter implements PSSetter {
		Object[] params;
		public ArrayParamPSSetter(Object[] params) {
			this.params = params;
		}
		@Override
		public void set(PreparedStatement pstmt) throws SQLException {
			for (int i = 0; i < params.length; i++) {
				pstmt.setObject(i+1, params[i]);
			}
		}
	}
	
	PSSetter NONE = pstmt->{};
	
}
