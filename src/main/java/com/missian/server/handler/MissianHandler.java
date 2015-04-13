package com.missian.server.handler;

import com.caucho.hessian.io.SerializerFactory;
import com.missian.common.beanlocate.BeanLocator;
import com.missian.common.exceptionhandler.MissianExceptionHandler;
import com.missian.common.io.IoBufferOutputStream;
import com.missian.server.codec.MissianRequest;
import com.missian.server.codec.MissianResponse;
import com.missian.server.network.RemoteAddressHolder;
import java.net.InetSocketAddress;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;

public class MissianHandler extends IoHandlerAdapter {
	private BeanLocator beanLocator;
	private MissianExceptionHandler missianExceptionHandler;
	private SerializerFactory serializerFactory = new SerializerFactory();

	public MissianHandler(BeanLocator beanLocator) {
		this.beanLocator = beanLocator;
	}

	public MissianHandler(BeanLocator beanLocator, MissianExceptionHandler missianExceptionHandler) {
		this.beanLocator = beanLocator;
		this.missianExceptionHandler = missianExceptionHandler;
	}

	public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
		session.close(true);
	}

	public void messageReceived(IoSession session, Object message) throws Exception {
		MissianRequest request = (MissianRequest) message;
		Object bean = this.beanLocator.lookup(request.getBeanName());
		MissianSkeleton service = new MissianSkeleton(bean, bean.getClass(), this.missianExceptionHandler);

		IoBufferOutputStream os = new IoBufferOutputStream();
		try {
			RemoteAddressHolder.setRemoteAddress(((InetSocketAddress) session.getRemoteAddress()).getAddress());
		} catch (Exception localException) {
		}
		String methodName = service.invoke(request.getInputStream(), os, this.serializerFactory);

		MissianResponse resp = new MissianResponse();
		resp.setTransportProtocol(request.getTransportProtocol());
		resp.setAsync(request.isAsync());
		resp.setOutputBuffer(os.flip());
		resp.setBeanName(request.getBeanName());
		resp.setMethodName(methodName);
		resp.setSequence(request.getSequence());
		session.write(resp);
	}

	public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
		session.close(false);
	}
}