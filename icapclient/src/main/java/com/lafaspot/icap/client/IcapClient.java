/**
 *
 */
package com.lafaspot.icap.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.io.OutputStream;
import java.net.URI;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.icap.client.session.IcapSession;

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
     */
    public IcapClient(final int threads) {

        try {
            this.bootstrap = new Bootstrap();
            this.group = new NioEventLoopGroup(threads);
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
     * @param scannedFile cleaned byte stream
     * @return the future object
     * @throws IcapException on failure
     */
    public Future<IcapResult> scanFile(@Nonnull final URI server, final long connectTimeout, final long inactivityTimeout,
            @Nonnull String fileName, @Nonnull byte[] toScanFile, @Nullable OutputStream scannedFile) throws IcapException {
        return new IcapSession("abc", server, connectTimeout, inactivityTimeout, bootstrap, null).scanFile(fileName, toScanFile,
                scannedFile);
    }

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for ICAP client. */
    private final EventLoopGroup group;

}
