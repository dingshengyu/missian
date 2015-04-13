package com.missian.common.io;

import java.io.IOException;
import java.io.InputStream;
import org.apache.mina.core.buffer.IoBuffer;

public class IoBufferInputStream extends InputStream {
	private IoBuffer buffer;

	public IoBufferInputStream(IoBuffer buffer) {
		this.buffer = buffer;
	}

	public int read() throws IOException {
		return this.buffer.get();
	}

	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}

	public int read(byte[] b, int off, int len) throws IOException {
		len = Math.min(len, this.buffer.remaining());
		len = Math.min(len, b.length);
		this.buffer.get(b, off, len);
		return len;
	}
}