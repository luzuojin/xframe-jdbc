package dev.xframe.jdbc.partition;

import java.util.function.BiFunction;
import java.util.function.Function;

import dev.xframe.jdbc.TypeQuery;

public enum PartitionStrategy {
    
    //db内置
    Builtin {
        final BiFunction<String, String, String> tNameFunc = (t, p) -> String.format("%s PARTITION(%s)", t, p);
        @Override
        public <T> TypeQuery<T> makePartitionningQuery(Function<T, String> pNameFunc, BiFunction<String, String, String> unused) {
            return new PartitioningQuery<>(pNameFunc, tNameFunc, true);
        }
    },
    //分表模拟
    Simulate {
        @Override
        public <T> TypeQuery<T> makePartitionningQuery(Function<T, String> pNameFunc, BiFunction<String, String, String> tNameFunc) {
            return new PartitioningQuery<>(pNameFunc, tNameFunc, false);
        }
    };
    
    public abstract <T> TypeQuery<T> makePartitionningQuery(Function<T, String> pNameFunc, BiFunction<String, String, String> tNameFunc);
    
}
