package dev.xframe.jdbc.codec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import dev.xframe.jdbc.codec.provides.ArrayCodec;
import dev.xframe.jdbc.codec.provides.EnumCodec;
import dev.xframe.jdbc.codec.provides.ListSetCodec;

public class FieldCodecs {
    
    static boolean isProvided(Class<?> c) {
        return c.isEnum() || ArrayCodec.isArray(c) || List.class.isAssignableFrom(c) || Set.class.isAssignableFrom(c);
    }
    
    static FieldCodec<?, ?> getProvided(Field field) {//enum or array or list/set
        Class<?> type = field.getType();
        return type.isEnum() ? EnumCodec.build(type) : (type.isArray() ? ArrayCodec.build(field) : ListSetCodec.build(field));
    }
    
    static Map<Predicate<Class<?>>, Function<Field, FieldCodec<?, ?>>> globals = new HashMap<>();
    static {
        globals.put(FieldCodecs::isProvided, FieldCodecs::getProvided);
    }
    
    public static void addGlobal(Predicate<Class<?>> p, Function<Field, FieldCodec<?, ?>> fc) {
        globals.put(p, fc);
    }
    public static void addGlobal(Class<?> c, FieldCodec<?, ?> fc) {
        addGlobal(x->c.equals(x), f->fc);
    }
    
    public static FieldCodec<?, ?> getGlobal(Field field) {
        for (Entry<Predicate<Class<?>>, Function<Field, FieldCodec<?, ?>>> entry : globals.entrySet()) {
            if(entry.getKey().test(field.getType())) return entry.getValue().apply(field);
        }
        return null;
    }
    
    
    
	private Map<String, FieldCodec<?, ?>> fcs = new HashMap<>();
	public void add(Class<?> fType, FieldCodec<?, ?> fc) {
		add(fType.getName(), fc);
	}
	public void add(String name, FieldCodec<?, ?> fc) {
		fcs.put(name.toLowerCase(), fc);
	}

    public void add(Function<?, ?> getter, FieldCodec<?, ?> fieldCodec) {
        add0(XGeneric.getLambdaBasedMethod(getter.getClass()), fieldCodec);
    }
    public void add(Function<?, ?> getter, int delimiters) {//use provided codecs
        Method method = XGeneric.getLambdaBasedMethod(getter.getClass());
        Class<?> rtype = method.getReturnType();//array or list/set
        add0(method, rtype.isArray() ? ArrayCodec.build(rtype, delimiters) : ListSetCodec.build(rtype, method.getGenericReturnType(), delimiters));
    }
    private void add0(Method method, FieldCodec<?, ?> fieldCodec) {
        add("@"+method.getName(), fieldCodec);
    }
    
    public FieldCodec<?, ?> get(Field field) {
        FieldCodec<?, ?> fc = get0(field.getName(), field.getType().getName(), toGetter0(field), toGetter1(field), toGetter2(field));
        return fc == null ? getGlobal(field) : fc;
    }
    private FieldCodec<?, ?> get0(String... keys) {
        FieldCodec<?, ?> r = null;
        for (String key : keys) if((r=fcs.get(key.toLowerCase()))!=null) break;
        return r;
    }
    private String toGetter0(Field field) {
        return "@get" + field.getName();
    }
    private String toGetter1(Field field) {
        return "@is" + field.getName();
    }
    private String toGetter2(Field field) {//isXXX
        return "@" + field.getName();
    }
	
}