package io.mosn.coder.intellij.template.filter;

import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.mosn.coder.intellij.option.AbstractOption.X_MOSN_DATA_ID;

public class FilterTemplate extends AbstractFilterTemplate {

    @Override
    public List<Source> filter(FilterOption option) {

        ArrayList<Source> source = new ArrayList<>();

        String name = option.getPluginName().toLowerCase();
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        String factory = name + "FilterFactory";
        String filter = name + "Filter";

        buffer.append("package main")
                .line()
                .append("import (")
                .append("\t\"context\"")
                .append("\t\"encoding/json\"")
                .line()
                .append("\t\"mosn.io/api\"")
                .append("\t\"mosn.io/pkg/buffer\"");

        if (option.getRequiredKeys() != null && !option.getRequiredKeys().isEmpty()) {
            buffer.append("\t\"mosn.io/pkg/log\"");
        }

        buffer.append(")")
                .line()
                .append("func CreateFilterFactory(conf map[string]interface{}) (api.StreamFilterChainFactory, error) {")
                .append("	b, _ := json.Marshal(conf)")
                .append("	m := make(map[string]string)")
                .append("	if err := json.Unmarshal(b, &m); err != nil {")
                .append("		return nil, err")
                .append("	}")
                .with("	return &").with(factory).append("{")
                .append("		config: m,")
                .append("	}, nil")
                .append("}")
                .line()
                .append("// An implementation of api.StreamFilterChainFactory")
                .with("type ").with(factory).append(" struct {")
                .append("	config map[string]string")
                .append("}")
                .line()
                .with("func (f *").with(factory).with(")").append(" CreateFilterChain(ctx context.Context, callbacks api.StreamFilterChainFactoryCallbacks) {")
                .append("	filter := newFilter(ctx, f.config)")
                .append("	// ReceiverFilter, run the filter when receive a request from downstream")
                .append("	// The FilterPhase can be BeforeRoute or AfterRoute, we use BeforeRoute in this demo")
                .append("	callbacks.AddStreamReceiverFilter(filter, api.BeforeRoute)")
                .append("	// SenderFilter, run the filter when receive a response from upstream")
                .append("	callbacks.AddStreamSenderFilter(filter, api.BeforeSend)")
                .append("}")
                .line()
                .with("type ").with(filter).append(" struct {")
                .append("	config         map[string]string")
                .append("	receiveHandler api.StreamReceiverFilterHandler")
                .line()
                .append("	sendHandler api.StreamSenderFilterHandler")
                .append("}")
                .line()
                .with("func newFilter(ctx context.Context, config map[string]string) *").with(filter).append(" {")
                .with("	return &").with(filter).append("{")
                .append("		config: config,")
                .append("	}")
                .append("}")
                .line();

        String key = null;

        if (option.getRequiredKeys() != null && !option.getRequiredKeys().isEmpty()) {
            buffer.append("type ServiceHeader struct {")
                    .append("\tTarget string `json:\"target\"`")
                    .append("}")
                    .line();

            // inject head key
            for (Map.Entry<AbstractOption.OptionKey<String>, AbstractOption.OptionValue<String>> entry : option.getRequiredKeys()) {
                AbstractOption.OptionValue<String> optionValue = entry.getValue();
                // skip invalid variable
                if (optionValue == null
                        || optionValue.getItems() == null
                        || optionValue.getItems().isEmpty()) {
                    continue;
                }

                if (entry.getKey().equals(X_MOSN_DATA_ID)) {
                    key = optionValue.first();
                }
            }
        }

        buffer.with("func (f *").with(filter).append(") OnReceive(ctx context.Context, headers api.HeaderMap, buf buffer.IoBuffer, trailers api.HeaderMap) api.StreamFilterStatus {")
                .line()
                .append("	// TODO: 实现拦截请求逻辑")
                .append("	// video: https://help.aliyun.com/document_detail/437892.html?spm=a2c4g.11186623.0.0.2d6a5fcfiju0Ms")
                .line();

        if (key != null) {
            buffer.with("\tif app, ok := headers.Get(\"").with(key).append("\"); ok && app != \"\" {")
                    .append("		return api.StreamFilterContinue")
                    .append("	}")
                    .line();

            buffer.append("	var request ServiceHeader")
                    .append("	if err := json.Unmarshal(buf.Bytes(), &request); err != nil {")
                    .with("\t\tlog.DefaultContextLogger.Warnf(ctx, \"[streamfilter][").with(name).append("] Unmarshal body stream to spring cloud header failed, content %s\", buf.Bytes())")
                    .append("		f.receiveHandler.SendHijackReply(403, headers)")
                    .append("	}")
                    .line();

            // inject header
            buffer.with("\theaders.Set(\"").with(key).append("\", request.Target)")
                    .line();
        }

        buffer.append("	return api.StreamFilterContinue")
                .append("}")
                .line()
                .with("func (f *").with(filter).append(") SetReceiveFilterHandler(handler api.StreamReceiverFilterHandler) {")
                .append("	f.receiveHandler = handler")
                .append("}")
                .line()
                .with("func (f *").with(filter).append(") SetSenderFilterHandler(handler api.StreamSenderFilterHandler) {")
                .append("	f.sendHandler = handler")
                .append("}")
                .line()
                .with("func (f *").with(filter).append(") Append(ctx context.Context, headers api.HeaderMap, buf api.IoBuffer, trailers api.HeaderMap) api.StreamFilterStatus {")
                .line()
                .append("	// TODO: 实现拦截响应逻辑")
                .append("	// video: https://help.aliyun.com/document_detail/437892.html?spm=a2c4g.11186623.0.0.2d6a5fcfiju0Ms")
                .line()
                .append("	return api.StreamFilterContinue")
                .append("}")
                .line()
                .with("func (f *").with(filter).with(") ").append("OnDestroy() {}");

        String path = "plugins/stream_filters/" + name + "/main";
        source.add(new Source(name + ".go", path, buffer.toString()));

        return source;
    }

