package autonavi.online.framework.jdbc.transactional.holder;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import autonavi.online.framework.util.ConcurrentStack;
@Deprecated
public class ConnectionHolder {
	private static final ThreadLocal<ConcurrentStack<Map<String, Object>>> connectionHolder = new ThreadLocal<ConcurrentStack<Map<String, Object>>>();
	private static final ThreadLocal<ConcurrentStack<ConcurrentStack<Map<String, Object>>>> connectionStackHolder = new ThreadLocal<ConcurrentStack<ConcurrentStack<Map<String, Object>>>>();
	private static final ThreadLocal<Integer> level=new ThreadLocal<Integer>();
	
	public static void setLevel(Integer level){
		ConnectionHolder.level.set(level);
	}
	public static Integer getLevel(){
		if(level.get()==null){
			return null;
		}
		return level.get();
	}
	public static void clearLevel(){
		level.remove();
	}


	public static void  cleanAllHolder(){
		connectionHolder.remove();
		connectionStackHolder.remove();
		level.remove();
	}
	
	public static void setConnectionHolder(ConcurrentStack<Map<String, Object>> stack){
		if(stack!=null)
			beforeStock().push(stack);
	}
	/**
	 * 初始化
	 * @return
	 */
	private static ConcurrentStack<ConcurrentStack<Map<String, Object>>> beforeStock() {
		ConcurrentStack<ConcurrentStack<Map<String, Object>>> connectionStacks = connectionStackHolder.get();
		if (connectionStacks == null) {
			connectionStacks = new ConcurrentStack<ConcurrentStack<Map<String, Object>>>();
			connectionStackHolder.set(connectionStacks);
		}
		return connectionStacks;
	}
	/**
	 * 刷新连接持有者
	 * @param isNew
	 */
	public static void refreshConnectionHolder(ConcurrentStack<Map<String, Object>> stack){
		connectionHolder.set(stack);
		
	}
	/**
	 * 刷新连接持有者
	 * @param isNew
	 */
	public static void refreshConnectionHolder(boolean isNew){
		if(isNew||beforeStock().size()==0){
			ConcurrentStack<Map<String, Object>> connectionStack=new ConcurrentStack<Map<String, Object>>();
			connectionHolder.set(connectionStack);
		}else{
			connectionHolder.set(beforeStock().pop());
		}
		
	}
	/**
	 * 获取嵌套连接的持有者
	 * @return
	 */
	public static ConcurrentStack<ConcurrentStack<Map<String, Object>>> getConnectionStackHolder() {
		ConcurrentStack<ConcurrentStack<Map<String, Object>>> conntectionStacks=beforeStock();
		return conntectionStacks;
	}
	
	public static void setConnectionHolder(DataSource ds, Connection conn) {
		ConcurrentStack<Map<String, Object>> connectionStack = connectionHolder
				.get();
		if (connectionStack == null || connectionStack.size() == 0) {
			connectionStack = new ConcurrentStack<Map<String, Object>>();
		}
		Map<String, Object> dcMap = new HashMap<String, Object>();
		dcMap.put("ds", ds);
		dcMap.put("conn", conn);
		connectionStack.push(dcMap);
		connectionHolder.set(connectionStack);
	}

	public static ConcurrentStack<Map<String, Object>> getConnectionHolder() {
		ConcurrentStack<Map<String, Object>> connectionStack = connectionHolder
				.get();
		if (connectionStack != null && connectionStack.size() > 0) {
			return connectionStack;
		}
		return new ConcurrentStack<Map<String, Object>>();
	}
}
