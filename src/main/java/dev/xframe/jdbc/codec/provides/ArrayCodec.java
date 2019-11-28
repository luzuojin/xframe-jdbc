package dev.xframe.jdbc.codec.provides;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.regex.Pattern;

import dev.xframe.jdbc.codec.Delimiters;
import dev.xframe.jdbc.codec.FieldCodec;

/**
 * 使用分隔符 Array与String之间有相互编码
 * @author luzj
 */
public abstract class ArrayCodec<T> implements FieldCodec<T, String>, ElementCodec<T> {
    
    public static boolean isArray(Class<?> c) {
        return c.isArray() && !(c.equals(byte[].class) || c.equals(Byte[].class) || c.equals(char[].class) || c.equals(Character[].class));
    }
    
    public static <T> FieldCodec<T, String> build(Field field) {
    	return build(field.getType(), CodecUtils.getCodecAnnVal(field));
    }
    @SuppressWarnings("unchecked")
    public static <T> FieldCodec<T, String> build(Class<?> clazz, int delimiters) {
        return (FieldCodec<T, String>) build0(clazz, Delimiters.getMajor(delimiters), Delimiters.getMinor(delimiters));
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected static FieldCodec<?, String> build0(Class<?> clazz, final String delimiter1, final String delimiter2) {
        Class<?> type = clazz;
        Class<?> componentType = clazz.getComponentType();
        boolean isDoubleDimensionalArray = false;;
        if(componentType.isArray()) {
            type = componentType;
            componentType = componentType.getComponentType();
            isDoubleDimensionalArray = true;
        }//最多只支持两层
        
        ArrayCodec<?> codec = componentType.isPrimitive() ?
            new PrimitiveArrayCodec(CodecUtils.getCollectionFactory(type), CodecUtils.getPrimitiveCodec(type), delimiter1) :
            new ObjectArrayCodec(CodecUtils.getCollectionFactory(type), CodecUtils.getElementCodec(componentType), delimiter1);
        
        return isDoubleDimensionalArray ? new ObjectArrayCodec(CodecUtils.getCollectionFactory(clazz), codec, delimiter2) : codec;
    }
    
    static boolean isEmpty(String src) {
        return src == null || src.length() == 0;
    }
    
    public static class ObjectArrayCodec<T> extends ArrayCodec<T[]> {
        protected CollectionFactory<T[]> factory;
        
        protected ElementCodec<T> codec;
        
        private String decodeDelimiter;
        private String encodeDelimiter;
        
        public ObjectArrayCodec(CollectionFactory<T[]> factory, ElementCodec<T> codec, String delimiter) {
            this.factory = factory;
            this.codec = codec;
            this.decodeDelimiter = Pattern.quote(delimiter);
            this.encodeDelimiter = delimiter;
        }
        
        @Override
        public String encode(T[] fieldValue) {
            if(fieldValue == null || fieldValue.length == 0) return "";
            StringBuilder sb = new StringBuilder();
            int idx = 0;
            for (T t : fieldValue) {
                if(idx++ > 0) sb.append(encodeDelimiter);
                sb.append(codec.encode(t));
            }
            return sb.toString();
        }
        @Override
        public T[] decode(String columnValue) {
            if(isEmpty(columnValue)) return factory.newCollection(0);
            String[] elements = columnValue.split(decodeDelimiter);
            int length = elements.length;
            T[] ret = factory.newCollection(length);
            for (int i = 0; i < length; i++) {
                ret[i] = codec.decode(elements[i]);
            }
            return ret;
        }
    }
    
    //T is Array
    public static class PrimitiveArrayCodec<T> extends ArrayCodec<T> {
        protected CollectionFactory<T> factory;
        protected PrimitiveCodec<T> codec;
        
        private String decodeDelimiter;
        private String encodeDelimiter;
        
        public PrimitiveArrayCodec(CollectionFactory<T> factory, PrimitiveCodec<T> codec, String delimiter) {
            this.factory = factory;
            this.codec = codec;
            this.decodeDelimiter = Pattern.quote(delimiter);
            this.encodeDelimiter = delimiter;
        }
        
        @Override
        public String encode(T fieldValue) {
        	if(fieldValue == null) return "";
            int len = Array.getLength(fieldValue);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < len; i++) {
                if(i > 0) sb.append(encodeDelimiter);
                sb.append(codec.encode(fieldValue, i));
            }
            return sb.toString();
        }
        
        @Override
        public T decode(String columnValue) {
            if(isEmpty(columnValue)) return factory.newCollection(0);
            String[] elements = columnValue.split(decodeDelimiter);
            T ret = factory.newCollection(elements.length);
            for (int i = 0; i < elements.length; i++) {
                codec.decode(ret, i, elements[i]);
            }
            return ret;
        }
    }
    
}
