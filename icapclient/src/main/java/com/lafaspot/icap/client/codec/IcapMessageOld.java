/**
 *
 */
package com.lafaspot.icap.client.codec;

import io.netty.buffer.ByteBuf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import org.apache.http.StatusLine;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;

import com.lafaspot.icap.client.exception.IcapException;

/**
 * Base IcapMessage object.
 *
 * @author kraman
 *
 */
public class IcapMessageOld {

    public IcapMessageOld(@Nonnull final ByteBuf buf) {
        this.buf = buf;
        try {
            final String header1 = getHeader();
            final String headers1[] = parseHeader(header1);
            if (headers1.length > 0) {
                if (headers1[0].startsWith(ICAP_PREFIX)) {
                    String toks[] = headers1[0].split(" ");
                    if (toks.length > 2) {
                        int status;
                        try {
                            status = Integer.parseInt(toks[1].trim());
                        } catch (NumberFormatException e) {
                            throw new IcapException("parse error");
                        }
                        icapHeaders = headers1;
                        switch (status) {
                        case 201:
                            handleIcap201Ok(headers1);
                            break;
                        case 200:
                            handleIcap200Ok(headers1);
                            break;
                        default:
                            throw new IcapException("parse error");
                        }
                    }
                }

            }
        } catch (final IcapException e) {
            cause = e;
        }

    }

    public Exception getCause() {
        return cause;
    }

    public String getViolationName() {
        return violationName;
    }

    public String getViolationId() {
        return violationId;
    }

    public String getViolationFilename() {
        return violationFilename;
    }

    public Disposition getDisposition() {
        return disposition;
    }

