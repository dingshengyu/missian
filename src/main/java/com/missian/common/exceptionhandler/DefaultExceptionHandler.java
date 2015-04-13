package com.missian.common.exceptionhandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultExceptionHandler implements MissianExceptionHandler {
	private Logger logger = LoggerFactory.getLogger(getClass());

	public void onException(Exception e) {
		this.logger.error("Call method error.", e);
	}
}