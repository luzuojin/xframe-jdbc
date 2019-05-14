package dev.xframe.jdbc.codec;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

public class FieldCodecs {
	
	static Map<Predicate<Class<?>>, Function<Field, FieldCodec<?, ?>>> DEFAULTS = new HashMap<>();
	static {
		DEFAULTS.put(BasicCodecs::isProvided, BasicCodecs::getFieldCodec);
	}
	
	public static void addDefault(Predicate<Class<?>> p, Function<Field, FieldCodec<?, ?>> fc) {
		DEFAULTS.put(p, fc);
	}
	public static void addDefault(Class<?> c, FieldCodec<?, ?> fc) {
		addDefault(x->c.equals(x), f->fc);
	}
	
	public static FieldCodec<?, ?> getDefault(Field field) {
		for (Entry<Predicate<Class<?>>, Function<Field, FieldCodec<?, ?>>> entry : DEFAULTS.entrySet()) {
			if(entry.getKey().test(field.getType())) return entry.getValue().apply(field);
		}
		return null;
	}
	
	Map<String, FieldCodec<?, ?>> fieldCodecs = new HashMap<>();
	public void add(Class<?> fType, FieldCodec<?, ?> fc) {
		add(fType.getName(), fc);
	}
	public void add(String name, FieldCodec<?, ?> fc) {
		fieldCodecs.put(name, fc);
	}
	
	public FieldCodec<?, ?> get(Field field) {
		FieldCodec<?, ?> m1 = fieldCodecs.get(field.getName());
		if(m1 != null) return m1;
		FieldCodec<?, ?> m2 = fieldCodecs.get(field.getType().getName());
		if(m2 != null) return m2;
		return getDefault(field);
	}
	
}