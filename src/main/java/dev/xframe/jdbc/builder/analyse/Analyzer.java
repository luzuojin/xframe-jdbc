package dev.xframe.jdbc.builder.analyse;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.builder.FieldMapping;
import dev.xframe.jdbc.codec.FieldCodecSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.Map;

/**
 * 
 * 分析
 * @author luzj
 *
 */
public class Analyzer {
	
	private static final String CONNECT_CLOSE = "关闭连接出错";
    private static final Logger logger = LoggerFactory.getLogger(Analyzer.class);

    public static FTable analyze(Class<?> clazz, String tableName, JdbcTemplate jdbcTemplate, FieldCodecSet fieldCodecs, FieldMapping fieldMapper) {
        return analyze(analyze(clazz), analyze(jdbcTemplate, tableName), fieldCodecs, fieldMapper);
    }
    
    /**
     * 对比双方 去掉多余的属性
     * @param dbtable
     * @param jtable
     * @param mapping
     */
    private static FTable analyze(JTable jtable, DBTable dbtable, FieldCodecSet fieldCodecs, FieldMapping mapping) {
    	FTable fTable = new FTable();
    	fTable.clazz = jtable.clazz;
    	fTable.fcSet = fieldCodecs;
    	
    	if(dbtable != null) {
    		fTable.tableName = dbtable.tableName;

		    for (DBColumn primaryKey : dbtable.primaryKeys) {
		        JColumn jcolumn = jtable.getJColumn(mapping.apply(primaryKey.name));
		        if(jcolumn != null) {
		            fTable.primaryKeys.add(new FColumn(primaryKey, jcolumn));
		        } else {
		            logger.warn("NONE MAPPING PRIMARYKEY TO {}.{}", dbtable.tableName, primaryKey.name);
		        }
            }
    		
		    for (DBIndex uniqueIndex : dbtable.uniqueIndexes) {
		        FIndex fIndex = new FIndex(uniqueIndex.keyNmae, uniqueIndex.nonUnique);
		        for (DBColumn indexColumn : uniqueIndex.columns) {
		            JColumn jcolumn = jtable.getJColumn(mapping.apply(indexColumn.name));
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
		            JColumn jcolumn = jtable.getJColumn(mapping.apply(indexColumn.name));
		            if(jcolumn != null) {
		                fIndex.columns.add(new FColumn(indexColumn, jcolumn));
		            } else {
		                logger.warn("NONE MAPPING INDEX TO {}.{}", dbtable.tableName, indexColumn.name);
		            }
		        }
		        fTable.indexs.add(fIndex);
		    }

    		for(String dbcolumnname : dbtable.columns.keySet()) {
    			JColumn jcolumn = jtable.getJColumn(mapping.apply(dbcolumnname));
    			if(jcolumn != null) {
    				FColumn fcolumn = new FColumn(dbtable.getDBColumn(dbcolumnname), jcolumn);
    				fTable.columns.add(fcolumn);
    				fTable.columnMap.put(dbcolumnname, fcolumn);
    			} else {
    				logger.debug("{} NONE MAPPING TO {}.{}", jtable.clazz.getSimpleName(), dbtable.tableName, dbtable.getDBColumn(dbcolumnname).name);
    			}
    		}

    		fTable.columns.sort(Comparator.comparingInt((FColumn o) -> o.dbColumn.index));
    	}
    	
    	for (Map.Entry<String, JColumn> entry : jtable.columns.entrySet()) {
    		//多出来的java字段, 特殊SQL可能需要用到(多表联合查询??)
    		String fieldName = entry.getKey();
			if(fTable.columnMap.get(fieldName) == null) {
			    DBColumn dbColumn = dbtable.getDBColumn(fieldName);
				fTable.columnMap.put(fieldName, new FColumn(dbColumn, jtable.columns.get(fieldName)));
			}
		}
    	return fTable;
	}
    
    /**
     * @param cls
     * @return
     */
    private static JTable analyze(Class<?> cls) {
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
				table.columns.put(jColumn.name, jColumn);
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
					column.isAutoIncrement = rsmd.isAutoIncrement(i);
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
            table.primaryKeys.sort(Comparator.comparingInt(k -> k.pkSEQ));
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
