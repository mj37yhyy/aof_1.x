package autonavi.online.framework.annotation.sharding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import autonavi.online.framework.jdbc.datasource.ResultSetCallback;

/**
 * 作者：姬昂 2014年3月14日 说明：
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Select {
	@SuppressWarnings("rawtypes")
	Class callbackClass() default ResultSetCallback.class;

	/**
	 * 索引名
	 * 
	 * @return
	 */
	String indexName();

	/**
	 * 索引列
	 * 
	 * @return
	 */
	String indexColumn();

	/**
	 * 分页
	 * 
	 * @return
	 */
	Paging paging() default @Paging;

	/**
	 * 返回类型，默认为Map
	 * 
	 * @return
	 */
	Class<?> resultType() default Map.class;

	/**
	 * 是否返回一条
	 * 
	 * @return
	 */
	boolean returnOne() default false;

	/**
	 * 是否查询总行数
	 * 
	 * @return
	 */
	boolean queryCount() default false;

	/**
	 * 是否缓存
	 * 
	 * @return
	 */
	Cache cache() default @Cache;

	/**
	 * 分页注解
	 * 
	 * @author jia.miao
	 * 
	 */
	public @interface Paging {
		String skip() default "";

		String size() default "";
	}

	/**
	 * 分页注解
	 * 
	 * @author jia.miao
	 * 
	 */
	public @interface Cache {
		int timeOut() default 1000;
	}
}