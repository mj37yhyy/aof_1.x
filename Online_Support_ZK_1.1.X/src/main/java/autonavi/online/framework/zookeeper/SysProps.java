package autonavi.online.framework.zookeeper;

public class SysProps {
	//ZK_MONITOR
	public static final String ZK_MONITOR_ROOT = "/aof_monitor/";

	public static final String UPDATE = "/update";
	public static final String SERVERS = "/servers";
	public static final String VERSION = "/version";
	public static final String VERSION_ZK = "/version_zk";
	
	public static final String PRECOMMIT_FLAG = "_P";
	public static final String COMMIT_FLAG = "_C";
	public static final String TIMESTAMPS="/TIMESTAMPS";
	public static final String BIZ_VERSION = "/biz_version";
	public static final String BIZ_VERSION_ZK = "/biz_version_zk";
	public static final String CHARSET = "utf-8";
	
	//ZK
	public final static String AOF_ROOT="/aof";
	public final static String AOF_PASS="/pass";
	public final static String AOF_APP_BASE="/base";
	public final static String AOF_APP_SHARD="/shard";
	public final static String AOF_APP_INDEX="/index";
	public final static String AOF_APP_TABLES="/tables";
	public final static String AOF_APP_SEG="/segment-tables";
	public final static String AOF_APP_DSS="/dss";
	public final static String AOF_APP_DS="/ds";
	public final static String AOF_APP_BIZ="/biz";
	public final static String AOF_APP_BIZS="/bizs";
	public final static String AOF_APP_BIZS_DEFALUT="/biz_default";
	public final static String AOF_APP_BIZS_VALUE="/value";
	public final static String AOF_APP_BIZS_COMMENTS="/comments";
	
	
	public final static String AOF_INDEX_LENGTH="/length";
	public final static String AOF_INDEX_TYPE="/type";
	public final static String AOF_INDEX_NAME="/name";
	
	public final static String NODE_VALUE="value";
	
	public final static String AOF_TEMP_ROOT="/aof_temp";
	public final static String AOF_MONITOR_ROOT="/aof_monitor";
	
	public final static String JTA_DS = "com.atomikos.jdbc.AtomikosDataSourceBean";
	public final static String C3P0_DS = "com.mchange.v2.c3p0.ComboPooledDataSource";
	
	public final static String PROXY_DS="autonavi.online.framework.jdbc.datasource.LoadBalancingDataSource";
	public final static String STRATEGY_NAME="strategyName";
	public final static String REAl_DSS="realDss";
	

}
