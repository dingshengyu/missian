package com.missian.client.async;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.future.IoFuture;
import org.apache.mina.core.future.IoFutureListener;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.executor.ExecutorFilter;
import org.apache.mina.filter.logging.LogLevel;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import com.caucho.hessian.io.HessianRemoteObject;
import com.missian.client.MissianProxyFactory;
import com.missian.client.NetworkConfig;
import com.missian.client.TransportURL;
import com.missian.client.async.codec.AsyncClientCodecFactory;
import com.missian.client.async.errorhandler.ErrorHandler;
import com.missian.common.beanlocate.BeanLocator;

public class AsyncMissianProxyFactory extends MissianProxyFactory {
	private static final int DEFAULT_THREAD_POOL = 4;
	private Map<String, Map<String, Callback>> callbackMap = new ConcurrentHashMap<String, Map<String, Callback>>();
	private Map<Integer, Callback> sequenceCallbackMap = new ConcurrentHashMap<Integer, Callback>();
	private int initBufSize = 256;
	private NioSocketConnector connector;
	private BeanLocator callbackLoacator;
	private int callbackIoProcesses;
	private boolean logBeforeCodec;
	private boolean logAfterCodec;
	private ExecutorService threadPool;
	private boolean threadPoolCreated;
	private ConcurrentHashMap<String, IoSession> sessionMap = new ConcurrentHashMap<String, IoSession>();
	private ReentrantLock lock = new ReentrantLock();
	private boolean init = false;
	private ErrorHandler errorHandler;
	private AtomicInteger sequenceGenerator = new AtomicInteger(0);

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, ExecutorService threadPool, int callbackIoProcesses, boolean logBeforeCodec, boolean logAfterCodec, NetworkConfig networkConfig,
			ErrorHandler errorHandler) {
		super(networkConfig);
		this.callbackLoacator = callbackLoacator;
		this.callbackIoProcesses = callbackIoProcesses;
		this.logBeforeCodec = logBeforeCodec;
		this.logAfterCodec = logAfterCodec;
		this.threadPool = threadPool;
		this.errorHandler = errorHandler;
		init();
	}

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, ExecutorService threadPool, int callbackIoProcesses, boolean logBeforeCodec, boolean logAfterCodec, ErrorHandler errorHandler) {
		this(callbackLoacator, threadPool, callbackIoProcesses, logBeforeCodec, logAfterCodec, new NetworkConfig(), errorHandler);
	}

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, ExecutorService threadPool, ErrorHandler errorHandler) {
		this(callbackLoacator, threadPool, 1, false, true, errorHandler);
	}

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, int threadPoolSize, int callbackIoProcesses, boolean logBeforeCodec, boolean logAfterCodec, ErrorHandler errorHandler) {
		this(callbackLoacator, Executors.newFixedThreadPool(threadPoolSize), callbackIoProcesses, logBeforeCodec, logAfterCodec, errorHandler);
		this.threadPoolCreated = true;
	}

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, ExecutorService threadPool, NetworkConfig networkConfig, ErrorHandler errorHandler) {
		this(callbackLoacator, threadPool, 1, false, true, networkConfig, errorHandler);
	}

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, int threadPoolSize, int callbackIoProcesses, boolean logBeforeCodec, boolean logAfterCodec, NetworkConfig networkConfig,
			ErrorHandler errorHandler) {
		this(callbackLoacator, Executors.newFixedThreadPool(threadPoolSize), callbackIoProcesses, logBeforeCodec, logAfterCodec, networkConfig, errorHandler);
		this.threadPoolCreated = true;
	}

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, int threadPoolSize, ErrorHandler errorHandler) {
		this(callbackLoacator, threadPoolSize, 1, false, true, errorHandler);
	}

	public AsyncMissianProxyFactory(BeanLocator callbackLoacator, ErrorHandler errorHandler) {
		this(callbackLoacator, DEFAULT_THREAD_POOL, errorHandler);
	}

	public AsyncMissianProxyFactory(ErrorHandler errorHandler) {
		this(null, DEFAULT_THREAD_POOL, errorHandler);
	}

	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	public int getInitBufSize() {
		return this.initBufSize;
	}

	public void setInitBufSize(int initBufSize) {
		this.initBufSize = initBufSize;
	}

	public void destroy() {
		this.connector.dispose();
		if (this.threadPoolCreated)
			this.threadPool.shutdown();
	}

	private void init() {
		this.connector = new NioSocketConnector(this.callbackIoProcesses);
		LoggingFilter loggingFilter = new LoggingFilter();
		loggingFilter.setMessageReceivedLogLevel(LogLevel.DEBUG);
		loggingFilter.setMessageSentLogLevel(LogLevel.DEBUG);
		loggingFilter.setSessionOpenedLogLevel(LogLevel.DEBUG);
		loggingFilter.setSessionCreatedLogLevel(LogLevel.DEBUG);
		loggingFilter.setSessionClosedLogLevel(LogLevel.DEBUG);
		if (this.logBeforeCodec) {
			this.connector.getFilterChain().addLast("log.1", loggingFilter);
		}
		this.connector.getFilterChain().addLast("codec", new ProtocolCodecFilter(new AsyncClientCodecFactory()));
		this.connector.getFilterChain().addLast("log.2", loggingFilter);
		if (this.logAfterCodec) {
			this.connector.getFilterChain().addLast("executor", new ExecutorFilter(this.threadPool));
		}
		this.connector.getSessionConfig().setReadBufferSize(getReceiveBufferSize());
		this.connector.getSessionConfig().setSendBufferSize(getSendBufferSize());
		this.connector.getSessionConfig().setReuseAddress(isReuseAddress());
		this.connector.getSessionConfig().setTcpNoDelay(isTcpNoDelay());
		this.connector.getSessionConfig().setSoLinger(getSoLinger());
		this.connector.setHandler(new AsyncClientHandler(this));
		this.connector.setConnectTimeoutMillis(getConnectTimeout() * 1000);
		this.init = true;
	}

	Callback getCallBack(String beanName, String methodName) {
		Map<String, Callback> submap = this.callbackMap.get(beanName);
		return submap == null ? null : (Callback) submap.get(methodName);
	}

	void setCallback(String beanName, String methodName, Callback callback) {
		Map<String, Callback> submap = this.callbackMap.get(beanName);
		if (submap == null) {
			submap = new ConcurrentHashMap<String, Callback>();
			this.callbackMap.put(beanName, submap);
		}
		submap.put(methodName, callback);
	}

	Callback getAndRemoveCallBack(int sequence) {
		Callback ret = (Callback) this.sequenceCallbackMap.get(Integer.valueOf(sequence));
		if (ret != null) {
			this.sequenceCallbackMap.remove(Integer.valueOf(sequence));
		}
		return ret;
	}

	int setCallback(Callback callback) {
		int sequence = this.sequenceGenerator.incrementAndGet();
		this.sequenceCallbackMap.put(Integer.valueOf(sequence), callback);
		return sequence;
	}

	public Object create(Class<?> api, String url, ClassLoader loader) throws IOException {
		if (!this.init) {
			throw new IOException("Factory is not initialized, please call init() before calling create()");
		}
		if (api == null)
			throw new NullPointerException("api must not be null for HessianProxyFactory.create()");
		InvocationHandler handler = null;
		TransportURL u = new TransportURL(url);
		handler = new AsyncMissianProxy(this.callbackLoacator, u, this);
		return Proxy.newProxyInstance(loader, new Class[] { api, HessianRemoteObject.class }, handler);
	}

	public Object create(Class<?> api, String url) throws IOException {
		return create(api, url, Thread.currentThread().getContextClassLoader());
	}

	@SuppressWarnings("rawtypes")
	IoSession getIoSession(String host, int port) {
		final String key = host + ":" + port;
		IoSession session = (IoSession) this.sessionMap.get(key);
		if (session == null) {
			this.lock.lock();
			try {
				if (this.sessionMap.get(key) == null) {
					ConnectFuture future = this.connector.connect(new InetSocketAddress(host, port));
					future.await();
					session = future.getSession();
					session.getCloseFuture().addListener(new IoFutureListener() {
						public void operationComplete(IoFuture future) {
							AsyncMissianProxyFactory.this.sessionMap.remove(key);
						}
					});
					this.sessionMap.put(key, session);
				} else {
					session = (IoSession) this.sessionMap.get(key);
				}
			} catch (InterruptedException localInterruptedException) {
			} finally {
				this.lock.unlock();
			}
		}
		return session;
	}
}