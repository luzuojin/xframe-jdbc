package dev.xframe.jdbc.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public interface FieldCodec<F, C> {
	
    C encode(F fieldValue);
    F decode(C columnValue);
    
    default Class<?> getColumnType() {
        Class<?> clazz = this.getClass();
        do {
            Type[] gInterfazes = clazz.getGenericInterfaces();
            for (Type type : gInterfazes) {
                if(type instanceof ParameterizedType) {
                    ParameterizedType ptype = (ParameterizedType) type;
                    if(ptype.getRawType().equals(FieldCodec.class)) {
                        return (Class<?>) ptype.getActualTypeArguments()[1];
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while((!Object.class.equals(clazz)));
        return null;//can`t be here
    }
}
