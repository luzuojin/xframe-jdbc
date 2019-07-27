package dev.xframe.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

/**
 * @author luzj
 */
public interface TypeHandler<T> {
    
	/**create instance*/
    T make(ResultSet rs) throws Exception ;
    
    /**after instance initial*/
    void apply(T t);
    
    public static <T> TypeHandler<T> of(TypeMaker<T> maker) {
        return new SimpleHandler<>(maker, t->{});
    }
    public static <T> TypeHandler<T> of(Consumer<T> consumer) {
        return new SimpleHandler<>(rs->null, consumer);
    }
    public static <T> TypeHandler<T> of(TypeMaker<T> maker, Consumer<T> consumer) {
        return new SimpleHandler<>(maker, consumer);
    }
    
    @FunctionalInterface
    interface TypeMaker<T> {
        T make(ResultSet rs) throws SQLException;
    }
    
    class SimpleHandler<T> implements TypeHandler<T> {
        final TypeMaker<T> maker;
        final Consumer<T> consumer;
        public SimpleHandler(TypeMaker<T> maker, Consumer<T> consumer) {
            this.maker = maker;
            this.consumer = consumer;
        }
        @Override
        public T make(ResultSet rs) throws SQLException {
            return maker.make(rs);
        }
        @Override
        public void apply(T t) {
            consumer.accept(t);
        }
    }

}
