package com.missian.server.codec;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.List;

import org.apache.asyncweb.common.MutableHttpRequest;
import org.apache.asyncweb.common.codec.HttpRequestDecodingStateMachine;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingStateMachine;
import org.apache.mina.filter.codec.statemachine.FixedLengthDecodingState;
import org.apache.mina.filter.codec.statemachine.IntegerDecodingState;

import com.missian.common.io.IoBufferInputStream;
import com.missian.common.io.TransportProtocol;

public class MissianDecoderMachine extends DecodingStateMachine {
	private CharsetDecoder charsetDecoder = Charset.forName("ASCII").newDecoder();

	private DecodingState checkTransportState = new DecodingState() {
		public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
			if (in.hasRemaining()) {
				byte b = in.get();
				if ((b != 0) && (b != 1)) {
					out.write(TransportProtocol.http);
					in.position(in.position() - 1);
					return MissianDecoderMachine.this.httpDecodingStateMachine;
				}
				out.write(TransportProtocol.tcp);
				out.write(Boolean.valueOf(b == 1));
				return MissianDecoderMachine.this.beanNameLengthState;
			}

			return this;
		}

		public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
			return null;
		}
	};

	private DecodingState beanNameLengthState = new IntegerDecodingState() {
		protected DecodingState finishDecode(int s, ProtocolDecoderOutput out) throws Exception {
			return new FixedLengthDecodingState(s) {
				protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
					out.write(product.getString(MissianDecoderMachine.this.charsetDecoder));
					return MissianDecoderMachine.this.sequenceState;
				}
			};
		}
	};

	private DecodingState sequenceState = new IntegerDecodingState() {
		protected DecodingState finishDecode(int value, ProtocolDecoderOutput out) throws Exception {
			out.write(Integer.valueOf(value));
			return MissianDecoderMachine.this.bodyState;
		}
	};

	private DecodingState bodyState = new IntegerDecodingState() {
		protected DecodingState finishDecode(int value, ProtocolDecoderOutput out) throws Exception {
			return new FixedLengthDecodingState(value) {
				protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
					out.write(new IoBufferInputStream(product));
					return null;
				}
			};
		}
	};

	private HttpRequestDecodingStateMachine httpDecodingStateMachine = new HttpRequestDecodingStateMachine() {
		@SuppressWarnings("rawtypes")
		protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
			for (Iterator localIterator = childProducts.iterator(); localIterator.hasNext();) {
				Object child = localIterator.next();
				MutableHttpRequest request = (MutableHttpRequest) child;
				String sequence = request.getHeader("Missian-Sequence");
				if (sequence != null)
					out.write(Boolean.valueOf(true));
				else {
					out.write(Boolean.valueOf(false));
				}
				String beanName = request.getRequestUri().getPath().substring(1);
				out.write(beanName);
				if (sequence != null)
					out.write(Integer.valueOf(Integer.parseInt(sequence)));
				else {
					out.write(Integer.valueOf(0));
				}

				out.write(new IoBufferInputStream(request.getContent()));
			}
			return null;
		}
	};

	protected void destroy() throws Exception {
		this.charsetDecoder.reset();
	}

	protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
		if (childProducts.size() < 5) {
			return null;
		}
		TransportProtocol transport = (TransportProtocol) childProducts.get(0);
		int childs = childProducts.size();
		for (int i = 1; i < childs; i += 4) {
			MissianRequest request = new MissianRequest();
			request.setTransportProtocol(transport);
			request.setAsync(((Boolean) childProducts.get(i)).booleanValue());
			request.setBeanName((String) childProducts.get(i + 1));
			request.setSequence(((Integer) childProducts.get(i + 2)).intValue());
			request.setInputStream((InputStream) childProducts.get(i + 3));
			out.write(request);
			this.charsetDecoder.reset();
		}

		return null;
	}

	protected DecodingState init() throws Exception {
		return this.checkTransportState;
	}
}