package autonavi.online.framework.sharding.dao.xml.builder.support;

import org.w3c.dom.Node;

public interface CodeGeneration {
	public String doGenerator(Class<?> interfaceClass, Node node) throws Exception;
}
