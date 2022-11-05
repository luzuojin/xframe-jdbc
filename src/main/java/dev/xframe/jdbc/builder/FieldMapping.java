package dev.xframe.jdbc.builder;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import dev.xframe.jdbc.JdbcEnviron;

/**
 * mapping columnName to fieldName
 * @author luzj
 */
@FunctionalInterface
public interface FieldMapping {
    
    /**
     * columnName 映射至 fieldName
     * @param columnName
     * @return
     */
    String apply(String columnName);
    
    /**
     * 合并
     *  JdbcEnviron.fieldMapping
     *  QueryBuilder.fieldMapping
     * @param cMapping
     * @return
     */
    static FieldMapping mixed(FieldMapping.Customized cMapping) {
        final FieldMapping gMapping = JdbcEnviron.getFieldMapping();
        return columnName -> {
            String rName = null;
            //有特定配置
            if((rName = cMapping.getFieldName(columnName)) != null) {
                return rName;
            }
            //全局格式转换
            String fieldName = gMapping.apply(columnName);
            //有特定配置(columnName配置的是转换后的)
            if((rName = cMapping.getFieldName(fieldName)) != null) {
                return rName;
            }
            return fieldName;
        };
    }

    /**
     * 针对单个字段进行的配置
     * @author luzj
     */
    class Customized implements FieldMapping {
        private List<String[]> customized = new LinkedList<>();
        public void set(String fieldName, String columnName) {
            customized.add(new String[] {fieldName, columnName});
        }
        private Optional<String> getColumnName0(String fieldName) {
            return customized.stream().filter(t->t[0].equalsIgnoreCase(fieldName)).map(t->t[1]).findAny();
        }
        public String getColumnName(String fieldName) {
            return getColumnName0(fieldName).orElse(null);
        }
        public String getColumnNameOrDefault(String fileName, String columnName) {
            return getColumnName0(fileName).orElse(columnName);
        }
        private Optional<String> getFieldName0(String columnName) {
            return customized.stream().filter(t->t[1].equalsIgnoreCase(columnName)).map(t->t[0]).findAny();
        }
        public String getFieldName(String columnName) {
            return getFieldName0(columnName).orElse(null);
        }
        public String getFieldNameOrDefault(String columnName, String fieldName) {
            return getFieldName0(columnName).orElse(fieldName);
        }
        @Override
        public String apply(String columnName) {
            return getFieldNameOrDefault(columnName, columnName);
        }
    }
    
}
