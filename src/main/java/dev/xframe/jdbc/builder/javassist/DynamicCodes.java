package dev.xframe.jdbc.builder.javassist;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import dev.xframe.jdbc.RSParser;
import dev.xframe.jdbc.TypeHandler;
import dev.xframe.jdbc.TypePSSetter;
import dev.xframe.jdbc.builder.analyse.FCodecs;
import dev.xframe.jdbc.builder.analyse.FColumn;
import dev.xframe.jdbc.builder.analyse.FTable;
import dev.xframe.jdbc.codec.FieldCodec;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;


/**
 * generate dynamic code 
 * @author luzj
 */
@SuppressWarnings("unchecked")
public class DynamicCodes {
	
	private static AtomicInteger suffix = new AtomicInteger(0);
    private static String suffix(boolean full) {
        return full ? "F" : String.valueOf(suffix.incrementAndGet());
    }
    
    public static <T> RSParser<T> makeParser(FTable ftable, List<FColumn> columns) throws Exception {
    	if(columns.isEmpty()) return null;
    	String clazzName = ftable.clazz.getName();
    	String newName = clazzName + "$TypeRSParser_" + (suffix(ftable.columns == columns));
    	Class<?> newClass = defineClass(newName);
    	
    	if(newClass == null) {
    		ClassPool pool = ClassPool.getDefault();
    		CtClass clazz = pool.makeClass(newName);
    		clazz.addInterface(pool.getCtClass(RSParser.class.getName()));
    		
    		StringBuilder body = new StringBuilder("{");
    		if(ftable.typeHandler != null) {
    			body.append(clazzName).append(" _tmp_ = (").append(clazzName).append(")this.").append(typeHandlerFieldName()).append(".apply(").append("$1").append(");")
    			.append("if(_tmp_ == null) {")
    			.append("_tmp_ = new ").append(clazzName).append("();")
    			.append("}");
    		} else {
    			body.append(clazzName).append(" _tmp_ = new ").append(clazzName).append("();");
    		}
    		
    		int index = 1;
    		for (FColumn fColumn : columns) {
    			body.append(FieldCodes.makeParserBodyElement(ftable.codecs, fColumn, "_tmp_", "$1", index));
    			++index;
    		}
    		
    		if(ftable.typeHandler != null) {
    			body.append(typeHandlerFieldName()).append(".apply(").append("_tmp_").append(");");
    		}
    		body.append("return _tmp_;").append("}");
    		
    		makeCodecFieldsAndConstructor(ftable.codecs, pool, clazz);//先解析columns才有codecFields, 先添加codecFields才能生成body
    		
    		CtMethod parser = CtNewMethod.copy(pool.getMethod(RSParser.class.getName(), "parse"), clazz, null);
    		parser.setBody(body.toString());
    		clazz.addMethod(parser);
    		
    		newClass = clazz.toClass();
    	}
        
        return (RSParser<T>) newInstance(ftable.codecs, newClass);
    }

    static <T> Object newInstance(FCodecs codecs, Class<?> clazz) throws Exception {
        return clazz.getConstructor(FCodecs.class).newInstance(codecs);
    }
    
    static <T> void makeCodecFieldsAndConstructor(FCodecs codecs, ClassPool pool, CtClass clazz) throws Exception {
        Map<String, FieldCodec<?, ?>> codeces = codecs.get();
        StringBuilder body = new StringBuilder("{");
        Set<String> keys = codeces.keySet();
        for (String key : keys) {
            CtField cf = CtField.make(makeCodecFieldBody(key), clazz);
            clazz.addField(cf);
            body.append(makeCodecAssignmentBody(key));
        }
        if(codecs.hasTypeHandler()) {
            CtField cf = CtField.make(makeTypeHandlerFieldBody(), clazz);
            clazz.addField(cf);
            body.append(makeTypeParserAssignmentBody());
        }
        CtClass[] paramters = new CtClass[]{pool.get(FCodecs.class.getName())};
        CtConstructor ctConstructor = new CtConstructor(paramters, clazz);
        ctConstructor.setBody(body.append("}").toString());
        clazz.addConstructor(ctConstructor);
    }
    
    static String makeTypeHandlerFieldBody() {
        return new StringBuilder().append("private ").append(TypeHandler.class.getName()).append(" ").append(typeHandlerFieldName()).append(";").toString();
    }
    static String makeTypeParserAssignmentBody() {
        return new StringBuilder().append("this.").append(typeHandlerFieldName()).append(" = $1.getTypeHandler();").toString();
    }
    
    static String makeCodecFieldBody(String jfieldName) {
        return new StringBuilder().append("private ").append(FieldCodec.class.getName()).append(" ").append(codecFieldName(jfieldName)).append(";").toString();
    }
    static String makeCodecAssignmentBody(String jfieldName) {
        return new StringBuilder().append("this.").append(codecFieldName(jfieldName)).append(" = (").append(FieldCodec.class.getName()).append(")$1.get(\"").append(jfieldName).append("\");").toString();
    }
    
    public static <T> TypePSSetter<T> makeSetter(FTable ftable, List<FColumn> columns) throws Exception {
    	if(columns == null) return null;
    	String clazzName = ftable.clazz.getName();
    	String newName = clazzName + "$TypePSSetter_" + suffix(ftable.columns == columns);
    	Class<?> newClass = defineClass(newName);
    	
    	if(newClass == null) {
    		ClassPool pool = ClassPool.getDefault();
    		CtClass clazz = pool.makeClass(newName);
    		clazz.addInterface(pool.getCtClass(TypePSSetter.class.getName()));
    		
    		StringBuilder body = new StringBuilder("{");
    		body.append(clazzName).append(" _tmp_ = (").append(clazzName).append(") $2;");
    		int index = 1;
    		for (FColumn fColumn : columns) {
    			body.append(FieldCodes.makeSetterBodyElement(ftable.codecs, fColumn, "_tmp_", "$1", index));
    			++index;
    		}
    		body.append("}");
    		
    		makeCodecFieldsAndConstructor(ftable.codecs, pool, clazz);//先解析columns才有codecFields, 先添加codecFields才能生成body
    		
    		CtMethod setter = CtNewMethod.copy(pool.getMethod(TypePSSetter.class.getName(), "set"), clazz, null);
    		setter.setBody(body.toString());
    		clazz.addMethod(setter);
    		
    		newClass = clazz.toClass();
    	}
        return (TypePSSetter<T>) newInstance(ftable.codecs, newClass);   
    }

    private static Class<?> defineClass(String name) {
    	try {
    		return Class.forName(name);
    	} catch (ClassNotFoundException e) {}//ignore;
    	return null;
	}

	public static String typeHandlerFieldName() {
        return "_typehandler";
    }
    public static String codecFieldName(String jfieldName) {
        return jfieldName + "_codec";
    }
    
}
