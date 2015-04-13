package com.missian.server.network;

import java.net.InetAddress;

public final class RemoteAddressHolder {
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static ThreadLocal<InetAddress> threadLocal = new ThreadLocal();

	public static void setRemoteAddress(InetAddress address) {
		threadLocal.set(address);
	}

	public static InetAddress getRemoteAddress() {
		return (InetAddress) threadLocal.get();
	}
}