package dev.xframe.jdbc.codec.transfer;

import java.lang.reflect.Array;
import java.lang.reflect.Field;

import dev.xframe.utils.XReflection;

public interface FieldWrap {
    
    Class<?> getType();
    
    void set(Object obj, Object val) throws Exception;
    void setBoolean(Object obj, boolean z) throws Exception;
    void setByte(Object obj, byte b) throws Exception;
    void setChar(Object obj, char c) throws Exception;
    void setShort(Object obj, short s) throws Exception;
    void setInt(Object obj, int i) throws Exception;
    void setLong(Object obj, long l) throws Exception;
    void setFloat(Object obj, float f) throws Exception;
    void setDouble(Object obj, double d) throws Exception;
    
    Object get(Object obj) throws Exception;
    boolean getBoolean(Object obj) throws Exception;
    byte getByte(Object obj) throws Exception;
    char getChar(Object obj) throws Exception;
    short getShort(Object obj) throws Exception;
    int getInt(Object obj) throws Exception;
    long getLong(Object obj) throws Exception;
    float getFloat(Object obj) throws Exception;
    double getDouble(Object obj) throws Exception;
    
    static class PojoBased implements FieldWrap {
        private final Field field;
        public PojoBased(Field field) {
            XReflection.setAccessible(field);
            this.field = field;
        }
        public Class<?> getType() {
            return field.getType();
        }
        public void set(Object obj, Object val) throws Exception {
            field.set(obj, val);
        }
        public void setBoolean(Object obj, boolean z) throws Exception {
            field.setBoolean(obj, z);
        }
        public void setByte(Object obj, byte b) throws Exception {
            field.setByte(obj, b);
        }
        public void setChar(Object obj, char c) throws Exception {
            field.setChar(obj, c);
        }
        public void setShort(Object obj, short s) throws Exception {
            field.setShort(obj, s);
        }
        public void setInt(Object obj, int i) throws Exception {
            field.setInt(obj, i);
        }
        public void setLong(Object obj, long l) throws Exception {
            field.setLong(obj, l);
        }
        public void setFloat(Object obj, float f) throws Exception {
            field.setFloat(obj, f);
        }
        public void setDouble(Object obj, double d) throws Exception {
            field.setDouble(obj, d);
        }
        public Object get(Object obj) throws Exception {
            return field.get(obj);
        }
        public boolean getBoolean(Object obj) throws Exception {
            return field.getBoolean(obj);
        }
        public byte getByte(Object obj) throws Exception {
            return field.getByte(obj);
        }
        public char getChar(Object obj) throws Exception {
            return field.getChar(obj);
        }
        public short getShort(Object obj) throws Exception {
            return field.getShort(obj);
        }
        public int getInt(Object obj) throws Exception {
            return field.getInt(obj);
        }
        public long getLong(Object obj) throws Exception {
            return field.getLong(obj);
        }
        public float getFloat(Object obj) throws Exception {
            return field.getFloat(obj);
        }
        public double getDouble(Object obj) throws Exception {
            return field.getDouble(obj);
        }
    }
    
    static class ArrayBased implements FieldWrap {
        private final Class<?> type;
        private final int index;
        public ArrayBased(Class<?> type, int index) {
            this.type = type;
            this.index = index;
        }
        public Class<?> getType() {
            return type;
        }
        public Object get(Object obj) throws Exception {
            return Array.get(obj, index);
        }
        public byte getByte(Object obj) throws Exception {
            return (Byte) get(obj);
        }
        public char getChar(Object obj) throws Exception {
            return (Character) get(obj);
        }
        public short getShort(Object obj) throws Exception {
            return (Short) get(obj);
        }
        public int getInt(Object obj) throws Exception {
            return (Integer) get(obj);
        }
        public float getFloat(Object obj) throws Exception {
            return (Float) get(obj);
        }
        public long getLong(Object obj) throws Exception {
            return (Long) get(obj);
        }
        public double getDouble(Object obj) throws Exception {
            return (Double) get(obj);
        }
        public boolean getBoolean(Object obj) throws Exception {
            return (Boolean) get(obj);
        }
        public void set(Object obj, Object val) throws Exception {
            Array.set(obj, index, val);
        }
        public void setByte(Object obj, byte b) throws Exception {
            set(obj, b);
        }
        public void setChar(Object obj, char c) throws Exception {
            set(obj, c);
        }
        public void setShort(Object obj, short s) throws Exception {
            set(obj, s);
        }
        public void setInt(Object obj, int i) throws Exception {
            set(obj, i);
        }
        public void setFloat(Object obj, float f) throws Exception {
            set(obj, f);
        }
        public void setLong(Object obj, long l) throws Exception {
            set(obj, l);
        }
        public void setDouble(Object obj, double d) throws Exception {
            set(obj, d);
        }
        public void setBoolean(Object obj, boolean b) throws Exception {
            set(obj, b);
        }
    }
    
}
