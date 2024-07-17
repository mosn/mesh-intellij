package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.FileReader;

import java.util.ArrayList;
import java.util.List;

public class InternalTemplate implements Template {

    @Override
    public List<Source> create(PluginOption option) {

        /**
         * Supports independent binary startup of MOSN
         */
        List<Source> sources = new ArrayList<>();

        appendVirtualOutbound(sources);
        appendVirtualInbound(sources);
        appendEgressHttpRouter(sources);
        appendIngressHttpRouter(sources);
        appendMosnConfig(sources);

        return sources;
    }

    private void appendMosnConfig(List<Source> sources) {
        /**
         * append mosn_config.json
         */
        String name = "mosn_config.json";
        String path = "configs/internal/base_conf";
        String templatePath = "/META-INF/template.internal/base_conf/mosn_config.json.template";
        String content = FileReader.plainText(templatePath);
        if (content != null && content.length() > 0) {
            sources.add(new Source(name, path, content));
        }
    }

    private void appendIngressHttpRouter(List<Source> sources) {
        /**
         * append routers http.json (ingress)
         */
        String name = "http.json";
        String path = "configs/internal/base_conf/routers/ingress_tp_http1_router";
        String templatePath = "/META-INF/template.internal/base_conf/routers/ingress_tp_http1_router/http.json.template";
        String content = FileReader.plainText(templatePath);
        if (content != null && content.length() > 0) {
            sources.add(new Source(name, path, content));
        }
    }

    private void appendEgressHttpRouter(List<Source> sources) {
        /**
         * append routers http.json (egress)
         */
        String name = "http.json";
        String path = "configs/internal/base_conf/routers/egress_tp_http1_router";
        String templatePath = "/META-INF/template.internal/base_conf/routers/egress_tp_http1_router/http.json.template";
        String content = FileReader.plainText(templatePath);
        if (content != null && content.length() > 0) {
            sources.add(new Source(name, path, content));
        }
    }

    private void appendVirtualInbound(List<Source> sources) {
        /**
         * append listener virtual_inbound.json
         */
        String name = "virtual_inbound.json";
        String path = "configs/internal/base_conf/listeners/sofarpc";
        String templatePath = "/META-INF/template.internal/base_conf/listeners/sofarpc/virtual_inbound.json.template";
        String content = FileReader.plainText(templatePath);
        if (content != null && content.length() > 0) {
            sources.add(new Source(name, path, content));
        }
    }

    private void appendVirtualOutbound(List<Source> sources) {
        /**
         * append listener virtual_outbound.json
         */
        String name = "virtual_outbound.json";
        String path = "configs/internal/base_conf/listeners/sofarpc";
        String templatePath = "/META-INF/template.internal/base_conf/listeners/sofarpc/virtual_outbound.json.template";
        String content = FileReader.plainText(templatePath);
        if (content != null && content.length() > 0) {
            sources.add(new Source(name, path, content));
        }
    }
}
