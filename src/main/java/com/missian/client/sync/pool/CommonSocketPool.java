package com.missian.client.sync.pool;

import java.net.Socket;

import org.apache.commons.pool.impl.GenericKeyedObjectPool;

import com.missian.client.NetworkConfig;

public class CommonSocketPool implements SocketPool {
	@SuppressWarnings("rawtypes")
	private GenericKeyedObjectPool pool;
	private NetworkConfig networkConfig;

	public CommonSocketPool() {
		this(new NetworkConfig());
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CommonSocketPool(NetworkConfig networkConfig) {
		this.pool = new GenericKeyedObjectPool(new CommonSocketFactory(networkConfig));
		this.networkConfig = networkConfig;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public CommonSocketPool(NetworkConfig networkConfig, GenericKeyedObjectPool.Config config) {
		this.pool = new GenericKeyedObjectPool(new CommonSocketFactory(networkConfig), config);
	}

	@SuppressWarnings("unchecked")
	public Socket getSocket(String host, int port) throws Exception {
		return (Socket) this.pool.borrowObject(new ServerAddress(host, port));
	}

	@SuppressWarnings("unchecked")
	public void returnSocket(String host, int port, Socket socket) throws Exception {
		this.pool.returnObject(new ServerAddress(host, port), socket);
	}

	public NetworkConfig getNetworkConfig() {
		return this.networkConfig;
	}
}