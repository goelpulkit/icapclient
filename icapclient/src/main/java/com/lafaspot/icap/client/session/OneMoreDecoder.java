/**
 *
 */
package com.lafaspot.icap.client.session;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

/**
 * @author kraman
 *
 */
public class OneMoreDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (out.size() > 0) {
            System.out.println(out.get(0));
        }
    }

}
