package com.missian.client.async;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;

import com.caucho.hessian.client.HessianRuntimeException;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.HessianProtocolException;
import com.caucho.services.server.AbstractSkeleton;
import com.missian.client.TransportURL;
import com.missian.client.async.message.AsyncClientRequest;
import com.missian.common.beanlocate.BeanLocator;
import com.missian.common.io.IoBufferOutputStream;
import com.missian.common.io.TransportProtocol;

/**
 * description: Async proxy implementation for missian clients. Applications
 * will generally use AsyncMissianProxyFactory to create proxy clients.
 */
public class AsyncMissianProxy implements InvocationHandler, Serializable {
	private static final Logger log = Logger.getLogger(AsyncMissianProxy.class.getName());
	private String host;
	private int port;
	private String beanName;
	private AsyncMissianProxyFactory _factory;
	private BeanLocator beanLocator;
	private TransportProtocol transportProtocol;

	public AsyncMissianProxy(BeanLocator beanLocator, TransportURL url, AsyncMissianProxyFactory asyncMissianProxyFactory) throws IOException {
		super();
		this.host = url.getHost();
		this.port = url.getPort();
		this.beanName = url.getQuery();
		this._factory = asyncMissianProxyFactory;
		this.beanLocator = beanLocator;
		this.transportProtocol = url.getTransport();
	}

	private WeakHashMap<Method, String> _mangleMap = new WeakHashMap<Method, String>();

	/**
	 * 
	 */
	private static final long serialVersionUID = -138089145263434181L;

	public Object invoke(Object proxy, final Method method, Object[] args) throws Throwable {
		String mangleName;
		Object returnObj = null;
		Class<?> returnType = method.getReturnType();
		if (returnType == int.class) {
			returnObj = (int) 0;
		} else if (returnType == long.class) {
			returnObj = (long) 0;
		} else if (returnType == float.class) {
			returnObj = (float) 0;
		} else if (returnType == double.class) {
			returnObj = (double) 0;
		} else if (returnType == byte.class) {
			returnObj = (byte) 0;
		} else if (returnType == char.class) {
			returnObj = (char) 0;
		} else if (returnType == boolean.class) {
			returnObj = false;
		}
		synchronized (_mangleMap) {
			mangleName = _mangleMap.get(method);
		}

		if (mangleName == null) {
			String methodName = method.getName();
			Class<?>[] params = method.getParameterTypes();

			// equals and hashCode are special cased
			if (methodName.equals("equals") && params.length == 1 && params[0].equals(Object.class)) {
				Object value = args[0];
				if (value == null || !Proxy.isProxyClass(value.getClass()))
					return Boolean.FALSE;

				Object proxyHandler = Proxy.getInvocationHandler(value);

				if (!(proxyHandler instanceof AsyncMissianProxy))
					return Boolean.FALSE;

				AsyncMissianProxy handler = (AsyncMissianProxy) proxyHandler;

				return equals(handler);
			} else if (methodName.equals("hashCode") && params.length == 0)
				return hashCode();
			else if (methodName.equals("getHessianType"))
				return proxy.getClass().getInterfaces()[0].getName();
			else if (methodName.equals("getHessianURL"))
				return "sync://" + host + ":" + port + "/" + beanName;
			else if (methodName.equals("toString") && params.length == 0)
				return toString();

			if (!_factory.isOverloadEnabled())
				mangleName = method.getName();
			else
				mangleName = mangleName(method, false);

			synchronized (_mangleMap) {
				_mangleMap.put(method, mangleName);
			}
		}

		int sequence = -1;

		try {
			if (log.isLoggable(Level.FINER))
				log.finer("Missian[" + toString() + "] calling " + mangleName);
			if (_factory.getCallBack(beanName, mangleName) == null) {
				CallbackTarget async = method.getDeclaringClass().getAnnotation(CallbackTarget.class);
				CallbackTargetMethod asyncMethod = method.getAnnotation(CallbackTargetMethod.class);

				if (async != null && asyncMethod != null) {
					final Object callbackBean = async == null ? null : (Object) beanLocator.lookup(async.value());
					if (callbackBean == null) {
						throw new IllegalAccessError("No callback found for '" + async.value() + "'.");
					}
					try {
						String callbackName = asyncMethod == null ? method.getName() : asyncMethod.value();
						final Method callbackMethod = callbackBean.getClass().getMethod(callbackName, method.getReturnType());
						_factory.setCallback(beanName, mangleName, new Callback() {
							@Override
							public void onMessageReceived(Object value) throws Exception {
								callbackMethod.invoke(callbackBean, value);
							}

							@Override
							public Class<?> getAcceptValueType() {
								return method.getReturnType();
							}

							@Override
							public void onException(Throwable e) throws Exception {
								// TODO Auto-generated method stub
							}
						});
					} catch (NoSuchMethodException e) {
						throw new IllegalAccessError("No callback method found for '" + method.getName() + "'.");
					}
				} else if (isLastParamACallback(method)) {
					Callback callback = (Callback) args[args.length - 1];
					Object[] newArgs = new Object[args.length - 1];
					for (int i = 0; i < args.length - 1; i++) {
						newArgs[i] = args[i];
					}
					args = newArgs;
					sequence = _factory.setCallback(callback);
					mangleName = mangleName(method, true);
				} else if (isReturnTypeAFuture(method) && args.length > 0) {
					// create the future and callback
					final AsyncFuture<Object> future = new AsyncFuture<Object>();
					final Class<?> lastArg = (Class<?>) args[args.length - 1];
					Callback callback = new Callback() {
						@Override
						public void onMessageReceived(Object value) throws Exception {
							future.done(value);
						}

						@Override
						public Class<?> getAcceptValueType() {
							return lastArg;
						}

						@Override
						public void onException(Throwable e) throws Exception {
							// TODO Auto-generated method stub
						}
					};
					// the last arg is the remote return type, needn't send to
					// the server side.
					Object[] newArgs = new Object[args.length - 1];
					for (int i = 0; i < args.length - 1; i++) {
						newArgs[i] = args[i];
					}
					args = newArgs;
					sequence = _factory.setCallback(callback);
					returnObj = future;
					mangleName = mangleName(method, true);
				}
			}

			sendRequest(mangleName, sequence, args);
			return returnObj;
		} catch (HessianProtocolException e) {
			throw new HessianRuntimeException(e);
		} finally {

		}
	}

