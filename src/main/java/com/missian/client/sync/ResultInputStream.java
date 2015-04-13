package com.missian.client.sync;

import com.caucho.hessian.io.AbstractHessianInput;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResultInputStream extends InputStream {
	private static final Logger log = Logger.getLogger(ResultInputStream.class.getName());
	private Socket _conn;
	private InputStream _connIs;
	private AbstractHessianInput _in;
	private InputStream _hessianIs;

	ResultInputStream(Socket conn, InputStream is, AbstractHessianInput in, InputStream hessianIs) {
		this._conn = conn;
		this._connIs = is;
		this._in = in;
		this._hessianIs = hessianIs;
	}

	public int read() throws IOException {
		if (this._hessianIs != null) {
			int value = this._hessianIs.read();

			if (value < 0) {
				close();
			}
			return value;
		}
		return -1;
	}

	public int read(byte[] buffer, int offset, int length) throws IOException {
		if (this._hessianIs != null) {
			int value = this._hessianIs.read(buffer, offset, length);

			if (value < 0) {
				close();
			}
			return value;
		}
		return -1;
	}

	public void close() throws IOException {
		Socket conn = this._conn;
		this._conn = null;

		InputStream connIs = this._connIs;
		this._connIs = null;

		AbstractHessianInput in = this._in;
		this._in = null;

		InputStream hessianIs = this._hessianIs;
		this._hessianIs = null;
		try {
			if (hessianIs != null)
				hessianIs.close();
		} catch (Exception e) {
			log.log(Level.FINE, e.toString(), e);
		}
		try {
			if (in != null) {
				in.completeReply();
				in.close();
			}
		} catch (Exception e) {
			log.log(Level.FINE, e.toString(), e);
		}
		try {
			if (connIs != null)
				connIs.close();
		} catch (Exception e) {
			log.log(Level.FINE, e.toString(), e);
		}
		try {
			if (conn != null)
				conn.close();
		} catch (Exception e) {
			log.log(Level.FINE, e.toString(), e);
		}
	}
}