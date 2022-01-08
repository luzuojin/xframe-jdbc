package dev.xframe.jdbc.sql;

public interface Condition {

	boolean isPlaceholder();

	String getColumn();

	class Plain implements Condition {
		String plain;
		public Plain(String plain) {
			this.plain = plain;
		}
		@Override
		public boolean isPlaceholder() {
			return false;
		}
		@Override
		public String getColumn() {
			throw new UnsupportedOperationException();
		}
		@Override
		public String toString() {
			return plain;
		}
	}

	class Placeholder implements Condition {
		String assign;
		String column;
		String value;
		public Placeholder(String assign, String column, String value) {
			this.assign = assign;
			this.column = column;
			this.value = value;
		}
		@Override
		public boolean isPlaceholder() {
			return true;
		}
		@Override
		public String getColumn() {
			return column;
		}
		@Override
		public String toString() {
			return String.format("`%s`%s%s", column, assign, value);
		}
	}

}
