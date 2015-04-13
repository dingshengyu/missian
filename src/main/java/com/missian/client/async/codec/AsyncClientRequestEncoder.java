package com.missian.client.async.codec;

import java.net.URI;

import org.apache.asyncweb.common.DefaultHttpRequest;
import org.apache.asyncweb.common.HttpMethod;
import org.apache.asyncweb.common.HttpVersion;
import org.apache.asyncweb.common.codec.HttpRequestEncoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.missian.client.async.message.AsyncClientRequest;
import com.missian.common.io.TransportProtocol;

public class AsyncClientRequestEncoder implements ProtocolEncoder {
	private HttpRequestEncoder httpEncoder = new HttpRequestEncoder();

	public void dispose(IoSession session) throws Exception {
	}

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		AsyncClientRequest request = (AsyncClientRequest) message;

		if (request.getTransportProtocol() == TransportProtocol.tcp) {
			IoBuffer buffer = IoBuffer.allocate(session.getConfig().getReadBufferSize());
			buffer.setAutoExpand(true);
			buffer.put((byte) 1);
			byte[] beanNameBytes = request.getBeanName().getBytes("ASCII");
			buffer.putInt(beanNameBytes.length);
			buffer.put(beanNameBytes);
			buffer.putInt(request.getSequence());
			buffer.putInt(request.getOutputBuffer().limit());
			buffer.put(request.getOutputBuffer());
			buffer.flip();
			out.write(buffer);
		} else {
			DefaultHttpRequest httpRequest = new DefaultHttpRequest();
			httpRequest.setMethod(HttpMethod.POST);
			httpRequest.setProtocolVersion(HttpVersion.HTTP_1_1);
			httpRequest.setRequestUri(new URI("/" + request.getBeanName()));
			httpRequest.setContentType("application/x-hessian");
			httpRequest.setKeepAlive(true);
			String host = request.getHost() + (request.getPort() == 80 ? "" : new StringBuilder(":").append(request.getPort()).toString());
			httpRequest.setHeader("Host", host);
			httpRequest.setHeader("Missian-Sequence", String.valueOf(request.getSequence()));
			httpRequest.setHeader("Content-Length", String.valueOf(request.getOutputBuffer().limit()));
			httpRequest.setContent(request.getOutputBuffer());
			this.httpEncoder.encode(session, httpRequest, out);
		}
	}
}