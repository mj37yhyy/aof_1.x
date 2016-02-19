package autonavi.online.framework.commons.bean;

import static java.util.Locale.ENGLISH;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import autonavi.online.framework.jdbc.dao.ReservedWord;
import net.sf.cglib.beans.BeanCopier;

public class PropertyUtils {
	private static Logger log = LogManager.getLogger(PropertyUtils.class);

	/**
	 * 获取实体中的内容，只允许是Map,List和JavaBean
	 * 
	 * @param root
	 * @param expression
	 * @return
	 * @throws Exception
	 */
	public static Object getValue(Object root, String expression)
			throws Exception {
		String[] eArray = expression.split("\\.");
		Object result = root;
		int i = 0;
		while (i < eArray.length) {
			if (result instanceof Map) {// map
				result = ((Map<?, ?>) result).get(eArray[i]);
			} else if (result instanceof List || result.getClass().isArray()) {// list
				int index = -1;
				String indexString = expression.substring(
						expression.indexOf("[") + 1, expression.indexOf("]"));
				if (indexString != null && !indexString.isEmpty()) {
					index = Integer.valueOf(indexString);
				}
				if (index > -1) {
					if (result instanceof List) {// list
						result = ((List<?>) result).get(index);
					} else if (result.getClass().isArray()) {
						result = ((Object[]) result)[index];
					}
				}

			} else {// bean
				String name = eArray[i];
				String methodName = null;
				if (name.charAt(1) >= 'A' && name.charAt(1) <= 'Z') {
					// 按照javabean规范的一条特殊规定：如果名字的第二个字母为大写，则第一个字母就不用变成大写。
					// 比如eName的getter方法为geteName()；而URL的getter方法为getURL()
					methodName = "get" + name;
				} else {// 如果第二个字母不是大写，则按一般规范将第一个字母大写处理
					methodName = "get"
							+ name.substring(0, 1).toUpperCase(ENGLISH)
							+ name.substring(1);
				}
				Method methodGetter = result.getClass().getMethod(methodName);
				result = methodGetter.invoke(result);
			}
			if (result == null) {
				log.warn("入参可能为空，请检查入参 expression="
						+ expression.replaceAll("\\[[0-9]\\]",
								ReservedWord.index));
			}
			i++;
		}
		return result;
	}

	/**
	 * 复制属性
	 * 
	 * @param srcObj
	 * @param destObj
	 */
	public static void copy(Object srcObj, Object destObj) {
		String key = genKey(srcObj.getClass(), destObj.getClass());
		BeanCopier copier = null;
		if (!BEAN_COPIERS.containsKey(key)) {
			copier = BeanCopier.create(srcObj.getClass(), destObj.getClass(),
					false);
			BEAN_COPIERS.put(key, copier);
		} else {
			copier = BEAN_COPIERS.get(key);
		}
		copier.copy(srcObj, destObj, null);
	}

	private static String genKey(Class<?> srcClazz, Class<?> destClazz) {
		return srcClazz.getName() + destClazz.getName();
	}

	static final Map<String, BeanCopier> BEAN_COPIERS = new HashMap<String, BeanCopier>();
}
