package dev.xframe.jdbc.builder.analyse;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.codec.FieldCodecSet;

/**
 * 
 * 分析
 * @author luzj
 *
 */
public class Analyzer {
	
	private static final String CONNECT_CLOSE = "关闭连接出错";
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);

    public static FTable analyze(Class<?> clazz, String tableName, JdbcTemplate jdbcTemplate, Map<String, String> mappings, FieldCodecSet fieldCodecs) {
        return analyze(analyze(clazz, mappings), analyze(jdbcTemplate, tableName), fieldCodecs);
    }
    
    /**
     * 对比双方 去掉多余的属性
     * @param dbtable
     * @param jtable
     */
    private static FTable analyze(JTable jtable, DBTable dbtable, FieldCodecSet fieldCodecs) {
    	FTable fTable = new FTable();
    	fTable.clazz = jtable.clazz;
    	fTable.fcSet = fieldCodecs;
    	
    	if(dbtable != null) {
    		fTable.tableName = dbtable.tableName;

		    for (DBColumn primaryKey : dbtable.primaryKeys) {
		        JColumn jcolumn = jtable.getJColumn(primaryKey.name);
		        if(jcolumn != null) {
		            fTable.primaryKeys.add(new FColumn(primaryKey, jcolumn));
		        } else {
		            logger.warn("NONE MAPPING PRIMARYKEY TO {}.{}", dbtable.tableName, primaryKey.name);
		        }
            }
    		
		    for (DBIndex uniqueIndex : dbtable.uniqueIndexes) {
		        FIndex fIndex = new FIndex(uniqueIndex.keyNmae, uniqueIndex.nonUnique);
		        for (DBColumn indexColumn : uniqueIndex.columns) {
		            JColumn jcolumn = jtable.getJColumn(indexColumn.name);
		            if(jcolumn != null) {
                        fIndex.columns.add(new FColumn(indexColumn, jcolumn));
                    } else {
                        logger.warn("NONE MAPPING UNIQUE INDEX TO {}.{}", dbtable.tableName, indexColumn.name);
                    }
                }
		        fTable.uniqueIndexs.add(fIndex);
            }
		    
		    for (DBIndex index : dbtable.indexes) {
		        FIndex fIndex = new FIndex(index.keyNmae, index.nonUnique);
		        for (DBColumn indexColumn : index.columns) {
		            JColumn jcolumn = jtable.getJColumn(indexColumn.name);
		            if(jcolumn != null) {
		                fIndex.columns.add(new FColumn(indexColumn, jcolumn));
		            } else {
		                logger.warn("NONE MAPPING INDEX TO {}.{}", dbtable.tableName, indexColumn.name);
		            }
		        }
		        fTable.indexs.add(fIndex);
		    }

    		for(String dbcolumnname : dbtable.columns.keySet()) {
    			JColumn jcolumn = jtable.getJColumn(dbcolumnname);
    			if(jcolumn != null) {
    				FColumn fcolumn = new FColumn(dbtable.getDBColumn(dbcolumnname), jcolumn);
    				fTable.columns.add(fcolumn);
    				fTable.columnMap.put(dbcolumnname, fcolumn);
    			} else {
    				logger.debug("{} NONE MAPPING TO {}.{}", jtable.clazz.getSimpleName(), dbtable.tableName, dbtable.getDBColumn(dbcolumnname).name);
    			}
    		}

    		Collections.sort(fTable.columns, new Comparator<FColumn>() {
    			@Override
    			public int compare(FColumn o1, FColumn o2) {
    				return o1.dbColumn.index - o2.dbColumn.index;
    			}
    		});
    	}
    	
    	for (Map.Entry<String, JColumn> entry : jtable.columns.entrySet()) {
    		//多出来的java字段, 特殊SQL可能需要用到(多表联合查询??)
    		String dbcolumname = entry.getKey();
			if(fTable.columnMap.get(dbcolumname) == null) {
			    DBColumn dbColumn = dbtable.getDBColumn(dbcolumname);
				fTable.columnMap.put(dbcolumname, new FColumn(dbColumn, jtable.getJColumn(dbcolumname)));
			}
		}
    	return fTable;
	}
    
    /**
     * @param cls
     * @param mappings  JAVA_FIELD_NAME <> DB_COLUMN_NAME
     * @param map 
     * @return
     */
    private static JTable analyze(Class<?> cls, Map<String, String> mappings) {
    	JTable table = new JTable(cls);
    	while(cls != null) {
    		Field[] fields = cls.getDeclaredFields();
    		for (Field field : fields) {
				int modifiers = field.getModifiers();
				if(Modifier.isStatic(modifiers) || Modifier.isTransient(modifiers)) {
					continue;
				}
				JColumn jColumn = new JColumn();
				jColumn.field = field;
				jColumn.name = field.getName();
				jColumn.type = field.getType();
				//DB_COLUMN_NAME as key
				table.addJColumn(mappings.getOrDefault(jColumn.name, jColumn.name), jColumn);
			}
    		cls = cls.getSuperclass();
    	}
    	return table;
    }
    
    private static DBTable analyze(JdbcTemplate jdbcTemplate, final String tableName) {
    	if(tableName == null) return null;
    	
		String sql = "select * from " + tableName + " where 1 = 0;";
		DBTable table = jdbcTemplate.fetch(sql, PSSetter.NONE, new RSParser<DBTable>() {
			@Override
			public DBTable parse(ResultSet rs) throws SQLException {
				ResultSetMetaData rsmd = rs.getMetaData();
	            int count = rsmd.getColumnCount();
	            DBTable table = new DBTable(tableName);
	            for (int i = 1; i <= count; i++) {
	            	DBColumn column = new DBColumn();
	            	column.index = i;
	            	column.name = rsmd.getColumnLabel(i);
	            	column.type = rsmd.getColumnType(i);
					table.addDBColumn(column);
				}
				return table;
			}
		});
		
		Connection conn = null;
        try {
            conn = jdbcTemplate.dataSource.getConnection();
            ResultSet primaryKeys = conn.getMetaData().getPrimaryKeys(conn.getCatalog(), conn.getSchema(), tableName);
            while(primaryKeys.next()) {
            	table.primaryKeys.add(table.getDBColumn(primaryKeys.getString("COLUMN_NAME")).withPKSEQ(primaryKeys.getInt("KEY_SEQ")));
            }
            Collections.sort(table.primaryKeys, (k1, k2)->Integer.compare(k1.pkSEQ, k2.pkSEQ));
            ResultSet uniqueIndexs = conn.getMetaData().getIndexInfo(conn.getCatalog(), conn.getSchema(), tableName, false, false);
            while(uniqueIndexs.next()) {
                table.addDBIndex(uniqueIndexs.getString("INDEX_NAME"), uniqueIndexs.getBoolean("NON_UNIQUE"), table.getDBColumn(uniqueIndexs.getString("COLUMN_NAME")));
            }
            return table;
        } catch (SQLException e) {
        	logger.error("Table compile wrong", e);
        } finally {
            close(conn);
        }
        return null;
	}
	
    private static void close(Connection conn) {
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            logger.error(CONNECT_CLOSE, e);
        }
    }
    
}
