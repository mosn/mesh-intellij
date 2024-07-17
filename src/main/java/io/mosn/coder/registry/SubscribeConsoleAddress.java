package io.mosn.coder.registry;

import io.mosn.coder.common.Constants;
import io.mosn.coder.common.NetUtils;
import io.mosn.coder.common.URL;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

/**
 * @author yiji@apache.org
 */
public class SubscribeConsoleAddress {

    public static final String ConsoleDataId = "com.alipay.sofa.cloud.middleware.plugin.server";

    public static final String INSTANCE_KEY = "SOFA_INSTANCE_ID";

    public static final String APP_KEY = "APPNAME";

    public static final String ENDPOINT_KEY = "SOFA_ANTVIP_ENDPOINT";

    public static final String ACCESS_KEY = "SOFA_ACCESS_KEY";

    public static final String SECRET_KEY = "SOFA_SECRET_KEY";

    public static final String PLUGIN_CONF = "PLUGIN_CONF";

    static ClassLoader pluginClassloader = null;


    private static final Map<String, DefaultNotify> notifies = new ConcurrentHashMap<>();

    public static DefaultNotify getProjectNotify(String project) {

        if (project != null) {

            /**
             * direct mesh server
             */
            File file = new File(project, "application.properties");
            if (file.exists()) {

                Properties properties = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    properties.load(in);

                    String address = properties.getProperty("mesh.server.address");
                    if (address != null && (address = address.trim()).length() > 0) {
                        /**
                         * append default port
                         */
                        if (!address.contains(":")) {
                            address += ":7777";
                        }

                        List<URL> urls = new ArrayList<>();
                        urls.add(URL.valueOf("go://" + address));

                        DefaultNotify defaultNotify = new DefaultNotify(null, null);
                        defaultNotify.urls = urls;

                        return defaultNotify;
                    }

                } catch (Exception e) {
                    /**
                     * fall back to registry
                     */
                    e.printStackTrace();
                }
            }

            /**
             * read project config, etc/ant/env_config
             */
            String confPath = System.getProperty(PLUGIN_CONF);
            if (confPath == null || confPath.length() == 0) {
                confPath = project + (project.endsWith("/") ? "" : "/") + "etc/ant/env_conf";
            }

            file = new File(confPath);
            if (!file.exists()) {
                /**
                 * support for offline deploy,
                 * look for the env_conf configuration
                 * in the project directory
                 */
                file = new File(project, "env_conf");
            }

            if (file.exists()) {

                Properties properties = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    properties.load(in);
                } catch (Exception e) {
                    return null;
                }

                URL registry = getRegistryUrl(properties);
                return getMeshServerAddress(registry);
            }
        }

        return null;

    }

    public static DefaultNotify getMeshServerAddress(URL registry) {
        String key = registry.toFullString();
        DefaultNotify notify = notifies.get(key);

        if (notify == null) {
            /**
             * create registry and notify
             */

            synchronized (notifies) {

                notify = notifies.get(key);
                if (notify != null) {
                    return notify;
                }

                DsrRegistry dsrRegistry = new DsrRegistry(registry);

                URL subscribeUrl = new URL("go", registry.getHost(), 0);

                /**
                 * copy registry url parameters.
                 */
                subscribeUrl = subscribeUrl.addParameters(registry.getParameters());

                /**
                 * update subscribe dataId
                 */
                subscribeUrl = subscribeUrl.setServiceInterface(ConsoleDataId);
                notify = new DefaultNotify(dsrRegistry, subscribeUrl);
                dsrRegistry.subscribe(subscribeUrl, notify);

                notifies.put(key, notify);
            }
        }

        if (notifies.size() > 1) {
            for (DefaultNotify defaultNotify : notifies.values()) {
                if (!defaultNotify.getUrl().getHost()
                        .equals(notify.getUrl().getHost())) {
                    defaultNotify.getRegistry()
                            .unSubscribe(defaultNotify.getUrl(), defaultNotify);

                    /**
                     * remove from cache.
                     */
                    notifies.remove(defaultNotify.getRegistry().getUrl().toFullString());
                }
            }
        }

        return notify;
    }

    public static Tenant getCurrentTenant(String project) {
        Tenant tenant = new Tenant();

        if (project != null) {

            /**
             * read project config, etc/ant/env_config
             */
            String confPath = System.getProperty(PLUGIN_CONF);
            if (confPath == null || confPath.length() == 0) {
                confPath = project + (project.endsWith("/") ? "" : "/") + "etc/ant/env_conf";
            }

            File file = new File(confPath);
            if (!file.exists()) {
                /**
                 * support for offline deploy,
                 * look for the env_conf configuration
                 * in the project directory
                 */
                file = new File(project, "env_conf");
            }

            if (file.exists()) {

                Properties properties = new Properties();
                try (FileInputStream in = new FileInputStream(file)) {
                    properties.load(in);
                } catch (Exception e) {
                    return tenant;
                }

                String instanceId = properties.getProperty(INSTANCE_KEY);
                tenant.instanceId = instanceId;

                String debugTimeout = properties.getProperty(Constants.DEBUG_TIMEOUT);
                if (debugTimeout != null && debugTimeout.length() > 0) {
                    tenant.debugTimeout = Long.parseLong(debugTimeout);
                }
            }
        }

        return tenant;

    }

    private static URL getRegistryUrl(Properties properties) {
        String endpoint = properties.getProperty(ENDPOINT_KEY);
        String app = properties.getProperty(APP_KEY);
        String instanceId = properties.getProperty(INSTANCE_KEY);
        String access = properties.getProperty(ACCESS_KEY);
        String secret = properties.getProperty(SECRET_KEY);

        URL registry = new URL(Constants.REGISTRY_PROTOCOL, endpoint, 0);
        registry = registry.addParameters(Constants.APPLICATION_KEY, app
                , Constants.INSTANCE_ID_KEY, instanceId
                , Constants.ACCESS_KEY, access
                , Constants.SECRET_KEY, secret
                , Constants.ENV_KEY, Constants.SHARED);
        return registry;
    }

    public static class DefaultNotify implements NotifyListener {

        DsrRegistry registry;

        URL url;

        List<URL> urls = new ArrayList<>();

        Runnable callback;

        public DefaultNotify(DsrRegistry registry, URL url) {
            this.registry = registry;
            this.url = url;
        }

        @Override
        public List<URL> filter(List<String> dataSet) {
            List<URL> urlSet = new ArrayList<>();
            if (!dataSet.isEmpty()) {
                for (String u : dataSet) {
                    if (u != null && u.length() > 0) {
                        urlSet.add(URL.valueOf(u));
                    }
                }
            }
            return urlSet;
        }

        @Override
        public void notify(List<URL> urls) {
            if (!urls.isEmpty()) {
                this.urls.clear();
                this.urls.addAll(urls);

                if (this.callback != null) {
                    this.callback.run();
                }
            }
        }

        public List<URL> getUrls() {
            return this.urls;
        }

        public URL getUrl() {
            return url;
        }

        public DsrRegistry getRegistry() {
            return registry;
        }

        public Runnable getCallback() {
            return callback;
        }

        public void setCallback(Runnable callback) {
            this.callback = callback;

            if (!this.urls.isEmpty() && callback != null) {
                this.callback.run();
            }
        }
    }

    public static class Tenant {

        private String instanceId;

        private Long debugTimeout;

        public String getInstanceId() {
            return instanceId;
        }

        public void setInstanceId(String instanceId) {
            this.instanceId = instanceId;
        }


        public Long getDebugTimeout() {
            return debugTimeout;
        }

        public void setDebugTimeout(Long debugTimeout) {
            this.debugTimeout = debugTimeout;
        }
    }

    public static void main(String[] args) throws InterruptedException {

//        URL url = URL.valueOf("registry://11.158.39.184?application=please-change-your-own-app-name&instanceId=000001");
//        URL url = URL.valueOf("registry://100.83.19.27?application=please-change-your-own-app-name&instanceId=000001");

        URL url = URL.valueOf("registry://100.69.196.239?application=please-change-your-own-app-name&instanceId=000001");

        DsrRegistry registry = new DsrRegistry(url);

        URL subscribeUrl = new URL("plugin", NetUtils.getLocalHost(), 0);
        subscribeUrl = subscribeUrl.setServiceInterface(ConsoleDataId);
        subscribeUrl = subscribeUrl.addParameter(Constants.APPLICATION_KEY, "please-change-your-own-app-name");

        registry.subscribe(subscribeUrl, new NotifyListener() {
            @Override
            public List<URL> filter(List<String> dataSet) {
                System.out.println(dataSet);
                return null;
            }

            @Override
            public void notify(List<URL> urls) {
                System.out.println(urls);
            }
        });

        CountDownLatch latch = new CountDownLatch(1);

        latch.await();
    }

}
