/**
 *
 */
package com.lafaspot.icap.client.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.codec.IcapMessage;

/**
 * @author kraman
 *
 */
public class IcapChannelHandler extends SimpleChannelInboundHandler<IcapMessage> {

    private final IcapSession session;

    public IcapChannelHandler(@Nonnull IcapSession session) {
        this.session = session;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, IcapMessage msg) throws Exception {
        messageReceived(ctx, msg);
    }

    protected void messageReceived(ChannelHandlerContext ctx, IcapMessage msg) throws Exception {
        session.processResponse(msg);
    }
}
