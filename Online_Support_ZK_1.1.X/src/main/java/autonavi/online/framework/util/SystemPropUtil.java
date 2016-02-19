package autonavi.online.framework.util;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

import autonavi.online.framework.commons.bean.DataSourceEntity;
import autonavi.online.framework.support.zookeeper.ZooKeeperProp;
import autonavi.online.framework.zookeeper.ZkUtils;

public class SystemPropUtil {
	private ZooKeeperProp zkEntity;
	@SuppressWarnings("unused")
	private ZooKeeper zk;
	private String projectPath;
	

	private Logger log = LogManager.getLogger(getClass());
	private String JTA_DS = "com.atomikos.jdbc.AtomikosDataSourceBean";
	private String C3P0_DS = "com.mchange.v2.c3p0.ComboPooledDataSource";
	private final String ROOT = "/aof";
	private final String BASE_DSS = "/base/dss";

	private static SystemPropUtil systemPropUtil = null;

	public static SystemPropUtil getInstance() {
		return systemPropUtil;
	}

	@PostConstruct
	private void init()throws Exception {
		systemPropUtil = this;
	}
	
	

	private void setProjectPath(String projectPath) {
		this.projectPath = projectPath;
	}
	private ZooKeeper getZKInstance() throws Exception{
		String address = this.zkEntity.getAddress();
		int sessionTimeout = this.zkEntity.getSessionTimeout();
		String projectPath = "/" + this.zkEntity.getProjectName();
		String pathPassword = this.zkEntity.getPassword();
		ZooKeeper zk = ZkUtils.Instance().Init(address, sessionTimeout,
				null);
		zk.addAuthInfo("digest",
				(this.zkEntity.getProjectName() + ":" + pathPassword).getBytes());
		this.setProjectPath(projectPath);
		return zk;
	}

	public void setZkEntity(ZooKeeperProp zkEntity) {
		if(systemPropUtil==null){
			this.zkEntity = zkEntity;
//			try {
//				this.setZk(this.getZKInstance());
//				
//			} catch (Exception e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			
			
		}
		
	}
	@SuppressWarnings("unused")
	private void setZk(ZooKeeper zk) {
		this.zk = zk;
	}

	/**
	 * 获取所有的数据源链接信息
	 * 
	 * @return
	 */
	public List<DataSourceEntity> getAllDataSourceEntityForZK()throws Exception{
		ZooKeeper zk=null;
		try {
			zk=this.getZKInstance();
			List<DataSourceEntity> l = new ArrayList<DataSourceEntity>();
			String base_dss_path = ROOT + projectPath + BASE_DSS;
			List<String> children = zk.getChildren(base_dss_path, false, null);
			for (String child : children) {
				DataSourceEntity entity = new DataSourceEntity();
				String thisPath = base_dss_path + "/" + child;
				byte[] dsData = zk.getData(thisPath, false, null);
				if (dsData != null) {
					String dsDataStr = new String(dsData);
					String clazz = dsDataStr;// 取出class属性
					if (clazz.equalsIgnoreCase(C3P0_DS)) {
						List<String> attrs = zk.getChildren(thisPath, false,
								null);
						this.getDSForC3P0(attrs, zk, entity, thisPath);
						l.add(entity);
					} else if (clazz.equals(JTA_DS)) {
						List<String> attrs = zk.getChildren(thisPath, false,
								null);
						this.getDSForJTA(attrs, zk, entity, thisPath);
						l.add(entity);
					} else {
						log.warn("数据源类型[" + clazz + "],不是框架支持的类型，不予处理，请检查配置");
					}
				}
			}
			return l;
		} catch (Exception e) {
			log.error(e.getMessage(),e);
			throw e;
		}finally{
			if(zk!=null)
			zk.close();
		}
	}

	private void getDSForC3P0(List<String> attrs, ZooKeeper zk,
			DataSourceEntity entity, String thisPath) throws KeeperException, InterruptedException, UnsupportedEncodingException {
		if (attrs != null) {
			for (String attr : attrs) {
				if (attr.equals("jdbcUrl")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setUrl(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("user")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setUser(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("driverClass")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setDriver(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("password")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setPassword(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("isIndex")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setIndex(new Boolean(new String(attrData,
								"utf-8")));
					}
				}
				if (attr.equals("isActive")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setActive(new Boolean(new String(attrData,
								"utf-8")));
					}
				}

			}

		}
	}

	private void getDSForJTA(List<String> attrs, ZooKeeper zk,
			DataSourceEntity entity, String thisPath) throws KeeperException, InterruptedException, UnsupportedEncodingException {
		if (attrs != null) {
			for (String attr : attrs) {
				if (attr.equals("URL")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setUrl(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("user")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setUser(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("xaDataSourceClassName")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setDriver(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("password")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setPassword(new String(attrData, "utf-8"));
					}
				}
				if (attr.equals("isIndex")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setIndex(new Boolean(new String(attrData,
								"utf-8")));
					}
				}
				if (attr.equals("isActive")) {
					byte[] attrData = zk.getData(thisPath + "/" + attr, false,
							null);
					if (attrData != null) {
						entity.setActive(new Boolean(new String(attrData,
								"utf-8")));
					}
				}

			}

		}
	}

}
