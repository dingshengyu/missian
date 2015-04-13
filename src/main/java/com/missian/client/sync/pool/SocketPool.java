package com.missian.client.sync.pool;

import com.missian.client.NetworkConfig;
import java.net.Socket;

public interface SocketPool {
	public Socket getSocket(String paramString, int paramInt) throws Exception;

	public void returnSocket(String paramString, int paramInt, Socket paramSocket) throws Exception;

	public NetworkConfig getNetworkConfig();
}