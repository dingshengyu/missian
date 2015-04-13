package com.missian.client.sync.pool;

import java.net.InetSocketAddress;
import java.net.Socket;

import org.apache.commons.pool.KeyedPoolableObjectFactory;

import com.missian.client.NetworkConfig;

@SuppressWarnings("rawtypes")
public class CommonSocketFactory implements KeyedPoolableObjectFactory {
	private NetworkConfig networkConfig;

	public CommonSocketFactory(NetworkConfig networkConfig) {
		this.networkConfig = networkConfig;
	}

	public void activateObject(Object key, Object obj) throws Exception {
	}

	public void destroyObject(Object key, Object obj) throws Exception {
		Socket socket = (Socket) obj;
		socket.close();
	}

	public Object makeObject(Object key) throws Exception {
		ServerAddress address = (ServerAddress) key;
		Socket conn = new Socket();
		conn.setSoTimeout(this.networkConfig.getReadTimeout() * 1000);
		conn.setTcpNoDelay(this.networkConfig.isTcpNoDelay());
		conn.setReuseAddress(this.networkConfig.isReuseAddress());
		conn.setSoLinger(this.networkConfig.getSoLinger() > 0, this.networkConfig.getSoLinger());
		conn.setSendBufferSize(this.networkConfig.getSendBufferSize());
		conn.setReceiveBufferSize(this.networkConfig.getReceiveBufferSize());
		conn.connect(new InetSocketAddress(address.getHost(), address.getPort()), this.networkConfig.getConnectTimeout() * 1000);
		return conn;
	}

	public void passivateObject(Object key, Object obj) throws Exception {
	}

	public boolean validateObject(Object key, Object obj) {
		Socket socket = (Socket) obj;
		return socket.isConnected();
	}
}