package dev.xframe.jdbc.tools;

import java.io.PrintStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.datasource.DBConf;
import dev.xframe.jdbc.datasource.DataSources;
import dev.xframe.jdbc.tools.MetaData.Column;
import dev.xframe.jdbc.tools.MetaData.DataBase;
import dev.xframe.jdbc.tools.MetaData.Index;
import dev.xframe.jdbc.tools.MetaData.Table;


/**
 * 对比两个数据库 找出差异内容生成对应的sql scripts
 * @author luzj
 */
public class SQLDiff {
    
    public static interface DiffSupplier {
        boolean test(String table);
        List<DiffRecord> diff(Table table, List<Record> baseDatas, List<Record> currDatas);
    }

    /**only structure*/
    public static void diffStructure(DBConf baseConf, DBConf currConf, PrintStream ps, DiffSupplier... supliers) {
        diff0(false, baseConf, currConf, Collections.emptyList(), ps, supliers);
    }
    public static void diffStructure(DBConf baseConf, DBConf currConf, List<String> tables, PrintStream ps, DiffSupplier... supliers) {
        diff0(false, baseConf, currConf, tables, ps, supliers);
    }
    /**both structure and datas*/
    public static void diff(DBConf baseConf, DBConf currConf, PrintStream ps, DiffSupplier... supliers) {
        diff0(true, baseConf, currConf, Collections.emptyList(), ps, supliers);
    }
    public static void diff(DBConf baseConf, DBConf currConf, List<String> tables, PrintStream ps, DiffSupplier... supliers) {
        diff0(true, baseConf, currConf, tables, ps, supliers);
    }
    private static void diff0(boolean dataDiff, DBConf baseConf, DBConf currConf, List<String> tables, PrintStream ps, DiffSupplier... supliers) {
        DiffSupplier suplier = buildSuplier(supliers);
        
    	DataBase currdb = getMeta(currConf, tables);
        List<Table> nonPri = checkPrimaryKey(currdb);
        if(dataDiff && nonPri.stream().filter(t->!suplier.test(t.name)).count()>0) {
            nonPri.forEach(t->System.out.println(t.name + " none primary key"));
        } else {
            Map<String, DiffTable> difftables = structure(getMeta(baseConf, tables), currdb);
            Map<String, DiffData> diffdatas = dataDiff ? data(baseConf, currConf, currdb, difftables, suplier) : new HashMap<>();
            
            Stream.concat(difftables.keySet().stream(), diffdatas.values().stream().filter(v->v.hasDiff()).map(v->v.table)).collect(Collectors.toSet())
                .forEach(name->{
                    ps.println("# " + name);
                    
                    DiffTable dt = difftables.get(name);
                    if(dt != null) ps.println(dt);
                    
                    DiffData dd = diffdatas.get(name);
                    if(dd != null) ps.println(dd);
                });

            ps.flush();
            ps.close();
        }
    }

    private static DiffSupplier buildSuplier(DiffSupplier... supliers) {
        DiffSupplier suplier = new DiffSupplier() {
            @Override
            public boolean test(String table) {
                return Arrays.stream(supliers).filter(s->s.test(table)).findAny().isPresent();
            }
            @Override
            public List<DiffRecord> diff(Table table, List<Record> baseDatas, List<Record> currDatas) {
                return Arrays.stream(supliers).filter(s->s.test(table.name)).findAny().get().diff(table, baseDatas, currDatas);
            }
        };
        return suplier;
    }
    
    public static DataBase getMeta(DBConf conf) {
        return MetaData.getDataBase(conf);
    }
    public static DataBase getMeta(DBConf conf, List<String> tables) {
        return MetaData.getDataBase(conf, tables);
    }
    
