package com.nepxion.thunder.protocol.apache;

/**
 * <p>Title: Nepxion Thunder</p>
 * <p>Description: Nepxion Thunder For Distribution</p>
 * <p>Copyright: Copyright (c) 2017-2020</p>
 * <p>Company: Nepxion</p>
 * @author Haojun Ren
 * @email 1394997@qq.com
 * @version 1.0
 */

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.impl.nio.conn.PoolingNHttpClientConnectionManager;
import org.apache.http.impl.nio.reactor.DefaultConnectingIOReactor;
import org.apache.http.impl.nio.reactor.IOReactorConfig;
import org.apache.http.nio.reactor.ConnectingIOReactor;
import org.apache.http.nio.reactor.IOReactorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nepxion.thunder.common.constant.ThunderConstant;
import com.nepxion.thunder.common.property.ThunderProperties;

public class ApacheAsyncClientExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(ApacheAsyncClientExecutor.class);

    private CloseableHttpAsyncClient httpAsyncClient;

    public void initialize(final ThunderProperties properties) throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        Executors.newCachedThreadPool().submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    IOReactorConfig ioReactorConfig = IOReactorConfig.custom()
                            .setIoThreadCount(ThunderConstant.CPUS)
                            .setConnectTimeout(properties.getInteger(ThunderConstant.APACHE_CONNECT_TIMEOUT_ATTRIBUTE_NAME))
                            .setSoTimeout(properties.getInteger(ThunderConstant.APACHE_SO_TIMEOUT_ATTRIBUTE_NAME))
                            .setSndBufSize(properties.getInteger(ThunderConstant.APACHE_SNDBUF_SIZE_ATTRIBUTE_NAME))
                            .setRcvBufSize(properties.getInteger(ThunderConstant.APACHE_RCVBUF_SIZE_ATTRIBUTE_NAME))
                            .setBacklogSize(properties.getInteger(ThunderConstant.APACHE_BACKLOG_SIZE_ATTRIBUTE_NAME))
                            .setTcpNoDelay(true)
                            .setSoReuseAddress(true)
                            .setSoKeepAlive(true)
                            .build();
                    ConnectingIOReactor ioReactor = new DefaultConnectingIOReactor(ioReactorConfig);
                    PoolingNHttpClientConnectionManager httpManager = new PoolingNHttpClientConnectionManager(ioReactor);
                    httpManager.setMaxTotal(ThunderConstant.CPUS * properties.getInteger(ThunderConstant.APACHE_MAX_TOTAL_ATTRIBUTE_NAME));

                    httpAsyncClient = HttpAsyncClients.custom().setConnectionManager(httpManager).build();
                    httpAsyncClient.start();

                    LOG.info("Create apache async client successfully");

                    barrier.await();
                } catch (IOReactorException e) {
                    LOG.error("Create apache async client failed", e);
                }

                return null;
            }
        });

        barrier.await(properties.getLong(ThunderConstant.APACHE_CONNECT_TIMEOUT_ATTRIBUTE_NAME) * 2, TimeUnit.MILLISECONDS);
    }

    public CloseableHttpAsyncClient getClient() {
        return httpAsyncClient;
    }
}