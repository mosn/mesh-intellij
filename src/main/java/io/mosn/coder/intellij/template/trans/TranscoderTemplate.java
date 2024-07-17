package io.mosn.coder.intellij.template.trans;

import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.ArrayList;
import java.util.List;

public class TranscoderTemplate extends AbstractTranscoderTemplate {

    @Override
    public Source config(PluginOption option) {

        String name = option.getPluginName().toLowerCase();
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.append("package main")
                .line()
                .append("import (")
                .append("\t\"context\"")
                .append("\t\"encoding/json\"")
                .append("\t\"fmt\"")
                .line()
                .append("\t\"mosn.io/api\"")
                .append(")")
                .line()
                .append("type Config struct {")
                .append("\tUniqueId    string      `json:\"unique_id\"`")
                .append("\tPath        string      `json:\"path\"`")
                .append("\tMethod      string      `json:\"method\"`")
                .append("\tTargetApp   string      `json:\"target_app\"`")
                .append("\tReqMapping  interface{} `json:\"req_mapping\"`")
                .append("\tRespMapping interface{} `json:\"resp_mapping\"`")
                .append("	// TODO : 新增协议转换其他自定义属性")
                .append("}")
                .line()
                .with("func (t *").with(name).append(") getConfig(ctx context.Context, headers api.HeaderMap) (*Config, error) {")
                .append("\tdetails, ok := t.cfg[\"details\"]")
                .append("	if !ok {")
                .append("\t\treturn nil, fmt.Errorf(\"the %s of details is not exist\", t.cfg)")
                .append("	}")
                .line()
                .append("	info, err := json.Marshal(details)")
                .append("	if err != nil {")
                .append("		return nil, err")
                .append("	}")
                .append("	var configs []*Config")
                .append("	if err := json.Unmarshal(info, &configs); err != nil {")
                .append("		return nil, err")
                .append("	}")
                .append("	if len(configs) == 1 {")
                .append("		return configs[0], nil")
                .append("	}")
                .line()
                .append("	panic(\"实现：查找当前headers请求的唯一转换配置\")")
                .append("	// TODO: 删除panic以及以下注释，实现当前请求唯一转换配置:")
                .append("	// 请参考 TranscodingRequest防范实现示例文档")
                .append("	// example: 根据调用方法名称查找转换对应的配置信息")
                .append("	// method, _ := headers.Get(\"method\")")
                .append("	// for _, cfg := range configs {")
                .append("		// if cfg.UniqueId == method {")
                .append("			// return cfg, nil")
                .append("		// }")
                .append("	// }")
                .append("	return nil, fmt.Errorf(\"config is not exist\")")
                .append("}");

        String path = "plugins/transcoders/" + name + "/main";

        return new Source("config.go", path, buffer.toString());
    }

    @Override
    public List<Source> transcoder(PluginOption option) {

        ArrayList<Source> source = new ArrayList<>();

        String name = option.getPluginName().toLowerCase();
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.append("package main")
                .line()
                .append("import (")
                .append("\t\"context\"")
                .line()
                .append("\t\"mosn.io/api\"")
                .append("\t\"mosn.io/api/extensions/transcoder\"")
                .append(")")
                .line()
                .with("type ").with(name).append(" struct {")
                .append("	cfg    map[string]interface{}")
                .append("	config *Config")
                .append("}")
                .line()
                .with("func (t *").with(name).append(") Accept(ctx context.Context, headers api.HeaderMap, buf api.IoBuffer, trailers api.HeaderMap) bool {")
                .append("\tpanic(\"实现：检查headers是否是转换请求类型\")")
                .append("	// TODO 删除panic以及以下注释，检查是否从原始请求开始转换:")
                .append("	// example 检查是否从xr协议请求开始转换")
                .append("	//_, ok := headers.(*xr.Request)")
                .append("	//if !ok {")
                .append("	//	return false")
                .append("	//}")
                .append("	config, err := t.getConfig(ctx, headers)")
                .append("	if err != nil {")
                .append("		return false")
                .append("	}")
                .append("	t.config = config")
                .append("	return true")
                .append("}")
                .line()
                .with("func (t *").with(name).append(") TranscodingRequest(ctx context.Context, headers api.HeaderMap, buf api.IoBuffer, trailers api.HeaderMap) (api.HeaderMap, api.IoBuffer, api.HeaderMap, error) {")
                .append("	panic(\"实现：将原始协议请求转换成目标协议请求，返回目标协议header、body、trailer\")")
                .append("	// video: https://help.aliyun.com/document_detail/437892.html?spm=a2c4g.11186623.0.0.2d6a5fcfiju0Ms")
                .append("	// transcoder介绍：https://github.com/mosn/extensions/blob/master/go-plugin/doc/2.6transcoder.md")
                .append("	// 转换示例：https://github.com/mosn/extensions/blob/master/go-plugin/doc/2.6.1dubbo2springcloud.md")
                .append("}")
                .line()
                .with("func (t *").with(name).append(") TranscodingResponse(ctx context.Context, headers api.HeaderMap, buf api.IoBuffer, trailers api.HeaderMap) (api.HeaderMap, api.IoBuffer, api.HeaderMap, error) {")
                .append("	panic(\"实现：将响应转换成返回给客户端的协议类型，返回给客户端协议header、body、trailer\")")
                .append("	// 请参考 TranscodingRequest防范实现示例文档")
                .append("}")
                .line()
                .append("func LoadTranscoderFactory(cfg map[string]interface{}) transcoder.Transcoder {")
                .with("	return &").with(name).append("{cfg: cfg}")
                .append("}");


        String path = "plugins/transcoders/" + name + "/main";
        source.add(new Source(name + ".go", path, buffer.toString()));

        return source;
    }

