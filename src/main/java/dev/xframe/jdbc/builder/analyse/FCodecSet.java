package dev.xframe.jdbc.builder.analyse;

import java.util.HashMap;
import java.util.Map;

import dev.xframe.jdbc.codec.FieldCodec;

public class FCodecSet {
	
	private Map<String, FieldCodec<?, ?>> codecs = new HashMap<>();
	
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
