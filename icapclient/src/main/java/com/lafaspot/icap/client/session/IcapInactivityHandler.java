/**
 *
 */
package com.lafaspot.icap.client.session;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import javax.annotation.Nonnull;

/**
 * @author kraman
 *
 */
public class IcapInactivityHandler extends ChannelDuplexHandler {

    private final IcapSession session;

    public IcapInactivityHandler(@Nonnull final IcapSession session) {
        this.session = session;
    }

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx, final Object evt) throws Exception {
        System.out.println("EEE user event");
    }
}
