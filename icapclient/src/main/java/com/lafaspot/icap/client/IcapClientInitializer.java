/**
 *
 */
package com.lafaspot.icap.client;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.handler.codec.string.StringEncoder;

/**
 * @author kraman
 *
 */
public class IcapClientInitializer extends ChannelInitializer<SocketChannel> {

    private static final StringEncoder STRING_ENCODER = new StringEncoder();
    // private static IcapMessageDecoder ICAP_DECODER = new IcapMessageDecoder();
    private static final ByteArrayEncoder BYTE_ENCODER = new ByteArrayEncoder();
    private static final ByteArrayDecoder BYTE_DECODER = new ByteArrayDecoder();

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        // ch.pipeline().addLast(ICAP_DECODER);
        ch.pipeline().addLast(STRING_ENCODER);
        ch.pipeline().addLast(BYTE_ENCODER);
        // ch.pipeline().addLast(BYTE_DECODER);
    }

}

