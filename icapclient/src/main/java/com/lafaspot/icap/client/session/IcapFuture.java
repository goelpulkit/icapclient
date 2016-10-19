/**
 *
 */
package com.lafaspot.icap.client.session;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.IcapResult;

/**
 * @author kraman
 *
 */
public class IcapFuture implements Future<IcapResult> {

    private final AtomicReference<IcapSession> sessionRef = new AtomicReference<IcapSession>();
    private final AtomicBoolean isDone = new AtomicBoolean(false);
    private final AtomicBoolean isCanceled = new AtomicBoolean(false);
    private final AtomicReference<Exception> causeRef = new AtomicReference<Exception>();
    private final Object lock = new Object();
    private final AtomicReference<IcapResult> resultRef = new AtomicReference<IcapResult>();
    /** Wait interval when the user calls get(). */
    private static final int GET_WAIT_INTERVAL_MILLIS = 1000;

    public IcapFuture(@Nonnull IcapSession session) {
        this.sessionRef.set(session);
    }

    public boolean cancel(boolean mayInterruptIfRunning) {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isCancelled() {
        // TODO Auto-generated method stub
        return false;
    }

    public boolean isDone() {
        return isDone.get();
    }

    /**
     * Invoked when the worker has completed its processing.
     *
     * @param canceled true if the call was the result of a cancellation
     */
    protected void done(@Nonnull final IcapResult result) {
        synchronized (lock) {
            if (!isDone.get()) {
                IcapSession session = sessionRef.get();
                if (sessionRef.compareAndSet(session, null)) {
                    resultRef.set(result);
                    isDone.set(true);
                }
            }
            lock.notify();
        }
    }

    /**
     * Invoked when the worker throws an exception.
     *
     * @param cause the exception that caused execution to fail
     */
    protected void done(final Exception cause) {
        synchronized (lock) {
            if (!isDone.get()) {
                IcapSession session = sessionRef.get();
                if (sessionRef.compareAndSet(session, null)) {
                    causeRef.set(cause);
                    isDone.set(true);
                }
            }
            lock.notify();
        }
    }

    public IcapResult get() throws InterruptedException, ExecutionException {
        synchronized (lock) {
            while (!isDone.get()) {
                lock.wait(GET_WAIT_INTERVAL_MILLIS);
            }
            lock.notify();
        }
        if (causeRef.get() != null) {
            throw new ExecutionException(causeRef.get());
        } else if (isCancelled()) {
            throw new CancellationException();
        } else {
            return resultRef.get();
        }
    }

    public IcapResult get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        synchronized (lock) {
            if (!isDone.get()) {
                lock.wait(unit.toMillis(timeout));
            }
            lock.notify();
        }
        if (isDone.get()) {
            if (causeRef.get() != null) {
                throw new ExecutionException(causeRef.get());
            } else if (isCancelled()) {
                throw new CancellationException();
            } else {
                return resultRef.get();
            }
        } else {
            throw new TimeoutException("Timeout reached.");
        }
    }

}
