package dev.xframe.jdbc.codec.provides;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import dev.xframe.jdbc.codec.Codec;
import dev.xframe.jdbc.codec.FieldCodec;

/**
 * 包含@Codec的enum与int之间的相互转换工具类
 * @author luzj
 */
public class EnumCodec implements FieldCodec<Enum<?>, Integer> {
	
	private Map<Integer, Enum<?>> key_cache = new HashMap<Integer, Enum<?>>();
	private Map<Enum<?>, Integer> val_cache = new HashMap<Enum<?>, Integer>();
	
	public EnumCodec(Class<? extends Enum<?>> clazz) {
	    try {
            Enum<?>[] tmp = (Enum[]) clazz.getMethod("values").invoke(null);
            for (Enum<?> key : tmp) {
                int val = clazz.getField(key.name()).getAnnotation(Codec.class).value();
                val_cache.put(key, val);
                key_cache.put(val, key);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
	}
	
    @Override
    public Integer encode(Enum<?> fieldValue) {
        return val_cache.get(fieldValue);
    }

    @Override
    public Enum<?> decode(Integer columnValue) {
        return key_cache.get(columnValue);
    }
    
    private static Map<Class<?>, EnumCodec> caches = new HashMap<Class<?>, EnumCodec>();
    
	@SuppressWarnings("unchecked")
    public static <V extends Enum<?>> V decode (Class<V> type, int code) {
	    return (V) build(type).decode(code);
	}
	
	public static EnumCodec build(Field field) {
	    return build(field.getType());
	}
	@SuppressWarnings("unchecked")
	public static EnumCodec build(Class<?> clazz) {
	    EnumCodec enumCodec = caches.get(clazz);
	    if(enumCodec == null) {
	        enumCodec = new EnumCodec((Class<? extends Enum<?>>) clazz);
	        caches.put(clazz, enumCodec);
	    }
        return enumCodec;
	}
	
	/**
     * enum的构造函数中使用
     * 不使用缓存
     * @param src
     * @return
     */
    public static int encodeBeforeConstructed(Enum<?> src) {
        try {
            return src.getClass().getField(src.name()).getAnnotation(Codec.class).value();
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
