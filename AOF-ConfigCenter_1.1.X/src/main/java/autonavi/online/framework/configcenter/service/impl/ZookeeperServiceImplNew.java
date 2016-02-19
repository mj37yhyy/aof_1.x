package autonavi.online.framework.configcenter.service.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.ACL;
import org.apache.zookeeper.data.Id;
import org.apache.zookeeper.data.Stat;
import org.apache.zookeeper.server.auth.DigestAuthenticationProvider;
import org.springframework.stereotype.Service;

import autonavi.online.framework.cc.CcBaseEntity;
import autonavi.online.framework.cc.CcDataSource;
import autonavi.online.framework.configcenter.commons.AppNode;
import autonavi.online.framework.configcenter.exception.AofException;
import autonavi.online.framework.configcenter.exception.AofExceptionEnum;
import autonavi.online.framework.configcenter.service.ZookeeperService;
import autonavi.online.framework.configcenter.util.ZookeeperInit;
import autonavi.online.framework.jdbc.dao.DaoHelper;
import autonavi.online.framework.metadata.entity.ColumnAttribute;
import autonavi.online.framework.sharding.index.SegmentTable;
import autonavi.online.framework.support.zookeeper.ConfigCenterFromZooKeeper;
import autonavi.online.framework.support.zookeeper.GetPropertiesDataFromZooKeeper;
import autonavi.online.framework.support.zookeeper.holder.ZooKeeperHolder;
import autonavi.online.framework.zookeeper.SysProps;
import autonavi.online.framework.zookeeper.ZooKeeperUtils;

@Service("zookeeperService")
public class ZookeeperServiceImplNew implements ZookeeperService {
	private Logger logger = LogManager.getLogger(this.getClass());
	@Resource
	ZookeeperInit zookeeperInit;
	@Resource
	ConfigCenterFromZooKeeper configCenterFromZooKeeper;
	@Resource
	GetPropertiesDataFromZooKeeper getPropertiesDataFromZooKeeper;

	private List<ACL> generateACL(String appRoot, String passwd)
			throws Exception {
		List<ACL> acls = new ArrayList<ACL>();
		Id id2 = new Id("digest",
				DigestAuthenticationProvider.generateDigest(appRoot + ":"
						+ passwd));
		ACL acl2 = new ACL(ZooDefs.Perms.ALL, id2);
		acls.add(zookeeperInit.getAcl());
		acls.add(acl2);
		return acls;
	}

