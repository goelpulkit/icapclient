/**
 *
 */
package com.lafaspot.icap.client.codec;

import java.net.URI;

import javax.annotation.Nonnull;

/**
 * Command to send RESPMOD request to AV scan server.
 *
 * @author kraman
 *
 */
public class IcapRespmod {


    final String icapMessage;

    final byte[] inBuffer;

    public IcapRespmod(@Nonnull final URI uri, final boolean keepAlive, @Nonnull String filename, @Nonnull byte[] inBuffer) {
        final StringBuffer buf = new StringBuffer();
        buf.append("RESPMOD icap://");
        buf.append(uri.getHost());
        buf.append(":");
        buf.append(uri.getPort());
        buf.append("/SYMCScanResp-AV ICAP/1.0\r\n");
        buf.append("Host: ");
        buf.append(uri.getHost());
        buf.append("\r\n");
        buf.append("Connection: ");
        if (keepAlive) {
            buf.append("keep-alive\r\n");
        } else {
            buf.append("close\r\n");
        }

        //res header
        final StringBuffer resHdr = new StringBuffer();
        resHdr.append("GET ");
        resHdr.append("/" + filename);
        resHdr.append(" HTTP/1.1\r\n");
        resHdr.append("Host: ");
        resHdr.append(uri.getHost());
        resHdr.append(":");
        resHdr.append(uri.getPort());
        resHdr.append("\r\n");
        resHdr.append("\r\n");

        // res body
        final StringBuffer resBody = new StringBuffer();
        resBody.append("HTTP/1.1 200 OK");
        resBody.append("\r\n");
        resBody.append("\r\n");

        int resHdrLen = resHdr.length();
        int resBodyLen = resHdrLen + resBody.length();

        //encapsulated header
        buf.append("Encapsulated: req-hdr=0, res-hdr=");
        buf.append(resHdrLen);
        buf.append(", res-body=");
        buf.append(resBodyLen);
        buf.append("\r\n");
        buf.append("\r\n");

        buf.append(resHdr);

        buf.append(resBody);

        buf.append(Integer.toHexString(inBuffer.length));
        buf.append("\r\n");
        // buf.append("\r\n");

        this.icapMessage = buf.toString();
        this.inBuffer = inBuffer;
    }

    /**
     * Return the ICAP headers as string.
     *
     * @return ICAP RESPMOD message
     */
    public String getIcapMessage() {
        return icapMessage;
    }

    /**
     * Return the bytes to be scanned.
     *
     * @return byte stream to be scanned
     */
    public byte[] getInStream() {

        // TODO - avoid array copy???
        byte copiedStream[] = new byte[inBuffer.length + TRAILER_BYTES.length];
        System.arraycopy(inBuffer, 0, copiedStream, 0, inBuffer.length);
        System.arraycopy(TRAILER_BYTES, 0, copiedStream, inBuffer.length, TRAILER_BYTES.length);
        return copiedStream;
    }


    private static final byte TRAILER_BYTES[] = { '\r', '\n', '0', '\r', '\n' };
    public byte[] getTrailerBytes() {
        return TRAILER_BYTES;
    }

}
