package autonavi.online.framework.support.zookeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import autonavi.online.framework.property.PropertiesData;
import autonavi.online.framework.support.zookeeper.exception.DuplicateZooKeeperBizNodeException;
import autonavi.online.framework.zookeeper.SysProps;
import autonavi.online.framework.zookeeper.ZkUtils;

public class GetPropertiesDataFromZooKeeper implements PropertiesData {
	private Logger log = LogManager.getLogger(getClass());
	private ZooKeeperProp zooKeeperProp;
	
	private Map<String, String> properties = new HashMap<String, String>();
	private final String ROOT = "/aof";
	private final String BIZ = "/biz";
	/**
	 * 支持多颗配置树
	 * 支持配置信息的注释信息
	 * @since 1.1.6
	 */
	private final String BIZS="/bizs";
	/**
	 * BIZ的属性值
	 * @since 1.1.6
	 */
	private final String VALUE="/value";
	private String path;
	

	public void setZooKeeperProp(ZooKeeperProp zooKeeperProp) {
		this.zooKeeperProp = zooKeeperProp;
		this.path=ROOT + "/"+zooKeeperProp.getProjectName() + BIZS;
	}

    
	@Override
	public Map<String, String> getProperties() throws Exception {
		Map<String, String> properties = new HashMap<String, String>();
		if (zooKeeperProp != null) {// 从zookeeper里获取
			properties.putAll(this.getPropertiesFromAllDS(zooKeeperProp));
		}
		return properties;
	}
	public Map<String, String> getProperties(ZooKeeper zk,String appName,String... tempPath) throws Exception {
		appName="/"+appName;
		String _tempPath=null;
		if(tempPath!=null&&tempPath.length>=1){
			_tempPath="/"+tempPath[0];
		}
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.putAll(this.getPropertiesFromAllDS(zk,appName,_tempPath));
		return properties;
	}
	private Map<String, String> getPropertiesFromAllDS(ZooKeeper zk,String appName,String tempPath) throws Exception{
		String path="";
		if(tempPath!=null&&!tempPath.equals("")){
			path=SysProps.AOF_TEMP_ROOT+appName+tempPath;
		}else{
			path=SysProps.AOF_ROOT+appName+SysProps.AOF_APP_BIZ;
		}
		return getBizFromZK(zk, path);
	}
	
	/**
	 * 从所有数据源得到属性，这里是多个ZooKeeper
	 * 
	 * @throws Exception
	 */
	private Map<String, String> getPropertiesFromAllDS(ZooKeeperProp zooKeeperProp) throws Exception {
		String address = zooKeeperProp.getAddress();
		int sessionTimeout = zooKeeperProp.getSessionTimeout();
		try {
			/**
			 * 调整BIZ刷新机制，和BASE的方式合并，走与刷新和刷新两个步骤
			 */
//			ZooKeeper zk = ZkUtils.Instance().Init(address, sessionTimeout,
//					new Watcher() {
//
//						/**
//						 * 当zookeeper节点发生变化时
//						 */
//						@Override
//						public void process(WatchedEvent event) {
//							if (!event.getType().equals(EventType.None)
//									&& event.getPath() != null
//									&& isInclude(event.getPath())) {// 如果当前变化的路径包含于配置的路径中
//								try {
//									Log.info("检测到节点变化 重新刷新");
//									PropertiesConfigUtil.refresh();// 当触发事件时，更新属性配置
//									zk_.close();// 关闭连接
//								} catch (Exception e) {
//									e.printStackTrace();
//								}
//							}
//						}
//					});
//			this.zk_=zk;
			ZooKeeper zk = ZkUtils.Instance().Init(address, sessionTimeout,null);
			zk.addAuthInfo("digest", (zooKeeperProp.getProjectName()+":"+zooKeeperProp.getPassword()).getBytes());
//			this.getAllPropertiesFromZK(zk, path,true);// 递归查询下级 监听
			//判定BIZ的配置的模式 如果是老模式 则迁移老模式的BIZ到新模式下（暂时屏蔽）
//			if(zk.exists(path, false)!=null){
//				//存在新节点，为新模式
//				this.getAllPropertiesFromZK(zk, path,false,true);
//			}else{
//				//若是老模式 则迁移数据到新模式下
//				log.warn("目前系统的运行模式为旧版BIZ配置方式,请尽快登陆配置工具将BIZ自动更新到新模式下");
//				path=ROOT + "/"+zooKeeperProp.getProjectName() + BIZ;
//				this.getAllPropertiesFromZK(zk, path,false,false);// 查询 不监听
//				
//			}
			path=ROOT + "/"+zooKeeperProp.getProjectName() + BIZ;
			this.getAllPropertiesFromZK(zk, path,false,false);// 查询 不监听
			MonitorZooKeeper.setBizVersion();
			log.info("关闭BIZ信息ZooKeeper获取连接");
			zk.close();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		return properties;
	}

	/**
	 * 判断变化的路径是否包含于配置的路径中
	 * 
	 * @param path
	 * @return
	 */
	@SuppressWarnings("unused")
	private boolean isInclude(String path) {
		String _path = this.path;
		if (path.indexOf(_path) > -1)
			return true;
		return false;
	}

	private void getAllPropertiesFromZK(ZooKeeper zk, String path,boolean isListen,boolean isSplit)
			throws KeeperException, InterruptedException,Exception {
//		String[] paths = path.split("/");
//		String pathName = paths[paths.length - 1];
//		if (!properties.containsKey(pathName)) {
//			byte[] data = zk.getData(path, true, null);
//			if (data != null)
//				properties.put(pathName, new String(data));// 先得到本身的值
//		}
		/*
		 * else { System.err.println("zookeeper中存在名字相同的节点，请检查！节点名：" + path); }
		 */
		// 得到孩子的
		List<String> zl = zk.getChildren(path, isListen, null);
		if(!isSplit){
			for (String key : zl) {

				String _path = path + "/" + key;
				byte[] data = zk.getData(_path, isListen, null);
				String value = null;
				if (data != null)
					value = new String(data,SysProps.CHARSET);// 从ZooKeeper得到孩子下的内容
				properties.put(key, value);
			}
		}else{
			//先跳过分类树
			for (String key : zl) {
				String _path = path + "/" + key;
				List<String> keyZl=zk.getChildren(_path, isListen, null);
				//分类树下的属性key
				for (String keyKey : keyZl) {
					String valuePath=_path+"/"+keyKey+VALUE;
					//value值获取
					byte[] data=zk.getData(valuePath, isListen, null);
					String value = null;
					if (data != null)
						value = new String(data,SysProps.CHARSET);// 从ZooKeeper得到孩子下的内容
					//校验是否重复
					if (!properties.containsKey(keyKey)){
						properties.put(keyKey, value);
					}else{
						log.error("属性["+keyKey+"]重复，无法完成获取");
						throw new DuplicateZooKeeperBizNodeException("属性["+keyKey+"]重复，无法完成获取");
					}
					
				}
				
			}
		}
		
	}
	private Map<String, String> getBizFromZK(ZooKeeper zk, String path)throws KeeperException, InterruptedException,Exception{
		Map<String, String> properties=new HashMap<String, String>();
		List<String> zl = zk.getChildren(path, false, null);
		for (String key : zl) {
			String _path = path + "/" + key;
			byte[] data = zk.getData(_path, false, null);
			String value = null;
			if (data != null)
				value = new String(data,SysProps.CHARSET);// 从ZooKeeper得到孩子下的内容
			properties.put(key, value);
		}
		return properties;
	}
	


}
