package autonavi.online.framework.annotation.sharding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Sharding {
	/**
	 * 索引字段
	 * 
	 * @return
	 */
	Column[] indexs();

	/**
	 * 索引表的名字
	 * 
	 * @return
	 */
	String tableName();

	@Retention(value = RetentionPolicy.RUNTIME)
	@Target(value = ElementType.METHOD)
	public @interface Column {
		/**
		 * 字段名称
		 * 
		 * @return
		 */
		String value();

		/**
		 * 字段类型
		 * 
		 * @return
		 */
		String columnType() default "string";

		/**
		 * 字段长度
		 * 
		 * @return
		 */
		int length() default 300;
	}
}
