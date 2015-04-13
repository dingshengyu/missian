/*
 *	 Copyright [2010] Stanley Ding(Dingshengyu)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   Missian is based on hessian, mina and spring. Any project who uses 
 *	 missian must agree to hessian, mima and spring's license.
 *	  Hessian: http://hessian.caucho.com/
 *    Mina:http://mina.apache.org
 *	  Spring(Optional):http://www.springsource.org/	 
 *
 *   @author stanley
 *	 @date 2010-11-28
 */
package com.missian.client.sync;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.Socket;

import com.caucho.hessian.io.HessianRemoteObject;
import com.missian.client.MissianProxyFactory;
import com.missian.client.NetworkConfig;
import com.missian.client.TransportURL;
import com.missian.client.sync.pool.SocketPool;

public class SyncMissianProxyFactory extends MissianProxyFactory {
	// tcp configurations
	private boolean connectionKeepAlive = false;
	private SocketPool socketPool;

	public SyncMissianProxyFactory() {
		this(new NetworkConfig());
	}

	public SyncMissianProxyFactory(NetworkConfig networkConfig) {
		super(networkConfig);
		this.connectionKeepAlive = false;
	}

	public SyncMissianProxyFactory(SocketPool socketPool) {
		super(socketPool.getNetworkConfig());
		this.socketPool = socketPool;
		this.connectionKeepAlive = true;
	}

	public <T> T create(Class<T> api, String url, ClassLoader loader) {
		if (api == null)
			throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
		InvocationHandler handler = null;
		TransportURL u = new TransportURL(url);
		handler = new SyncMissianProxy(u, this);

		@SuppressWarnings("unchecked")
		T proxy = (T) Proxy.newProxyInstance(loader, new Class[] { api, HessianRemoteObject.class }, handler);

		return proxy;
	}

	public <T> T create(Class<T> api, String url) {
		return create(api, url, Thread.currentThread().getContextClassLoader());
	}

	public Socket getSocket(String host, int port) throws Exception {
		if (connectionKeepAlive) {
			return socketPool.getSocket(host, port);
		}
		return createSocket(host, port);
	}

	public Socket createSocket(String host, int port) throws IOException {
		Socket conn = new Socket();
		conn.setSoTimeout(getReadTimeout() * 1000);
		conn.setTcpNoDelay(isTcpNoDelay());
		conn.setReuseAddress(isReuseAddress());
		conn.setSoLinger(getSoLinger() > 0, getSoLinger());
		conn.setSendBufferSize(getSendBufferSize());
		conn.setReceiveBufferSize(getReceiveBufferSize());
		conn.connect(new InetSocketAddress(host, port), getConnectTimeout() * 1000);
		return conn;
	}

	public void destroySocket(String host, int port, Socket socket) throws Exception {
		if (connectionKeepAlive) {
			socketPool.returnSocket(host, port, socket);
		} else {
			socket.close();
		}
	}
}
