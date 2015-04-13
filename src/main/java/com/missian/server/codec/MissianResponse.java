package com.missian.server.codec;

import com.missian.common.io.MissianMessage;
import org.apache.mina.core.buffer.IoBuffer;

public class MissianResponse extends MissianMessage {
	private IoBuffer outputBuffer;
	private String methodName;
	private boolean async;

	public boolean isAsync() {
		return this.async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public String getMethodName() {
		return this.methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public IoBuffer getOutputBuffer() {
		return this.outputBuffer;
	}

	public void setOutputBuffer(IoBuffer outputBuffer) {
		this.outputBuffer = outputBuffer;
	}
}