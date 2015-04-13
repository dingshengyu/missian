package com.missian.common.io;

import java.io.IOException;
import java.io.OutputStream;
import org.apache.mina.core.buffer.IoBuffer;

public class IoBufferOutputStream extends OutputStream {
	private static final int DEFAULT_SIZE = 1024;
	private IoBuffer outputBuffer;

	public IoBufferOutputStream() {
		this(DEFAULT_SIZE);
	}

	public IoBufferOutputStream(int initBufferSize) {
		this.outputBuffer = IoBuffer.allocate(initBufferSize);
		this.outputBuffer.setAutoExpand(true);
	}

	public IoBufferOutputStream(IoBuffer outputBuffer) {
		this.outputBuffer = outputBuffer;
	}

	public void write(int b) throws IOException {
		this.outputBuffer.put((byte) b);
	}

	public void write(byte[] b, int off, int len) throws IOException {
		this.outputBuffer.put(b, off, len);
	}

	public void write(byte[] b) throws IOException {
		this.outputBuffer.put(b);
	}

	public IoBuffer flip() {
		this.outputBuffer.flip();
		return this.outputBuffer;
	}
}