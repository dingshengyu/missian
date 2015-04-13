package com.missian.client.async.message;

import com.missian.common.io.MissianMessage;
import org.apache.mina.core.buffer.IoBuffer;

public class AsyncClientRequest extends MissianMessage {
	private IoBuffer outputBuffer;
	private String host;
	private int port;

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return this.port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public IoBuffer getOutputBuffer() {
		return this.outputBuffer;
	}

	public void setOutputBuffer(IoBuffer outputBuffer) {
		this.outputBuffer = outputBuffer;
	}
}