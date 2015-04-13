package com.missian.client.async.errorhandler;

import java.net.InetSocketAddress;

public interface ErrorHandler {
	public void onSessionError(InetSocketAddress paramInetSocketAddress, Throwable paramThrowable);
}