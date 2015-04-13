package com.missian.client.async;

import java.io.InputStream;
import java.net.InetSocketAddress;

import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.HessianProtocolException;
import com.missian.client.async.message.AsyncClientResponse;

public class AsyncClientHandler extends IoHandlerAdapter {
	private AsyncMissianProxyFactory _factory;
	private Logger log = LoggerFactory.getLogger(AsyncClientHandler.class);

	public AsyncClientHandler(AsyncMissianProxyFactory factory) {
		this._factory = factory;
	}

	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		InetSocketAddress socketAddress = (InetSocketAddress) session.getRemoteAddress();
		session.close(true);

		this._factory.getErrorHandler().onSessionError(socketAddress, cause);
	}

	public void messageReceived(IoSession session, Object message) throws Exception {
		AsyncClientResponse response = (AsyncClientResponse) message;
		String beanName = response.getBeanName();

		String methodName = response.getMethodName();
		Callback callback = this._factory.getCallBack(beanName, methodName);
		if (callback == null) {
			callback = this._factory.getAndRemoveCallBack(response.getSequence());
		}
		if (callback == null) {
			return;
		}
		InputStream is = response.getInputStream();

		int code = is.read();

		if (code == 72) {
			is.read();
			is.read();

			AbstractHessianInput in = this._factory.getHessian2Input(is);

			Object value = null;
			boolean received = false;
			try {
				value = in.readReply(callback.getAcceptValueType());
				received = true;
			} catch (Throwable e) {
				callback.onException(e);
				this.log.error("remote exception", e);
			}
			if (received)
				try {
					callback.onMessageReceived(value);
				} catch (Throwable e) {
					this.log.error("callback failed. bean={}, method={}, value={}", new Object[] { beanName, methodName, value });
					this.log.error("callback failed", e);
				}
		} else if (code == 114) {
			is.read();
			is.read();

			AbstractHessianInput in = this._factory.getHessianInput(is);
			try {
				in.startReplyBody();
			} catch (Throwable e) {
				callback.onException(e);
				this.log.error("remote exception", e);
				return;
			}
			Object value = null;
			boolean received = false;
			try {
				value = in.readObject(callback.getAcceptValueType());
				in.completeReply();
				received = true;
			} catch (Throwable e) {
				callback.onException(e);
				this.log.error("remote exception", e);
			}
			if (received)
				try {
					callback.onMessageReceived(value);
				} catch (Throwable e) {
					this.log.error("callback failed. bean={}, method={}, value={}", new Object[] { beanName, methodName, value });
					this.log.error("callback failed", e);
				}
		} else {
			throw new HessianProtocolException("'" + (char) code + "' is an unknown code");
		}
		AbstractHessianInput in;
	}

	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		session.close(false);
	}
}