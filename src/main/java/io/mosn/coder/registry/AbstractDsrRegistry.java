/*-
 * #%L
 * dubbo-sofa-registry
 * %%
 * Copyright (C) 2016 - 2020 Ant Financial Services Group
 * %%
 * This software is developed by Ant Financial Services Group.This software and all the relevant information,
 * including but not limited to any signs, images, photographs, animations, text, interface design,
 * audios and videos, and printed materials, are protected by copyright laws and other intellectual property laws and treaties.
 * The use of this software shall abide by the laws and regulations as well as Software Installation License Agreement/Software
 * Use Agreement updated from time to time. Without authorization from Ant Financial Services Group ,
 * no one may conduct the following actions:
 *
 * 1) reproduce, spread, present, set up a mirror of, upload, download this software;
 *
 * 2) reverse engineer, decompile the source code of this software or try to find the source code in any other ways;
 *
 * 3) modify, translate and adapt this software, or develop derivative products, works, and services based on this software;
 *
 * 4) distribute, lease, rent, sub-license, demise or transfer any rights in relation to this software,
 * or authorize the reproduction of this software on other computers.
 * #L%
 */

/**
 * Alipay.com Inc.
 * Copyright (c) 2004-2020 All Rights Reserved.
 */
package io.mosn.coder.registry;


import com.alipay.sofa.registry.client.api.Subscriber;
import com.alipay.sofa.registry.client.api.SubscriberDataObserver;
import com.alipay.sofa.registry.client.api.model.RegistryType;
import com.alipay.sofa.registry.client.api.model.UserData;
import com.alipay.sofa.registry.client.api.registration.PublisherRegistration;
import com.alipay.sofa.registry.client.api.registration.SubscriberRegistration;
import com.alipay.sofa.registry.core.model.ScopeEnum;
import io.mosn.coder.common.Constants;
import io.mosn.coder.common.StringUtils;
import io.mosn.coder.common.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author yiji@apche.org
 */
public abstract class AbstractDsrRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDsrRegistry.class.getName());

    private static final String defaultGroup = "SOFA";

    private final Map<String, Subscriber> subscribers = new ConcurrentHashMap<>();

    protected URL url;

    public AbstractDsrRegistry(URL url) {
        this.url = url;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Build dsr registry by url:" + url);
        }
    }

    public URL getUrl() {
        return url;
    }

    public void register(URL url) {

        if (!url.getParameter(Constants.REGISTER_KEY, true)) {
            return;
        }

        String serviceName = buildServiceName(url);
        String serviceData = url.toFullString();

        PublisherRegistration dsrRegistration;

        dsrRegistration = new PublisherRegistration(serviceName);
        addAttributesForPub(dsrRegistration);

        DsrRegistryClient.getClient(this.url).register(dsrRegistration, serviceData);
    }

    public void subscribe(URL url, final NotifyListener listener) {

        if (!url.getParameter(Constants.SUBSCRIBE_KEY, true)) {
            return;
        }

        String serviceName = buildServiceName(url);

        Subscriber listSubscriber = subscribers.get(serviceName);

        if (listSubscriber != null) {

            LOGGER.warn("Service name [" + serviceName + "] have bean registered in sofa registry.");

            CountDownLatch countDownLatch = new CountDownLatch(1);
            handleRegistryData(serviceName, listSubscriber.peekData(), listener, countDownLatch);

            waitAddress(serviceName, countDownLatch);

            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);

        SubscriberRegistration subscriberRegistration = new SubscriberRegistration(
                serviceName, new SubscriberDataObserver() {
            @Override
            public void handleData(String dataId, UserData data) {
                handleRegistryData(dataId, data, listener, latch);
            }
        });

        addAttributesForSub(subscriberRegistration);

        ClassLoader backup = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(DsrRegistryClient.class.getClassLoader());
            listSubscriber = DsrRegistryClient.getClient(this.url).register(subscriberRegistration);
        } finally {
            Thread.currentThread().setContextClassLoader(backup);
        }

        subscribers.put(serviceName, listSubscriber);

        waitAddress(serviceName, latch);
    }

    private void addAttributesForPub(PublisherRegistration dsrRegistration) {
        dsrRegistration.setGroup(defaultGroup);
    }

    private void waitAddress(String serviceName, CountDownLatch countDownLatch) {
        try {
            boolean isWaitedAddress = countDownLatch.await(getWaitAddressTimeout(),
                    TimeUnit.MILLISECONDS);
            if (!isWaitedAddress) {
                LOGGER.warn("Subscribe data failed by dataId " + serviceName);
            }
        } catch (Exception exception) {
            LOGGER.error("Error ", exception);
        }
    }

    public void unRegister(URL url) {
        if (!url.getParameter(Constants.REGISTER_KEY, true)) {
            return;
        }
        String serviceName = buildServiceName(url);
        DsrRegistryClient.getClient(this.url).unregister(serviceName, defaultGroup,
                RegistryType.PUBLISHER);
    }

    public void unSubscribe(URL url, NotifyListener listener) {
        if (!url.getParameter(Constants.SUBSCRIBE_KEY, true)) {
            return;
        }
        String serviceName = buildServiceName(url);
        DsrRegistryClient.getClient(this.url).unregister(serviceName, defaultGroup,
                RegistryType.SUBSCRIBER);
    }

    private void handleRegistryData(String dataId, UserData data, NotifyListener notifyListener, CountDownLatch latch) {
        try {
            if (data != null) {
                List<String> dataSet = flatUserData(data);
                if (notifyListener != null) {
                    List<URL> urls = notifyListener.filter(dataSet);

                    //record filtered change
                    printAddressData(dataId, urls);
                    notifyListener.notify(urls);
                }
            }
        } finally {
            latch.countDown();
        }
    }

    /**
     * 构建关键字
     *
     * @param url URL对象
     * @return dataId
     */
    private String buildServiceName(URL url) {
        StringBuilder buf = new StringBuilder();
        buf.append(url.getServiceInterface());
        String version = url.getParameter(Constants.VERSION_KEY);
        if (StringUtils.isNotEmpty(version) && !"0.0.0".equals(version)) {
            buf.append(":").append(version);
        }

        return buf.toString();
    }

    /**
     * 打印出订阅的配置中心地址
     */
    protected void printAddressData(String dataId, List<URL> urls) {

        List<String> dataSet = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        for (URL provider : urls) {
            sb.append("  >>> ").append(provider).append("\n");
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Receive updated RPC service addresses: service[" + dataId
                    + "]\n  .Available target addresses size [" + dataSet.size() + "]\n" + sb);
        }
    }

    private void addAttributesForSub(SubscriberRegistration dsrRegistration) {
        dsrRegistration.setGroup(defaultGroup);
        dsrRegistration.setScopeEnum(ScopeEnum.global);
    }

    protected List<String> flatUserData(UserData userData) {
        List<String> result = new ArrayList<String>();
        Map<String, List<String>> zoneData = userData.getZoneData();

        for (Map.Entry<String, List<String>> entry : zoneData.entrySet()) {
            result.addAll(entry.getValue());
        }

        return result;
    }

    protected abstract long getWaitAddressTimeout();

}
