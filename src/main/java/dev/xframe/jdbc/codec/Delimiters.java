package dev.xframe.jdbc.codec;

import java.lang.reflect.Field;

/**
 * 
 * (major & 0xFF) | (minor << 8)
 * @author luzj
 * 
 */
public class Delimiters {
	
	static int DEFAULTS = pack(',', '|');
	
	public static void setDefaults(char major, char minor) {
		DEFAULTS = pack(major, minor);
	}

	public static String getMajor(Field field) {
		return Character.toString(unpackMajor(getCodecAnnVal(field), DEFAULTS));
	}

	public static String getMinor(Field field) {
		return Character.toString(unpackMinor(getCodecAnnVal(field), DEFAULTS));
	}

	private static int getCodecAnnVal(Field field) {
		return field.isAnnotationPresent(Codec.class) ? field.getAnnotation(Codec.class).value() : 0;
	}
	
	private static int pack(char major, char minor) {
        return (major & 0xFF) | (minor << 8);
    }
    
	private static char unpackMajor(int val) {
        return (char) (val & 0xFF);
    }
    
    private static char unpackMajor(int del, int defDel) {
        char r = unpackMajor(del);
        return r == 0 ? unpackMajor(defDel) : r;
    }
    
    private static char unpackMinor(int val) {
        return (char) ((val >> 8) & 0xFF);
    }
    
    private static char unpackMinor(int del, int defDel) {
        char r = unpackMinor(del);
        return r == 0 ? unpackMinor(defDel) : r;
    }

}
