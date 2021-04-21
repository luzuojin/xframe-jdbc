package dev.xframe.jdbc.codec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Predicate;

import dev.xframe.jdbc.codec.provides.ArrayCodec;
import dev.xframe.jdbc.codec.provides.ListSetCodec;

public interface FieldCodecSet {

    FieldCodec<?, ?> get(Field field);
    
    /**类型配置,全局*/
    public class Typed implements FieldCodecSet {
        private Map<Predicate<Class<?>>, Function<Field, FieldCodec<?, ?>>> fcFuncs = new HashMap<>();
        public Typed add(Class<?> type, FieldCodec<?, ?> fc) {
            return add(type, f->fc);
        }
        public Typed add(Class<?> type, Function<Field, FieldCodec<?, ?>> fcFunc) {
            return add(c->c.equals(type), fcFunc);
        }
        public Typed add(Predicate<Class<?>> p, FieldCodec<?, ?> fc) {
            return add(p, f->fc);
        }
        public Typed add(Predicate<Class<?>> p, Function<Field, FieldCodec<?, ?>> fcFunc) {
            fcFuncs.put(p, fcFunc);
            return this;
        }
        public FieldCodec<?, ?> get(Field field) {
            for (Entry<Predicate<Class<?>>, Function<Field, FieldCodec<?, ?>>> entry : fcFuncs.entrySet()) {
                if(entry.getKey().test(field.getType()))
                    return entry.getValue().apply(field);
            }
            return null;
        }
    }

    /**从前至后*/
    public class Chained implements FieldCodecSet {
        final FieldCodecSet[] elements;
        public Chained(FieldCodecSet... elements) {
            this.elements = elements;
        }
        public FieldCodec<?, ?> get(Field field) {
            FieldCodec<?, ?> fc = null;
            for (FieldCodecSet element : elements) {
                if((fc = element.get(field)) != null) break;
            }
            return fc;
        }
    }
    
    /**手动配置类*/
    public class Setting implements FieldCodecSet {
        private Map<Predicate<Field>, FieldCodec<?, ?>> fcs = new HashMap<>();
        private void add0(Predicate<Field> f, FieldCodec<?, ?> fc) {
            fcs.put(f, fc);
        }
        public FieldCodec<?, ?> get(Field field) {
            for (Entry<Predicate<Field>, FieldCodec<?, ?>> entry : fcs.entrySet()) {
                if(entry.getKey().test(field))
                    return entry.getValue();
            }
            return null;
        }
        public void add(Class<?> fType, FieldCodec<?, ?> fc) {
            add0(f->f.getType().equals(fType), fc);
        }
        public void add(String name, FieldCodec<?, ?> fc) {
            add0(f->f.getName().equalsIgnoreCase(name), fc);
        }
        public void add(Function<?, ?> getter, FieldCodec<?, ?> fc) {
            addByGetterMethod(InternalReflection.getLambdaImplMethod(getter.getClass()), fc);
        }
        public void add(Function<?, ?> getter, int delimiters) {//use provided codecs
            Method method = InternalReflection.getLambdaImplMethod(getter.getClass());
            Class<?> rtype = method.getReturnType();//array or list/set
            addByGetterMethod(method, rtype.isArray() ? ArrayCodec.build(rtype, delimiters) : ListSetCodec.build(rtype, method.getGenericReturnType(), delimiters));
        }
        private void addByGetterMethod(Method method, FieldCodec<?, ?> fc) {
            final String getterMethodName = method.getName();
            add0(f->getterNameMatchField(f, getterMethodName), fc);
        }
        private static boolean getterNameMatchField(Field f, String mName) {
            return toGetter0(f).equalsIgnoreCase(mName) || toGetter1(f).equalsIgnoreCase(mName) || toGetter2(f).equalsIgnoreCase(mName);
        }
        private static String toGetter0(Field field) {
            return "get" + field.getName();
        }
        private static String toGetter1(Field field) {
            return "is" + field.getName();
        }
        private static String toGetter2(Field field) {//isXXX
            return field.getName();
        }
    }
    
}
