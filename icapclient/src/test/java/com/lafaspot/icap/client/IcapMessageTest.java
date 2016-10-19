package com.lafaspot.icap.client;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicHeaderValueParser;
import org.apache.http.message.BasicLineParser;
import org.apache.http.message.LineParser;
import org.apache.http.message.ParserCursor;
import org.apache.http.util.CharArrayBuffer;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.lafaspot.icap.client.codec.IcapMessageOld;
import com.lafaspot.icap.client.codec.IcapRespmod;
import com.lafaspot.icap.client.exception.IcapException;

public class IcapMessageTest {

    @Test
    public void test() {

        Pattern pat = Pattern.compile("\r\n");

        final String res = "RESPMOD icap://127.0.0.1/SYMCScanResp-AV ICAP/1.0" + "\r\n" + "Host: 127.0.0.1" + "\r\n"
                + "Connection: keep-alive" + "\r\n" + "Encapsulated: req-hdr=0, res-hdr=43, res-body=19" + "\r\n" + "\r\n";

        BasicHeaderValueParser hvp = new BasicHeaderValueParser();
        String h[] = pat.split(res);
        for (int i = 0; i < h.length; i++) {


            ParserCursor c = new ParserCursor(0, res.length());
            CharArrayBuffer b = new CharArrayBuffer(64);
            b.append(h[i]);
            System.out.println(" === " + i + ", " + hvp.parseElements(b, c));
        }

    }

    @Test
    public void testParseHeaders() throws IcapException {
        final String res = "RESPMOD icap://127.0.0.1/SYMCScanResp-AV ICAP/1.0" + "\r\n" + "Host: 127.0.0.1" + "\r\n"
                + "Connection: keep-alive" + "\r\n" + "Encapsulated: req-hdr=0, res-hdr=43, res-body=19" + "\r\n" + "\r\n"
        + "GET virus.msg HTTP/1.1" + "\r\n"
 + "Host: 127.0.0.1" + "\r\n" + "\r\n"
        + "HTTP/1.1 200 OK" + "\r\n" + "\r\n"
        + "258600" + "\r\n" + "\r\n";

        final String expectedH1 = "RESPMOD icap://127.0.0.1/SYMCScanResp-AV ICAP/1.0" + "\r\n" + "Host: 127.0.0.1" + "\r\n"
                + "Connection: keep-alive" + "\r\n" + "Encapsulated: req-hdr=0, res-hdr=43, res-body=19";

        ByteBuf buf = Unpooled.copiedBuffer(res.getBytes(StandardCharsets.UTF_8));
        IcapMessageOld m = new IcapMessageOld(buf);

    }

    @Test
    public void parseResWithVirus() {
        final String res = "ICAP/1.0 201 Created\r\n" + "ISTag: \"CE2CECDA7FA257776EC1E8B63060EB49\"\r\n"
                + "Date: Thu Oct 13 03:07:36 2016 GMT\r\n" + "Service: Symantec Scan Engine/5.2.11.131\r\n"
                + "Service-ID: SYMCSCANRESP-AV\r\n" + "X-Violations-Found: 3\r\n" + "virus.msg\r\n" + "W32.Beagle.AO@mm\r\n" + "18411\r\n"
                + "2\r\n" + "virus.msg/price.html\r\n" + "W32.Beagle.AO@mm\r\n" + "18411\r\n" + "2\r\n" + "virus.msg/price/price.exe\r\n"
                + "W32.Beagle.AO@mm\r\n" + "18411\r\n" + "2\r\n" + "X-Outer-Container-Is-Mime: 0\r\n"
                + "Encapsulated: res-hdr=0, res-body=83\r\n" + "\r\n" + "10\r\n" + "bbbbbbbbbbbbbbbb";

        ByteBuf buf = Unpooled.copiedBuffer(res.getBytes(StandardCharsets.UTF_8));
        IcapMessageOld msg = new IcapMessageOld(buf);
        Assert.assertNull(msg.getCause());

        Assert.assertEquals(msg.getDisposition(), IcapMessageOld.Disposition.DELETED);
        final String violationFilename = "virus.msg";
        final String violationName = "W32.Beagle.AO@mm";
        final String violationId = "18411";
        final int numViolations = 3;

        Assert.assertEquals(msg.getViolationName(), violationName);
        Assert.assertEquals(msg.getViolationFilename(), violationFilename);
        Assert.assertEquals(msg.getViolationId(), violationId);
        Assert.assertEquals(msg.getNumViolations(), numViolations);
    }

