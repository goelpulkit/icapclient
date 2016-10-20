package com.lafaspot.icap.client.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

public class IcapMessageDecoder extends ReplayingDecoder<IcapMessage> {

    public IcapMessageDecoder() {
        super(new IcapMessage());
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        System.out.println("<- replay ri " + buf.readerIndex() + ", wi " + buf.writerIndex() + ", th " + Thread.currentThread().getId());
        IcapMessage msg = state();
        msg.parse(buf, this);
        if (msg.parsingDone()) {
            out.add(msg);
        }
    }
}
