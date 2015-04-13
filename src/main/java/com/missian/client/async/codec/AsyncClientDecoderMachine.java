package com.missian.client.async.codec;

import com.missian.client.async.message.AsyncClientResponse;
import com.missian.common.io.IoBufferInputStream;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.Iterator;
import java.util.List;

import org.apache.asyncweb.common.MutableHttpResponse;
import org.apache.asyncweb.common.codec.HttpResponseDecodingState;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.filter.codec.ProtocolDecoderOutput;
import org.apache.mina.filter.codec.statemachine.DecodingState;
import org.apache.mina.filter.codec.statemachine.DecodingStateMachine;
import org.apache.mina.filter.codec.statemachine.FixedLengthDecodingState;
import org.apache.mina.filter.codec.statemachine.IntegerDecodingState;

public class AsyncClientDecoderMachine extends DecodingStateMachine {
	private CharsetDecoder charsetDecoder = Charset.forName("ASCII").newDecoder();

	private DecodingState checkTransportState = new DecodingState() {
		public DecodingState decode(IoBuffer in, ProtocolDecoderOutput out) throws Exception {
			if (in.hasRemaining()) {
				byte b = in.get();
				if ((b != 0) && (b != 1)) {
					in.position(in.position() - 1);
					return AsyncClientDecoderMachine.this.httpDecodingState;
				}
				return AsyncClientDecoderMachine.this.beanNameState;
			}

			return this;
		}

		public DecodingState finishDecode(ProtocolDecoderOutput out) throws Exception {
			return null;
		}
	};

	private DecodingState beanNameState = new IntegerDecodingState() {
		protected DecodingState finishDecode(int s, ProtocolDecoderOutput out) throws Exception {
			return new FixedLengthDecodingState(s) {
				protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
					out.write(product.getString(AsyncClientDecoderMachine.this.charsetDecoder));
					return AsyncClientDecoderMachine.this.methodNameState;
				}
			};
		}
	};

	private DecodingState methodNameState = new IntegerDecodingState() {
		protected DecodingState finishDecode(int s, ProtocolDecoderOutput out) throws Exception {
			return new FixedLengthDecodingState(s) {
				protected DecodingState finishDecode(IoBuffer product, ProtocolDecoderOutput out) throws Exception {
					out.write(product.getString(AsyncClientDecoderMachine.this.charsetDecoder));
					return AsyncClientDecoderMachine.this.sequenceState;
				}
			};
		}
	};

	private DecodingState sequenceState = new IntegerDecodingState() {
		protected DecodingState finishDecode(int value, ProtocolDecoderOutput out) throws Exception {
			out.write(Integer.valueOf(value));
			return AsyncClientDecoderMachine.this.bodyState;
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

	private HttpResponseDecodingState httpDecodingState = new HttpResponseDecodingState() {
		@SuppressWarnings("rawtypes")
		protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
			for (Iterator localIterator = childProducts.iterator(); localIterator.hasNext();) {
				Object child = localIterator.next();
				MutableHttpResponse response = (MutableHttpResponse) child;
				String beanName = response.getHeader("Missian-Bean");
				String methodName = response.getHeader("Missian-Method");
				String sequence = response.getHeader("Missian-Sequence");
				IoBuffer content = response.getContent();
				out.write(beanName);
				out.write(methodName);
				out.write(Integer.valueOf(Integer.parseInt(sequence)));
				out.write(new IoBufferInputStream(content));
			}
			return null;
		}
	};

	protected void destroy() throws Exception {
		this.charsetDecoder.reset();
	}

	protected DecodingState finishDecode(List<Object> childProducts, ProtocolDecoderOutput out) throws Exception {
		if (childProducts.size() < 4) {
			return null;
		}
		int childs = childProducts.size();
		for (int i = 0; i < childs; i += 4) {
			AsyncClientResponse response = new AsyncClientResponse();
			response.setBeanName((String) childProducts.get(i));
			response.setMethodName((String) childProducts.get(i + 1));
			response.setSequence(((Integer) childProducts.get(i + 2)).intValue());
			response.setInputStream((InputStream) childProducts.get(i + 3));
			out.write(response);
			this.charsetDecoder.reset();
		}
		return null;
	}

	protected DecodingState init() throws Exception {
		return this.checkTransportState;
	}
}