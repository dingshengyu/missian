package com.missian.server.codec;

import org.apache.asyncweb.common.DefaultHttpResponse;
import org.apache.asyncweb.common.HttpResponseStatus;
import org.apache.asyncweb.common.HttpVersion;
import org.apache.asyncweb.common.codec.HttpResponseEncoder;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

import com.missian.common.io.TransportProtocol;

public class MissianEncoder implements ProtocolEncoder {
	private HttpResponseEncoder httpResponseEncoder = new HttpResponseEncoder();

	public void dispose(IoSession session) throws Exception {
	}

	public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception {
		MissianResponse resp = (MissianResponse) message;
		if (resp.getTransportProtocol() == TransportProtocol.tcp) {
			IoBuffer buf = IoBuffer.allocate(session.getConfig().getReadBufferSize());
			buf.setAutoExpand(true);
			buf.put((byte) (resp.isAsync() ? 1 : 0));
			if (resp.isAsync()) {
				byte[] beanNameBytes = resp.getBeanName().getBytes("ASCII");
				buf.putInt(beanNameBytes.length);
				buf.put(beanNameBytes);
				byte[] methodNameBytes = resp.getMethodName().getBytes("ASCII");
				buf.putInt(methodNameBytes.length);
				buf.put(methodNameBytes);
				buf.putInt(resp.getSequence());
				buf.putInt(resp.getOutputBuffer().limit());
			}
			buf.put(resp.getOutputBuffer());
			buf.flip();
			out.write(buf);
		} else {
			DefaultHttpResponse httpResponse = new DefaultHttpResponse();

			httpResponse.setStatus(HttpResponseStatus.OK);
			httpResponse.setProtocolVersion(HttpVersion.HTTP_1_1);
			httpResponse.setContentType("application/x-hessian");
			httpResponse.setKeepAlive(true);
			httpResponse.setHeader("Server", "Missian");
			if (resp.isAsync()) {
				httpResponse.setHeader("Missian-Sequence", String.valueOf(resp.getSequence()));
				httpResponse.setHeader("Missian-Bean", resp.getBeanName());
				httpResponse.setHeader("Missian-Method", resp.getMethodName());
			}

			IoBuffer body = resp.getOutputBuffer();
			httpResponse.addHeader("Content-Length", String.valueOf(body.limit()));
			httpResponse.setContent(body);

			this.httpResponseEncoder.encode(session, httpResponse, out);
		}
	}
}