package autonavi.online.framework.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class ShardNamespaceHandler extends NamespaceHandlerSupport {

	@Override
	public void init() {

//		registerBeanDefinitionParser("advice",
//				new ShardAdviceBeanDefinitionParser());
		registerBeanDefinitionParser("dao",
				new ShardDaoBeanDefinitionParser());
	}

}
