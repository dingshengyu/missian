package com.missian.server.codec;

import com.missian.common.io.MissianMessage;
import java.io.InputStream;

public class MissianRequest extends MissianMessage {
	private InputStream inputStream;
	private boolean async;

	public boolean isAsync() {
		return this.async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public InputStream getInputStream() {
		return this.inputStream;
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}
}