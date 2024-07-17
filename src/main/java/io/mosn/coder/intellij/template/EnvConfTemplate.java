package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.option.TraceOption;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class EnvConfTemplate extends AbstractScriptTemplate {

    public static final String Name = "env_conf";

    public static final String Path = "etc/ant";

    public static Source envConfSource(PluginOption option) {

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append("FEATURE_GATES=CloudMultiTenantMode=true,CloudTraceEnabled=true,CloudRateLimiterEnabled=true,CloudXProtocolEnabled=true,CloudSpringCloudEnabled=true,CloudMetricsPushEnabled=false,MOSN_FEATURE_MSG_ENABLE=false,MOSN_FEATURE_ZQUEUE_ENABLE=false,MOSN_FEATURE_ANTQ_ENABLE=false,MOSN_FEATURE_SOFARPC_ENABLE=true,MOSN_FEATURE_GATEWAY_ENABLE=false,MOSN_FEATURE_TRAAS_ENABLE=false,MOSN_FEATURE_MVC_ENABLE=false,MOSN_FEATURE_REST_ENABLE=false")
                .append("DBMODE=dbmode")
                .append("ZMODE=false")
                .append("SOFA_INSTANCE_ID=000001")
                .append("SOFA_ACCESS_KEY=")
                .append("AUTO_REGISTER_MODE=true")
                .append("APPNAME=please-change-your-own-app-name")
                .append("ANT_MONITOR_ADDRESS=111")
                .append("DOMAINNAME=rz00a.alipay.net")
                .append("SOFA_CAFE_CLUSTER_NAME=cb7aaa7a31fd0490299c530e83a868231")
                .append("PILOT_SERVER_ADDRESS=127.0.0.1")
                .append("POD_NAMESPACE=contentfusion")
                .append("POD_NAME=bar-mosn-tls-deployment-db75c5d64-mwrlw")
                .append("SOFA_CAFE_TENANT_NAME=ALIPAYCN")
                .append("SOFA_CAFE_WORKSPACE_NAME=middleware")
                .append("RUNTIME_ENV=publiccloud")
                .append("SOFA_ANTVIP_ENDPOINT=127.0.0.1")
                .append("TRACING_SWITCH=SOFATracer")
                .append("DRM_INIT_ENABLE=false")
                .append("MOSN_ACTUATOR_MTLS_ENABLED=false")
                .append("REGISTRY_END_POINT_LISTEN_ADDRESS=0.0.0.0")
                .append("MOSN_PUB_IP_ENV=PUB_BOLT_LOCAL_IP")
                .append("MOSN_PUB_LOCAL_IP_ENV=PUB_BOLT_LOCAL_IP")
                .append("PUB_BOLT_LOCAL_IP");

        buffer.with("PLUGINS_ACTIVE=[");

        appendPluginActiveEnv(option, buffer);

        buffer.append("]")
                .append("DEV_MATCHER_ENABLE=true")
                .append("DEBUG_TRANSCODER_ENABLE=true");

        buffer.append("LOCAL_AUTH_SWITCH=false");
        buffer.append("DEBUG_MODE=true");

        if (option instanceof TraceOption) {
            buffer.append("MOSN_GENERATOR_SAPN_ENABLED=true");

            switch (((TraceOption) option).getRealTraceType()) {
                case TraceOption.SKY_WALKING: {
                    buffer.append("SKY_WALKING_ADDRESS=${PUB_BOLT_LOCAL_IP}:11800");
                    break;
                }
                case TraceOption.ZIP_KIN: {
                    buffer.append("ZIPKIN_ADDRESS=http://${PUB_BOLT_LOCAL_IP}:9411/api/v2/spans");
                    break;
                }
            }
        }

        String content = buffer.toString();

        return new Source(Name, Path, content);
    }

    private static void appendPluginActiveEnv(PluginOption option, CodeBuilder buffer) {

        if (option == null) return;

        // append active plugin
        if (option instanceof ProtocolOption) {
            ProtocolOption opt = (ProtocolOption) option;
            StringBuilder buf = new StringBuilder();
            buf.append("{\"kind\":\"protocol\",\"plugins\":[");
            {
                // append protocol plugin
                String name = opt.getPluginName().toLowerCase();
                buf.append("{\"name\":\"").append(name).append("\",\"version\":\"default\"}");
                if (opt.getEmbedded() != null && !opt.getEmbedded().isEmpty()) {
                    for (ProtocolOption o : opt.getEmbedded()) {
                        // append embed proto name
                        name = o.getPluginName().toLowerCase();
                        buf.append(",")
                                .append("{\"name\":\"").append(name).append("\",\"version\":\"default\"}");
                    }
                }
            }
            buf.append("]}");

            buffer.with(buf.toString());
            return;
        }

        StringBuilder buf = new StringBuilder();
        buf.append("{\"kind\":\"").append(option.pluginTypeDescriptor()).append("\",\"plugins\":[");
        {
            // append transcoder plugin
            String name = option.getPluginName().toLowerCase();
            buf.append("{\"name\":\"").append(name).append("\",\"version\":\"default\"}");
        }
        buf.append("]}");

        buffer.with(buf.toString());
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(envConfSource(option));
    }

}