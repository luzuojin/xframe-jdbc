package dev.xframe.jdbc.codec;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> FieldCodec<T, String> fetchCodec(Field field) {
        Class<?> factory = List.class.isAssignableFrom(field.getType()) ? List.class : Set.class;
        ParameterizedType parameterizedType = (ParameterizedType) field.getGenericType();
        Class<?> genericType = (Class<?>) parameterizedType.getActualTypeArguments()[0];
        return new ListSetCodec(BasicCodecs.getCollectionFactory(factory), BasicCodecs.getElementCodec(genericType), Delimiters.getMajor(field));
    }

}
