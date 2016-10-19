/**
 *
 */
package com.lafaspot.icap.client.session;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateHandler;

import java.io.OutputStream;
import java.net.URI;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.log4j.LogManager;

import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.codec.IcapMessageOld;
import com.lafaspot.icap.client.codec.IcapMessage;
import com.lafaspot.icap.client.codec.IcapMessageDecoder;
import com.lafaspot.icap.client.codec.IcapOptions;
import com.lafaspot.icap.client.codec.IcapRespmod;
import com.lafaspot.icap.client.exception.IcapException;

/**
 * @author kraman
 *
 */
public class IcapSession extends SimpleChannelInboundHandler<ByteBuf> {

    /**
     * Creates a ICAP session.
     *
     * @param sessionId identifier for the session
     * @param uri remote IMAP server URI
     * @param configVal configuration for this session
     * @param bootstrap the bootstrap
     * @param logManager the LogManager instance
     * @throws IMAPSessionException on SSL or connect failure
     */
    public IcapSession(@Nonnull final String sessionId, @Nonnull final URI uri, @Nonnull final Properties configVal,
            @Nonnull final Bootstrap bootstrap, @Nonnull final LogManager logManager) throws IcapException {
        this.serverUri = uri;
        this.bootstrap = bootstrap;
        this.connectTimeout = 0;
        this.inactivityTimeout = 0;
    }

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
            channel.pipeline().addLast("idleStateHandler", new IdleStateHandler(0, 0, inactivityTimeout));
            channel.pipeline().addLast("inactivityHandler", new IcapInactivityHandler(this));
            channel.pipeline().addLast(new IcapMessageDecoder());
            // channel.pipeline().addLast(new OneMoreDecoder());
            channel.pipeline().addLast(new IcapChannelHandler(this));

            if (!channel.isActive()) {
                throw new IcapException("not connected");
            } else {
                System.out.println("connected, sending");
                stateRef.set(IcapSessionState.INIT.OPTIONS);
                f = channel.writeAndFlush(new IcapOptions(serverUri).getMessage());

            }

            // Thread.sleep(10 * 60 * 1000);
            // Thread.sleep(5 * 1000);
        } catch (InterruptedException e) {
            throw new IcapException("not connected");
        }

        futureRef.set(new IcapFuture(this));
        return futureRef.get();
    }

    public IcapResult getResult() {
        return resultRef.get();
    }

    public boolean hasErrors() {
        return (null != cause);
    }

    public Exception getCause() {
        return cause;
    }



    @Override
    public void channelRead(ChannelHandlerContext ctx, Object buf) throws Exception {

        System.out.println(" ch read " + ((ByteBuf) buf).readableBytes());
        final IcapMessageOld msg = new IcapMessageOld((ByteBuf) buf);
        processResponse(msg);
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

            if (msg.getCause() != null) {
                System.out.println(" SCAN state - failed " + msg.getCause());
                futureRef.get().done(msg.getCause());
            } else {
                System.out.println(" SCAN state - success " + msg.getResult());
                futureRef.get().done(msg.getResult());
            }
            break;
        }
    }

    private final AtomicReference<IcapResult> resultRef = new AtomicReference<IcapResult>();
    private final AtomicReference<IcapFuture> futureRef = new AtomicReference<IcapFuture>();
    private Exception cause;

    private byte[] fileToScan;
    private OutputStream outStream;

    private String filename;

    /** Server to connect to. */
    private final URI serverUri;

    /** Bootstrap. */
    private final Bootstrap bootstrap;

    private final int inactivityTimeout;
    private final int connectTimeout;

    private final AtomicBoolean isUsed = new AtomicBoolean(false);

    private AtomicReference<IcapSessionState> stateRef = new AtomicReference<IcapSession.IcapSessionState>(IcapSessionState.NULL);

    private Channel channel;

    enum IcapSessionState {
        NULL, INIT, CONNECTED, OPTIONS, OPTIONS_DONE, SCAN, SCAN_DONE
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        channelRead(ctx, msg);
    };

}
