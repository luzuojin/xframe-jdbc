package dev.xframe.jdbc.builder.javassist;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Time;
import java.sql.Timestamp;

import dev.xframe.jdbc.builder.analyse.FCodecSet;
import dev.xframe.jdbc.builder.analyse.FColumn;
import dev.xframe.jdbc.builder.analyse.JColumn;


/**
 * getter setter 解析
 * @author luzj
 */
class FieldCodes {
	
    static String specsName(String name) {
        char first = name.charAt(0);
        if (Character.isUpperCase(first)) {
            return name;
        }
        if (name.length() > 1) {
            char second = name.charAt(1);
            if (Character.isUpperCase(second)) {
                return name;
            }
        }
        return Character.toString(Character.toUpperCase(first)) + name.substring(1);
    }
    
    static String castingName(Class<?> c) {
        if(c.isArray()) {
            String end = "[]";
            Class<?> t = c.getComponentType();
            while(t.isArray()) {
                end += "[]";
                t = t.getComponentType();
            }
            return t.getName() + end;
        }
        return c.getName();
    }
    
	//setter
	static <T> String makeSetterBodyElement(FCodecSet codecs, FColumn fColumn, String type, String rs, int index) {
		return new StringBuilder()
			.append(rs).append(".").append(psSetter(codecs, fColumn.jColumn)).append("(")
			.append(index).append(", ").append(columnSetVal(codecs, type, fColumn.jColumn))
			.append(");").toString();
	}
	
	static <T> String columnSetVal(FCodecSet codecs, String type, JColumn jColumn) {
	    String val = fieldGetVal(type, jColumn);
	    if(codecs.hasCodec(jColumn.name)) {//pstmt.setInt(1, (int)this.xxxCodec.encode((Integer)this.getXxx()))
	        return objToPrimitiveCasting(codecs.getColumnActualType(jColumn.name), String.format("this.%s.encode(%s)", DynamicCodes.codecFieldName(jColumn.name), primitiveToObjCasting(jColumn.type, val)));
	    }
	    return objToPrimitiveCasting(jColumn.type, val);//pstmt.set(1, (int)this.getXxx())
	}

	private static String fieldGetVal(String type, JColumn jColumn) {
	    if(jColumn.getter != null) {   //优先有Getter
	        return type + "." + jColumn.getter + "()";
	    }
	    if(!jColumn.isPrivate) {       //没有Getter且非Private时 直接访问属性
	        return type + "." + jColumn.name;
	    }
        //给一个默认getter来访问
        return type + ".get" + specsName(jColumn.name) + "()";
	}
	
	static <T> String psSetter(FCodecSet codecs, JColumn jColumn) {
		Class<?> c = codecs.hasCodec(jColumn.name) ? codecs.getColumnActualType(jColumn.name) : jColumn.type;
        if(Boolean.class.equals(c) || boolean.class.equals(c)) {
            return "setBoolean";
        } else if(Byte.class.equals(c) || byte.class.equals(c)) {
            return "setByte";
        } else if(Character.class.equals(c) || char.class.equals(c)) {
            return "setShort";
        } else if(Short.class.equals(c) || short.class.equals(c)) {
            return "setShort";
        } else if(c.isEnum() || Integer.class.equals(c) || int.class.equals(c)) {
            return "setInt";
        } else if(Long.class.equals(c) || long.class.equals(c)) {
            return "setLong";
        } else if(Float.class.equals(c) || float.class.equals(c)) {
            return "setFloat";
        } else if(Double.class.equals(c) || double.class.equals(c)) {
            return "setDouble";
        } else if(Byte[].class.equals(c) || byte[].class.equals(c)) {
            return "setBytes";
        } else if(BigDecimal.class.equals(c)) {
            return "setBigDecimal";
        } else if(Reader.class.equals(c)) {
            return "setCharacterStream";
        } else if(InputStream.class.equals(c)) {
            return "setBinaryStream";
        } else if(java.sql.Date.class.equals(c)) {
            return "setDate";
        } else if(Time.class.equals(c)) {
            return "setTime";
        } else if(java.util.Date.class.equals(c) || Timestamp.class.equals(c)) {
            return "setTimestamp";
        } else if(Clob.class.equals(c)) {
            return "setClob";
        } else if(Blob.class.equals(c)) {
            return "setBlob";
        } else if(Character[].class.equals(c) || String.class.equals(c) || char[].class.equals(c)) {
            return "setString";
        }
        return "setObject";
	}
	

	//parser
	static <T> String makeParserBodyElement(FCodecSet codecs, FColumn fColumn, String type, String rs, int index) {
		JColumn jColumn = fColumn.jColumn;
		String val = fieldSetVal(codecs, rs, jColumn, index);
		if(jColumn.setter != null) {
		    return setBySetter0(type, jColumn, val);
		}
		if(!jColumn.isPrivate) {
		    return setByAssign(type, jColumn, val);
		}
		return setBySetter1(type, jColumn, val);
	}
	
	static String setBySetter0(String type, JColumn jColumn, String val) {
	    return new StringBuilder().append(type).append(".").append(jColumn.setter).append("(").append(val).append(");").toString();
	}
	static String setBySetter1(String type, JColumn jColumn, String val) {
		return new StringBuilder().append(type).append(".set").append(specsName(jColumn.name)).append("(").append(val).append(");").toString();
	}

	static String setByAssign(String type, JColumn jColumn, String get) {
		return new StringBuilder().append(type).append(".").append(jColumn.name).append("=").append(get).append(";").toString();
	}
	