	private boolean isReturnTypeAFuture(Method method) {
		return AsyncFuture.class.isAssignableFrom(method.getReturnType());
	}

	private boolean isLastParamACallback(Method method) {
		Class<?>[] parameterTypes = method.getParameterTypes();
		if (parameterTypes.length == 0) {
			return false;
		}
		Class<?> lastParam = parameterTypes[parameterTypes.length - 1];
		return Callback.class.isAssignableFrom(lastParam);
	}

	private void sendRequest(String mangleName, int sequence, Object[] args) throws IOException {
		try {
			AsyncClientRequest request = new AsyncClientRequest();
			request.setBeanName(beanName);
			request.setTransportProtocol(transportProtocol);
			request.setHost(host);
			request.setPort(port);
			request.setSequence(sequence);
			IoBufferOutputStream baos = new IoBufferOutputStream(_factory.getInitBufSize());
			AbstractHessianOutput out = _factory.getHessianOutput(baos);
			out.call(mangleName, args);
			out.flush();

			IoBuffer body = baos.flip();
			request.setOutputBuffer(body);

			getSession().write(request);
		} finally {

		}
	}

	/**
	 * @return
	 */
	private IoSession getSession() {
		return _factory.getIoSession(host, port);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((beanName == null) ? 0 : beanName.hashCode());
		result = prime * result + port;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AsyncMissianProxy other = (AsyncMissianProxy) obj;
		if (beanName == null) {
			if (other.beanName != null)
				return false;
		} else if (!beanName.equals(other.beanName))
			return false;
		if (port != other.port)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "AsyncMissianProxy [beanName=" + beanName + ", port=" + port + ", server=" + host + "]";
	}

	public static String mangleName(Method method, boolean isCallback) {
		StringBuffer sb = new StringBuffer();

		sb.append(method.getName());

		Class<?>[] params = method.getParameterTypes();
		int len = isCallback ? params.length - 1 : params.length;
		for (int i = 0; i < len; i++) {
			sb.append('_');
			sb.append(AbstractSkeleton.mangleClass(params[i], false));
		}

		return sb.toString();
	}

	public static final byte[] intToByteArray(int value) {
		return new byte[] { (byte) (value >>> 24), (byte) (value >>> 16), (byte) (value >>> 8), (byte) value };
	}

	public static final int byteArrayToInt(byte[] b) {
		return (b[0] << 24) + ((b[1] & 0xFF) << 16) + ((b[2] & 0xFF) << 8) + (b[3] & 0xFF);
	}

}
