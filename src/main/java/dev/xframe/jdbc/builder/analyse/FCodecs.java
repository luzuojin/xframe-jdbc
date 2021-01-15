package dev.xframe.jdbc.builder.analyse;

import java.util.HashMap;
import java.util.Map;

import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.codec.FieldCodec;

public class FCodecs {
	
	private Map<String, FieldCodec<?, ?>> codecs = new HashMap<>();
	
	private TypeHandler<?> typeHandler;
	
	public TypeHandler<?> getTypeHandler() {
        return typeHandler;
    }

    public void setTypeHandler(TypeHandler<?> typeHandler) {
        this.typeHandler = typeHandler;
    }
    
    public boolean hasTypeHandler() {
        return this.typeHandler != null;
    }
    
    public void put(String fieldName, FieldCodec<?, ?> codec) {
		codecs.put(fieldName, codec);
	}
	
	public boolean hasCodec(String fieldName) {
		return codecs.containsKey(fieldName);
	}

	public Class<?> getColumnActualType(String fieldName) {
        return codecs.get(fieldName).getColumnActualType();
    }

	public FieldCodec<?, ?> get(String key) {
	    return codecs.get(key);
	}
	
	public Map<String, FieldCodec<?, ?>> get() {
		return codecs;
	}
	
}