    public static Map<String, DiffData> data(DBConf base, DBConf curr, DataBase curdb, Map<String, DiffTable> diffs) {
        return data(base, curr, curdb, diffs, null);
    }
    public static Map<String, DiffData> data(DBConf base, DBConf curr, DataBase curdb, Map<String, DiffTable> diffs, DiffSupplier suplier) {
        Map<String, DiffData> ret = new HashMap<>();
        JdbcTemplate baseJdbc = new JdbcTemplate(DataSources.tomcatJdbc(base));
        JdbcTemplate currJdbc = new JdbcTemplate(DataSources.tomcatJdbc(curr));
        curdb.tables.values().forEach(t->{
            if(diffs.containsKey(t.name)) {//有修改
                DiffTable diff = diffs.get(t.name);
                if(diff.option == Option.DELETE) return;//删表不需要对比数据
                
                if(diff.option == Option.CREATE) {
                	ret.put(t.name, new DiffData(t.name, Option.CREATE, fetchDatas(currJdbc, t).stream().map(v->new DiffRecord(Option.CREATE, v)).collect(Collectors.toList())));
                	return;//新表所有数据
                }
                
                if(diff.columns.values().stream().filter(c->Option.CREATE==c.option).count() > 0) {//有字段增加
                    Map<String, Value> avals = diff.columns.values().stream().map(dc->new Value(dc.column.name, dc.column.defaultValue)).collect(Collectors.toMap(v->v.key, v->v));
                	ret.put(t.name, compareDatas(fetchDatas(baseJdbc, t, avals), fetchDatas(currJdbc, t), t, suplier));
                    return;
                }
            }
            //index chnage or none structure change
            ret.put(t.name, compareDatas(baseJdbc, currJdbc, t, suplier));
        });
        return ret;
    }
    
    private static BinaryOperator<Record> merger() {
        return (k, v) -> {
            System.out.println(String.format("Duplicate key %s, %s", v.table, k));
            return v;
        };
    }
    
    private static DiffData compareDatas(JdbcTemplate baseJdbc, JdbcTemplate currJdbc, Table table, DiffSupplier suplier) {
        return compareDatas(fetchDatas(baseJdbc, table), fetchDatas(currJdbc, table), table, suplier);
    }
    private static DiffData compareDatas(List<Record> baseDatas, List<Record> currDatas, Table table, DiffSupplier suplier) {
        if(suplier!=null && suplier.test(table.name)) {//自定义比较
            return new DiffData(table.name, Option.MODIFY, suplier.diff(table, baseDatas, currDatas));
        }
        
    	Map<String, Record> base = baseDatas.stream().collect(Collectors.toMap(r->r.toKey(),r->r,merger(),LinkedHashMap::new));
    	List<DiffRecord> drs = new ArrayList<>();
    	currDatas.forEach(r->{
    		Record br = base.remove(r.toKey());
    		if(br == null) {
    			drs.add(new DiffRecord(Option.CREATE, r));
    		} else if(!br.equals(r)) {
    			drs.add(new DiffRecord(Option.MODIFY, r));
    		}
    	});
    	base.values().forEach(br->{//被删除的数据
    		drs.add(new DiffRecord(Option.DELETE, br));
    	});
		return new DiffData(table.name, Option.MODIFY, drs);
	}

    private static List<Record> fetchDatas(JdbcTemplate jdbc, Table table) {
        return fetchDatas(jdbc, table, Collections.emptyMap());
    }
	private static List<Record> fetchDatas(JdbcTemplate jdbc, Table table, Map<String, Value> createVals) {
    	return jdbc.fetchMany("SELECT * FROM `" + table.name + "`", PSSetter.NONE, new RSParser<Record>() {
			@Override
			public Record parse(ResultSet rs) throws SQLException {
				Index index = table.indexes.get("PRIMARY");
				Collection<Column> cs = table.columns.values();
				Value[] primary = new Value[index==null ? 0 : index.columns.size()];
				Value[] values = new Value[cs.size()];
				int idx = 0;
				int pri = 0;
				for (Column c : cs) {
					Value value = new Value(c.name, createVals.containsKey(c.name) ? createVals.get(c.name).val : rs.getObject(c.name));
					values[idx] = value;
					++ idx;
					if(index!=null && index.columns.containsKey(c.name)) {
						primary[pri] = value;
						++ pri;
					}
				}
				return new Record(table.name, primary, values);
			}
		});
	}

	/**
     * 检测是否有主键
     * @param currentDB
     * @return 没有主键的表集合
     */
    public static List<Table> checkPrimaryKey(DataBase currentDB) {
        return currentDB.tables.values().stream().filter(t->!t.indexes.containsKey("PRIMARY")).collect(Collectors.toList());
    }
    
