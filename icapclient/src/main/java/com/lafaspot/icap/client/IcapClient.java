/**
 *
 */
package com.lafaspot.icap.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.URI;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.icap.client.session.IcapSession;
import com.lafaspot.logfast.logging.LogManager;

/**
 * IcapClient - used to communicate with Symantec AV server using ICAP protocol.
 *
 * @author kraman
 *
 */
public class IcapClient {

    /**
     * IcapClient constructor.
     *
     * @param threads number of threads to be used in the event loop.
     * @param logManager the logger framework
     */
    public IcapClient(final int threads, @Nonnull LogManager logManager) {

        try {
            this.bootstrap = new Bootstrap();
            this.group = new NioEventLoopGroup(threads);
            this.logManager = logManager;
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new IcapClientInitializer());
        } finally {
            // this.group.shutdownGracefully();
        }
    }

    /**
     * API to scan a file, will return a future object to be polled for result.
     *
     * @param server URI pointing to the Symantec AV scan server
     * @param connectTimeout socket connect timeout value
     * @param inactivityTimeout channel inactivity timeout
     * @param fileName name of the file to be scanned
     * @param toScanFile byte stream of the file to be scanned
     * @return the future object
     * @throws IcapException on failure
     */
    public Future<IcapResult> scanFile(@Nonnull final URI server, final long connectTimeout, final long inactivityTimeout,
            @Nonnull String fileName, @Nonnull byte[] toScanFile) throws IcapException {
        return new IcapSession("abc", server, connectTimeout, inactivityTimeout, bootstrap, logManager).scanFile(fileName, toScanFile);
    }

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for ICAP client. */
    private final EventLoopGroup group;

    /** The logger. */
    private final LogManager logManager;

}
