package autonavi.online.framework.annotation.sharding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import autonavi.online.framework.sharding.index.ShardingHandleSupport;
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface Shard {
	Class<?> handle() default ShardingHandleSupport.class;

}
