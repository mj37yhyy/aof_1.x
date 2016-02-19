package autonavi.online.framework.support.config;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;

import autonavi.online.framework.config.ISupportConfig;
import autonavi.online.framework.jdbc.dao.DaoSupport;
import autonavi.online.framework.support.init.ShardIndexInit;

public class ShardIndexInitConfig implements ISupportConfig {

	@Override
	public void processSupportConfig(
			BeanDefinitionRegistry beanDefinitionRegistry) throws Exception {
		RootBeanDefinition shardIndexInitBeanDefinition = new RootBeanDefinition(
				ShardIndexInit.class);
		String[] beanName=beanDefinitionRegistry.getBeanDefinitionNames();
		for(String _beanName:beanName){
			BeanDefinition bean=beanDefinitionRegistry.getBeanDefinition(_beanName);
		    ClassLoader loader=Thread.currentThread().getContextClassLoader();
		    Class<?> clazz=loader.loadClass(bean.getBeanClassName());
		    if(DaoSupport.class.isAssignableFrom(clazz)){
		    	RuntimeBeanReference ref=new RuntimeBeanReference(_beanName);
		    	shardIndexInitBeanDefinition.getPropertyValues().add("daoSupport", ref);
		    	break;
		    }
		    
		}
		beanDefinitionRegistry.registerBeanDefinition(ShardIndexInit.class.getName(), shardIndexInitBeanDefinition);

	}

}
