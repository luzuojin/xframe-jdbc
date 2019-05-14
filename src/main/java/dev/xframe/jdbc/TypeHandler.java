package dev.xframe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * 
 * @author luzj
 *
 * @param <T>
 */
public interface TypeHandler<T> {
    
    T apply(ResultSet rs) throws SQLException ;
    
    void apply(T t);
    
    
    public static <T> TypeHandler<T> of(TypeSupplier<T> supplier) {
        return new SimpleHandler<>(supplier, t->{});
    }
    public static <T> TypeHandler<T> of(Consumer<T> consumer) {
        return new SimpleHandler<>(rs->null, consumer);
    }
    public static <T> TypeHandler<T> of(TypeSupplier<T> supplier, Consumer<T> consumer) {
        return new SimpleHandler<>(supplier, consumer);
    }
    
    @FunctionalInterface
    interface TypeSupplier<T> {
        T apply(ResultSet rs) throws SQLException;
    }
    
    class SimpleHandler<T> implements TypeHandler<T> {
        final TypeSupplier<T> supplier;
        final Consumer<T> consumer;
        public SimpleHandler(TypeSupplier<T> supplier, Consumer<T> consumer) {
            this.supplier = supplier;
            this.consumer = consumer;
        }
        @Override
        public T apply(ResultSet rs) throws SQLException {
            return supplier.apply(rs);
        }
        @Override
        public void apply(T t) {
            consumer.accept(t);
        }
    }

}
