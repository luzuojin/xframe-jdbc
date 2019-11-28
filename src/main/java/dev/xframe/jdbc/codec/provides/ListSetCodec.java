package dev.xframe.jdbc.codec.provides;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import dev.xframe.jdbc.codec.Delimiters;
import dev.xframe.jdbc.codec.FieldCodec;

public class ListSetCodec<T extends Collection<V>, V> implements FieldCodec<T, String> {
    
    private CollectionFactory<T> factory;
    private ElementCodec<V> codec;
    private String decodeDelimiter;
    private String encodeDelimiter;
    
    public ListSetCodec(CollectionFactory<T> factory, ElementCodec<V> codec, String delimiter) {
        this.factory = factory;
        this.codec = codec;
        this.decodeDelimiter = Pattern.quote(delimiter);
        this.encodeDelimiter = delimiter;
    }
    
    @Override
    public String encode(T fieldValue) {
        if(fieldValue == null || fieldValue.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        int idx = 0;
        for (V v : fieldValue) {
            if(idx++ > 0) sb.append(encodeDelimiter);
            sb.append(codec.encode(v));
        }
        return sb.toString();
    }

    @Override
    public T decode(String columnValue) {
        T ret = factory.newCollection(0);
        if(isEmpty(columnValue)) return ret;
        String[] elements = columnValue.split(decodeDelimiter);
        for (String element : elements) {
            ret.add(codec.decode(element));
        }
        return ret;
    }
    
    private static boolean isEmpty(String src) {
        return src == null || src.length() == 0;
    }

    public static boolean isListOrSet(Field field) {
        return List.class.isAssignableFrom(field.getType()) || Set.class.isAssignableFrom(field.getType());
    }
    
    public static <T> FieldCodec<T, String> build(Field field) {
        return build(field.getType(), field.getGenericType(), CodecUtils.getCodecAnnVal(field));
    }
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> FieldCodec<T, String> build(Class<?> type, Type genericType, int delimiterVal) {
        Class<?> factory = List.class.isAssignableFrom(type) ? List.class : Set.class;
        ParameterizedType parameterizedType = (ParameterizedType) genericType;
        Class<?> genericActualType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return new ListSetCodec(CodecUtils.getCollectionFactory(factory), CodecUtils.getElementCodec(genericActualType), Delimiters.getMajor(delimiterVal));
    }

}
