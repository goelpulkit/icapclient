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
 * @author kraman
 *
 */
public class IcapClient {

    public IcapClient(final int threads) {

        try {
            this.bootstrap = new Bootstrap();
            this.group = new NioEventLoopGroup(threads);
            bootstrap.group(group).channel(NioSocketChannel.class).handler(new IcapClientInitializer());
        } finally {
            // this.group.shutdownGracefully();
        }
    }

    public Future<IcapResult> scanFile(@Nonnull final URI server, @Nonnull String fileName, @Nonnull byte[] toScanFile,
            @Nullable OutputStream scannedFile) throws IcapException {
        return new IcapSession("abc", server, null, bootstrap, null).scanFile(fileName, toScanFile, scannedFile);
    }

    /** The netty bootstrap. */
    private final Bootstrap bootstrap;

    /** Event loop group that will serve all channels for ICAP client. */
    private final EventLoopGroup group;

}
