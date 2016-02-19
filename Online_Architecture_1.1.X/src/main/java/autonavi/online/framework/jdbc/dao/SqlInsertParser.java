package autonavi.online.framework.jdbc.dao;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class SqlInsertParser {

	private static SqlInsertParser sqlParser;
	static Object obj = new Object();

	public static SqlInsertParser getInstance() {
		synchronized (obj) {
			if (sqlParser == null) {
				sqlParser = new SqlInsertParser();
			}
			return sqlParser;
		}
	}
	public String parserSql(String sql,Object obj,String sqlParameter){
		Map<String, Object> m=transBean2Map(obj);
		String prefix="";
		if(sqlParameter!=null&&!sqlParameter.equals("")){
			prefix=sqlParameter+".";
		}
		for(String key:m.keySet()){
			if(m.get(key)==null){
				sql=sql.replace("#{"+prefix+key+"}", "null");
			}
		}
		return sql;
	}
	private Map<String, Object> transBean2Map(Object obj) {  
		  
        if(obj == null){  
            return null;  
        }          
        Map<String, Object> map = new HashMap<String, Object>();  
        try {  
            BeanInfo beanInfo = Introspector.getBeanInfo(obj.getClass());  
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();  
            for (PropertyDescriptor property : propertyDescriptors) {  
                String key = property.getName();  
  
                // 过滤class属性  
                if (!key.equals("class")) {  
                    // 得到property对应的getter方法  
                    Method getter = property.getReadMethod();  
                    Object value = getter.invoke(obj);  
  
                    map.put(key, value);  
                }  
  
            }  
        } catch (Exception e) {  
            System.out.println("transBean2Map Error " + e);  
        }  
  
        return map;  
  
    }   

}
