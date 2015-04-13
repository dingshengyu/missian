package com.missian.server.codec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.statemachine.DecodingStateProtocolDecoder;

public class MissianCodecFactory implements ProtocolCodecFactory {
	public ProtocolDecoder getDecoder(IoSession session) throws Exception {
		ProtocolDecoder decoder = (ProtocolDecoder) session.getAttribute("_DECODER_");
		if (decoder != null) {
			return decoder;
		}
		synchronized (session) {
			decoder = (ProtocolDecoder) session.getAttribute("_DECODER_");
			if (decoder == null) {
				decoder = new DecodingStateProtocolDecoder(new MissianDecoderMachine());
				session.setAttribute("_DECODER_", decoder);
			}
		}
		return decoder;
	}

	public ProtocolEncoder getEncoder(IoSession session) throws Exception {
		return new MissianEncoder();
	}
}