    @Test
    public void testIcapRespmodMessage2() {
        final String msgStr = "ICAP/1.0 200 OK\r\n" + "ISTag: \"CE2CECDA7FA257776EC1E8B63060EB49\"\r\n"
                + "Date: Mon Oct 17 00:27:10 2016 GMT\r\n" + "Service: Symantec Scan Engine/5.2.11.131\r\n"
                + "Service-ID: SYMCSCANRESP-AV\r\n" + "X-Outer-Container-Is-Mime: 0\r\n" + "Encapsulated: res-hdr=0, res-body=57\r\n"
                + "\r\n" + "HTTP/1.1 200 OK\r\n" + "Via: 1.1 Symantec Scan Engine (ICAP)\r\n" + "\r\n" + "4\r\n" + "abcd\r\n\0";

        final IcapMessageOld msg = new IcapMessageOld(Unpooled.copiedBuffer(msgStr.getBytes(StandardCharsets.UTF_8)));
        if (null != msg.getCause()) {
            msg.getCause().printStackTrace();
        }
        Assert.assertNull(msg.getCause());
    }

    @Test
    public void parseResWithoutVirus() {
        final String res = "ICAP/1.0 200 OK\r\n" + "ISTag: \"CE2CECDA7FA257776EC1E8B63060EB49\"\r\n"
                + "Date: Thu Oct 13 03:12:50 2016 GMT\r\n" + "Service: Symantec Scan Engine/5.2.11.131\r\n"
                + "Service-ID: SYMCSCANRESP-AV\r\n" + "X-Outer-Container-Is-Mime: 0\r\n" + "Encapsulated: res-hdr=0, res-body=57"
                + "\r\n\r\n" + "HTTP/1.1 200 OK\r\n" + "Via: 1.1 Symantec Scan Engine (ICAP)\r\n" + "\r\n" + "20\r\n"
                + "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

        ByteBuf buf = Unpooled.copiedBuffer(res.getBytes(StandardCharsets.UTF_8));
        IcapMessageOld msg = new IcapMessageOld(buf);
        if (null != msg.getCause()) {
            msg.getCause().printStackTrace();
        }
        Assert.assertNull(msg.getCause());
        Assert.assertNull(msg.getDisposition());

        Assert.assertNotNull(msg.getResponseStream());
    }

    @Test
    public void t2() {
        URI uri = URI.create("icap://localhost:1344");
        String s = "HTTP/1.1 200 OK";
        LineParser parser = new BasicLineParser();

        ParserCursor cursor = new ParserCursor(0, s.length());
        CharArrayBuffer buf = new CharArrayBuffer(64);
        buf.append(s);


        StatusLine sl = parser.parseStatusLine(buf, cursor);
        System.out.println(sl.getStatusCode());
    }

    @Test
    public void testIcapResmodMessage() {

        final String expected = "RESPMOD icap://127.0.0.1/SYMCScanResp-AV ICAP/1.0\r\n" + "Host: 127.0.0.1\r\n"
                + "Connection: keep-alive\r\n" + "Encapsulated: req-hdr=0, res-hdr=43, res-body=19\r\n" + "\r\n"
                + "GET virus.msg HTTP/1.1\r\n" + "Host: 127.0.0.1\r\n" + "\r\n" + "HTTP/1.1 200 OK\r\n" + "\r\n" + "f\r\n" + "\r\n";

        final byte[] inBuffer = { '0', '1', '2', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
        final String filename = "virus.msg";
        final boolean keepAlive = true;
        final URI uri = URI.create("icap://127.0.0.1:1344");

        final IcapRespmod msg = new IcapRespmod(uri, keepAlive, filename, inBuffer);

        Assert.assertEquals(msg.getInStream(), inBuffer);
        Assert.assertEquals(msg.getIcapMessage(), expected);
    }

    @Test
    public void testHttpHeaderParser() {
        // String tmp = "Encapsulated: req-hdr=0, req-body=23, res-hdr=33, res-body=57, ";
        String tmp = "Encapsulated: null-body=0";

        BasicLineParser lp = new BasicLineParser();
        CharArrayBuffer encBuf = new CharArrayBuffer(128);
        encBuf.append(tmp);
        Header h = lp.parseHeader(encBuf);
        System.out.println(" name [" + h.getName() + "], val[" + h.getValue() + "]");

        encBuf.setLength(0);

        int encIdx = tmp.indexOf("Encapsulated:");
        int resHdr = 0;
        int resBody = 0;
        if (-1 != encIdx) {
            String encapsulatedHeader = tmp.substring("Encapsulated:".length() + 1);
            BasicHeaderValueParser encParser = new BasicHeaderValueParser();
            encBuf.append(encapsulatedHeader);
            ParserCursor encCursor = new ParserCursor(0, encapsulatedHeader.length());
            HeaderElement elems[] = encParser.parseElements(encBuf, encCursor);

            try {
                for (int i = 0; i < elems.length; i++) {
                    if (elems[i].getName().startsWith("res-hdr")) {
                        resHdr = Integer.parseInt(elems[i].getValue().trim());
                    }

                    if (elems[i].getName().startsWith("res-body")) {
                        resBody = Integer.parseInt(elems[i].getValue().trim());
                    }
                }
            } catch (NumberFormatException e) {
                throw e;
            }

            Assert.assertEquals(resHdr, 33);
            Assert.assertEquals(resBody, 57);
        }
    }

}
