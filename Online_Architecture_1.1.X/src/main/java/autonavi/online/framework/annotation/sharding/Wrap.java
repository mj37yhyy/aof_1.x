package autonavi.online.framework.annotation.sharding;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于包裹在用户SQL的最外层,相当于生成一个子查询。该注解主要是用于在多表的hash查询中进行排序等操作。也可以用做一切想对SQL进行包裹的情况。<br/>
 * 需要使用${sql}标示表式SQL语句，运行时会进行替换。<br/>
 * 假设下面是一个会产生hash查询SQL的方法:<br/>
 * 
 * (a)Select<br/>
 * (a)Surround(value="select a,b from (${sql}) order by a")<br/>
 * public selectTest (){<br/>
 * &nbsp;&nbsp;return "select * from demo";<br/>
 * }<br/>
 * 那么可能会得到下面的SQL:<br/>
 * select a,b from (<br/>
 * &nbsp;&nbsp;select * from demo_1<br/>
 * &nbsp;&nbsp;union all<br/>
 * &nbsp;&nbsp;select * from demo_2<br/>
 * )  order by a<br/>
 * 
 * @author jia.miao
 * 
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@Documented
public @interface Wrap {
	String value();
}
