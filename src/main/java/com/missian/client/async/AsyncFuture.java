package com.missian.client.async;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class AsyncFuture<V> implements Future<V> {
	private boolean canceled = false;
	private boolean done = false;
	private V value;
	private ReentrantLock lock = new ReentrantLock();
	private Condition notNull = this.lock.newCondition();
	private ArrayList<AsyncListener<V>> listenerList = new ArrayList<AsyncListener<V>>();

	public boolean cancel(boolean mayInterruptIfRunning) {
		this.canceled = true;
		return false;
	}

	public V get() throws InterruptedException, ExecutionException {
		this.lock.lock();
		try {
			if (!this.done)
				this.notNull.await();
		} finally {
			this.lock.unlock();
		}
		return this.value;
	}

	public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		this.lock.lock();
		try {
			if (!this.done) {
				boolean success = this.notNull.await(timeout, unit);
				if (!success)
					throw new TimeoutException();
			}
		} finally {
			this.lock.unlock();
		}
		this.lock.unlock();

		return this.value;
	}

	public boolean isCancelled() {
		return this.canceled;
	}

	public boolean isDone() {
		return this.done;
	}

	public void done(V value) {
		this.lock.lock();
		try {
			if (!this.done) {
				this.value = value;
				this.done = true;
				this.notNull.signalAll();
			}
		} finally {
			this.lock.unlock();
		}
		for (AsyncListener listener : this.listenerList)
			listener.asyncReturn(value);
	}

	public synchronized void addAsyncListener(AsyncListener<V> listener) {
		this.listenerList.add(listener);
	}
}