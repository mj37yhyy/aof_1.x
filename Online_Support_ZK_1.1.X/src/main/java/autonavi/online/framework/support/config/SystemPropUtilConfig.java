package autonavi.online.framework.support.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

import autonavi.online.framework.config.ISupportConfig;
import autonavi.online.framework.support.zookeeper.ZooKeeperProp;
import autonavi.online.framework.util.SystemPropUtil;

public class SystemPropUtilConfig implements ISupportConfig {

	@Override
	public void processSupportConfig(
			BeanDefinitionRegistry beanDefinitionRegistry) throws Exception {
		RootBeanDefinition systemPropUtilBeanDefinition = new RootBeanDefinition(
				SystemPropUtil.class);
		String[] beanName=beanDefinitionRegistry.getBeanDefinitionNames();
		for(String _beanName:beanName){
			BeanDefinition bean=beanDefinitionRegistry.getBeanDefinition(_beanName);
		    ClassLoader loader=Thread.currentThread().getContextClassLoader();
		    Class<?> clazz=loader.loadClass(bean.getBeanClassName());
		    if(ZooKeeperProp.class.isAssignableFrom(clazz)){
		    	RuntimeBeanReference ref=new RuntimeBeanReference(_beanName);
		    	systemPropUtilBeanDefinition.getPropertyValues().add("zkEntity", ref);
		    	break;
		    }
		    
		}
		beanDefinitionRegistry.registerBeanDefinition(SystemPropUtilConfig.class.getName(), systemPropUtilBeanDefinition);

	}

}
