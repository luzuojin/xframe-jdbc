package dev.xframe.jdbc.codec;

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

	public static String getMajor(int delVal) {
		return Character.toString(unpackMajor(delVal, DEFAULTS));
	}

	public static String getMinor(int delVal) {
		return Character.toString(unpackMinor(delVal, DEFAULTS));
	}

	public static int pack(char major, char minor) {
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