    public static Map<String, DiffTable> structure(DataBase baseDB, DataBase currentDB) {
        Map<String, DiffTable> diffs = new LinkedHashMap<>();
        currentDB.tables.values().forEach(t->{
        	DiffTable diff = new DiffTable(t);
        	Table ot = baseDB.tables.get(t.name);
        	if(ot == null) {
        		diff.of(Option.CREATE);
        	} else {
        	    t.columns.values().forEach(c->{
                    Column oc = ot.columns.get(c.name);
                    if(oc == null) {
                        diff.of(Option.MODIFY).columns.put(c.name, new DiffColumn(Option.CREATE, c));
                    } else if(!c.equals(oc)) {
                        diff.of(Option.MODIFY).columns.put(c.name, new DiffColumn(Option.MODIFY, c));
                    }
                });
                ot.columns.values().forEach(oc->{
                    if(!t.columns.containsKey(oc.name)) diff.of(Option.MODIFY).columns.put(oc.name, new DiffColumn(Option.DELETE, oc));
                });
                
        		t.indexes.values().forEach(i->{
        		    Index oi = ot.indexes.get(i.name);
        		    if(oi == null) {
        		        diff.of(Option.MODIFY).indexes.put(i.name, new DiffIndex(Option.CREATE, i));
        		    } else if(!i.equals(oi)){
        		        diff.of(Option.MODIFY).indexes.put(i.name, new DiffIndex(Option.MODIFY, i));
        		    }
        		});
        		ot.indexes.values().forEach(oi -> {
        			if(!t.indexes.containsKey(oi.name)) diff.of(Option.MODIFY).indexes.put(oi.name, new DiffIndex(Option.DELETE, oi));
        		});
        	}
        	if(diff.option != null) diffs.put(diff.table.name, diff);
        });
        
        return diffs;
    }
    
	public static enum Option {
    	CREATE, MODIFY, DELETE;
    }
	
    public static class DiffTable {
    	public DiffTable(Table table) {
    		this.table = table;
    	}
    	public DiffTable of(Option option) {
    		this.option = option;
    		return this;
    	}
    	public Table table;
    	Option option;
    	Map<String, DiffIndex> indexes = new LinkedHashMap<>();
    	Map<String, DiffColumn> columns = new LinkedHashMap<>();
        @Override
        public String toString() {
            StringBuilder sql = new StringBuilder();
            if(option == Option.CREATE) {
                sql.append(table.toString()).append("\n");
            } else {
                columns.values().forEach(c->sql.append(c.toString(table.name)).append("\n"));
                indexes.values().forEach(i->sql.append(i.toString(table.name)).append("\n"));
            }
            return sql.toString();
        }
    }
    
    public static class DiffIndex {
    	public DiffIndex(Option option, Index index) {
    		this.option = option;
    		this.index = index;
		}
		Option option;
    	Index index;
		public String toString(String tableName) {
			switch (option) {
			case CREATE:
				return String.format("ALTER TABLE `%s` ADD %s;", tableName, index.toString());
			case MODIFY:
				return String.format("ALTER TABLE `%s` DROP %s;", tableName, index.toName()) + "\n" +
					   String.format("ALTER TABLE `%s` ADD %s;", tableName, index.toString());
			case DELETE:
				return String.format("ALTER TABLE `%s` DROP %s;", tableName, index.toName());
			default:
				throw new IllegalArgumentException("Option can`t be " + option);
			}
		}
    }
    
    public static class DiffColumn {
    	public DiffColumn(Option option, Column column) {
    		this.option = option;
    		this.column = column;
		}
		Option option;
    	Column column;
		public String toString(String tableName) {
			switch (option) {
			case CREATE:
				return String.format("ALTER TABLE `%s` ADD %s;", tableName, column.toString());
			case MODIFY:
				return String.format("ALTER TABLE `%s` MODIFY %s;", tableName, column.toString());
			case DELETE:
				return String.format("ALTER TABLE `%s` DROP %s;", tableName, column.name);
			default:
				throw new IllegalArgumentException("Option can`t be " + option);
			}
		}
    }
    
