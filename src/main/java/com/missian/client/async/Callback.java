package com.missian.client.async;

public interface Callback {
	public void onMessageReceived(Object paramObject) throws Exception;

	public void onException(Throwable paramThrowable) throws Exception;

	public Class<?> getAcceptValueType();
}