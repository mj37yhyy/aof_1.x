package autonavi.online.framework.jdbc.dao;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;

public class DaoContextHolder implements ApplicationContextAware {
	private ConfigurableApplicationContext applicationContext;
	private static DaoContextHolder contextHolder=null;
	
	@PostConstruct
	private void init(){
		contextHolder=this;
	}
	protected static DaoContextHolder getInstance(){
		return contextHolder;
	}

	protected ConfigurableApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;

	}

}
