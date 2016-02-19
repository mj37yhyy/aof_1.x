package autonavi.online.framework.annotation.sharding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 *	作者：姬昂
 *	2014年2月21日
 *	说明：标识当前字段是分区字段的注解
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PartitionColumn {
	String columnName();
}
