package com.missian.common.io;

public class MissianMessage {
	private String beanName;
	private TransportProtocol transportProtocol;
	private int sequence;

	public int getSequence() {
		return this.sequence;
	}

	public void setSequence(int sequence) {
		this.sequence = sequence;
	}

	public TransportProtocol getTransportProtocol() {
		return this.transportProtocol;
	}

	public void setTransportProtocol(TransportProtocol transportProtocol) {
		this.transportProtocol = transportProtocol;
	}

	public String getBeanName() {
		return this.beanName;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}
}