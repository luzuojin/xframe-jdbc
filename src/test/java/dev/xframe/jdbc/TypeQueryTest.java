package dev.xframe.jdbc;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import dev.xframe.jdbc.codec.Codec;
import dev.xframe.jdbc.codec.Delimiters;
import dev.xframe.jdbc.codec.FieldCodec;
import dev.xframe.jdbc.datasource.DBIdent;
import dev.xframe.jdbc.datasource.DBSource;
import dev.xframe.jdbc.datasource.DataSources;

public class TypeQueryTest implements DBIdent {
    
    public static enum XEnum {
        @Codec(1) val;
    }
    
    public static class XRecord {
        int xId;
        String xName;
        List<Integer> xList;
        XEnum xEnum;
        int[] xArray ;
        boolean xBool;
        Long xLong;
        short xCust;
        public int[] getxArray() {
            return xArray;
        }
        public short getxCust() {
            return xCust;
        }
        @Override
        public String toString() {
            return "XRecord [xId=" + xId + ", xName=" + xName + ", xList=" + xList + ", xEnum=" + xEnum + ", xArray=" + Arrays.toString(xArray) + ", xBool=" + xBool + ", xLong=" + xLong + ", xCust=" + xCust + "]";
        }
    }
    
    TypeQuery<XRecord> query;
    
    @Before
    public void setup() {
        String driver = "org.h2.Driver";
        String dburl = String.format("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=false;");
        String user = "embed";
        String pass = "embed";
        JdbcEnviron.getConfigurator()
                .setInstupUsage(false, false)
                .setDatasource(this, DataSources.tomcatJdbc(new DBSource(user, pass, driver, dburl, 1, 1)));
        
        String script = ""
+                "CREATE TABLE `X_RECORD` ("
+                        "`XId` int(11) NOT NULL PRIMARY KEY,"
+                        "`XName` varchar(64) NOT NULL,"
+                        "`XList` varchar(1024) NOT NULL,"
+                        "`XEnum` int(11) NOT NULL,"
+                        "`XArray` varchar(1024) NOT NULL,"
+                        "`XBool` tinyint(1) NOT NULL,"
+                        "`XLong` bigint(20) NOT NULL,"
+                        "`XCust` varchar(20) NOT NULL"
+                      ") DEFAULT CHARSET=utf8;"
;
        
        JdbcEnviron.getJdbcTemplate(this).execute(script);
        
        query = TypeQuery.newBuilder(XRecord.class)
                    .setTable(this, "X_RECORD")
                    .setFieldCodec(XRecord::getxArray, Delimiters.pack(';', '@'))
                    .setFieldCodec(XRecord::getxCust, FieldCodec.of((Short f)->String.valueOf(f), (String c)->Short.valueOf(c)))
                    .build();
    }
    
    @Test
    public void test() {
        XRecord xr = new XRecord();
        xr.xId = 10086;
        xr.xName = "XName";
        xr.xList = Arrays.asList(1);
        xr.xEnum = XEnum.val;
        xr.xBool = true;
        xr.xArray = new int[] {2};
        xr.xLong = 10010L;
        xr.xCust = 64;
        
        query.insert(xr);
        
        XRecord qr = query.fetchOne(xr.xId);
        
        assert0(xr, qr);
        
        xr.xName = "XAnother";
        query.update(xr);
        
        qr = query.fetchOne(xr.xId);
        assert0(xr, qr);
    }

    private void assert0(XRecord xr, XRecord qr) {
        Assert.assertEquals(xr.xId, qr.xId);
        Assert.assertEquals(xr.xName, qr.xName);
        Assert.assertEquals(xr.xList, qr.xList);
        Assert.assertEquals(xr.xEnum, qr.xEnum);
        Assert.assertEquals(xr.xBool, qr.xBool);
        Assert.assertArrayEquals(xr.xArray, qr.xArray);
        Assert.assertEquals(xr.xLong, qr.xLong);
        Assert.assertEquals(xr.xCust, qr.xCust);
    }

}
