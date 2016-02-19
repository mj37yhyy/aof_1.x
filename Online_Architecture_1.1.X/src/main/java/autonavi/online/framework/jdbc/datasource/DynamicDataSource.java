package autonavi.online.framework.jdbc.datasource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

public class DynamicDataSource {
	private static int counter = 0;
	private static Map<Integer, DataSource> targetDataSources = Collections
			.synchronizedMap(new HashMap<Integer, DataSource>());
	private static Map<DataSource, PlatformTransactionManager> targetTransactionManagers = Collections
			.synchronizedMap(new HashMap<DataSource, PlatformTransactionManager>());

	private static Set<Integer> keySet = null;
	private static int targetDataSourceCount = 0;
	private static Integer indexDSKey = Integer.MIN_VALUE;

	/**
	 * 获取所有的数据源KEY
	 * 
	 * @return
	 */
	public static Set<Integer> getKeySet() {
		return keySet;
	}

	/**
	 * 返回一个随机数据源的Key
	 * 
	 * @return
	 */
	public static Integer getRandomKey() {
		Random random = new Random();
		Integer[] keys = keySet.toArray(new Integer[0]);
		return keys[random.nextInt(targetDataSourceCount)];
	}

	/**
	 * 序列返回一个数据源的Key。此处进行加锁处理是因为要防止并发情况导致出现分片错误的情况
	 * 
	 * @return
	 */
	public synchronized static Integer getOrderKey() {
		Integer[] keys = keySet.toArray(new Integer[0]);
		int dsKey = 0;
		if (targetDataSourceCount == 1) {
			dsKey = keys[0];
		} else if (targetDataSourceCount > 1) {
			if (counter >= targetDataSourceCount)
				counter = counter % targetDataSourceCount;
			dsKey = keys[counter];
			counter++;
		}
		return dsKey;
	}

	/**
	 * 获取索引片的KEY
	 * 
	 * @return
	 */
	public static Integer getIndexKey() {
		return indexDSKey;
	}

	public void setIndexKey(Integer indexDSKey) {
		DynamicDataSource.indexDSKey = indexDSKey;
	}

	public synchronized void setTargetDataSources(
			Map<Integer, DataSource> _targetDataSources) {
		// 如果重置数据源，则要先清空之前的数据源，然后再重新加入。主要用于在线更新数据源的情况
		// 如果重置事务管理器，则要先清空之前的事务管理器，然后再重新加入。主要用于在线更新数据源的情况
		if (_targetDataSources != null && _targetDataSources.size() > 0) {
			targetDataSources.clear();
			targetTransactionManagers.clear();
			for (Integer _key : _targetDataSources.keySet()) {
				LoadBalancingDataSource ds = (LoadBalancingDataSource) _targetDataSources.get(_key);
				targetDataSources.put(_key, ds);
				setTargetTransactionManagers(ds);
			}
			_targetDataSources = null;
			targetDataSourceCount = targetDataSources.size();
			keySet = targetDataSources.keySet();
		}
	}

	/**
	 * 初始化事务管理器
	 * 
	 * @param ds
	 */
	private synchronized void setTargetTransactionManagers(LoadBalancingDataSource ds) {
		Map<String, DataSource> realMap = ds.getRealDataSourceMap();
		for (String key : realMap.keySet()) {
			DataSource realDataSource = realMap.get(key);
			targetTransactionManagers.put(realDataSource,
					new DataSourceTransactionManager(realDataSource));
		}
	}

	/**
	 * 获取当前的数据源
	 * 
	 * @return
	 */
	protected static DataSource getCurrentDataSource() {
		LoadBalancingDataSource ds = (LoadBalancingDataSource) targetDataSources
				.get(CustomerContextHolder.getCustomerType());
		return ds.getBalancedRealDataSource();
	}

	/**
	 * 获取当前代理的数据源
	 * 
	 * @return
	 */
	protected static LoadBalancingDataSource getCurrentProxyDataSource() {
		LoadBalancingDataSource ds = (LoadBalancingDataSource) targetDataSources
				.get(CustomerContextHolder.getCustomerType());
		return ds;
	}

	/**
	 * 获取当前的事务管理器
	 * 
	 * @return
	 */
	protected static PlatformTransactionManager getTransactionMananger(
			DataSource ds) {
		return targetTransactionManagers.get(ds);
	}
}
