package com.lafaspot.icap.client;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.lafaspot.icap.client.exception.IcapException;
import com.lafaspot.logfast.logging.LogContext;
import com.lafaspot.logfast.logging.LogManager;
import com.lafaspot.logfast.logging.Logger;
import com.lafaspot.logfast.logging.Logger.Level;

public class IcapClientTest {

    private static final long CONNECT_TIMEOUT_MILLIS = 500;
    private static final long INACTIVITY_TIMEOUT_MILLIS = 1000;

    private LogManager logManager;
    private Logger logger;

    @BeforeClass
    public void init() {
        logManager = new LogManager(Level.DEBUG, 5);
        logManager.setLegacy(true);
        logger = logManager.getLogger(new LogContext(IcapClientTest.class.getName()) {
        });
    }
    @Test
    public void scanBadFile() throws IcapException, IOException, InterruptedException, ExecutionException {

        final String filename = "badAvScanDoc.doc";
        InputStream in = getClass().getClassLoader().getResourceAsStream("badAvScanDoc.doc");
        byte buf[] = new byte[8192];

        int o =0;
        int n = 0;

         while ((o < 8192) && (n = in.read(buf, o, 1)) != -1) {
             o += n;
         }
         byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        IcapClient cli = new IcapClient(2, logManager);
        java.util.concurrent.Future<IcapResult> future = cli.scanFile(uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, filename,
                copiedBuf, null);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
    }

    @Test
    public void scanImgFile() throws IcapException, IOException, InterruptedException, ExecutionException {

        final String filename = "koenigsegg.jpg";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        IcapClient cli = new IcapClient(2, logManager);
        java.util.concurrent.Future<IcapResult> future = cli.scanFile(uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, filename,
                copiedBuf, null);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);

        Assert.assertEquals(r.getCleanedBytes().length, fileLen);
    }

    @Test
    public void scanLogFile() throws Exception {

        final String filename = "somelog.log";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        final int fileLen = in.available();
        byte buf[] = new byte[fileLen];

        int o = 0;
        int n = 0;

        while ((o < fileLen) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }

        URI uri = URI.create("icap://localhost:1344");
        IcapClient cli = new IcapClient(2, logManager);
        java.util.concurrent.Future<IcapResult> future = cli.scanFile(uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, filename,
                buf, null);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
    }

    @Test
    public void scanTestFile() throws IcapException, IOException, InterruptedException, ExecutionException {

        final String filename = "test.log";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        byte buf[] = new byte[8192];

        int o = 0;
        int n = 0;

        while ((o < 8192) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        IcapClient cli = new IcapClient(2, logManager);
        java.util.concurrent.Future<IcapResult> future = cli.scanFile(uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, filename,
                copiedBuf, null);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
    }

    @Test
    public void scanTestFileTwice() throws IcapException, IOException, InterruptedException, ExecutionException {

        final String filename = "test.log";
        // final String filename = "somelog.log";
        InputStream in = getClass().getClassLoader().getResourceAsStream(filename);
        byte buf[] = new byte[8192];

        int o = 0;
        int n = 0;

        while ((o < 8192) && (n = in.read(buf, o, 1)) != -1) {
            o += n;
        }
        byte copiedBuf[] = Arrays.copyOfRange(buf, 0, o);

        URI uri = URI.create("icap://localhost:1344");
        IcapClient cli = new IcapClient(2, logManager);
        java.util.concurrent.Future<IcapResult> future = cli.scanFile(uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, filename,
                copiedBuf, null);
        IcapResult r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);

        future = cli.scanFile(uri, CONNECT_TIMEOUT_MILLIS, INACTIVITY_TIMEOUT_MILLIS, filename, copiedBuf, null);
        r = future.get();
        Assert.assertEquals(r.getNumViolations(), 0);
    }
}
