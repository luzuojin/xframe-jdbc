package dev.xframe.jdbc.codec.provides;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.xframe.jdbc.codec.Codec;

class CodecUtils {
    
    private static final Map<Class<?>, ElementCodec<?>> ELEMENT_CODECES = new HashMap<Class<?>, ElementCodec<?>>();
    
    private static final Map<Class<?>, PrimitiveCodec<?>> PRIMITIVE_CODECES = new HashMap<Class<?>, PrimitiveCodec<?>>();
    
    private static final Map<Class<?>, CollectionFactory<?>> COLLECTION_FACTORIES = new HashMap<Class<?>, CollectionFactory<?>>();
    
    public static int getCodecAnnVal(Field field) {
		return field.isAnnotationPresent(Codec.class) ? field.getAnnotation(Codec.class).value() : 0;
	}
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> ElementCodec<T> getElementCodec(Class<T> clazz) {
        if (Enum.class.isAssignableFrom(clazz)) {
            return (ElementCodec<T>) new EnumElementCodec(EnumCodec.build((Class<? extends Enum<?>>) clazz));
        }
        return (ElementCodec<T>) ELEMENT_CODECES.get(clazz);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> PrimitiveCodec<T> getPrimitiveCodec(Class<T> clazz) {
        return (PrimitiveCodec<T>) PRIMITIVE_CODECES.get(clazz);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> CollectionFactory<T> getCollectionFactory(Class<T> clazz) {
        CollectionFactory<T> factory = (CollectionFactory<T>) COLLECTION_FACTORIES.get(clazz);
        if(factory == null && clazz.isArray()) {//Array factory 自动创建
            final Class<?> component = clazz.getComponentType();
            factory = new CollectionFactory<T>() {
                @Override
                public T newCollection(int size) {
                    return (T) Array.newInstance(component, size);
                }
            };
            COLLECTION_FACTORIES.put(clazz, factory);
        }
        return factory;
    }
    
    static {
        PRIMITIVE_CODECES.put(int[].class, new PrimitiveCodec<int[]>() {
            public void decode(int[] f, int index, String val) {
                f[index] = Integer.parseInt(val);
            }
            public String encode(int[] f, int index) {
                return String.valueOf(f[index]);
            }
        });
        PRIMITIVE_CODECES.put(long[].class, new PrimitiveCodec<long[]>() {
            public void decode(long[] f, int index, String val) {
                f[index] = Long.parseLong(val);
            }
            public String encode(long[] f, int index) {
                return String.valueOf(f[index]);
            }
        });
        PRIMITIVE_CODECES.put(float[].class, new PrimitiveCodec<float[]>() {
            public void decode(float[] f, int index, String val) {
                f[index] = Float.parseFloat(val);
            }
            public String encode(float[] f, int index) {
                return String.valueOf(f[index]);
            }
        });
        PRIMITIVE_CODECES.put(double[].class, new PrimitiveCodec<double[]>() {
            public void decode(double[] f, int index, String val) {
                f[index] = Double.parseDouble(val);
            }
            public String encode(double[] f, int index) {
                return String.valueOf(f[index]);
            }
        });
        
        
        ELEMENT_CODECES.put(Integer.class, new ElementCodec<Integer>() {
            public Integer decode(String va) {
                return Integer.parseInt(va);
            }
            public String encode(Integer val) {
                return String.valueOf(val);
            }
        });
        ELEMENT_CODECES.put(Long.class, new ElementCodec<Long>() {
            public Long decode(String va) {
                return Long.parseLong(va);
            }
            public String encode(Long val) {
                return String.valueOf(val);
            }
        });
        ELEMENT_CODECES.put(Float.class, new ElementCodec<Float>() {
            public Float decode(String va) {
                return Float.parseFloat(va);
            }
            public String encode(Float val) {
                return String.valueOf(val);
            }
        });
        ELEMENT_CODECES.put(Double.class, new ElementCodec<Double>() {
            public Double decode(String va) {
                return Double.parseDouble(va);
            }
            public String encode(Double val) {
                return String.valueOf(val);
            }
        });
        
        
        ELEMENT_CODECES.put(String.class, new ElementCodec<String>() {
            public String decode(String va) {
                return va;
            }
            public String encode(String val) {
                return val;
            }
        });
        
        
        COLLECTION_FACTORIES.put(List.class, new CollectionFactory<List<?>>() {
            @Override @SuppressWarnings("rawtypes")
            public List<?> newCollection(int size) {
                return new ArrayList();
            }
        });
        COLLECTION_FACTORIES.put(Set.class, new CollectionFactory<Set<?>>() {
            @Override @SuppressWarnings("rawtypes")
            public Set<?> newCollection(int size) {
                return new HashSet();
            }
        });
    }
}

@SuppressWarnings("unchecked")
class EnumElementCodec<X extends Enum<?>> implements ElementCodec<X> {
    private EnumCodec codec;
    public EnumElementCodec(EnumCodec codec) {
        this.codec = codec;
    }
    @Override
    public X decode(String val) {
        return (X) codec.decode(Integer.parseInt(val));
    }
    @Override
    public String encode(X val) {
        return String.valueOf(codec.encode(val));
    }
}

interface PrimitiveCodec<F> {
    public void decode(F f, int index, String val);
    public String encode(F f, int index);
}

interface ElementCodec<V> {
    public V decode(String val);
    public String encode(V val);
}

interface CollectionFactory<T> {
    public T newCollection(int size);
}
