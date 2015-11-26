package io.logz.jmx2graphite;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

/**
 * @author amesika
 *
 */
public class TestGraphiteClient {

    private final static Logger logger = LoggerFactory.getLogger(TestGraphiteClient.class);

    private int port = new Random().nextInt(65000 - 10000) + 10000;
    private DummyGraphiteServer server;

    public void startMockGraphiteServer() {
        // Generate random port between [10000, 65000)
        server = new DummyGraphiteServer(port);
        logger.info("Starting dummy graphite server on port {}", port);
        new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                logger.error("failed: " + e.getMessage(), e);
            }
        }).start();
    }

    public void stopMockGrahiteServer() {
        logger.info("Shutting down mock graphite server");
        server.stop();
    }

    @Test(timeout = 10000)
    public void testOnServerShutdown() throws Exception {
        int connectTimeout = 1000;
        int socketTimeout = 1000;
        GraphiteClient client = new GraphiteClient("bla-host.com", "bla-service", "localhost", port, connectTimeout, socketTimeout, 2000);

        ArrayList<MetricValue> dummyMetrics = Lists.newArrayList(new MetricValue("dice", 4, TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())));

        startMockGraphiteServer();
        client.sendMetrics(dummyMetrics);
        stopMockGrahiteServer();

        try {
            for (int i = 0; i < 1000; i++) {
                client.sendMetrics(dummyMetrics);
            }
        } catch (GraphiteClient.GraphiteWriteFailed e) {
            // Great
            return;
        }
        fail("Send metrics succeeded but server is down");
    }
}