    public int getNumViolations() {
        return numViolations;
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

    protected void handleHttpresonse(@Nonnull final String headers[]) throws IcapException {
        int index = 0;
        LineParser parser = new BasicLineParser();
        ParserCursor cursor = new ParserCursor(0, headers[index].length());
        CharArrayBuffer statusBuffer = new CharArrayBuffer(64);
        statusBuffer.append(headers[index]);
        StatusLine statusLine = parser.parseStatusLine(statusBuffer, cursor);

        switch (statusLine.getStatusCode()) {
        case 200:
            String line = getLine();
            int length;
            try {
                length = Integer.parseInt(line.trim(), 16);
            } catch (NumberFormatException e) {
                throw new IcapException("parse error");
            }
            readResponseStream(length);
            break;
        default:
            // error
        }

    }

    protected void readResponseStream(final int len) throws IcapException {
        int available = ((buf.readerIndex() + buf.readableBytes()) - buf.readerIndex());
        if (available != len) {
            throw new IcapException("parse error");
        }
        byte buffer[] = new byte[available];

        try {
            buf.readBytes(buffer);
            resStream = new ByteArrayOutputStream();
            resStream.write(buffer);
        } catch (IndexOutOfBoundsException e) {
            throw new IcapException("parse error");
        } catch (IOException e) {
            throw new IcapException("parse error");
        }
    }



    protected void handleIcap200Ok(@Nonnull final String headers[]) throws IcapException {
        int index = 1;
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_ENCAPSULATED_PREFIX)) {
                int j = headers[index].indexOf(ICAP_RES_BODY_PREFIX);
                if (-1 != j) {
                    String resBodyStr = headers[index].substring(j + ICAP_RES_BODY_PREFIX.length() + 1);
                    int resBodyVal;
                    try {
                        resBodyVal = Integer.parseInt(resBodyStr.trim());
                        final String resHeaders[] = parseHeader(getHeader());
                        handleHttpresonse(resHeaders);
                        break;
                    } catch (NumberFormatException e) {
                        throw new IcapException("parse error");
                    }
                } else if (headers[index].indexOf(ICAP_NULL_BODY_PREFIX) != -1) {
                    // done
                    return;
                }
            }
        }
    }

    protected void handleIcap201Ok(@Nonnull final String headers[]) throws IcapException {

        int index = 1;
        for (; index < headers.length; index++) {
            if (headers[index].startsWith(ICAP_VIOLATIONS_PREFIX)) {
                break;
            }
        }

        if (index < headers.length) {
            final int k = headers[index].indexOf(':');
            if (-1 != k) {
                try {
                    numViolations = Integer.parseInt(headers[index].substring(k + 1).trim());
                } catch (NumberFormatException e) {
                    throw new IcapException("parse error");
                }
            } else {
                throw new IcapException("parse error");
            }
            // increment
            index++;

            // validate header size
            if (index + (4 * numViolations) < headers.length) {
                // look at first violation only
                violationFilename = headers[index++];
                violationName = headers[index++];
                violationId = headers[index++];
                String dispositionStr = headers[index++];
                disposition = Disposition.fromStrng(dispositionStr.trim());

                // skip the other violations
                index = index + ((numViolations - 1) * 4);

                // look for Encapsulated:
                for (; index < headers.length; index++) {
                    if (headers[index].startsWith(ICAP_ENCAPSULATED_PREFIX)) {
                        int j = headers[index].indexOf(ICAP_RES_BODY_PREFIX);
                        if (-1 != j) {
                            String resBodyStr = headers[index].substring(j + ICAP_RES_BODY_PREFIX.length() + 1);
                            int resBodyVal;
                            try {
                                resBodyVal = Integer.parseInt(resBodyStr.trim());
                                break;
                            } catch (NumberFormatException e) {
                                throw new IcapException("parse error");
                            }
                        } else if (headers[index].indexOf(ICAP_NULL_BODY_PREFIX) != -1) {
                            // done
                            return;
                        } else {
                            throw new IcapException("parse error");
                        }
                    }
                }
                if (index >= headers.length) {
                    // done
                    return;
                }


            } else {
                throw new IcapException("parse error");
            }

        } else {
            throw new IcapException("parse error");
        }
    }

    /**
     * Parse ICAP headers.
     *
     * @param buf
     * @return headers
     */
    protected String[] parseHeader(final String buf) {
        final Pattern pat = Pattern.compile("\r\n");
        return pat.split(buf);
    }

    protected String getHeader() throws IcapException {
        final byte[] endOfHeader = {'\r', '\n', '\r', '\n'};

        if (buf.readableBytes() < endOfHeader.length) {
            // error
            throw new IcapException("Error in getHeader() method");
        }
        int eohIdx = 0;
        for (int idx = buf.readerIndex(); idx < (buf.readerIndex() + buf.readableBytes()); idx++) {
            final byte msg = buf.getByte(idx);
            if (msg == endOfHeader[eohIdx]) {
                eohIdx++;
                if (eohIdx == (endOfHeader.length)) {
                    final int len = idx - buf.readerIndex() - (endOfHeader.length - 1);
                    final String ret = buf.getCharSequence(buf.readerIndex(), len, StandardCharsets.UTF_8).toString();
                    buf.readerIndex(idx + 1);
                    return ret;
                }
                continue;
            } else {
                eohIdx = 0;
            }
        }

        throw new IcapException("Error in getHeader() method");
    }

    protected String getLine() throws IcapException {
        final byte[] endOfLine = { '\r', '\n' };

        if (buf.readableBytes() < endOfLine.length) {
            // error
            throw new IcapException("Error in getHeader() method");
        }
        int eolIdx = 0;
        for (int idx = buf.readerIndex(); idx < (buf.readerIndex() + buf.readableBytes()); idx++) {
            final byte msg = buf.getByte(idx);
            if (msg == endOfLine[eolIdx]) {
                eolIdx++;
                if (eolIdx == (endOfLine.length)) {
                    final int len = idx - buf.readerIndex() - (endOfLine.length - 1);
                    final String ret = buf.getCharSequence(buf.readerIndex(), len, StandardCharsets.UTF_8).toString();
                    buf.readerIndex(idx + 1);
                    return ret;
                }
                continue;
            } else {
                eolIdx = 0;
            }
        }

        throw new IcapException("Error in getHeader() method");

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

    private String icapHeaders[];

    private String violationFilename;

    private String violationId;

    private String violationName;

    private int numViolations;

    private Exception cause;

    private final ByteBuf buf;

    private OutputStream resStream;

    private static final String ICAP_PREFIX = "ICAP/1.0";

    private static final String ICAP_VIOLATIONS_PREFIX = "X-Violations-Found:";
    private static final String ICAP_ENCAPSULATED_PREFIX = "Encapsulated:";

    private static final String ICAP_RES_BODY_PREFIX = "res-body=";
    private static final String ICAP_RES_HDR_PREFIX = "res-hdr=";
    private static final String ICAP_NULL_BODY_PREFIX = "null-body";

    private Disposition disposition;

    public enum Disposition {
        NOT_FIXED(0), REPAIRED(1), DELETED(2);

        private Disposition(int v) {
            intVal = v;
        }

        public static Disposition fromStrng(@Nonnull final String val) throws IcapException {
            try {
                int intVal = Integer.parseInt(val);
                switch (intVal) {
                case 0:
                    return NOT_FIXED;
                case 1:
                    return REPAIRED;
                case 2:
                    return DELETED;
                default:
                    throw new IcapException("parse error");
                }
            } catch (NumberFormatException e) {
                throw new IcapException("parse error");
            }

        }

        private final int intVal;
    }

}
