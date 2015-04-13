package com.missian.server.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;

public class CheckTransportState implements DecodingState {
	public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
		return null;
	}

	public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
		return null;
	}
}