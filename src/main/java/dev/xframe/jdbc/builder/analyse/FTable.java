package dev.xframe.jdbc.builder.analyse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.xframe.jdbc.TypeFactory;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.codec.FieldCodecSet;

/**
 * analyzed final table
 * @author luzj
 *
 */
public class FTable {
	
	public Class<?> clazz;
    public String tableName;
    
	public List<FColumn> primaryKeys = new ArrayList<>();
	public List<FIndex> uniqueIndexs = new ArrayList<>();
	public List<FIndex> indexs = new ArrayList<>();
	public List<FColumn> columns = new ArrayList<>();
	
	public Map<String, FColumn> columnMap = new HashMap<>();//key column name (lowercase)
	
	public FieldCodecSet fcSet;
	public TypeHandler<?> typeHandler;
	public TypeFactory<?> typeFactory;
	
	public boolean hasPrimaryKey() {
	    return this.primaryKeys.size() > 0;
	}
	public boolean hasUniqueIndex() {
	    return this.uniqueIndexs.size() > 0;
	}
	public boolean hasOnlyOneUniqueIndex() {
	    return this.uniqueIndexs.size() == 1;
	}
	public boolean hasColumnNotInUniqueIndex() {
	    return uniqueIndexs.isEmpty() ? true : (columns.size() > uniqueIndexs.get(0).columns.size());
	}
	public int batchLimit(int placeHolderCount) {
	    return Math.min(65535/Math.max(1, placeHolderCount), 2048);
	}
	public void setTypeFactory(TypeFactory<?> typeFactory) {
	    this.typeFactory = typeFactory;
	}
    public void setTypeHandler(TypeHandler<?> typeHandler) {
        this.typeHandler = typeHandler;
    }
    public boolean hasTypeFactory() {
        return typeFactory != null;
    }
    public boolean hasTypeHandler() {
        return typeHandler != null;
    }
    @SuppressWarnings("unchecked")
    public <T> TypeFactory<T> typeFactory() {
        return (TypeFactory<T>) typeFactory;
    }
    @SuppressWarnings("unchecked")
    public <T> TypeHandler<T> typeHandler() {
        return (TypeHandler<T>) typeHandler;
    }
}