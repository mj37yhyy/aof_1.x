package autonavi.online.framework.support.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

import autonavi.online.framework.config.ISupportConfig;
import autonavi.online.framework.support.zookeeper.MonitorZooKeeper;
import autonavi.online.framework.support.zookeeper.ZooKeeperProp;

public class MonitorZooKeeperConfig implements ISupportConfig {

	@Override
	public void processSupportConfig(
			BeanDefinitionRegistry beanDefinitionRegistry) throws Exception {
		RootBeanDefinition monitorZooKeeperBeanDefinition = new RootBeanDefinition(
				MonitorZooKeeper.class);
		String[] beanName=beanDefinitionRegistry.getBeanDefinitionNames();
		for(String _beanName:beanName){
			BeanDefinition bean=beanDefinitionRegistry.getBeanDefinition(_beanName);
		    ClassLoader loader=Thread.currentThread().getContextClassLoader();
		    Class<?> clazz=loader.loadClass(bean.getBeanClassName());
		    if(ZooKeeperProp.class.isAssignableFrom(clazz)){
		    	RuntimeBeanReference ref=new RuntimeBeanReference(_beanName);
		    	monitorZooKeeperBeanDefinition.getPropertyValues().add("zooKeeperProp", ref);
		    	break;
		    }
		    
		}
		beanDefinitionRegistry.registerBeanDefinition(MonitorZooKeeper.class.getName(), monitorZooKeeperBeanDefinition);

	}

}
