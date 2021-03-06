package autonavi.online.framework.sharding.shards;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.esotericsoftware.minlog.Log;

import autonavi.online.framework.annotation.Author;
import autonavi.online.framework.annotation.sharding.Delete;
import autonavi.online.framework.annotation.sharding.Insert;
import autonavi.online.framework.annotation.sharding.Select;
import autonavi.online.framework.annotation.sharding.Select.Paging;
import autonavi.online.framework.annotation.sharding.Shard;
import autonavi.online.framework.annotation.sharding.SingleDataSource;
import autonavi.online.framework.annotation.sharding.SqlParameter;
import autonavi.online.framework.annotation.sharding.Update;
import autonavi.online.framework.cc.Miscellaneous;
import autonavi.online.framework.commons.bean.PropertyUtils;
import autonavi.online.framework.exception.CoreException;
import autonavi.online.framework.jdbc.dao.DaoEntity;
import autonavi.online.framework.jdbc.dao.ReservedWord;
import autonavi.online.framework.jdbc.dao.TableOperation;
import autonavi.online.framework.jdbc.datasource.ResultSetCallback;
import autonavi.online.framework.sharding.index.ShardingHandle;
import autonavi.online.framework.sharding.index.ShardingHandleSupport;
import autonavi.online.framework.util.StopWatchLogger;

public class InitParameters4Aspect {

	protected DaoEntity getDaoEntity(ProceedingJoinPoint proceedingJoinPoint,
			TableOperation tableOperation) throws Throwable {
		DaoEntity daoEntity = new DaoEntity();

		StopWatchLogger swlogger = new StopWatchLogger(this.getClass());// 打印耗时日志
		swlogger.start("DaoAspectEntity");
		// Class<?> clazz = proceedingJoinPoint.getTarget().getClass();// 目录类

		swlogger.stop();
		Method method = this.getMethod(proceedingJoinPoint);// 得到目标方法
		Annotation[][] allParameterAnnotations = method
				.getParameterAnnotations();// 得到入参注解二维数组
		Object[] parameters = proceedingJoinPoint.getArgs();// 得到入参
		/**
		 * 循环所有参数得到供SQL使用的参数
		 */
		Map<String, Object> parameterMap = this.initParameterMap(method,
				allParameterAnnotations, parameters, daoEntity);
		daoEntity.setParameterMap(parameterMap);

		// sql = proceedingJoinPoint.proceed().toString();// 得到的SQL返回
		String sql = (String) method.invoke(proceedingJoinPoint.getTarget(),
				parameters);// 得到的SQL返回
		daoEntity.setSql(sql);

		String author = this.checkAuthor(method);// 校验是否写了用户
		daoEntity.setAuthor(author);

		// 分片算法实现
		Class<?> handle = this.checkShard(method);
		if (!ShardingHandle.class.isAssignableFrom(handle)) {
			throw new RuntimeException("Shard注解中的类必须实现ShardingHandle接口");
		}
		daoEntity.setShardingHandle((ShardingHandle) handle.newInstance());

		int singleDataSourceKey = this
				.getSingleDataSource(method, parameterMap);// 获得单数据源key
		daoEntity.setSingleDataSourceKey(singleDataSourceKey);

		this.initIndex(tableOperation, method, daoEntity);// 得到类上的indexName和indexColumn注解
		this.getPaging(method, daoEntity);// 分页注解

		boolean returnOne = this.getReturnOne(method);// 是否返回一条
		daoEntity.setReturnOne(returnOne);

		Class<?> resultBean = this.getResultType(method);// 查询时的返回类型
		daoEntity.setResultType(resultBean);

		if (daoEntity.getCallback() == null) {
			Class<?> callbackClass = this.getCallBackClass(method);// 支持回调处理查询结果
			if (callbackClass != null && !callbackClass.isInterface())
				daoEntity.setCallback((ResultSetCallback<?>) callbackClass
						.newInstance());
		}

		boolean queryCount = this.isQueryCount(method);// 是否查询行数
		daoEntity.setQueryCount(queryCount);

		swlogger.stop();
		swlogger.writeLog();
		return daoEntity;
	}

	private Method getMethod(ProceedingJoinPoint proceedingJoinPoint)
			throws Throwable {
		MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint
				.getSignature();
		Method m = methodSignature.getMethod();
		if (m.getDeclaringClass().isInterface()) {
			m = proceedingJoinPoint.getTarget().getClass()
					.getMethod(m.getName(), m.getParameterTypes());
		}
		return m;
	}

