package dev.xframe.jdbc.codec;

import java.util.function.Function;

import dev.xframe.utils.XGeneric;

/**
 * @author luzj
 * @param <F>
 * @param <C> 如果是基础类型的转换,需求处理好默认值(ResultSet.getObject中数据库中为null时默认值是null而不是0)
 */
public interface FieldCodec<F, C> {
	
    C encode(F fieldValue);
    F decode(C columnValue);
    
    default Class<?> getColumnActualType() {
        return XGeneric.parse(getClass(), FieldCodec.class).getByIndex(1);
    }

    public static <F, C> FieldCodec<F, C> ofEncoder(Function<F, C> encoder) {
        return new CompositFieldCodec<>(encoder, null, XGeneric.parse(encoder.getClass(), Function.class).getByIndex(1));
    }
    public static <F, C> FieldCodec<F, C> ofDecoder(Function<C, F> decoder) {
        return new CompositFieldCodec<>(null, decoder, XGeneric.parse(decoder.getClass(), Function.class).getByIndex(0));
    }
    public static <F, C> FieldCodec<F, C> of(Function<F, C> encoder, Function<C, F> decoder) {
        return new CompositFieldCodec<>(encoder, decoder, XGeneric.parse(encoder.getClass(), Function.class).getByIndex(1));
    }
    
    static class CompositFieldCodec<F, C> implements FieldCodec<F, C> {
        final Function<F, C> encoder;
        final Function<C, F> decoder;
        final Class<?> columnType;
        public CompositFieldCodec(Function<F, C> encoder, Function<C, F> decoder, Class<?> columnType) {
            this.encoder = encoder;
            this.decoder = decoder;
            this.columnType = columnType;
        }
        public C encode(F fieldValue) {
            return encoder.apply(fieldValue);
        }
        public F decode(C columnValue) {
            return decoder.apply(columnValue);
        }
        public Class<?> getColumnActualType() {
            return columnType;
        }
    }
    
}