	private void initAofConfigCenter(String appRoot, ZooKeeper zk) {
		try {
			// 根冲突
			if (ZooKeeperUtils.checkZKNodeIsExist(zk, SysProps.AOF_ROOT + "/"
					+ appRoot, false)) {
				logger.error("应用[" + appRoot + "]已经存在");
				throw new AofException(AofExceptionEnum.APPROOT_IS_EXIST);
			} else {
				String root = SysProps.AOF_ROOT + "/" + appRoot;
				ZooKeeperUtils.createSafeZKNode(root, null);
				// base
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BASE,
						null);
				// base-dss
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BASE
						+ SysProps.AOF_APP_DSS, null);
				// base-shard
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BASE
						+ SysProps.AOF_APP_SHARD, null);
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BASE
						+ SysProps.AOF_APP_SHARD + SysProps.AOF_APP_DSS, null);
				ZooKeeperUtils
						.createSafeZKNode(root + SysProps.AOF_APP_BASE
								+ SysProps.AOF_APP_SHARD
								+ SysProps.AOF_APP_INDEX, null);
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BASE
						+ SysProps.AOF_APP_SHARD + SysProps.AOF_APP_INDEX
						+ SysProps.AOF_APP_DS, null);
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BASE
						+ SysProps.AOF_APP_SHARD + SysProps.AOF_APP_INDEX
						+ SysProps.AOF_APP_TABLES, null);
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BASE
						+ SysProps.AOF_APP_SHARD + SysProps.AOF_APP_SEG, null);
				// biz
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_BIZ,
						null);
				// 临时目录初始化
				ZooKeeperUtils.createSafeZKNode(SysProps.AOF_TEMP_ROOT + "/"
						+ appRoot, null);
				
				//monitor
				ZooKeeperUtils.createZKNode(SysProps.AOF_MONITOR_ROOT+"/"+appRoot, null);
				ZooKeeperUtils.createZKNode(SysProps.AOF_MONITOR_ROOT+"/"+appRoot+SysProps.TIMESTAMPS, null);
				ZooKeeperUtils.createZKNode(SysProps.AOF_MONITOR_ROOT+"/"+appRoot+SysProps.SERVERS, null);
				ZooKeeperUtils.createZKNode(SysProps.AOF_MONITOR_ROOT+"/"+appRoot+SysProps.VERSION, "1".getBytes(SysProps.CHARSET));
				ZooKeeperUtils.createZKNode(SysProps.AOF_MONITOR_ROOT+"/"+appRoot+SysProps.BIZ_VERSION, "1".getBytes(SysProps.CHARSET));
				logger.info("初始化应用成功，应用名称[" + appRoot + "]");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}

	}

	@Override
	public void addAppRoot(String appRoot, String passwd) {
		ZooKeeper zk = null;
		try {
			// 根冲突
			if (ZooKeeperUtils.checkZKNodeIsExist(zookeeperInit.getZoo(),
					SysProps.AOF_ROOT + SysProps.AOF_PASS + "/" + appRoot,
					false)) {
				logger.error("应用[" + appRoot + "]已经存在");
				throw new AofException(AofExceptionEnum.APPROOT_IS_EXIST);
			} else {
				ZooKeeperUtils.startTransaction(zookeeperInit.getZoo());
				if (!ZooKeeperUtils.checkZKNodeIsExist(zookeeperInit.getZoo(),
						SysProps.AOF_ROOT, false)) {
					ZooKeeperUtils.createZKNode(SysProps.AOF_ROOT, null);
				}
				if (!ZooKeeperUtils.checkZKNodeIsExist(zookeeperInit.getZoo(),
						SysProps.AOF_ROOT + SysProps.AOF_PASS, false)) {
					ZooKeeperUtils.createZKNode(SysProps.AOF_ROOT
							+ SysProps.AOF_PASS, null);
				}
				ZooKeeperUtils.commit();
				String root = SysProps.AOF_ROOT + SysProps.AOF_PASS + "/"
						+ appRoot;
				zk = zookeeperInit.getNewZoo();
				ZooKeeperUtils.startTransaction(zk);
				ZooKeeperUtils.setACL(generateACL(appRoot, passwd));
				ZooKeeperUtils.createSafeZKNode(root, passwd.getBytes());
				logger.info("初始化应用权限成功，应用名称[" + appRoot + "]");
				this.initAofConfigCenter(appRoot, zk);
				ZooKeeperUtils.commit();
			}
		} catch (Exception e) {
			if (e instanceof AofException) {
				throw (AofException) e;
			}
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		} finally {
			try {
				if (zk != null) {
					zk.close();
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
				if (!ZooKeeperHolder.commit.get())
					throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
			}
		}

	}

	@Override
	public ZooKeeper loginAppRoot(String appRoot, String passwd) {
		// 从ZK获取应用的校验信息
		try {
			if (!ZooKeeperUtils.checkZKNodeIsExist(zookeeperInit.getZoo(),
					SysProps.AOF_ROOT + SysProps.AOF_PASS + "/" + appRoot,
					false)) {
				logger.error("应用[" + appRoot + "]的配置不存在");
				throw new AofException(AofExceptionEnum.APPROOT_IS_NOT_EXIST);
			}
			String pass = ZooKeeperUtils.getZkNode(zookeeperInit.getZoo(),
					SysProps.AOF_ROOT + SysProps.AOF_PASS + "/" + appRoot);
			if (pass.equals(passwd)) {
				logger.info("校验成功[" + appRoot + "]");
				return zookeeperInit.generateAppZoo(appRoot, passwd);
			} else {
				logger.error("校验失败[" + appRoot + "]");
				throw new AofException(AofExceptionEnum.APPROOT_LOGIN_ERROR);
			}
		} catch (Exception e) {
			if (e instanceof AofException) {
				throw (AofException) e;
			}
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public void deleteNode(String path, ZooKeeper zk) {
		// TODO Auto-generated method stub

	}

	@Override
	public List<String> getAppNodeTree(String path, ZooKeeper zk) {
		List<String> l = new ArrayList<String>();
		try {
			l = zk.getChildren(path, false);
			return l;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public Map<String, String> getAppChildNodeMap(String path, ZooKeeper zk,
			String nodeName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initAppProp(File f) {
		// TODO Auto-generated method stub

	}

	@Override
	public void saveOrUpdate(CcBaseEntity ccBaseEntity) {
		// TODO Auto-generated method stub

	}

	@Override
	public Stat getAppNodeStat(String path, ZooKeeper zk) {
		try {
			return zk.exists(path, false);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public void copyAppNode(String sourcePath, String desPath, ZooKeeper zk,
			String appName, String passwd) {
		try {
			ZooKeeperUtils.setACL(this.generateACL(appName, passwd));
			ZooKeeperUtils.startTransaction(zk);
			this.clearBaseConfig(desPath, zk, false);
			if (!ZooKeeperUtils.checkZKNodeIsExist(zk, desPath, false)) {
				ZooKeeperUtils.createSafeZKNode(desPath,
						zk.getData(sourcePath, false, null));
			}
			this.copyAppNode(sourcePath, desPath, zk);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}

	}

	private void copyAppNode(String sourcePath, String desPath, ZooKeeper zk)
			throws Exception {
		List<String> l = new ArrayList<String>();
		try {
			l = zk.getChildren(sourcePath, false);
			for (String url : l) {
				ZooKeeperUtils.createSafeZKNode(desPath + "/" + url,
						zk.getData(sourcePath + "/" + url, false, null));
				copyAppNode(sourcePath + "/" + url, desPath + "/" + url, zk);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}
	
	private void copyAppNode(String sourcePath, String desPath, ZooKeeper zk,List<String> exPath)
			throws Exception {
		List<String> l = new ArrayList<String>();
		if(exPath==null)exPath=new ArrayList<String>();
		try {
			l = zk.getChildren(sourcePath, false);
			for (String url : l) {
				boolean flag=false;
				for(String path:exPath){
					if(path.equals(desPath + "/" + url)){
						flag=true;
						break;
					}
				}
				if(!flag){
					ZooKeeperUtils.createSafeZKNode(desPath + "/" + url,
							zk.getData(sourcePath + "/" + url, false, null));
				}
				
				copyAppNode(sourcePath + "/" + url, desPath + "/" + url, zk,exPath);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}
	
	

	@Override
	public CcBaseEntity getCcBaseEntity(ZooKeeper zk, String appName,
			String tempPath) {
		try {
			if (tempPath == null || tempPath.equals("")) {
				return configCenterFromZooKeeper.getDssConfig(zk, appName);
			} else {
				return configCenterFromZooKeeper.getDssConfig(zk, appName,
						tempPath);
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}

	}

	@Override
	public void saveBaseConfigToTemp(CcBaseEntity ccBaseEntity,
			String fileName, ZooKeeper zk, String appName, String password) {
		try {
			String path = SysProps.AOF_TEMP_ROOT + "/" + appName + "/"
					+ fileName;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setACL(this.generateACL(appName, password));
			ZooKeeperUtils.createSafeZKNode(path, null);
			this.initBaseConfigTemp(path);
			this.saveDssConfig(ccBaseEntity, path);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}

	}

	@Override
	public void saveBaseConfig(CcBaseEntity ccBaseEntity, ZooKeeper zk,
			String appName, String password) {
		try {
			String path = SysProps.AOF_ROOT + "/" + appName
					+ SysProps.AOF_APP_BASE;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setACL(this.generateACL(appName, password));
			// //先进行清理
			this.clearBaseConfig(path, zk, true);
			this.saveDssConfig(ccBaseEntity, path);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}

	}

	private void clearBaseConfig(String root, ZooKeeper zk, boolean clearIndexDs) {
		// 清理
		ZooKeeperUtils.deleteNode(root + SysProps.AOF_APP_DSS, zk, false);
		ZooKeeperUtils.deleteNode(root + SysProps.AOF_APP_SHARD
				+ SysProps.AOF_APP_DSS, zk, false);
		if (clearIndexDs) {
			ZooKeeperUtils.deleteNode(root + SysProps.AOF_APP_SHARD
					+ SysProps.AOF_APP_INDEX + SysProps.AOF_APP_DS, zk, true);
		}

	}
	private void clearBizConfig(String root, ZooKeeper zk) {
		// 清理
		ZooKeeperUtils.deleteNode(root, zk, false);

	}

	private void clearShardingConfig(String root, ZooKeeper zk) {
		ZooKeeperUtils.deleteNode(root + SysProps.AOF_APP_SHARD
				+ SysProps.AOF_APP_SEG, zk, false);
		ZooKeeperUtils.deleteNode(root + SysProps.AOF_APP_SHARD
				+ SysProps.AOF_APP_INDEX + SysProps.AOF_APP_TABLES, zk, false);
	}

	private void initBaseConfigTemp(String root) throws Exception {
		// 初始化
		ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_DSS, null);
		ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD, null);
		ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
				+ SysProps.AOF_APP_DSS, null);
		ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
				+ SysProps.AOF_APP_INDEX, null);

	}

	private void saveBizConfig(Map<String,String> ccBase, String root)throws Exception{
		for(String _key:ccBase.keySet()){
			ZooKeeperUtils.createSafeZKNode(root+"/"+_key,ccBase.get(_key).getBytes(SysProps.CHARSET));
		}
	}
	
	private void saveDssConfig(CcBaseEntity ccBaseEntity, String root)
			throws Exception {
//		ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
//				+ SysProps.AOF_APP_INDEX + SysProps.AOF_APP_DS, ccBaseEntity
//				.getDataSources().get(ccBaseEntity.getShardIndex()).getName()
//				.toString().getBytes(SysProps.CHARSET));
		ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
				+ SysProps.AOF_APP_INDEX + SysProps.AOF_APP_DS, ccBaseEntity.getShardIndex().toString().getBytes(SysProps.CHARSET));

		// 循环处理数据源热部署相关
		for (String keyName : ccBaseEntity.getRealDataSources().keySet()) {
			CcDataSource ccDataSource = ccBaseEntity.getRealDataSources()
					.get(keyName);
			ccDataSource.setName(keyName);
			ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_DSS + "/"
					+ ccDataSource.getName(), ccDataSource.getBeanClass()
					.getBytes(SysProps.CHARSET));
			// dss
			for (String key : ccDataSource.getProps().keySet()) {
				Object obj = ccDataSource.getProps().get(key);
				// String
				if (obj instanceof String) {
					ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_DSS
							+ "/" + ccDataSource.getName() + "/" + key,
							((String) obj).getBytes(SysProps.CHARSET));
				} else if (obj instanceof Properties) {
					// 属性
					Properties p = (Properties) obj;
					for (Object _key : p.keySet()) {
						ZooKeeperUtils.createSafeZKNode(
								root + SysProps.AOF_APP_DSS + "/"
										+ ccDataSource.getName() + "/" + _key,
								((String) p.get(_key))
										.getBytes(SysProps.CHARSET));
					}
				} else if (obj instanceof Map) {
					@SuppressWarnings("rawtypes")
					Map m = (Map) obj;
					for (Object _key : m.keySet()) {
						ZooKeeperUtils.createSafeZKNode(
								root + SysProps.AOF_APP_DSS + "/"
										+ ccDataSource.getName() + "/" + _key,
								((String) m.get(_key))
										.getBytes(SysProps.CHARSET));
					}
				}
			}
			
			

		}
		for (Integer dsKey : ccBaseEntity.getDataSources().keySet()){
			CcDataSource ccDataSource = ccBaseEntity.getDataSources()
					.get(dsKey);
			ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
					+ SysProps.AOF_APP_DSS + "/" + dsKey, null);
			ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
					+ SysProps.AOF_APP_DSS + "/" + dsKey+"/"+SysProps.STRATEGY_NAME, ccDataSource.getProps().get(SysProps.STRATEGY_NAME).toString().getBytes(SysProps.CHARSET));
			ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
					+ SysProps.AOF_APP_DSS + "/" + dsKey+"/"+SysProps.REAl_DSS, null);
			@SuppressWarnings("unchecked")
			List<String> ll=(List<String>)ccDataSource.getProps().get(SysProps.REAl_DSS);
			for(String dss:ll){
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
						+ SysProps.AOF_APP_DSS + "/" + dsKey+"/"+SysProps.REAl_DSS+"/"+dss, null);
			}
			
			
//			// shard_dss
//			ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
//								+ SysProps.AOF_APP_DSS + "/" + dsKey, ccDataSource
//								.getName().getBytes(SysProps.CHARSET));
		}

	}

	private void saveShardingConfig(CcBaseEntity ccBaseEntity, String root)
			throws Exception {
		// 分表处理
		for (SegmentTable seg : ccBaseEntity.getSegmentTables()) {
			ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
					+ SysProps.AOF_APP_SEG + "/" + seg.getName(),
					(seg.getCount() + "").getBytes(SysProps.CHARSET));
		}
		//索引表处理
		for (String key : ccBaseEntity.getIndexTableMap().keySet()){
			ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
					+ SysProps.AOF_APP_INDEX +SysProps.AOF_APP_TABLES+ "/" + key,
					null);
			List<ColumnAttribute> l=ccBaseEntity.getIndexTableMap().get(key);
			for(ColumnAttribute column:l){
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
						+ SysProps.AOF_APP_INDEX +SysProps.AOF_APP_TABLES+ "/" + key
						+ "/" +column.getColumnName(),
						null);
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
						+ SysProps.AOF_APP_INDEX +SysProps.AOF_APP_TABLES+ "/" + key
						+ "/" +column.getColumnName()+SysProps.AOF_INDEX_TYPE,
						column.getColumnType().getBytes(SysProps.CHARSET));
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
						+ SysProps.AOF_APP_INDEX +SysProps.AOF_APP_TABLES+ "/" + key
						+ "/" +column.getColumnName()+SysProps.AOF_INDEX_LENGTH,
						(column.getLength()+"").getBytes(SysProps.CHARSET));
				ZooKeeperUtils.createSafeZKNode(root + SysProps.AOF_APP_SHARD
						+ SysProps.AOF_APP_INDEX +SysProps.AOF_APP_TABLES+ "/" + key
						+ "/" +column.getColumnName()+SysProps.AOF_INDEX_NAME,
						column.getName().getBytes(SysProps.CHARSET));
			}
		}
	}

	@Override
	public CcBaseEntity getCcShardingEntity(ZooKeeper zk, String appName) {
		try {
			return configCenterFromZooKeeper.getShardingConfig(zk, appName);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public void saveShardingConfig(CcBaseEntity ccBaseEntity, ZooKeeper zk,
			String appName, String password) {
		try {
			String path = SysProps.AOF_ROOT + "/" + appName
					+ SysProps.AOF_APP_BASE;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setACL(this.generateACL(appName, password));
			// //先进行清理
			this.clearShardingConfig(path, zk);
			this.saveShardingConfig(ccBaseEntity, path);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public List<AppNode> checkAppNodesStats(ZooKeeper zk,String appName) {
		String serverPath=SysProps.ZK_MONITOR_ROOT+appName+SysProps.SERVERS;
		List<AppNode> rt=new ArrayList<AppNode>();
		try {
			Stat stat=getZKSystemStat(zk,appName);
			List<String> l=zk.getChildren(serverPath, false);
			for(String _path:l){
				AppNode node=getAppNode(_path,serverPath,zk,stat);
				rt.add(node);
			}
			return rt;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}
	private Stat getZKSystemStat(ZooKeeper zk,String appName)throws Exception{
		ZooKeeperUtils.setZkNode(SysProps.ZK_MONITOR_ROOT+appName+SysProps.TIMESTAMPS, null, zk);
		Stat stat=zk.exists(SysProps.ZK_MONITOR_ROOT+appName+SysProps.TIMESTAMPS, false);
		return stat;
	}
	private AppNode getAppNode(String path,String serverPath,ZooKeeper zk,Stat stat)throws Exception{
		Long time=zk.exists(serverPath+"/"+path+SysProps.UPDATE, false).getMtime();
		AppNode node=new AppNode();
		node.setNodeName(path);
		node.setUpdateTime(time);
		node.setSystemTime(stat.getMtime());
		node.setNodeVersion(new String(zk.getData(serverPath+"/"+path+SysProps.VERSION, false, null),SysProps.CHARSET));
		node.setNodeBizVersion(new String(zk.getData(serverPath+"/"+path+SysProps.BIZ_VERSION, false, null),SysProps.CHARSET));
		return node;
	}

	@Override
	public String preBaseActive(ZooKeeper zk, String appName,List<AppNode> nodeList) {
		try {
			String basePath=SysProps.ZK_MONITOR_ROOT+appName;
			String version=DaoHelper.createPrimaryKey()+SysProps.PRECOMMIT_FLAG;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setZkNode(basePath+SysProps.VERSION, version.getBytes(SysProps.CHARSET));
			for(AppNode node:nodeList){
				ZooKeeperUtils.setZkNode(basePath+SysProps.SERVERS+"/"+node.getNodeName()+SysProps.VERSION_ZK,version.getBytes(SysProps.CHARSET));
			}
			ZooKeeperUtils.commit();
			return version;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
		
	}

	@Override
	public List<AppNode> checkPreBaseActive(ZooKeeper zk, String appName,
			List<AppNode> nodeList) {
		String serverPath=SysProps.ZK_MONITOR_ROOT+appName+SysProps.SERVERS;
		List<AppNode> rt=new ArrayList<AppNode>();
		try {
			Stat stat=getZKSystemStat(zk,appName);
			for(AppNode _node :nodeList){
				AppNode node=getAppNode(_node.getNodeName(),serverPath,zk,stat);
				rt.add(node);
			}
			return rt;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public String commitBaseActive(ZooKeeper zk, String appName,
			List<AppNode> nodeList,String tempFile,String pass) {
		try {
			String basePath=SysProps.ZK_MONITOR_ROOT+appName;
			String version="";
			ZooKeeperUtils.startTransaction(zk);
			for(AppNode node:nodeList){
				if(version.equals("")){
					version=node.getNodeVersion().replaceAll(SysProps.PRECOMMIT_FLAG, SysProps.COMMIT_FLAG);
					ZooKeeperUtils.setZkNode(basePath+SysProps.VERSION, version.getBytes(SysProps.CHARSET));
				}
				String nodeVersion=new String(zk.getData(basePath+SysProps.SERVERS+"/"+node.getNodeName()+SysProps.VERSION_ZK, false, null),SysProps.CHARSET);
				if(!node.getNodeVersion().equals(nodeVersion)){
					throw new AofException(AofExceptionEnum.ZOOKEEPER_DEPLOY_ERROR);
				}
				ZooKeeperUtils.setZkNode(basePath+SysProps.SERVERS+"/"+node.getNodeName()+SysProps.VERSION_ZK,version.getBytes(SysProps.CHARSET));
			}
            ZooKeeperUtils.setACL(this.generateACL(appName, pass));
			
			String desPath= SysProps.AOF_ROOT+"/"+appName+SysProps.AOF_APP_BASE;
			String sourcePath=SysProps.AOF_TEMP_ROOT+"/"+appName+"/"+tempFile;
			this.clearBaseConfig(desPath, zk, false);
			if (!ZooKeeperUtils.checkZKNodeIsExist(zk, desPath, false)) {
				ZooKeeperUtils.createSafeZKNode(desPath,
						zk.getData(sourcePath, false, null));
			}
			List<String> exPath=new ArrayList<String>();
			exPath.add(desPath+SysProps.AOF_APP_DSS);
			exPath.add(desPath+SysProps.AOF_APP_SHARD);
			exPath.add(desPath+SysProps.AOF_APP_SHARD+SysProps.AOF_APP_DSS);
			exPath.add(desPath+SysProps.AOF_APP_SHARD+SysProps.AOF_APP_INDEX);
			exPath.add(desPath+SysProps.AOF_APP_SHARD+SysProps.AOF_APP_INDEX+SysProps.AOF_APP_DS);
			this.copyAppNode(sourcePath, desPath, zk,exPath);
			ZooKeeperUtils.commit();
			return version;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public List<AppNode> checkCommitBaseActive(ZooKeeper zk, String appName,
			List<AppNode> nodeList) {
		return checkPreBaseActive(zk,appName,nodeList);
	}

	@Override
	public Map<String, String> getBizInfo(ZooKeeper zk, String appName,String tempPath) {
		try {
			if (tempPath == null || tempPath.equals("")) {
				return getPropertiesDataFromZooKeeper.getProperties(zk, appName);
			} else {
				return getPropertiesDataFromZooKeeper.getProperties(zk, appName, tempPath);
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public void saveBizConfigToTemp(Map<String, String> ccBase,
			String fileName, ZooKeeper zk, String appName, String password) {
		try {
			String path = SysProps.AOF_TEMP_ROOT + "/" + appName + "/"
					+ fileName;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setACL(this.generateACL(appName, password));
			ZooKeeperUtils.createSafeZKNode(path, null);
			this.saveBizConfig(ccBase, path);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
		
	}

	@Override
	public void saveBizConfig(Map<String, String> ccBase, ZooKeeper zk,
			String appName, String password) {
		try {
			String path = SysProps.AOF_ROOT + "/" + appName+SysProps.AOF_APP_BIZ;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setACL(this.generateACL(appName, password));
			// //先进行清理
			this.clearBizConfig(path, zk);
			this.saveBizConfig(ccBase, path);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
		
	}

	@Override
	public String preBizActive(ZooKeeper zk, String appName,
			List<AppNode> nodeList) {
		try {
			String basePath=SysProps.ZK_MONITOR_ROOT+appName;
			String version=DaoHelper.createPrimaryKey()+SysProps.PRECOMMIT_FLAG;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setZkNode(basePath+SysProps.BIZ_VERSION, version.getBytes(SysProps.CHARSET));
			for(AppNode node:nodeList){
				ZooKeeperUtils.setZkNode(basePath+SysProps.SERVERS+"/"+node.getNodeName()+SysProps.BIZ_VERSION_ZK,version.getBytes(SysProps.CHARSET));
			}
			ZooKeeperUtils.commit();
			return version;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}


	@Override
	public String commitBizActive(ZooKeeper zk, String appName,
			List<AppNode> nodeList,String tempFile,String pass) {
		try {
			String basePath=SysProps.ZK_MONITOR_ROOT+appName;
			String version="";
			ZooKeeperUtils.startTransaction(zk);
			for(AppNode node:nodeList){
				if(version.equals("")){
					version=node.getNodeVersion().replaceAll(SysProps.PRECOMMIT_FLAG, SysProps.COMMIT_FLAG);
					ZooKeeperUtils.setZkNode(basePath+SysProps.BIZ_VERSION, version.getBytes(SysProps.CHARSET));
				}
				String nodeVersion=new String(zk.getData(basePath+SysProps.SERVERS+"/"+node.getNodeName()+SysProps.BIZ_VERSION_ZK, false, null),SysProps.CHARSET);
				if(!node.getNodeVersion().equals(nodeVersion)){
					throw new AofException(AofExceptionEnum.ZOOKEEPER_DEPLOY_ERROR);
				}
				ZooKeeperUtils.setZkNode(basePath+SysProps.SERVERS+"/"+node.getNodeName()+SysProps.BIZ_VERSION_ZK,version.getBytes(SysProps.CHARSET));
			}
			
			ZooKeeperUtils.setACL(this.generateACL(appName, pass));
			
			String desPath= SysProps.AOF_ROOT+"/"+appName+SysProps.AOF_APP_BIZ;
			String sourcePath=SysProps.AOF_TEMP_ROOT+"/"+appName+"/"+tempFile;
			this.clearBizConfig(desPath, zk);
			if (!ZooKeeperUtils.checkZKNodeIsExist(zk, desPath, false)) {
				ZooKeeperUtils.createSafeZKNode(desPath,
						zk.getData(sourcePath, false, null));
			}
			this.copyAppNode(sourcePath, desPath, zk);
			ZooKeeperUtils.commit();
			return version;
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public void deleteServerNode(ZooKeeper zk, String appName, String nodeName) {
		try {
			String path=SysProps.AOF_MONITOR_ROOT+"/"+appName+SysProps.SERVERS+"/"+nodeName;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.deleteNode(path, zk, true);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public void deleteTempConfig(ZooKeeper zk, String appName, String nodeName) {
		try {
			String path=SysProps.AOF_TEMP_ROOT+"/"+appName+"/"+nodeName;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.deleteNode(path, zk, true);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public Boolean checkAdminUser(String userName, String pass) {
		if(userName.equals(zookeeperInit.getZooKeeperProp().getProjectName())&&pass.equals(zookeeperInit.getZooKeeperProp().getPassword())){
			return true;
		}
		return false;
	}

	@Override
	public void importDssConfig(CcBaseEntity ccBaseEntity, ZooKeeper zk,
			String userName, String pass) {
		try {
			String path = SysProps.AOF_ROOT + "/" + userName
					+ SysProps.AOF_APP_BASE;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setACL(this.generateACL(userName, pass));
			this.clearBaseConfig(path, zk, true);
			this.saveDssConfig(ccBaseEntity, path);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}

	@Override
	public void importBizConfig(Map<String, String> ccBase, ZooKeeper zk,
			String userName, String pass) {
		try {
			String path = SysProps.AOF_ROOT + "/" + userName+SysProps.AOF_APP_BIZ;
			ZooKeeperUtils.startTransaction(zk);
			ZooKeeperUtils.setACL(this.generateACL(userName, pass));
			// //先进行清理
			this.clearBizConfig(path, zk);
			this.saveBizConfig(ccBase, path);
			ZooKeeperUtils.commit();
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw new AofException(AofExceptionEnum.ZOOKEEPER_USE_ERROR);
		}
	}


}
