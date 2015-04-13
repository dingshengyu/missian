package com.missian.common.beanlocate;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class SpringLocator implements BeanLocator, ApplicationContextAware {
	private ApplicationContext applicationContext;

	public Object lookup(String beanName) {
		return this.applicationContext.getBean(beanName);
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
}