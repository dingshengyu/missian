package com.missian.client.async.message;

import com.missian.common.io.MissianMessage;
import java.io.InputStream;

public class AsyncClientResponse extends MissianMessage {
	private InputStream inputStream;
	private String methodName;

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}
}