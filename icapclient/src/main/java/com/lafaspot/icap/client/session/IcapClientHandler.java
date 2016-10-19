package com.lafaspot.icap.client.session;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import javax.annotation.Nonnull;

import com.lafaspot.icap.client.codec.IcapMessageOld;

public class IcapClientHandler extends ChannelInboundHandlerAdapter {

    private final IcapSession session;

    public IcapClientHandler(@Nonnull final IcapSession session) {
        this.session = session;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object buf) {
        final IcapMessageOld m = new IcapMessageOld((ByteBuf) buf);
        System.out.println("<-- (got msg) " + m.toString());
    }


}