	static <T> String fieldSetVal(FCodecSet codecs, String rs, JColumn jColumn, int index) {
		String rsget = new StringBuilder().append(rs).append(".").append(rsGetter(codecs, jColumn)).append("(").append(index).append(")").toString();//rs.getInt(1)
		if(codecs.hasCodec(jColumn.name)) {//(int) this.xxxCodec.decode((Integer)rs.getInt(1));
			return objToPrimitive(jColumn.type, String.format("this.%s.decode(%s)", DynamicCodes.codecFieldName(jColumn.name), primitiveToObj(codecs.getColumnActualType(jColumn.name), rsget)));
		}
		return primitiveToObj(jColumn.type, rsget);//(Integer) rs.getInt(1);
	}

	static <T> String rsGetter(FCodecSet codecs, JColumn jColumn) {
	    Class<?> c = codecs.hasCodec(jColumn.name) ? codecs.getColumnActualType(jColumn.name) : jColumn.type;
        if(Boolean.class.equals(c) || boolean.class.equals(c)) {
            return "getBoolean";
        } else if(Byte.class.equals(c) || byte.class.equals(c)) {
            return "getByte";
        } else if(Character.class.equals(c) || char.class.equals(c)) {
            return "getShort";
        } else if(Short.class.equals(c) || short.class.equals(c)) {
            return "getShort";
        } else if(Integer.class.equals(c) || int.class.equals(c)) {
            return "getInt";
        } else if(Long.class.equals(c) || long.class.equals(c)) {
            return "getLong";
        } else if(Float.class.equals(c) || float.class.equals(c)) {
            return "getFloat";
        } else if(Double.class.equals(c) || double.class.equals(c)) {
            return "getDouble";
        } else if(Byte[].class.equals(c) || byte[].class.equals(c)) {
            return "getBytes";
        } else if(BigDecimal.class.equals(c)) {
            return "getBigDecimal";
        } else if(Reader.class.equals(c)) {
            return "getCharacterStream";
        } else if(InputStream.class.equals(c)) {
            return "getBinaryStream";
        } else if(java.sql.Date.class.equals(c)) {
            return "getDate";
        } else if(Time.class.equals(c)) {
            return "getTime";
        } else if(java.util.Date.class.equals(c) || Timestamp.class.equals(c)) {
            return "getTimestamp";
        } else if(Clob.class.equals(c)) {
            return "getClob";
        } else if(Blob.class.equals(c)) {
            return "getBlob";
        } else if(Character[].class.equals(c) || String.class.equals(c) || char[].class.equals(c)) {
            return "getString";
        }
        return "getObject";
	}

	static String primitiveToObjCasting(Class<?> c, String expr) {
	    if(boolean.class.equals(c)) {
	        return "Boolean.valueOf(" + expr + ")";
	    } else if(byte.class.equals(c)) {
	        return "Byte.valueOf(" + expr + ")";
	    } else if(short.class.equals(c)) {
	        return "Short.valueOf(" + expr + ")";
	    } else if(int.class.equals(c)) {
	        return "Integer.valueOf(" + expr + ")";
	    } else if(long.class.equals(c)) {
	        return "Long.valueOf(" + expr + ")";
	    } else if(float.class.equals(c)) {
	        return "Float.valueOf(" + expr + ")";
	    } else if(double.class.equals(c)) {
	        return "Double.valueOf(" + expr + ")";
	    }
	    return expr;
	}
	
	static String primitiveToObj(Class<?> c, String expr) {
	    if(Boolean.class.equals(c)) {
	        return "Boolean.valueOf(" + expr + ")";
	    } else if(Byte.class.equals(c)) {
	        return "Byte.valueOf(" + expr + ")";
	    } else if(Short.class.equals(c)) {
	        return "Short.valueOf(" + expr + ")";
	    } else if(Integer.class.equals(c)) {
	        return "Integer.valueOf(" + expr + ")";
	    } else if(Long.class.equals(c)) {
	        return "Long.valueOf(" + expr + ")";
	    } else if(Float.class.equals(c)) {
	        return "Float.valueOf(" + expr + ")";
	    } else if(Double.class.equals(c)) {
	        return "Double.valueOf(" + expr + ")";
	    }
	    return expr;
	}

	static String objToPrimitiveCasting(Class<?> c, String expr) {
	    expr = "((" + castingName(c) + ")" + expr + ")";
	    if(Boolean.class.equals(c)) {
	        return expr + ".booleanValue()";
	    } else if(Byte.class.equals(c)) {
	        return expr + ".byteValue()";
	    } else if(Short.class.equals(c)) {
	        return expr + ".shortValue()";
	    } else if(Integer.class.equals(c)) {
	        return expr + ".intValue()";
	    } else if(Long.class.equals(c)) {
	        return expr + ".longValue()";
	    } else if(Float.class.equals(c)) {
	        return expr + ".floatValue()";
	    } else if(Double.class.equals(c)) {
	        return expr + ".doubleValue()";
	    }
	    return expr;
	}
	
	static String objToPrimitive(Class<?> c, String expr) {
	    if(boolean.class.equals(c)) {
	        return "((Boolean)" + expr + ").booleanValue()";
	    } else if(byte.class.equals(c)) {
	        return "((Byte)" + expr + ").byteValue()";
	    } else if(short.class.equals(c)) {
	        return "((Short)" + expr + ").shortValue()";
	    } else if(int.class.equals(c)) {
	        return "((Integer)" + expr + ").intValue()";
	    } else if(long.class.equals(c)) {
	        return "((Long)" + expr + ").longValue()";
	    } else if(float.class.equals(c)) {
	        return "((Float)" + expr + ").floatValue()";
	    } else if(double.class.equals(c)) {
	        return "((Double)" + expr + ").doubleValue()";
	    }
	    return "(" + castingName(c) + ")" + expr;
	}
	
}

