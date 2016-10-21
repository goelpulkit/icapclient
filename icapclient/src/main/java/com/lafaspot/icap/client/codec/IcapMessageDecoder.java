package com.lafaspot.icap.client.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;

import java.util.List;

import javax.annotation.Nonnull;

import com.lafaspot.logfast.logging.Logger;

public class IcapMessageDecoder extends ReplayingDecoder<IcapMessage> {

    private final Logger logger;

    public IcapMessageDecoder(@Nonnull Logger logger) {
        super(new IcapMessage(logger));
        this.logger = logger;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
        logger.debug("<- replay ri " + buf.readerIndex() + ", wi " + buf.writerIndex() + ", th " + Thread.currentThread().getId(), null);
        IcapMessage msg = state();
        msg.parse(buf, this);
        if (msg.parsingDone()) {
            out.add(msg);
        }
    }
}
