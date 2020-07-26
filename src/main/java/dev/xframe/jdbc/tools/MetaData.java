package dev.xframe.jdbc.tools;


import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collector;

import dev.xframe.jdbc.JdbcTemplate;
import dev.xframe.jdbc.PSSetter;
import dev.xframe.jdbc.datasource.DBConf;
import dev.xframe.jdbc.datasource.DataSources;

/**
 * 
 * database 结构
 * 
 * @author luzj
 *
 */
public class MetaData {

    static DataBase getDataBase(DBConf conf) {
        return getDataBase(conf, Collections.emptyList());
    }
    static DataBase getDataBase(DBConf conf, List<String> tables) {
    	JdbcTemplate jdbc = new JdbcTemplate(DataSources.tomcatJdbc(conf));
        return jdbc.fetchMany("SHOW TABLES;", PSSetter.NONE, r->r.getString(1)).stream().filter(tablefilter(tables)).map(s->getTable(jdbc, s)).collect(Collector.of(DataBase::new, (d,t)->d.add(t), (d1,d2)->null));
    }
    
    static Predicate<? super String> tablefilter(List<String> tables) {
        return tables.isEmpty() ? t->true : t->tables.contains(t);
    }

    static Table getTable(JdbcTemplate jdbc, String name) {
        Table table = new Table(name);
        jdbc.fetch("DESC `" + name + "`", PSSetter.NONE, r->{
            while(r.next()) {
                Column c = new Column();
                c.name = r.getString(1);
                c.type = r.getString(2);
                c.nullable = r.getString(3);
                                    //(4) key;
                c.defaultValue = r.getString(5);
                c.extra = r.getString(6);
                table.columns.put(c.name, c);
            }
            return table;
        });
        
        Connection conn = null;
        try {
            conn = jdbc.dataSource.getConnection();
            DatabaseMetaData metaData = conn.getMetaData();
            ResultSet indexes = metaData.getIndexInfo(conn.getCatalog(), conn.getSchema(), name, false, false);
            while(indexes.next()) {
                String indexName = indexes.getString("INDEX_NAME");
                Column column = table.columns.get(indexes.getString("COLUMN_NAME"));
                boolean nonUnique = indexes.getBoolean("NON_UNIQUE"); 
                int type = nonUnique ? 2 : ("PRIMARY".equals(indexName) ? 0 : 1);
                table.addIndexColumn(indexName, type, column);
            }
            return table;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return table;
    }
    
    public static class DataBase {
        public Map<String, Table> tables = new LinkedHashMap<>();
        void add(Table t) {
            tables.put(t.name, t);
        }
        @Override
        public String toString() {
            return tables.keySet().toString();
        }
    }
    
    public static class Table {
        public String name;
        public Map<String, Index> indexes = new LinkedHashMap<>();
        public Map<String, Column> columns = new LinkedHashMap<>();
        public Table(String name) {
            this.name = name;
        }
        void addIndexColumn(String name, int type, Column c) {
            Index index = indexes.get(name);
            if(index == null) {
                index = new Index(name, type);
                indexes.put(name, index);
            }
            index.columns.put(c.name, c);
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder()
                    .append("CREATE TABLE `").append(name).append("` (");
            columns.values().forEach(c->sb.append(c).append(","));
            indexes.values().forEach(i->sb.append(i).append(","));
            sb.delete(sb.length()-1, sb.length()).append(");");
            return sb.toString();
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((name == null) ? 0 : name.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            Table other = (Table) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            return true;
        }
    }
    
    public static class Index {
        public Index(String name, int type) {
            this.name = name;
            this.type = type;
        }
        public final int type;//0 primaryKey, 1 uniqueKey, 2 index
        public final String name;
        public final Map<String, Column> columns = new LinkedHashMap<>();
        public boolean isPrimaryKey() {
            return type == 0;
        }
        public String toName() {
        	return type == 0 ? "PRIMARY KEY" : "INDEX " + name;
        }
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder()
                    .append(type == 0 ? "PRIMARY KEY" : ((type == 1 ? "UNIQUE KEY" : "INDEX") + " `" + name + "`"))
                    .append(" (");
            columns.values().forEach(c->sb.append("`").append(c.name).append("`").append(","));
            sb.delete(sb.length()-1, sb.length()).append(")");
            return sb.toString();
        }
        @Override
        public boolean equals(Object obj) {
            Index other = (Index) obj;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (type != other.type)
                return false;
            if(!columns.keySet().equals(other.columns.keySet()))
                return false;
            return true;
        }
    }
    
    public static class Column {
        String name;
        String type;
        String nullable;
        String defaultValue;
        String extra;
        
        @Override
        public String toString() {
             StringBuilder sb = new StringBuilder()
                    .append("`").append(name).append("`")
                    .append(" ").append(type)
                    .append(" ").append("NO".equals(nullable) ? "NOT NULL" : "NULL");
             
             if(!("NO".equals(nullable) && defaultValue == null))
                 sb.append(" ").append("DEFAULT ").append(defValStr());
             if(extra != null && extra.length() > 0)
                 sb.append(" ").append(extra);
             
             return sb.toString();
        }

        protected String defValStr() {
            if(defaultValue == null)
                return "NULL";
            if(defaultValue.startsWith("CURRENT_TIMESTAMP"))//TODO other contants 
                return defaultValue;
            return "'" + defaultValue + "'";
        }

        @Override
        public boolean equals(Object obj) {
            Column other = (Column) obj;
            if (defaultValue == null) {
                if (other.defaultValue != null)
                    return false;
            } else if (!defaultValue.equals(other.defaultValue))
                return false;
            if (extra == null) {
                if (other.extra != null)
                    return false;
            } else if (!extra.equals(other.extra))
                return false;
            if (name == null) {
                if (other.name != null)
                    return false;
            } else if (!name.equals(other.name))
                return false;
            if (nullable == null) {
                if (other.nullable != null)
                    return false;
            } else if (!nullable.equals(other.nullable))
                return false;
            if (type == null) {
                if (other.type != null)
                    return false;
            } else if (!type.equals(other.type))
                return false;
            return true;
        }
    }
    
}
