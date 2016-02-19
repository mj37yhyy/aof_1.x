package autonavi.online.framework.support.zookeeper;

import java.net.InetAddress;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.ZooKeeper;

import autonavi.online.framework.cc.CcConfigUtils;
import autonavi.online.framework.cc.Miscellaneous;
import autonavi.online.framework.property.PropertiesConfigUtil;
import autonavi.online.framework.support.zookeeper.exception.SetZooKeeperNodeException;
import autonavi.online.framework.support.zookeeper.exception.WatcherZooKeeperNodeException;
import autonavi.online.framework.support.zookeeper.holder.ZooKeeperHolder;
import autonavi.online.framework.zookeeper.SysProps;
import autonavi.online.framework.zookeeper.ZkUtils;
import autonavi.online.framework.zookeeper.ZooKeeperUtils;

public class MonitorZooKeeper {
	private final String ZK_WATCHER_BASE = "BASE";
	private final String ZK_WATCHER_BIZ = "BIZ";

	private String appRoot = "";
	private String appTimeStamp = "";
	private static String appServer = "";
	private String appServers = "";
	private String appVersion = "";
	private String zkVersion = "";
	private static String appBizVersion = "";
	private static String zkBizVersion = "";

	private Logger log = LogManager.getLogger(getClass());
	private ZooKeeperProp zooKeeperProp;
	private static ZooKeeper zooKeeper = null;

	public void setZooKeeperProp(ZooKeeperProp zooKeeperProp) throws Exception {
		this.zooKeeperProp = zooKeeperProp;
		if (zooKeeper == null && ZooKeeperHolder.use.get())
			this.init();
	}

	private void init() throws Exception {
		String ip = InetAddress.getLocalHost().getHostAddress().toString();
		String name = InetAddress.getLocalHost().getHostName().toString();
		appRoot = SysProps.ZK_MONITOR_ROOT + zooKeeperProp.getProjectName();
		appTimeStamp = appRoot + SysProps.TIMESTAMPS;
		appServer = appRoot + SysProps.SERVERS + "/" + name + "_" + ip + "_"
				+ Miscellaneous.getNodeIndex();
		appVersion = appRoot + SysProps.VERSION;
		zkVersion = appServer + SysProps.VERSION_ZK;
		appServers = appRoot + SysProps.SERVERS;
		appBizVersion = appRoot + SysProps.BIZ_VERSION;
		zkBizVersion = appServer + SysProps.BIZ_VERSION_ZK;
		zooKeeper = initZkClient();
		ZooKeeperUtils.startTransaction(zooKeeper);
		if (!ZooKeeperUtils.checkZKNodeIsExist(zooKeeper, appRoot, false)) {
			ZooKeeperUtils.createZKNode(appRoot, null);
			ZooKeeperUtils.createZKNode(appTimeStamp, null);
			ZooKeeperUtils.createZKNode(appServers, null);
			ZooKeeperUtils.createZKNode(appVersion,
					"1".getBytes(SysProps.CHARSET));
			ZooKeeperUtils.createZKNode(appBizVersion,
					"1".getBytes(SysProps.CHARSET));
			ZooKeeperUtils.createZKNode(appServer, null);
			ZooKeeperUtils.createZKNode(appServer + SysProps.VERSION, null);
			ZooKeeperUtils.createZKNode(appServer + SysProps.UPDATE, null);
			ZooKeeperUtils.createZKNode(appServer + SysProps.BIZ_VERSION,
					"1".getBytes(SysProps.CHARSET));
			ZooKeeperUtils.createZKNode(zkVersion,
					"1".getBytes(SysProps.CHARSET));
			ZooKeeperUtils.createZKNode(zkBizVersion,
					"1".getBytes(SysProps.CHARSET));
		} else if (!ZooKeeperUtils.checkZKNodeIsExist(zooKeeper, appServer,
				false)) {
			ZooKeeperUtils.createZKNode(appServer, null);
			ZooKeeperUtils.createZKNode(appServer + SysProps.VERSION, null);
			ZooKeeperUtils.createZKNode(appServer + SysProps.UPDATE, null);
			ZooKeeperUtils.createZKNode(appServer + SysProps.BIZ_VERSION,
					"1".getBytes(SysProps.CHARSET));
			ZooKeeperUtils.createZKNode(
					zkVersion,
					ZooKeeperUtils.getZkNode(zooKeeper, appVersion).getBytes(
							SysProps.CHARSET));
			ZooKeeperUtils.createZKNode(zkBizVersion,
					ZooKeeperUtils.getZkNode(zooKeeper, appBizVersion)
							.getBytes(SysProps.CHARSET));
		}
		ZooKeeperUtils.commit();
		// 监听路径
		startListenZk();
		ZooKeeperUtils.startTransaction(zooKeeper);
		ZooKeeperUtils.setZkNode(appServer + SysProps.VERSION, ZooKeeperUtils
				.getZkNode(zooKeeper, appVersion).getBytes(SysProps.CHARSET));
		ZooKeeperUtils.setZkNode(
				appServer + SysProps.BIZ_VERSION,
				ZooKeeperUtils.getZkNode(zooKeeper, appBizVersion).getBytes(
						SysProps.CHARSET));
		ZooKeeperUtils.setZkNode(appServer + SysProps.UPDATE,
				(new Date().getTime() + "").getBytes(SysProps.CHARSET));
		ZooKeeperUtils.commit();
		if (!ZooKeeperHolder.biz.get()) {
			setBizVersion();
		}
		// 心跳
		Thread t = new Thread() {
			public void run() {
				boolean reConnect=false;
				while (true) {
					try {
						if(reConnect){
							log.info("尝试重新连接ZooKeeper");
							zooKeeper=initZkClient();
							log.info("重新连接ZooKeeper完毕");
							reConnect=false;
						}
						ZooKeeperUtils.startTransaction(zooKeeper);
						ZooKeeperUtils.setZkNode(appServer + SysProps.UPDATE,
								(new Date().getTime() + "")
										.getBytes(SysProps.CHARSET));
						ZooKeeperUtils.commit();
						Thread.sleep(5000);
					} catch (Exception e) {
						log.error("ZooKeeper连接出现异常，将重试" + e.getMessage());
						log.info("将重新连接ZooKeeper");
						reConnect=true;

					}
				}
			}
		};
		t.start();
	}

