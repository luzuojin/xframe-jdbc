package dev.xframe.jdbc;

/**
 * @author luzj
 */
@FunctionalInterface
public interface TypeHandler<T> {
    /**after instanced and field setted*/
    void apply(T t);

}
