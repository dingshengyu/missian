package com.missian.client.async.codec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.statemachine.DecodingStateProtocolDecoder;

public class AsyncClientCodecFactory implements ProtocolCodecFactory {
	public ProtocolDecoder getDecoder(IoSession session) throws Exception {
		ProtocolDecoder decoder = (ProtocolDecoder) session.getAttribute("_DECODER_");
		if (decoder != null) {
			return decoder;
		}
		synchronized (session) {
			decoder = (ProtocolDecoder) session.getAttribute("_DECODER_");
			if (decoder == null) {
				decoder = new DecodingStateProtocolDecoder(new AsyncClientDecoderMachine());
				session.setAttribute("_DECODER_", decoder);
			}
		}
		return decoder;
	}

	public ProtocolEncoder getEncoder(IoSession session) throws Exception {
		ProtocolEncoder encoder = (ProtocolEncoder) session.getAttribute("_ENCODER_");
		if (encoder != null) {
			return encoder;
		}
		synchronized (session) {
			encoder = (ProtocolEncoder) session.getAttribute("_ENCODER_");
			if (encoder == null) {
				encoder = new AsyncClientRequestEncoder();
				session.setAttribute("_ENCODER_", encoder);
			}
		}
		return encoder;
	}
}