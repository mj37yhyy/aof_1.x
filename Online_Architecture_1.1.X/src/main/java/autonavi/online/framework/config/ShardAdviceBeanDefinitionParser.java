package autonavi.online.framework.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.aop.aspectj.annotation.AnnotationAwareAspectJAutoProxyCreator;
import org.springframework.aop.aspectj.autoproxy.AspectJAwareAdvisorAutoProxyCreator;
import org.springframework.aop.framework.autoproxy.InfrastructureAdvisorAutoProxyCreator;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Ordered;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

import autonavi.online.framework.exception.DataSourceKeyOutOfRangeException;
import autonavi.online.framework.exception.ExistingDataSourceKeyException;
import autonavi.online.framework.jdbc.dao.DaoSupport;
import autonavi.online.framework.jdbc.dao.SqlHelper;
import autonavi.online.framework.jdbc.datasource.DataSourceRoute;
import autonavi.online.framework.jdbc.datasource.DynamicDataSource;
import autonavi.online.framework.metadata.entity.ColumnAttribute;
import autonavi.online.framework.sharding.index.CreateIndexTables;
import autonavi.online.framework.sharding.index.SegmentTable;
import autonavi.online.framework.sharding.index.ShardingIndex;
import autonavi.online.framework.sharding.shards.DaoAspect;