    @Override
    public List<Configuration> configurations(PluginOption option) {

        ArrayList<Configuration> configurations = new ArrayList<>();
        if (option instanceof TranscoderOption) {

            TranscoderOption opt = (TranscoderOption) option;
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
                    break;
                }
            }

        }

        return configurations;
    }

    @Override
    public Metadata metadata(PluginOption option) {
        String name = option.getPluginName().toLowerCase();

        String config = "metadata.json";
        String path = "configs/transcoders/" + name;

        if (option instanceof TranscoderOption) {

            CodeBuilder buffer = new CodeBuilder(new StringBuilder());
            buffer.append("{")
                    .with("\t\"name\" : \"").with(name).append("\",")
                    .append("\t\"kind\": \"transcoder\",")
                    .append("\t\"dependencies\": [{")
                    .with("\t\t\"mosn_api\": \"").with(option.getApi()).append("\",")
                    .with("\t\t\"mosn_pkg\": \"").with(option.getPkg()).append("\"")
                    .append("\t}]")
                    .append("}");

            return new Metadata(config, path, buffer.toString());

        }

        return null;
    }

    private void appendFilterConfiguration(TranscoderOption opt, String config, ArrayList<Configuration> configurations) {
        String name = opt.getPluginName().toLowerCase();
        String path = "configs/transcoders/" + name;

        appendConfiguration(configurations, opt, name, config, path);
    }

    protected void appendConfiguration(ArrayList<Configuration> configurations, TranscoderOption opt, String name, String config, String path) {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());
        buffer.append("{")
                .with("\t\"type\": \"").with(name).append("\",")
                .append("\t\"go_plugin_config\": {")
                .with("\t\t\"so_path\": \"./transcoder-").with(name).append(".so\",")
                .with("\t\t\"src_protocol\": \"").with(opt.getSrcProtocol()).append("\",")
                .with("\t\t\"dst_protocol\": \"").with(opt.getDstProtocol()).append("\"")
                .append("\t},")
                .append("\t\"matcher_config\": {")
                .append("\t\t\"matcher_type\": \"multiple_matcher\",")
                .append("\t\t\"config\": {")
                .with("\t\t\t\"name\": \"").with(name).append("\",")
                .append("\t\t\t\"enable\": true,")
                .append("\t\t\t\"variables\": [{")
                .append("\t\t\t\t\"name\": \"x-mosn-data-id\",")
                .append("\t\t\t\t\"values\": [")
                .append("\t\t\t\t\t\"// TODO: 替换成本地测试的服务标识，比如java接口：com.alipay.sofa.ms.service.EchoService\"")
                .append("\t\t\t\t],")
                .append("\t\t\t\t\"config\": \"{\\\"details\\\":[{\\\"unique_id\\\":\\\"// TODO: 查找请求唯一配置, 请添加任意自定义属性，对应Config结构体\\\"}]}\"")
                .append("\t\t\t}]")
                .append("\t\t}")
                .append("\t},")
                .append("\t\"rule_info\": {")
                .with("\t\t\"upstream_protocol\": \"").with(opt.getDstProtocol()).append("\",")
                .with("\t\t\"description\": \"").with(opt.getSrcProtocol()).with(" -> ").with(opt.getDstProtocol()).append("\"")
                .append("\t}")
                .append("}");

        configurations.add(new Configuration(config, path, buffer.toString()));
    }

}
