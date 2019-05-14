package dev.xframe.jdbc.builder.analyse;

import java.util.HashMap;
import java.util.Map;

import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.codec.FieldCodec;

public class FCodecs {
	
	private Map<String, FieldCodec<?, ?>> codecs = new HashMap<>();
	
	private TypeHandler<?> typeParser;
	
	public TypeHandler<?> getTypeHandler() {
        return typeParser;
    }

    public void setTypeHandler(TypeHandler<?> typeHandler) {
        this.typeParser = typeHandler;
    }
    
    public boolean hasTypeHandler() {
        return this.typeParser != null;
    }
    
    public void put(String fieldName, FieldCodec<?, ?> codec) {
		codecs.put(fieldName, codec);
	}
	
	public boolean hasCodec(String fieldName) {
		return codecs.containsKey(fieldName);
	}

	public Class<?> findColumnTypeFromCodec(String fieldName) {
        return codecs.get(fieldName).getColumnType();
    }

	public FieldCodec<?, ?> get(String key) {
	    return codecs.get(key);
	}
	
	public Map<String, FieldCodec<?, ?>> get() {
		return codecs;
	}
	
}