	/**
	 * 初始化入参MAP
	 * 
	 * @param method
	 * 
	 * @param allParameterAnnotations
	 * @return
	 * @throws IOException
	 */
	private Map<String, Object> initParameterMap(Method method,
			Annotation[][] allParameterAnnotations, Object[] parameters,
			DaoEntity daoEntity) throws IOException {
		Map<String, Object> parameterMap = new HashMap<String, Object>();// 存放所有可能的参数

		for (int i = 0; i < parameters.length; i++) {
			Annotation[] _parameterAnnotations = allParameterAnnotations[i];
			if (_parameterAnnotations.length == 0) {// 没有注解代表普通参数，跳过
				if (parameters[i] != null
						&& ResultSetCallback.class
								.isAssignableFrom(parameters[i].getClass())) {
					// 设置ResultSetCallback实现 注意判断是否已经设置过
					if (daoEntity.getCallback() != null) {
						throw new RuntimeException("只能传入一个Callback实现类");
					}
					daoEntity.setCallback((ResultSetCallback<?>) parameters[i]);
				}
				continue;
			}
			for (Annotation _parameterAnnotation : _parameterAnnotations) {
				if (_parameterAnnotation instanceof SqlParameter) {// 如果有@SqlParameter注解，就放入MAP中供调用
					SqlParameter _sqlParameter = (SqlParameter) _parameterAnnotation;
					parameterMap.put(_sqlParameter.value(), parameters[i]);// 放入MAP中待用
				}
			}
		}

		// if (allParameterAnnotations.length > 0) {
		// for (int i = 0; i < allParameterAnnotations.length; i++) {
		// Annotation[] _parameterAnnotations = allParameterAnnotations[i];
		// if (_parameterAnnotations.length == 0) {// 没有注解代表普通参数，跳过
		// continue;
		// }
		// for (Annotation _parameterAnnotation : _parameterAnnotations) {
		// if (_parameterAnnotation instanceof SqlParameter) {//
		// 如果有@SqlParameter注解，就放入MAP中供调用
		// SqlParameter _sqlParameter = (SqlParameter) _parameterAnnotation;
		// parameterMap.put(_sqlParameter.value(), parameters[i]);// 放入MAP中待用
		// }
		// }
		// }
		// }
		/*
		 * if (method.getParameterTypes().length > 0 && parameterMap.size() ==
		 * 0) {// 如果一个注解也没有，直接取字段名 String[] parameterNames = MethodUtils
		 * .getMethodParamNames4Asm(method); for (int i = 0; i <
		 * parameterNames.length; i++) { parameterMap.put(parameterNames[i],
		 * parameters[i]);// 放入MAP中待用 } }
		 */
		return parameterMap;
	}

	/**
	 * 获取分片算法
	 * 
	 * @param method
	 * @return
	 * @throws CoreException
	 */
	private Class<?> checkShard(Method method) throws CoreException {
		Shard handle = method.getAnnotation(Shard.class);
		if (handle == null) {
			return ShardingHandleSupport.class;
		}
		return handle.handle();
	}

	/**
	 * 校验是否写了用户
	 * 
	 * @param method
	 * @return
	 * @throws CoreException
	 */
	private String checkAuthor(Method method) throws CoreException {
		Author author = method.getAnnotation(Author.class);
		if (author == null) {
			throw new CoreException("必须填写作者名称! DAO名["
					+ method.getDeclaringClass().getName() + "]");
		}
		Pattern pattern = Pattern.compile("([a-zA-Z0-9_.])+");
		Matcher matcher = pattern.matcher(author.value());
		if(!matcher.find()){
			throw new CoreException("必须填写作者名称! DAO名["
					+ method.getDeclaringClass().getName() + "]");
		}
		return author.value();
	}

	/**
	 * 获取唯一DS的KEY
	 * 
	 * @param method
	 *            代理方法
	 * @param parameters
	 *            接口入参
	 * @return
	 * @throws CoreException
	 */
	protected int getSingleDataSource(Method method,
			Map<String, Object> parameterMap) throws CoreException {
		int singleDataSourceKey = -1;
		SingleDataSource singleDataSource = method
				.getAnnotation(SingleDataSource.class);
		if (singleDataSource != null) {
			int key = singleDataSource.value();
			String keyName = singleDataSource.keyName();
			if (key > 0) {
				singleDataSourceKey = key;
			} else if (keyName != null && !keyName.equals("")) {
				if (keyName.indexOf(ReservedWord.index) > -1
						|| keyName.indexOf(ReservedWord.snowflake) > -1) {
					throw new CoreException("@SingleDataSource的参数中不允许出现"
							+ ReservedWord.index + "和" + ReservedWord.snowflake
							+ "系统关键字");
				}
				try {
					singleDataSourceKey = (Integer) PropertyUtils.getValue(
							parameterMap, keyName);
				} catch (Exception e) {
					Log.error(e.getMessage(), e);
					throw new CoreException(
							"在@SingleDataSource中获取keyName对应值时失败，请检查入参是否为空或为非int外的数据类型。");
				}
			} else {
				throw new CoreException(
						"在@SingleDataSource中的value与keyName至少需要填写一项。");
			}
			if (!Miscellaneous.isInDsKeyInterval(singleDataSourceKey)) {
				throw new CoreException("数据源Key超出取值范围，只能为"
						+ Miscellaneous.minDsKey + "到" + Miscellaneous.maxDsKey
						+ "的整数");
			}
		}
		return singleDataSourceKey;
	}

