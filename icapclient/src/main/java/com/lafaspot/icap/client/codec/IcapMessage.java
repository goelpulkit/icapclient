/**
 *
 */
package com.lafaspot.icap.client.codec;

import io.netty.buffer.ByteBuf;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

import com.lafaspot.icap.client.IcapResult;
import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.logfast.logging.Logger;

/**
 * Base IcapMessage object.
 *
 * @author kraman
 *
 */
public class IcapMessage {

    private List<State> nextStates = new ArrayList<State>();
    private State state = State.PARSE_ICAP_MESSAGE;
    private int payloadOffset;

    /** The logger object. */
    private final Logger logger;

    public IcapMessage(@Nonnull Logger logger) {
        this.logger = logger;
    }

    public void reset() {
        state = State.PARSE_ICAP_MESSAGE;
        currentMessage.setLength(0);
        cause = null;
        resPayload = null;
        icapHeaders = null;
        result = new IcapResult();
    }

    public boolean parsingDone() {
        return (state == State.PARSE_DONE);
    }

    public void parse(@Nonnull final ByteBuf buf, @Nonnull IcapMessageDecoder dec) {
        try {
            logger.debug("<- parse in - " + state + " - " + this.hashCode(), null);
            switch (state) {
            case PARSE_ICAP_MESSAGE: {
                if (!parseForHeader(buf, ICAP_ENDOFHEADER_DELIM)) {
                    return;
                }
                String header = currentMessage.toString();
                currentMessage.setLength(0);

                String headers[] = parseHeader(header);

                BasicLineParser lineParser = new BasicLineParser();
                CharArrayBuffer charBuf = new CharArrayBuffer(128);

                int resHdr = -1;
                int resBody = -1;
                String encapsulatedHeaderStr = getEncapsulatedHeader(headers);
                charBuf.setLength(0);
                charBuf.append(encapsulatedHeaderStr);
                Header encapsulateHeder = lineParser.parseHeader(charBuf);
                String encapsulateHeaderVal = encapsulateHeder.getValue();
                if (null != encapsulateHeaderVal) {
                    BasicHeaderValueParser encParser = new BasicHeaderValueParser();

                    charBuf.setLength(0);
                    charBuf.append(encapsulateHeaderVal);
                    ParserCursor encCursor = new ParserCursor(0, encapsulateHeaderVal.length());
                    HeaderElement elems[] = encParser.parseElements(charBuf, encCursor);

                    try {
                        for (int i = 0; i < elems.length; i++) {
                            if (elems[i].getName().startsWith(ICAP_RES_HDR_PREFIX)) {
                                resHdr = Integer.parseInt(elems[i].getValue().trim());
                            }

                            if (elems[i].getName().startsWith(ICAP_RES_BODY_PREFIX)) {
                                resBody = Integer.parseInt(elems[i].getValue().trim());
                            }
                        }
                    } catch (NumberFormatException e) {
                        throw new IcapException(IcapException.FailureType.PARSE_ERROR);
                    }
                }

                logger.debug("<- encap " + encapsulateHeaderVal + ", rh " + resHdr + ", rb " + resBody, null);

                if (-1 != resHdr) {
                    nextStates.add(State.PARSE_RES_HEADER);
                }

                if (-1 != resBody) {
                    // nextStates.add(State.PARSE_RES_BODY);
                    nextStates.add(State.PARSE_RES_PAYLOAD_LENGTH);
                    nextStates.add(State.PARSE_PAYLOAD);
                }

                nextStates.add(State.PARSE_DONE);

                // now handle the ICAP message
                handleIcapMessage(headers);
                state = nextStates.remove(0);
                break;
            }

            case PARSE_RES_HEADER: {
                if (!parseForHeader(buf, ICAP_ENDOFHEADER_DELIM)) {
                    return;
                }

                String header = currentMessage.toString();
                currentMessage.setLength(0);
                String headers[] = parseHeader(header);

                LineParser parser = new BasicLineParser();
                ParserCursor cursor = new ParserCursor(0, headers[0].length());
                CharArrayBuffer statusBuffer = new CharArrayBuffer(64);
                statusBuffer.append(headers[0]);
                StatusLine statusLine = parser.parseStatusLine(statusBuffer, cursor);

                // handle 200 OK
                if (statusLine.getStatusCode() == 200) {
                    // todo
                } else {
                    // todo
                }

                state = nextStates.remove(0);
                logger.debug(" done with parsing body - moving to " + state, null);
                break;
            }

            case PARSE_RES_PAYLOAD_LENGTH: {
                if (!parseForHeader2(buf, dec, ICAP_ENDOFHEADER_DELIM)) {
                    return;
                }
                final String lengthStr = currentMessage.toString().trim();

                currentMessage.setLength(0);
                try {
                    payloadLen = Integer.parseInt(lengthStr, 16);
                } catch (NumberFormatException e) {
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR);
                }

                resPayload = new byte[payloadLen];
                payloadOffset = 0;
                state = nextStates.remove(0);
                logger.debug(" done with parsing payload len=" + payloadLen + " - moving to " + state, null);
                break;
            }

            case PARSE_PAYLOAD:
                if (0 == payloadLen) {
                    // bad
                    logger.debug("bad - payloadLen is 0", null);
                    throw new IcapException(IcapException.FailureType.PARSE_ERROR);
                }
                final int availableLen = buf.writerIndex() - buf.readerIndex();
                final int toReadLen = payloadLen - payloadOffset;

                // the readBytes() API will update readerIndex
                if (toReadLen < availableLen) {
                    buf.readBytes(resPayload, payloadOffset, toReadLen);
                    payloadOffset += toReadLen;

                } else {
                    buf.readBytes(resPayload, payloadOffset, availableLen);
                    payloadOffset += availableLen;
                }
                if (payloadOffset < payloadLen) {
                    logger.debug("more to raad o " + payloadOffset + ", l " + payloadLen + ", ri " + buf.readerIndex(), null);
                    // still more to read
                    return;
                }

                // reset the readIndex to avoid replay
                buf.readerIndex(buf.writerIndex());

                result.setCleanedBytes(resPayload);

                state = nextStates.remove(0);
                logger.debug(" done with parsing payload of " + payloadLen + " bytes - moving to " + state, null);

            case PARSE_DONE:
            default:

            }
        } catch (IcapException e) {

        }
    }

    private void handleIcapMessage(@Nonnull final String headers[]) throws IcapException {
        if (headers[0].startsWith(ICAP_PREFIX)) {

            icapHeaders = headers;
            String toks[] = headers[0].split(" ");
            if (toks.length > 2) {
                int status;
                try {
                    status = Integer.parseInt(toks[1].trim());
                } catch (NumberFormatException e) {
                    throw new IcapException (IcapException.FailureType.PARSE_ERROR);
                }
                logger.debug("-- icap status code " + status, null);
                switch (status) {
                case 201:
                    handleIcap201Ok(headers);
                    break;
                case 200:
                    handleIcap200Ok(headers);
                    break;
                default:
                    throw new IcapException (IcapException.FailureType.PARSE_ERROR);
                }
            }
        }
    }

    private String getEncapsulatedHeader(String headers[]) throws IcapException {

        for (int index = 0; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_ENCAPSULATED_PREFIX)) {
                int j = headers[index].indexOf(ICAP_ENCAPSULATED_PREFIX);
                if (-1 != j) {
                    return headers[index]; // .substring(j + ICAP_RES_BODY_PREFIX.length() + 1);
                }
            }
        }

        throw new IcapException (IcapException.FailureType.PARSE_ERROR);
    }

    private boolean parseForHeader(@Nonnull final ByteBuf buf, @Nonnull final byte[] delim) throws IcapException {
        if (buf.readableBytes() < delim.length) {
            // error
            throw new IcapException (IcapException.FailureType.PARSE_ERROR);
        }
        int eohIdx = 0;
        for (int idx = buf.readerIndex(); idx < buf.writerIndex(); idx++) {
            final char msg = (char) buf.getByte(idx);
            if (msg == delim[eohIdx]) {
                eohIdx++;
                if (eohIdx == (delim.length)) {
                    buf.readerIndex(idx + 1);
                    // remove last 3 bytes because we did not add the 4th delim byte yet
                    currentMessage.setLength(currentMessage.length() - 3);
                    return true;
                } else {
                    currentMessage.append(msg);
                }
            } else {
                currentMessage.append(msg);
                eohIdx = 0;
            }
        }

        // drop the entire message and parse again
        currentMessage.setLength(0);
        return false;
    }

    private boolean parseForHeader2(@Nonnull final ByteBuf buf, @Nonnull IcapMessageDecoder dec, @Nonnull final byte[] delim)
            throws IcapException {

        if (buf.readableBytes() < delim.length) {
            // error
            throw new IcapException (IcapException.FailureType.PARSE_ERROR);
        }
        int eohIdx = 0;
        int newEohLen = 2;
        for (int idx = buf.readerIndex(); idx < buf.writerIndex(); idx++) {
            final char msg = (char) buf.getByte(idx);
            if (msg == delim[eohIdx]) {
                eohIdx++;
                if (eohIdx == (delim.length)) {
                    // remove last 3 bytes
                    currentMessage.setLength(currentMessage.length() - 3);
                    buf.readerIndex(idx + 1);
                    return true;
                } else {
                    currentMessage.append(msg);
                }
            } else {
                if (newEohLen == eohIdx) {
                    // remove last 2 bytes, the 0xA and 0xD
                    currentMessage.setLength(currentMessage.length() - newEohLen);
                    buf.readerIndex(idx + 1);
                    return true;
                } else {
                    currentMessage.append(msg);
                    eohIdx = 0;
                }
            }
        }

        currentMessage.setLength(0);
        return false;
    }

    private void handleIcap200Ok(@Nonnull final String headers[]) throws IcapException {
        int index = 1;
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_ENCAPSULATED_PREFIX)) {
                int j = headers[index].indexOf(ICAP_RES_BODY_PREFIX);
                if (-1 != j) {
                    String resBodyStr = headers[index].substring(j + ICAP_RES_BODY_PREFIX.length() + 1);
                    int resBodyVal;
                    try {
                        resBodyVal = Integer.parseInt(resBodyStr.trim());
                        result.setNumViolations(0);
                        break;
                    } catch (NumberFormatException e) {
                        throw new IcapException (IcapException.FailureType.PARSE_ERROR);
                    }

                } else if (headers[index].indexOf(ICAP_NULL_BODY_PREFIX) != -1) {
                    // done
                    return;
                }
            }
        }
    }

    private void handleIcap201Ok(@Nonnull final String headers[]) throws IcapException {

        // skip first status line
        int index = 1;
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_VIOLATIONS_PREFIX)) {
                break;
            }
        }

        int numViolations;
        if (index < headers.length) {
            final int k = headers[index].indexOf(':');
            if (-1 != k) {
                try {
                    numViolations = Integer.parseInt(headers[index].substring(k + 1).trim());
                    result.setNumViolations(numViolations);
                } catch (NumberFormatException e) {
                    throw new IcapException (IcapException.FailureType.PARSE_ERROR);
                }
            } else {
                throw new IcapException (IcapException.FailureType.PARSE_ERROR);
            }
            // increment
            index++;

            // validate header size
            if (index + (4 * numViolations) < headers.length) {
                // look at first violation only
                result.setViolationFilename(headers[index++]);
                result.setViolationName(headers[index++]);
                result.setViolationId(headers[index++]);
                String dispositionStr = headers[index++];
                result.setDispositionAsStr(dispositionStr);
            } else {
                throw new IcapException (IcapException.FailureType.PARSE_ERROR);
            }

        } else {
            throw new IcapException (IcapException.FailureType.PARSE_ERROR);
        }
    }

    /**
     * Get AV scan result.
     *
     * @return
     */
    public IcapResult getResult() {
        return result;
    }

    /**
     * Get AV scan failure cause.
     *
     * @return
     */
    public Exception getCause() {
        return cause;
    }

    public OutputStream getResponseStream() {
        return resStream;
    }

    @Override
    public String toString() {
        final StringBuffer buf = new StringBuffer();
        if (null != cause) {
            buf.append(cause);
        }

        if (null != icapHeaders) {
            for (int i = 0; i < icapHeaders.length; i++) {
                buf.append(icapHeaders[i]);
                buf.append("\n");
            }
        }

        return buf.toString();
    }

    /**
     * Parse ICAP headers.
     *
     * @param buf
     * @return headers
     */
    private String[] parseHeader(final String buf) {
        final Pattern pat = Pattern.compile("\r\n");
        return pat.split(buf);
    }

    public class Options {

        private final String optionPrefix[] = { "X-Allow-Out:", "Preview:", "Max-Connections:" };

        private final List<String> optionHeaders = new ArrayList<String>();
        public Options (@Nonnull final String headers[]) {
            for (int i = 0; i < headers.length; i++) {
                for (int j = 0; j < optionPrefix.length; j++) {
                    if (headers[i].startsWith(optionPrefix[j])) {
                        optionHeaders.add(headers[j]);
                    }
                }
            }
        }

        public String[] getHeaders() {
            return (String[]) optionHeaders.toArray();
        }
    }

    private IcapResult result = new IcapResult();
    private String icapHeaders[];

    private Exception cause;


    private OutputStream resStream;

    private static final String ICAP_PREFIX = "ICAP/1.0";

    private static final String ICAP_VIOLATIONS_PREFIX = "X-Violations-Found:";
    private static final String ICAP_ENCAPSULATED_PREFIX = "Encapsulated:";

    private static final String ICAP_RES_BODY_PREFIX = "res-body";
    private static final String ICAP_RES_HDR_PREFIX = "res-hdr";
    private static final String ICAP_NULL_BODY_PREFIX = "null-body";

    private static final byte[] ICAP_ENDOFHEADER_DELIM = { '\r', '\n', '\r', '\n' };
    private static final byte[] ICAP_ENDOFPAYLOAD_DELIM = { '\0' };
    StringBuffer currentMessage = new StringBuffer(1024);
    int payloadLen;
    byte resPayload[];

    enum State {
        PARSE_ICAP_MESSAGE, PARSE_RES_HEADER, PARSE_RES_BODY, PARSE_RES_PAYLOAD_LENGTH, PARSE_PAYLOAD, PARSE_DONE
    }

}
