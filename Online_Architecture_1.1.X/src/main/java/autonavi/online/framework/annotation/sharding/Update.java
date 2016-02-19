package autonavi.online.framework.annotation.sharding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import autonavi.online.framework.annotation.sharding.Sharding.Column;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Update {
	/**
	 * 索引名
	 * @return
	 */
	String indexName();
	/**
	 * 索引列
	 * @return
	 */
	String indexColumn();
	
}
