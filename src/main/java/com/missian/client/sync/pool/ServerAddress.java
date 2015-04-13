package com.missian.client.sync.pool;

class ServerAddress {
	private String host;
	private int port;

	public ServerAddress(String host, int port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}
}