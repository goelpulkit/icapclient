/**
 *
 */
package com.lafaspot.icap.client.codec;

import java.net.URI;

/**
 * @author kraman
 *
 */
public class IcapOptions {

    final String message;

    public IcapOptions(final URI uri) {
        final StringBuffer buf = new StringBuffer();
        buf.append("OPTIONS icap://");
        buf.append(uri.getHost());
        buf.append("/SYMCScanResp-AV ICAP/1.0\r\n");
        buf.append("Host:");
        buf.append(uri.getHost());
        buf.append("\r\n");
        buf.append("User-Agent: JEDI ");
        buf.append("ICAP Client/1.1.\r\n");
        buf.append("Encapuslated: null-body=0\r\n");
        buf.append("\r\n");

        message = buf.toString();
    }

    public String getMessage() {
        return message;
    }

}
