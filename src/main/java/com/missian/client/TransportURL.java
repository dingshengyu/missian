package com.missian.client;

import com.missian.common.io.TransportProtocol;

public class TransportURL {
	private String url;
	private TransportProtocol transport;
	private String host;
	private int port;
	private String query;

	public TransportURL(String url) {
		this.url = url;
		int idx1 = url.indexOf("://");
		if (idx1 <= 0) {
			throw new IllegalArgumentException("Illegal url:" + url);
		}
		int idx2 = url.indexOf('/', idx1 + 4);
		if (idx2 <= 0) {
			throw new IllegalArgumentException("Illegal url:" + url);
		}
		this.transport = TransportProtocol.valueOf(url.substring(0, idx1).toLowerCase());
		this.query = url.substring(idx2 + 1);
		String hostPort = url.substring(idx1 + 3, idx2);
		int idx3 = hostPort.indexOf(':');
		if (idx3 < 0) {
			this.host = hostPort;
			this.port = this.transport.getDefaultPort();
		} else {
			this.host = hostPort.substring(0, idx3);
			this.port = Integer.parseInt(hostPort.substring(idx3 + 1));
		}
	}

	public String getUrl() {
		return this.url;
	}

	public TransportProtocol getTransport() {
		return this.transport;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public String getQuery() {
		return this.query;
	}
}