	private ZooKeeper initZkClient() throws Exception {
		ZooKeeper zk=ZkUtils.Instance().Init(zooKeeperProp.getAddress(),
				zooKeeperProp.getSessionTimeout(), new Watcher() {
					private void processCommit(String version,
							String writeVersionPath, String flag) {
						try {
							if (version.endsWith(SysProps.PRECOMMIT_FLAG)) {// 预提交处理
								// 回写版本号
								ZooKeeperUtils
										.startTransaction(MonitorZooKeeper.zooKeeper);
								ZooKeeperUtils.setZkNode(writeVersionPath,
										version.getBytes(SysProps.CHARSET));
								ZooKeeperUtils.commit();
								log.info("回写预提交版本号(" + flag + ")" + version);

							} else if (version.endsWith(SysProps.COMMIT_FLAG)) {// 正式提交处理
								// 刷新
								if (flag.equals(ZK_WATCHER_BASE)) {
									CcConfigUtils.getInstance().refresh();
									log.info("刷新数据源配置");
								} else if (flag.equals(ZK_WATCHER_BIZ)) {
									log.info("刷新自定义属性配置");
									PropertiesConfigUtil.refresh();// 当触发事件时，更新属性配置
								}
								// 回写版本号
								ZooKeeperUtils
										.startTransaction(MonitorZooKeeper.zooKeeper);
								ZooKeeperUtils.setZkNode(writeVersionPath,
										version.getBytes(SysProps.CHARSET));
								ZooKeeperUtils.commit();
								log.info("回写正式提交的版本号(" + flag + ")" + version);
							}
						} catch (Exception e) {
							// TODO Auto-generated catch block
							throw new WatcherZooKeeperNodeException(e
									.getMessage(), e);
						}
					}

					@Override
					public void process(WatchedEvent event) {

						if (event.getType().equals(EventType.NodeDataChanged)
								&& event.getPath() != null
								&& checkPath(event.getPath(), zkVersion)) {
							startListenZkBase();
							String version = ZooKeeperUtils.getZkNode(
									MonitorZooKeeper.zooKeeper, event.getPath());
							processCommit(version,
									appServer + SysProps.VERSION,
									ZK_WATCHER_BASE);

						} else if (event.getType().equals(
								EventType.NodeDataChanged)
								&& event.getPath() != null
								&& checkPath(event.getPath(), zkBizVersion)) {
							startListenZkBiz();
							String version = ZooKeeperUtils.getZkNode(
									MonitorZooKeeper.zooKeeper, event.getPath());
							processCommit(version, appServer
									+ SysProps.BIZ_VERSION, ZK_WATCHER_BIZ);

						}

					}

				});
		return zk;
	}

	/**
	 * 启动监听
	 */
	private void startListenZk() {
		startListenZkBase();
		startListenZkBiz();
	}

	/**
	 * 启动监听
	 */
	private void startListenZkBase() {
		ZooKeeperUtils.checkZKNodeIsExist(zooKeeper, zkVersion, true);
	}

	/**
	 * 启动监听
	 */
	private void startListenZkBiz() {
		ZooKeeperUtils.checkZKNodeIsExist(zooKeeper, zkBizVersion, true);
	}

	/**
	 * BIZ版本信息初始化
	 */
	protected static void setBizVersion() {
		ZooKeeperHolder.biz.set(true);
		ZooKeeperHolder.use.set(true);
		try {
			if (zooKeeper == null
					|| zooKeeper.exists(appBizVersion, false) == null) {
				ZooKeeperHolder.biz.set(false);
				return;
			}
			zooKeeper.setData(appServer + SysProps.BIZ_VERSION,
					zooKeeper.getData(appBizVersion, false, null), -1);
		} catch (Exception e) {
			throw new SetZooKeeperNodeException(e.getMessage(), e);
		}

	}

	/**
	 * 检测路径
	 * 
	 * @param path
	 * @return
	 */
	private boolean checkPath(String path, String compare) {
		if (path.indexOf(compare) != -1) {
			return true;
		}
		return false;
	}

}
