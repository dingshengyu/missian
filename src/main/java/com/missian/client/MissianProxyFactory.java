/*
 *	 Copyright [2010] Stanley Ding(Dingshengyu)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   Missian is based on hessian, mina and spring. Any project who uses 
 *	 missian must agree to hessian, mima and spring's license.
 *	  Hessian: http://hessian.caucho.com/
 *    Mina:http://mina.apache.org
 *	  Spring(Optional):http://www.springsource.org/	 
 *
 *   @author stanley
 *	 @date 2010-11-28
 */
package com.missian.client;

import java.io.InputStream;
import java.io.OutputStream;

import com.caucho.hessian.io.AbstractHessianInput;
import com.caucho.hessian.io.AbstractHessianOutput;
import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;
import com.caucho.hessian.io.HessianInput;
import com.caucho.hessian.io.HessianOutput;
import com.caucho.hessian.io.SerializerFactory;

/**
 * description: Base proxy factory class.
 */
public abstract class MissianProxyFactory {
	private ClassLoader loader;
	private SerializerFactory serializerFactory;
	// tcp configurations
	// hessian configurationso
	private boolean hessian2Request = true;
	private boolean hessian2Response = true;
	private boolean overloadEnabled = true;
	private NetworkConfig networkConfig = null;

	public int getConnectTimeout() {
		return networkConfig.getConnectTimeout();
	}

	public int getReadTimeout() {
		return networkConfig.getReadTimeout();
	}

	public int getReceiveBufferSize() {
		return networkConfig.getReceiveBufferSize();
	}

	public int getSendBufferSize() {
		return networkConfig.getSendBufferSize();
	}

	public int getSoLinger() {
		return networkConfig.getSoLinger();
	}

	public boolean isReuseAddress() {
		return networkConfig.isReuseAddress();
	}

	public boolean isTcpNoDelay() {
		return networkConfig.isTcpNoDelay();
	}

	public void setOverloadEnabled(boolean overloadEnabled) {
		this.overloadEnabled = overloadEnabled;
	}

	public MissianProxyFactory(NetworkConfig networkConfig) {
		this(networkConfig, Thread.currentThread().getContextClassLoader());
	}

	public MissianProxyFactory(NetworkConfig networkConfig, ClassLoader loader) {
		this.loader = loader;
		this.networkConfig = networkConfig;
	}

	public boolean isHessian2Request() {
		return hessian2Request;
	}

	public void setHessian2Request(boolean hessian2Request) {
		this.hessian2Request = hessian2Request;
	}

	public boolean isHessian2Response() {
		return hessian2Response;
	}

	public void setHessian2Response(boolean hessian2Response) {
		this.hessian2Response = hessian2Response;
	}

	public ClassLoader getLoader() {
		return loader;
	}

	public void setLoader(ClassLoader loader) {
		this.loader = loader;
	}

	public AbstractHessianOutput getHessianOutput(OutputStream os) {
		AbstractHessianOutput out;

		if (hessian2Request)
			out = new Hessian2Output(os);
		else {
			HessianOutput out1 = new HessianOutput(os);
			out = out1;

			if (hessian2Response)
				out1.setVersion(2);
		}

		out.setSerializerFactory(getSerializerFactory());

		return out;
	}

	public AbstractHessianInput getHessianInput(InputStream is) {
		return getHessian2Input(is);
	}

	public AbstractHessianInput getHessian1Input(InputStream is) {
		AbstractHessianInput in;

		in = new HessianInput(is);

		in.setSerializerFactory(getSerializerFactory());

		return in;
	}

	public AbstractHessianInput getHessian2Input(InputStream is) {
		AbstractHessianInput in;

		in = new Hessian2Input(is);

		in.setSerializerFactory(getSerializerFactory());

		return in;
	}

	/**
	 * Gets the serializer factory.
	 */
	public SerializerFactory getSerializerFactory() {
		if (serializerFactory == null)
			serializerFactory = new SerializerFactory(loader);

		return serializerFactory;
	}

	public boolean isOverloadEnabled() {
		return overloadEnabled;
	}
}
