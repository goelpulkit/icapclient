/**
 *
 */
package com.lafaspot.icap.client.session;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

/**
 * Handles channel inactivity.
 *
 * @author kraman
 *
 */
public class IcapInactivityHandler extends IdleStateHandler {

    /** the session object. */
    private final IcapSession session;

    /**
     * Constructor to handle inactivity events.
     *
     * @param session the session
     * @param inactivityTimeout timeout value
     */
    public IcapInactivityHandler(@Nonnull final IcapSession session, final long inactivityTimeout) {
        super(0, 0, inactivityTimeout, TimeUnit.MILLISECONDS);
        this.session = session;
    }

    @Override
    protected void channelIdle(ChannelHandlerContext ctx, IdleStateEvent evt) {
        System.out.println(" <-> channel inactive " + evt.state());
        if (evt.state() == IdleState.ALL_IDLE) {
            ctx.close();
        }
        session.onTimeout();
    }
}