    @Override
    public List<Configuration> configurations(FilterOption option) {

        ArrayList<Configuration> configurations = new ArrayList<>();
        if (option instanceof FilterOption) {

            FilterOption opt = (FilterOption) option;
            switch (opt.getActiveMode()) {
                case Client: {
                    appendFilterConfiguration(opt, "egress_config.json", configurations);
                    break;
                }
                case Server: {
                    appendFilterConfiguration(opt, "ingress_config.json", configurations);
                    break;
                }
                case ALL: {
                    appendFilterConfiguration(opt, "egress_config.json", configurations);
                    appendFilterConfiguration(opt, "ingress_config.json", configurations);
                    break;
                }
            }

        }

        return configurations;
    }

    private void appendFilterConfiguration(FilterOption opt, String config, ArrayList<Configuration> configurations) {
        String name = opt.getPluginName().toLowerCase();
        String path = "configs/stream_filters/" + name;

        appendConfiguration(configurations, opt, name, config, path);
    }

    protected void appendConfiguration(ArrayList<Configuration> configurations, FilterOption opt, String name, String config, String path) {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .with("  \"type\": \"").with(name).append("\",")
                .append("  \"go_plugin_config\": {")
                .with("    \"so_path\": \"./stream_filter-").with(name).append(".so\"")
                .with("  }");

        if (opt.getBefore() != null && opt.getBefore().length() > 0) {
            buffer.append(",")
                    .append("  \"config\": {")
                    .with("    \"before\": \"").with(opt.getBefore()).append("\"")
                    .append("  }");
        } else {
            // pretty
            buffer.line();
        }

        buffer.append("}");

        configurations.add(new Configuration(config, path, buffer.toString()));
    }

    @Override
    public Metadata metadata(FilterOption option) {
        String name = option.getPluginName().toLowerCase();

        String config = "metadata.json";
        String path = "configs/stream_filters/" + name;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .with("\t\"name\" : \"").with(name).append("\",")
                .append("\t\"kind\": \"stream_filter\",")
                .append("\t\"dependencies\": [{")
                .with("\t\t\"mosn_api\": \"").with(option.getApi()).append("\",")
                .with("\t\t\"mosn_pkg\": \"").with(option.getPkg()).append("\"")
                .append("\t}]")
                .append("}");

        return new Metadata(config, path, buffer.toString());
    }

}