public class ShardAdviceBeanDefinitionParser extends
		AbstractSingleBeanDefinitionParser {
	private static final String AUTO_PROXY_CREATOR_BEAN_NAME = "org.springframework.aop.config.internalAutoProxyCreator";

	private static final String SHARDS_ELEMENT = "shards";
	private static final String DATASOURCES_ELEMENT = "datasources";
	private static final String DATASOURCE_ELEMENT = "datasource";
	private static final String SEGMENT_TABLES_ELEMENT = "segment-tables";
	private static final String SEGMENT_TABLE_ELEMENT = "segment-table";

	private static final String KEY_ATTRIBUTE = "key";
	private static final String REF_ATTRIBUTE = "ref";
	private static final String INDEX_ELEMENT = "index";
	private static final String CACHE_ATTRIBUTE = "cache";

	private static final String TABLE_ELEMENT = "table";
	private static final String NAME_ATTRIBUTE = "name";
	private static final String COUNT_ATTRIBUTE = "count";
	private static final String COLUMN_ELEMENT = "column";
	private static final String TYPE_ATTRIBUTE = "type";
	private static final String lENGTH_ATTRIBUTE = "length";

	private static final int minDsKey = 1;
	private static final int maxDsKey = 1024;

	/**
	 * Stores the auto proxy creator classes in escalation order.
	 */
	private static final List<Class<?>> APC_PRIORITY_LIST = new ArrayList<Class<?>>();

	/**
	 * Setup the escalation list.
	 */
	static {
		APC_PRIORITY_LIST.add(InfrastructureAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AspectJAwareAdvisorAutoProxyCreator.class);
		APC_PRIORITY_LIST.add(AnnotationAwareAspectJAutoProxyCreator.class);
	}

	// TypeConverter converter = new SimpleTypeConverter();

	@Override
	protected Class<?> getBeanClass(Element element) {
		return DaoAspect.class;
	}

	protected void doParse(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {

		// 生成AnnotationAwareAspectJAutoProxyCreator的bean
		registerOrEscalateApcAsRequired(parserContext.getRegistry());

		/**
		 * 生成路由DataSourceRoute的RootBeanDefinition
		 */
		RootBeanDefinition dataSourceRouteBeanDefinition = new RootBeanDefinition(
				DataSourceRoute.class);
		/**
		 * 解析datasources部分，生成DynamicDataSource的RootBeanDefinition。
		 * 并将多数据源注册到路由DataSourceRoute
		 */
		Element shardsEle = DomUtils.getChildElementByTagName(element,
				SHARDS_ELEMENT);
		RootBeanDefinition dynamicDataSourceBeanDefinition = null;
		try {
			dynamicDataSourceBeanDefinition = this.parseDataSources(shardsEle,
					parserContext, builder);
		} catch (Exception e) {// 发生异常，直接中断启动进程
			e.printStackTrace();
			System.exit(0);
		}
		dataSourceRouteBeanDefinition.getPropertyValues().add(
				"dynamicDataSource", dynamicDataSourceBeanDefinition);

		/**
		 * 解析segment-tables部分，生成segment-table的List
		 */
		ManagedList<SegmentTable> segmentTables = this.parseSegmentTables(
				shardsEle, parserContext, builder);

		/**
		 * 解析index部分,生成shardingIndex
		 */
		Element indexEle = DomUtils.getChildElementByTagName(element,
				INDEX_ELEMENT);
		RootBeanDefinition shardingIndexBeanDefinition = this.parseIndex(
				indexEle, parserContext, builder);
		/**
		 * 将shardingIndex注册进路由DataSourceRoute
		 */
		dataSourceRouteBeanDefinition.getPropertyValues()
				.add("shardingIndex", shardingIndexBeanDefinition)
				.add("segmentTables", segmentTables);// 将segment-table的List注入供DataSourceRoute使用

		/**
		 * 初始化SqlHelper，将路由DataSourceRoute注入
		 */
		RootBeanDefinition sqlHelperBeanDefinition = new RootBeanDefinition(
				SqlHelper.class);
		sqlHelperBeanDefinition.getPropertyValues().add("dataSourceRoute",
				dataSourceRouteBeanDefinition);

		/**
		 * 初始化daoSupport，将sqlHelper注入
		 */
		RootBeanDefinition daoSupportBeanDefinition = new RootBeanDefinition(
				DaoSupport.class);
		daoSupportBeanDefinition.getPropertyValues().add("sqlHelper",
				sqlHelperBeanDefinition);

		/**
		 * 将SqlHelp注册进daoAspect
		 */
		builder.addPropertyValue("daoSupport", daoSupportBeanDefinition);

		/**
		 * 将daoSupport注册进容器，供其它地方使用
		 */
		parserContext.getRegistry().registerBeanDefinition("daoSupport",
				daoSupportBeanDefinition);

	}

	/**
	 * 生成AnnotationAwareAspectJAutoProxyCreator的bean
	 * 
	 * @param registry
	 * @return
	 */
	private BeanDefinition registerOrEscalateApcAsRequired(
			BeanDefinitionRegistry registry) {
		Class<?> cls = AnnotationAwareAspectJAutoProxyCreator.class;
		if (registry.containsBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME)) {
			BeanDefinition apcDefinition = registry
					.getBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME);
			if (!cls.getName().equals(apcDefinition.getBeanClassName())) {
				int currentPriority = findPriorityForClass(apcDefinition
						.getBeanClassName());
				int requiredPriority = findPriorityForClass(cls);
				if (currentPriority < requiredPriority) {
					apcDefinition.setBeanClassName(cls.getName());
				}
			}
			return null;
		}
		RootBeanDefinition beanDefinition = new RootBeanDefinition(cls);
		beanDefinition.getPropertyValues().add("order",
				Ordered.HIGHEST_PRECEDENCE);
		beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
		registry.registerBeanDefinition(AUTO_PROXY_CREATOR_BEAN_NAME,
				beanDefinition);
		return beanDefinition;
	}

	private int findPriorityForClass(Class<?> clazz) {
		return APC_PRIORITY_LIST.indexOf(clazz);
	}

	private int findPriorityForClass(String className) {
		for (int i = 0; i < APC_PRIORITY_LIST.size(); i++) {
			Class<?> clazz = APC_PRIORITY_LIST.get(i);
			if (clazz.getName().equals(className)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Class name [" + className
				+ "] is not a known auto-proxy creator class");
	}

	/**
	 * 解析datasources部分
	 * 
	 * @param dataSources
	 * @param parserContext
	 * @param builder
	 * @throws ExistingDataSourceKeyException
	 * @throws DataSourceKeyOutOfRangeException
	 */
	private RootBeanDefinition parseDataSources(Element shardsEle,
			ParserContext parserContext, BeanDefinitionBuilder builder)
			throws ExistingDataSourceKeyException,
			DataSourceKeyOutOfRangeException {
		Element dataSourcesEle = DomUtils.getChildElementByTagName(shardsEle,
				DATASOURCES_ELEMENT);
		List<Element> dataSource = DomUtils.getChildElementsByTagName(
				dataSourcesEle, DATASOURCE_ELEMENT);

		ManagedMap<Integer, Object> targetDataSources = new ManagedMap<Integer, Object>(
				dataSource.size());

		for (Element entryEle : dataSource) {
			Integer key = Integer.valueOf(entryEle.getAttribute(KEY_ATTRIBUTE));
			if (targetDataSources.containsKey(key))
				throw new ExistingDataSourceKeyException("存在重复的数据源key，请检查配置");
			if (key < minDsKey || key > maxDsKey) {
				throw new DataSourceKeyOutOfRangeException(
						"数据源Key超出取值范围，只能为1到1024的整数");
			}
			// 如果是ref，创建一个ref的数据对象RuntimeBeanReference，这个对象封装了ref的信息。
			RuntimeBeanReference ref = new RuntimeBeanReference(
					entryEle.getAttribute(REF_ATTRIBUTE));
			ref.setSource(parserContext.extractSource(entryEle));
			targetDataSources.put(key, ref);
		}

		RootBeanDefinition dynamicDataSourceBeanDefinition = new RootBeanDefinition(
				DynamicDataSource.class);
		dynamicDataSourceBeanDefinition.getPropertyValues().add(
				"targetDataSources", targetDataSources);
		return dynamicDataSourceBeanDefinition;
	}

	/**
	 * 解析segment-tables部分
	 * 
	 * @param shardsEle
	 * @param parserContext
	 * @param builder
	 */
	private ManagedList<SegmentTable> parseSegmentTables(Element shardsEle,
			ParserContext parserContext, BeanDefinitionBuilder builder) {
		ManagedList<SegmentTable> segmentTables = new ManagedList<SegmentTable>();
		Element segmentTablesEle = DomUtils.getChildElementByTagName(shardsEle,
				SEGMENT_TABLES_ELEMENT);
		List<Element> segmentTableEleList = DomUtils.getChildElementsByTagName(
				segmentTablesEle, SEGMENT_TABLE_ELEMENT);
		for (Element segmentTableEle : segmentTableEleList) {
			SegmentTable segmentTable = new SegmentTable();
			segmentTable.setName(segmentTableEle.getAttribute(NAME_ATTRIBUTE));
			String countStr = segmentTableEle.getAttribute(COUNT_ATTRIBUTE);
			if (countStr != null && !countStr.isEmpty())
				segmentTable.setCount(Integer.valueOf(countStr));
			segmentTables.add(segmentTable);
		}
		return segmentTables;
	}

	/**
	 * 解析index部分
	 * 
	 * @param indexEle
	 * @param parserContext
	 * @param builder
	 * @return
	 */
	private RootBeanDefinition parseIndex(Element indexEle,
			ParserContext parserContext, BeanDefinitionBuilder builder) {
		String cache, tableName, columnName, columnType, columnLength;
		/**
		 * 得到cache配置
		 */
		cache = indexEle.getAttribute(CACHE_ATTRIBUTE);
		/**
		 * 得到索引表的配置
		 */
		Map<String, List<ColumnAttribute>> indexTableMap = new HashMap<String, List<ColumnAttribute>>();
		List<Element> tableEles = DomUtils.getChildElementsByTagName(indexEle,
				TABLE_ELEMENT);
		for (Element _tableEle : tableEles) {
			// 得到表名
			tableName = _tableEle.getAttribute(NAME_ATTRIBUTE);

			List<ColumnAttribute> columnAttributeList = new ArrayList<ColumnAttribute>();
			// 得到column元素，开始组装ColumnAttributeList
			for (Element columnEle : DomUtils.getChildElementsByTagName(
					_tableEle, COLUMN_ELEMENT)) {
				columnName = columnEle.getAttribute(NAME_ATTRIBUTE);// 得到字段名
				columnType = columnEle.getAttribute(TYPE_ATTRIBUTE);// 得到字段类型
				columnLength = columnEle.getAttribute(lENGTH_ATTRIBUTE);// 得到字段长度

				// 开始组装ColumnAttributeList
				ColumnAttribute columnAttribute = new ColumnAttribute();

				columnAttribute.setName(columnName);
				columnAttribute.setColumnName(columnName);
				columnAttribute.setColumnType(columnType);
				columnAttribute.setLength(Integer.valueOf(columnLength));
				columnAttributeList.add(columnAttribute);
			}

			indexTableMap.put(tableName, columnAttributeList);
		}

		/**
		 * 得到索datasource的配置
		 */
		Element dsEle = DomUtils.getChildElementByTagName(indexEle,
				DATASOURCE_ELEMENT);

		// 如果是ref，创建一个ref的数据对象RuntimeBeanReference，这个对象封装了ref的信息。
		RuntimeBeanReference dsRef = new RuntimeBeanReference(
				dsEle.getAttribute(REF_ATTRIBUTE));
		dsRef.setSource(parserContext.extractSource(dsEle));

		/**
		 * 创建并注册CreateIndexTables
		 */
		RootBeanDefinition createIndexTablesDefinition = new RootBeanDefinition(
				CreateIndexTables.class);
		ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();// 构造函数
		constructorArgumentValues.addIndexedArgumentValue(0, dsRef);// 第一个参数为DS
		constructorArgumentValues.addIndexedArgumentValue(1, indexTableMap);// 第二个参数为Map<String,
																			// List<ColumnAttribute>>
		createIndexTablesDefinition
				.setConstructorArgumentValues(constructorArgumentValues);// 插入构造函数

		// 将CreateIndexTables注册到容器
		parserContext.getRegistry().registerBeanDefinition("createIndexTables",
				createIndexTablesDefinition);

		/**
		 * 创建并注册ShardingIndex
		 */
		RootBeanDefinition shardingIndexBeanDefinition = new RootBeanDefinition(
				ShardingIndex.class);
		shardingIndexBeanDefinition.getPropertyValues()
				.add("dataSource", dsRef).add("cache", cache)
				.add("indexTableMap", indexTableMap);

		return shardingIndexBeanDefinition;
	}

}
