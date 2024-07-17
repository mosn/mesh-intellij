
package io.mosn.coder.registry;

import com.alipay.sofa.registry.client.api.RegistryClient;
import com.alipay.sofa.registry.client.factory.RegistryClientFactory;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfig;
import com.alipay.sofa.registry.client.provider.DefaultRegistryClientConfigBuilder;
import io.mosn.coder.common.Constants;
import io.mosn.coder.common.URL;

/**
 * @author yiji@apache.org
 */
public class DsrRegistryClient {

    public static RegistryClient getClient(URL url) {

        DefaultRegistryClientConfig config =
                DefaultRegistryClientConfigBuilder.start()
                        .setEnv(url.getParameter(Constants.ENV_KEY, Constants.SHARED))
                        .setDataCenter(url.getParameter(Constants.DATACENTER_KEY))
                        .setZone(url.getParameter(Constants.ZONE_KEY))
                        .setRegistryEndpoint(url.getParameter(Constants.ENDPOINT_KEY, url.getHost()))
                        .setAppName(url.getParameter(Constants.APPLICATION_KEY))
                        // The registry address is obtained from the default tenant
                        .setInstanceId(/*url.getParameter(Constants.INSTANCE_ID_KEY)*/ "000001")
                        .setAccessKey(url.getParameter(Constants.ACCESS_KEY))
                        .setSecretKey(url.getParameter(Constants.SECRET_KEY))
                        .build();

        /**
         * shit ant vip : com.antcloud.antvip.client.AntVipConfigure#AntVipConfigure(java.lang.String)
         */
        if (config.getInstanceId() != null) {
            System.setProperty("com.alipay.instanceid", config.getInstanceId());
        }

        if (config.getRegistryEndpoint() != null) {
            System.setProperty("com.antcloud.antvip.endpoint", config.getRegistryEndpoint());
        }

        if (config.getAccessKey() != null) {
            System.setProperty("com.antcloud.mw.access", config.getAccessKey());
        }

        if (config.getSecretKey() != null) {
            System.setProperty("com.antcloud.mw.secret", config.getSecretKey());
        }

        // thread safe
        return RegistryClientFactory.getRegistryClient(config);
    }
}