	/**
	 * 得到类上的indexName和indexColumn注解
	 * 
	 * @param tableOperation
	 * @param method
	 * @return
	 * @throws NoSuchMethodException
	 * @throws InvocationTargetException
	 * @throws IllegalAccessException
	 */
	private void initIndex(TableOperation tableOperation, Method method,
			DaoEntity daoEntity) throws Exception {
		String indexName = "";
		String[] indexColumn = {};
		if (tableOperation == TableOperation.Select) {// 如果是Select
			Select selectAnnotation = method.getAnnotation(Select.class);
			indexName = selectAnnotation.indexName();
			indexColumn = selectAnnotation.indexColumn().split(",");
		} else if (tableOperation == TableOperation.Update) {// 如果是Update
			Update updateAnnotation = method.getAnnotation(Update.class);
			indexName = updateAnnotation.indexName();
			indexColumn = updateAnnotation.indexColumn().split(",");
		} else if (tableOperation == TableOperation.Insert) {// 如果是Insert
			Insert insertAnnotation = method.getAnnotation(Insert.class);
			indexName = insertAnnotation.indexName();
			indexColumn = insertAnnotation.indexColumn().split(",");
			// indexColumnValue = PropertyUtils.getProperty(parameterMap,
			// insertAnnotation.indexColumn());
			// indexColumnValue = Ognl.getValue(insertAnnotation.indexColumn(),
			// parameterMap);
		} else if (tableOperation == TableOperation.Delete) {// 如果是Delete
			Delete deleteAnnotation = method.getAnnotation(Delete.class);
			indexName = deleteAnnotation.indexName();
			indexColumn = deleteAnnotation.indexColumn().split(",");
		}
		daoEntity.setIndexName(indexName);
		daoEntity.setIndexColumn(indexColumn);
	}

	/**
	 * 得到类上的Paging注解
	 * 
	 * @param method
	 * @return
	 */
	private boolean isQueryCount(Method method) {
		Select selectAnnotation = method.getAnnotation(Select.class);
		boolean queryCount = false;
		if (selectAnnotation != null) {
			queryCount = selectAnnotation.queryCount();
		}
		return queryCount;
	}

	/**
	 * 得到类上的Paging注解
	 * 
	 * @param method
	 * @return
	 */
	private void getPaging(Method method, DaoEntity daoEntity) {
		Paging paging = null;
		Select selectAnnotation = method.getAnnotation(Select.class);
		if (selectAnnotation != null) {
			paging = selectAnnotation.paging();
		}

		if (null != paging) {
			// this.startOrSkip = paging.startOrSkip();
			daoEntity.setStartOrSkip(paging.skip());
			// this.endOrRowSize = paging.endOrRowSize();
			daoEntity.setEndOrRowSize(paging.size());
		}
	}

	/**
	 * 得到返回类型
	 * 
	 * @param method
	 * @return
	 */
	private Class<?> getResultType(Method method) {
		Class<?> resultType = null;
		Select selectAnnotation = method.getAnnotation(Select.class);
		if (selectAnnotation != null)
			resultType = selectAnnotation.resultType();
		return resultType;
	}

	/**
	 * 得到是否只返回单条记录
	 * 
	 * @param method
	 * @return
	 */
	private boolean getReturnOne(Method method) {
		boolean returnOne = false;
		Select selectAnnotation = method.getAnnotation(Select.class);
		if (selectAnnotation != null)
			returnOne = selectAnnotation.returnOne();
		return returnOne;
	}

	/**
	 * 得到CallBack类
	 * 
	 * @param method
	 * @return
	 */
	private Class<?> getCallBackClass(Method method) {
		Class<?> callBackClass = null;
		Select selectAnnotation = method.getAnnotation(Select.class);
		if (selectAnnotation != null)
			callBackClass = selectAnnotation.callbackClass();
		return callBackClass == null ? ResultSetCallback.class
				: (ResultSetCallback.class.isAssignableFrom(callBackClass) ? callBackClass
						: ResultSetCallback.class);
	}

}
