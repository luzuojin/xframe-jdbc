package dev.xframe.jdbc.partition;

import java.util.function.BiFunction;

public enum PartitionStrategy {
    
    Builtin,    //db内置
    
    Simulate;   //分表模拟

    public static BiFunction<String, String, String> BuiltinTableNameFunc = (t, p) -> String.format("%s (`%s`)", t, p);
    
}
