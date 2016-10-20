/**
 *
 */
package com.lafaspot.icap.client.session;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.GenericFutureListener;

import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.LogManager;

import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.codec.IcapMessageDecoder;
import com.lafaspot.icap.client.codec.IcapMessageOld;
import com.lafaspot.icap.client.codec.IcapOptions;
import com.lafaspot.icap.client.codec.IcapRespmod;
import com.lafaspot.icap.client.exception.IcapException;

/**
 * IcapSession - identifies a session that represents one scan request.
 *
 * @author kraman
 *
 */
public class IcapSession {

    /**
     * Creates a ICAP session.
     *
     * @param sessionId identifier for the session
     * @param uri remote IMAP server URI
     * @param connectTimeout timeout for socket connection
     * @param inactivityTimeout channel inactivity timeout
     * @param bootstrap the bootstrap
     * @param logManager the LogManager instance
     * @throws IMAPSessionException on SSL or connect failure
     */
    public IcapSession(@Nonnull final String sessionId, @Nonnull final URI uri, final long connectTimeout, final long inactivityTimeout,
            @Nonnull final Bootstrap bootstrap, @Nonnull final LogManager logManager) throws IcapException {
        this.serverUri = uri;
        this.bootstrap = bootstrap;
        this.connectTimeout = connectTimeout;
        this.inactivityTimeout = inactivityTimeout;
    }

    /**
     * Request to scan an file, a request will be sent to the Symantec AV server to scan the request to clean/determine if the file is clean.
     *
     * @param filename name of the file to be scanned
     * @param fileToScan byte stream of the file to be scanned
     * @param scannedFile byte stream of the cleaned file
     * @return the future object
     * @throws IcapException on failure
     */
    public Future<IcapResult> scanFile(@Nonnull String filename, @Nonnull byte[] fileToScan, @Nullable OutputStream scannedFile)
            throws IcapException {

        if (!isUsed.compareAndSet(false, true)) {
            throw new IcapException("session already in use.");
        }

        this.filename = filename;
        this.fileToScan = fileToScan;
        this.outStream = scannedFile;

        ChannelFuture f;
        try {
            // sync wait for connect to complete todo make this async
            channel = bootstrap.connect(serverUri.getHost(), serverUri.getPort()).sync().channel();
            channel.pipeline().addLast("inactivityHandler", new IcapInactivityHandler(this, inactivityTimeout));
            channel.pipeline().addLast(new IcapMessageDecoder());
            channel.pipeline().addLast(new IcapChannelHandler(this));
            final IcapSession thisSession = this;
            channel.closeFuture().addListener(new GenericFutureListener() {
                public void operationComplete(io.netty.util.concurrent.Future future) throws Exception {
                    thisSession.onDisconnect();
                }
            });

            if (!channel.isActive()) {
                throw new IcapException("not connected");
            } else {
                System.out.println("connected, sending");
                stateRef.set(IcapSessionState.INIT.OPTIONS);
                f = channel.writeAndFlush(new IcapOptions(serverUri).getMessage());

            }

        } catch (InterruptedException e) {
            throw new IcapException("not connected");
        }

        futureRef.set(new IcapFuture(this));
        return futureRef.get();
    }

    public void processResponse(@Nonnull final IcapMessageOld msg) {

        System.out.println(" messageReceived in " + stateRef.get() + ", [\r\n" + msg.toString() + "\r\n]");
        switch (stateRef.get()) {
        case OPTIONS:
            if (null != msg.getCause()) {
                System.out.println("options failed " + msg.getCause());
                futureRef.get().done(msg.getCause());
            } else {
                stateRef.set(IcapSessionState.SCAN);
                final IcapRespmod scanReq = new IcapRespmod(serverUri, false, filename, fileToScan);

                System.out.println(" sending scan req " + scanReq.getIcapMessage());
                channel.writeAndFlush(scanReq.getIcapMessage());
                channel.writeAndFlush(scanReq.getInStream());
                byte[] endOfHttp = { '\r', '\n', '0' };
                channel.writeAndFlush(endOfHttp);
            }
            break;
        case SCAN:
            System.out.println(" SCAN state " + msg.toString());
            break;
        }
    }

    /**
     * Callback from netty on receiving a message from the network.
     *
     * @param msg incoming message
     */
    public void processResponse(@Nonnull final IcapMessage msg) {
        System.out.println("<- messageReceived in " + stateRef.get() + ", [\r\n" + msg.toString() + "\r\n]");
        switch (stateRef.get()) {
        case OPTIONS:
            if (null != msg.getCause()) {
                System.out.println("options failed " + msg.getCause());
                futureRef.get().done(msg.getCause());
            } else {
                stateRef.set(IcapSessionState.SCAN);
                final IcapRespmod scanReq = new IcapRespmod(serverUri, false, filename, fileToScan);

                System.out.println(" sending scan req [\r\n" + scanReq.getIcapMessage() + "\r\n]");

                msg.reset();
                channel.writeAndFlush(scanReq.getIcapMessage());
                channel.writeAndFlush(scanReq.getInStream());
                channel.writeAndFlush(scanReq.getTrailerBytes());
                System.out.println(" written payload -> ");
                channel.flush();
            }
            break;
        case SCAN:
            stateRef.set(IcapSessionState.SCAN_DONE);
            if (msg.getCause() != null) {
                System.out.println(" SCAN state - failed " + msg.getCause());
                futureRef.get().done(msg.getCause());
            } else {
                System.out.println(" SCAN state - success " + msg.getResult());
                futureRef.get().done(msg.getResult());
            }
            break;
        case SCAN_DONE:
        default:
        }
    }

    /**
     * Callback from netty on channel inactivity.
     */
    public void onTimeout() {
        System.out.println("**channel timeout** TH " + Thread.currentThread().getId());
        stateRef.set(IcapSessionState.NULL);
        if (null != futureRef.get()) {
            futureRef.get().done(new IcapException("inactivity timeout"));
            futureRef.set(null);
        }
    }

    /**
     * Callback from netty on channel closure.
     */
    public void onDisconnect() {
        System.out.println("**channel disconnected (not-ignored)** TH " + Thread.currentThread().getId());
        stateRef.set(IcapSessionState.NULL);
        if (futureRef.get() != null) {
            futureRef.get().done(new IcapException("channel disconnected"));
            futureRef.set(null);
        }

    }

    /** Reference to the current IcapFuture object. */
    private final AtomicReference<IcapFuture> futureRef = new AtomicReference<IcapFuture>();

    /** pointer to the byte stream of the file to be scanned. */
    private byte[] fileToScan;

    /** pointer to the cleaned/scanned byte stream from the AV server. */
    private OutputStream outStream;

    /** filename of the input file to be scanned. */
    private String filename;

    /** Server to connect to. */
    private final URI serverUri;

    /** Bootstrap. */
    private final Bootstrap bootstrap;

    /** channel inactivity timeout. */
    private final long inactivityTimeout;

    /** socket connect timeout. */
    private final long connectTimeout;

    private final AtomicBoolean isUsed = new AtomicBoolean(false);

    /** Reference to the current state of the session. */
    private AtomicReference<IcapSessionState> stateRef = new AtomicReference<IcapSession.IcapSessionState>(IcapSessionState.NULL);

    /** The channel associated with this session. */
    private Channel channel;

    /** Enum identifying the session states. */
    enum IcapSessionState {
        NULL, INIT, CONNECTED, OPTIONS, OPTIONS_DONE, SCAN, SCAN_DONE
    };

}
