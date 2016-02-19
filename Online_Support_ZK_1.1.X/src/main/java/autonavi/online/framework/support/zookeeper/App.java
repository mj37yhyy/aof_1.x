package autonavi.online.framework.support.zookeeper;

import java.lang.reflect.Method;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
    	ComboPooledDataSource cpds=new ComboPooledDataSource(true);  
    	Method[] m=ComboPooledDataSource.class.getMethods();   	
    	for(Method ms:m){
    		if(ms.getName().startsWith("set")){
    			System.out.println(ms.getName()+ms.getParameterTypes()[0]);
    		}
    	}
    }
}