    public static class DiffData {//一个表的数据
    	public DiffData(String table, Option option, List<DiffRecord> records) {
    		this.table = table;
    		this.option = option;
    		this.records = records;
		}
    	public String table;
		Option option;
    	List<DiffRecord> records;
		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder();
			if(Option.DELETE == option) {
				sb.append("TRUNCATE TABLE ").append(table).append(";\n");
			}
			records.stream().filter(r->r.option!=Option.CREATE).forEach(r->sb.append(r.toString()).append("\n"));
			List<DiffRecord> inserts = records.stream().filter(r->r.option==Option.CREATE).collect(Collectors.toList());
			if(!inserts.isEmpty()) {
			    sb.append(inserts.get(0).toInsertKeyPart()).append("\n\t")
			        .append(String.join(",\n\t", inserts.stream().map(r->r.toInsertValPart()).collect(Collectors.toList())))
			        .append(";\n");
			}
			return sb.toString();
		}
		public boolean hasDiff() {
		    return !records.isEmpty();
		}
    }
    
    public static class DiffRecord {//一行数据
    	public DiffRecord(Option option, Record record) {
    		this.option = option;
    		this.record = record;
		}
		Option option;
		Record record;
		
		public String toString() {
			switch (option) {
			case CREATE:
				return toInsert();
			case MODIFY:
				return toUpdate();
			case DELETE:
				return toDelete();
			default:
				throw new IllegalArgumentException("can`t be here");
			}
		}
		protected String toWhere() {
			StringBuilder sb = new StringBuilder("WHERE ");
			Arrays.stream(record.primary).forEach(v->sb.append("`").append(v.key).append("`").append("=").append(v).append(" AND "));
			return sb.substring(0, sb.length() - 5);
		}
		protected String toDelete() {
			return "DELETE FROM " + record.table + " " + toWhere() + ";";
		}
		protected String toUpdate() {
			StringBuilder sb = new StringBuilder("UPDATE ").append(record.table).append(" SET ");
			Arrays.stream(record.values).forEach(v->sb.append("`").append(v.key).append("`").append("=").append(v).append("").append(","));
			return sb.substring(0, sb.length()-1) + " " + toWhere() + ";";
		}
		protected String toInsert() {
		    return toInsert(record);
		}
		protected String toInsert(Record record) {
			StringBuilder keys = new StringBuilder();
			StringBuilder vals = new StringBuilder();
			Arrays.stream(record.values).forEach(v->{
				keys.append("`").append(v.key).append("`").append(",");
				vals.append(v).append(",");
			});
			return "INSERT INTO " + record.table + "(" + keys.substring(0, keys.length()-1) + ") VALUES (" + vals.substring(0, vals.length()-1) + ");";
		}
		
		protected String toInsertValPart() {
		    return toInsertValPart(record);
		}
		protected String toInsertValPart(Record record) {
		    StringBuilder vals = new StringBuilder();
		    Arrays.stream(record.values).forEach(v->vals.append(v).append(","));
		    return "(" + vals.substring(0, vals.length()-1) + ")";
		}
		protected String toInsertKeyPart() {
		    return toInsertKeyPart(record);
		}
		protected String toInsertKeyPart(Record record) {
		    StringBuilder keys = new StringBuilder();
            Arrays.stream(record.values).forEach(v->keys.append("`").append(v.key).append("`").append(","));
            return "INSERT INTO " + record.table + "(" + keys.substring(0, keys.length()-1) + ") VALUES ";
		}
    }
    
    public static class Record {
    	public Record(String table, Value[] primary, Value[] values) {
    	    this.table = table;
    		this.primary = primary;
    		this.values = values;
		}
		public String toKey() {
			return Arrays.toString(Arrays.stream(primary).map(v->v.val).toArray());
		}
		public final String table;
		public final Value[] primary;
    	public final Value[] values;
		@Override
        public String toString() {
            return toKey();
        }
        @Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + Arrays.hashCode(values);
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Record other = (Record) obj;
			if (!Arrays.deepEquals(values, other.values))
				return false;
			return true;
		}
    }
    
    public static class Value {
    	public Value(String key, Object val) {
    		this.key = key;
    		this.val = val == null ? null : val.toString();
		}
		public final String key;
		public final String val;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((key == null) ? 0 : key.hashCode());
			result = prime * result + ((val == null) ? 0 : val.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Value other = (Value) obj;
			if (key == null) {
				if (other.key != null)
					return false;
			} else if (!key.equals(other.key))
				return false;
			if (val == null) {
				if (other.val != null)
					return false;
			} else if (!val.equals(other.val))
				return false;
			return true;
		}
        @Override
        public String toString() {
            //why
            if("false".equals(val)) return "'0'";
            if("true".equals(val)) return "'1'";
            return val == null ? "NULL" : ("'" + val.replace("'", "''") + "'");
        }
    }
    
    /**
     * 根据指定字段作为column来找出差异
     * 对应记录中如果有任意一条记录不同, 则删除对应所有记录并新增
     * @author luzj
     */
    public static class ColumnSupplier implements DiffSupplier {
    	final String table;
    	final String[] columns;
    	public ColumnSupplier(String table, String... columns) {
    		this.table = table;
    		this.columns = columns;
		}
		@Override
		public boolean test(String table) {
			return this.table.equals(table);
		}
		Map<String, Value[]> keyMap = new LinkedHashMap<>();//保证key对象相同
		@Override
		public List<DiffRecord> diff(Table table, List<Record> baseDatas, List<Record> currDatas) {
			Map<Value[], List<Record>> base = toMap(baseDatas);
			Map<Value[], List<Record>> curr = toMap(currDatas);

			List<DiffRecord> diffs = new ArrayList<>();
			curr.forEach((k, rs)->{
				List<Record> brs = base.remove(k);
				if(brs == null || brs.isEmpty()) {
					diffs.add(new NoneUniqueDiffRecord(Option.CREATE, k, rs));
				} else if(rs.stream().filter(r->!brs.stream().filter(br->br.equals(r)).findAny().isPresent()).findAny().isPresent() ||
				        brs.stream().filter(br->!rs.stream().filter(r->r.equals(br)).findAny().isPresent()).findAny().isPresent()) {
					diffs.add(new NoneUniqueDiffRecord(Option.MODIFY, k, rs));
				}
			});
			base.forEach((k, rs)->{
				diffs.add(new NoneUniqueDiffRecord(Option.DELETE, k, rs));
			});
			return diffs;
		}
	    Map<Value[], List<Record>> toMap(List<Record> baseDatas) {
	    	Map<Value[], List<Record>> ret = new LinkedHashMap<>();
	    	baseDatas.forEach(r->{
	    		Value[] key = new Value[columns.length];
	    		for (int i = 0; i < columns.length; i++) {
					String column = columns[i];
					key[i] = Arrays.stream(r.values).filter(v->v.key.equals(column)).findAny().get();
				}
	    		String keyStr = Arrays.deepToString(key);
                keyMap.putIfAbsent(keyStr, key);
	    		addToMapList(ret, keyMap.get(keyStr), r);
	    	});
	    	return ret;
	    }
	    <K, V> Map<K, List<V>> addToMapList(Map<K, List<V>> map, K k, V v) {
	        List<V> vs = map.get(k);
	        if(vs == null) {
	            vs = new ArrayList<V>();
	            map.put(k, vs);
	        }
	        vs.add(v);
	        return map;
	    }
	    class NoneUniqueDiffRecord extends DiffRecord {
	        Option option;
	        Value[] keys;
	        List<Record> records;
	        public NoneUniqueDiffRecord(Option option, Value[] keys, List<Record> records) {
	            super(option, null);
	            this.keys = keys;
	            this.option = option;
	            this.records = records;
	        }
	        protected String toWhere() {
				StringBuilder sb = new StringBuilder("WHERE ");
				Arrays.stream(keys).forEach(v->sb.append("`").append(v.key).append("`").append("=").append(v).append(" AND "));
				return sb.substring(0, sb.length()-5);
			}
	        protected String toDelete() {
	            return "DELETE FROM " + ColumnSupplier.this.table + " " + toWhere() + ";\n"; 
	        }
	        @Override
	        protected String toUpdate() {
	            return toDelete() + toInsert();
	        }
	        protected String toInsert() {
	            StringBuilder sb = new StringBuilder();
	            records.forEach(r->sb.append(toInsert(r)).append("\n"));
	            return sb.toString();
	        }
            @Override
            protected String toInsertValPart() {
                return String.join(",\n\t", records.stream().map(r->toInsertValPart(r)).collect(Collectors.toList()));
            }
            @Override
            protected String toInsertKeyPart() {
                return super.toInsertKeyPart(records.get(0));
            }
	    }
    }
    
}
