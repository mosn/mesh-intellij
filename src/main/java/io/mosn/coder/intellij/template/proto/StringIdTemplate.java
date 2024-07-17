package io.mosn.coder.intellij.template.proto;

import io.mosn.coder.intellij.option.PoolMode;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

public class StringIdTemplate extends StandardTemplate {

    @Override
    public Source api(ProtocolOption option) {
        return null;
    }

    @Override
    public Source command(ProtocolOption option) {
        String name = "command.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "pkg/protocol/" + proto;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.with("package ").append(proto)
                .line()
                .append("import (")
                .with("\t\"").with(option.context().getModule()).with("/pkg/common").append("\"")
                .append("\t\"mosn.io/api\"")
                .append(")")
                .line()
                .append("const defaultTimeout = 10000 // default request timeout(10 seconds).")
                .line()
                .append("type Request struct {")
                .append("	common.Header              // request key value pair")
                .append("	RequestId     string       // request id (biz id)")
                .append("	SteamId       interface{}  // sidecar request id (replaced by sidecar, uint64 or nil)")
                .append("	Timeout       uint32       // request timeout")
                .append("	Content       api.IoBuffer // it refers to the service parameters of a packet（xml）")
                .append("	Data          api.IoBuffer // full package bytes")
                .line()
                .append("	// TODO: 请添加请求自定义字段")
                .append("	HeaderLen  uint32 // header length")
                .append("	ContentLen uint32 // body length")
                .line()
                .append("	Changed bool // indicates whether the packet payload is modified")
                .append("}")
                .line()
                .append("func (r *Request) IsHeartbeatFrame() bool {")
                .append("	return false")
                .append("}")
                .line()
                .append("func (r *Request) GetTimeout() int32 {")
                .append("	return int32(r.Timeout)")
                .append("}")
                .line()
                .append("func (r *Request) GetHeader() api.HeaderMap {")
                .append("	return r")
                .append("}")
                .line()
                .append("func (r *Request) GetData() api.IoBuffer {")
                .append("	return r.Content")
                .append("}")
                .line()
                .append("func (r *Request) SetData(data api.IoBuffer) {")
                .append("	r.Content = data")
                .append("}")
                .line()
                .append("func (r *Request) GetStreamType() api.StreamType {")
                .append("	return api.Request")
                .append("}")
                .line();

        {
            buffer.append("func (r *Request) GetRequestId() uint64 {");

            if (option.getPoolMode() == PoolMode.PingPong) {
                buffer.append("	return 0");
            } else {
                buffer.append("	if r.SteamId != nil {")
                        .append("		return r.SteamId.(uint64)")
                        .append("	}")
                        .line()
                        .append("	// we don't care about it")
                        .append("	return hash(r.RequestId)");
            }
            buffer.append("}");

            buffer.append("func (r *Request) SetRequestId(id uint64) {");
            if (option.getPoolMode() != PoolMode.PingPong) {
                buffer.append("	r.SteamId = id");
            }
            buffer.append("}");
        }

        buffer.line()
                .append("// check command implement api interface.")
                .append("var _ api.XFrame = &Request{}")
                .append("var _ api.XRespFrame = &Response{}")
                .line();


        buffer.append("type Response struct {")
                .append("	common.Header              // response key value pair")
                .append("	RequestId     string       // response id")
                .append("	SteamId       interface{}  // sidecar request id (replaced by sidecar id)")
                .append("	Status        uint32       // response status")
                .append("	Data          api.IoBuffer // full package bytes")
                .append("	Content       api.IoBuffer // it refers to the service parameters of a packet（xml）")
                .line()
                .append("	// TODO: 请添加响应自定义字段")
                .append("	HeaderLen  uint32 // header length")
                .append("	ContentLen uint32 // body length")
                .line()
                .append("	Changed bool // indicates whether the packet payload is modified")
                .append("}")
                .line()
                .append("func (r *Response) IsHeartbeatFrame() bool {")
                .append("	return false")
                .append("}")
                .line()
                .append("func (r *Response) GetTimeout() int32 {")
                .append("	return defaultTimeout")
                .append("}")
                .line()
                .append("func (r *Response) GetHeader() api.HeaderMap {")
                .append("	return r")
                .append("}")
                .line()
                .append("func (r *Response) GetData() api.IoBuffer {")
                .append("	return r.Content")
                .append("}")
                .line()
                .append("func (r *Response) SetData(data api.IoBuffer) {")
                .append("	r.Content = data")
                .append("}")
                .line()
                .append("func (r *Response) GetStreamType() api.StreamType {")
                .append("	return api.Response")
                .append("}")
                .line();

        {
            buffer.append("func (r *Response) GetRequestId() uint64 {");

            if (option.getPoolMode() == PoolMode.PingPong) {
                buffer.append("	return 0");
            } else {
                buffer.append("	if r.SteamId != nil {")
                        .append("		return r.SteamId.(uint64)")
                        .append("	}")
                        .line()
                        .append("	// we don't care about it")
                        .append("	return hash(r.RequestId)");
            }
            buffer.append("}");

            buffer.append("func (r *Response) SetRequestId(id uint64) {");
            if (option.getPoolMode() != PoolMode.PingPong) {
                buffer.append("	r.SteamId = id");
            }
            buffer.append("}");
        }

        buffer.line()
                .append("func (r *Response) GetStatusCode() uint32 {")
                .append("	return r.Status")
                .append("}")
                .line();

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.append("func hash(s string) uint64 {")
                    .append("	var h uint64")
                    .append("	for i := 0; i < len(s); i++ {")
                    .append("		h ^= uint64(s[i])")
                    .append("		h *= 16777619")
                    .append("	}")
                    .append("	return h")
                    .append("}");
        }

        return new Source(name, path, buffer.toString());
    }

    @Override
    public Source decoder(ProtocolOption option) {

        String name = "decoder.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "pkg/protocol/" + proto;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        String p = option.Alias();

        buffer.with("package ").append(proto)
                .line()
                .append("import (")
                .with("\t\"").with(option.context().getModule()).append("/pkg/common\"")
                .append("\t\"context\"")
                .append("\t\"mosn.io/api\"")
                .append("\t\"mosn.io/pkg/buffer\"")
                .append(")")
                .line()
                .with("func (proto *").with(p).append("Protocol) decodeRequest(ctx context.Context, buf api.IoBuffer, header *common.Header, headerLen int, contentLen int) (interface{}, error) {")
                .append("	bufLen := buf.Len()")
                .append("	data := buf.Bytes()")
                .line()
                .append("	// The buf does not contain the complete packet length,")
                .append("	// So we wait for the next decoder notification.");

        if (option.isXmlCodec()) {
            buffer.with("	totalLen := headerLen + contentLen").with(" + ").with(String.valueOf(option.getCodecOption().length)).append(" // fixed length");
        } else {
            buffer.append("	totalLen := /** 固定协议头部字节长度，如果有需要算进去， eg: 固定22字节 */ /**  22 +  */ headerLen + contentLen");
        }

        buffer
                .append("	if bufLen < totalLen {")
                .append("		return nil, nil")
                .append("	}")
                .line()
                .append("	// Read the complete packet data from the connection")
                .append("	buf.Drain(totalLen)")
                .line()
                .append("	request := &Request{}")
                .line()
                .append("	// decode request field")
                .append("	request.Header = *header")
                .append("	request.RequestId, _ = header.Get(externalReferenceKey)")
                .line()
                .append("	request.HeaderLen = uint32(headerLen)")
                .append("	request.ContentLen = uint32(contentLen)");

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.line()
                    .append("	if val, found := proto.StreamId(ctx, request.RequestId); found {")
                    .append("		request.SteamId = val")
                    .append("	}").line();
        }

        buffer.append("	request.Timeout = defaultTimeout")
                .line()
                .append("	request.Data = buffer.GetIoBuffer(totalLen)")
                .append("	request.Data.Write(data[:totalLen])")
                .line()
                .append("	payload := request.Data.Bytes()[RequestHeaderLen:totalLen]")
                .append("	request.Content = buffer.NewIoBufferBytes(payload)")
                .line()
                .append("	return request, nil")
                .append("}");

        buffer.line()
                .with("func (proto *").with(p).append("Protocol) decodeResponse(ctx context.Context, buf api.IoBuffer, header *common.Header, headerLen int, contentLen int) (interface{}, error) {")
                .append("	bufLen := buf.Len()")
                .append("	data := buf.Bytes()")
                .line()
                .append("	// The buf does not contain the complete packet length,")
                .append("	// So we wait for the next decoder notification.");

        if (option.isXmlCodec()) {
            buffer.with("	totalLen := headerLen + contentLen").with(" + ").with(String.valueOf(option.getCodecOption().length)).append(" // fixed length");
        } else {
            buffer.append("	totalLen := /** 固定协议头部字节长度，如果有需要算进去， eg: 固定22字节 */ /**  22 +  */ headerLen + contentLen");
        }

        buffer.append("	if bufLen < totalLen {")
                .append("		return nil, nil")
                .append("	}")
                .line()
                .append("	// Read the complete packet data from the connection")
                .append("	buf.Drain(totalLen)")
                .line()
                .append("	response := &Response{}")
                .line()
                .append("	// decode response field")
                .append("	response.Header = *header")
                .append("	response.RequestId, _ = header.Get(externalReferenceKey)")
                .line()
                .append("	response.HeaderLen = uint32(headerLen)")
                .append("	response.ContentLen = uint32(contentLen)");

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.line()
                    .append("	if val, found := proto.StreamId(ctx, response.RequestId); found {")
                    .append("		response.SteamId = val")
                    .line()
                    .append("		// remove stream id, help gc")
                    .append("		proto.RemoveStreamId(ctx, response.RequestId)")
                    .append("	}");
        }

        buffer.line()
                .append("	response.Data = buffer.GetIoBuffer(totalLen)")
                .append("	response.Data.Write(data[:totalLen])")
                .line()
                .append("	payload := response.Data.Bytes()[RequestHeaderLen:totalLen]")
                .append("	response.Content = buffer.NewIoBufferBytes(payload)")
                .line()
                .append("	return response, nil")
                .append("}");

        return new Source(name, path, buffer.toString());
    }

    @Override
    public Source encoder(ProtocolOption option) {
        String name = "encoder.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "pkg/protocol/" + proto;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        String p = option.Alias();

        buffer.with("package ").append(proto)
                .line()
                .append("import (")
                .append("\t\"context\"")
                .append("\t\"mosn.io/api\"")
                .append("\t\"mosn.io/pkg/buffer\"");

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.append("\t\"mosn.io/pkg/log\"");
        }

        if (option.isXmlCodec()) {
            buffer.append("\t\"strconv\"");
        }

        buffer.append(")")
                .line()
                .with("func (proto *").with(p).append("Protocol) encodeRequest(ctx context.Context, request *Request) (api.IoBuffer, error) {")
                .line()
                .append("	if len(request.Header.Kvs) != 0 {")
                .append("		request.HeaderLen = getEncodeHeaderLength(&request.Header)")
                .append("	}")
                .append("	if request.Content != nil {")
                .append("		request.ContentLen = uint32(request.Content.Len())")
                .append("	}")
                .append("	frameLen := RequestHeaderLen + int(request.HeaderLen) + int(request.ContentLen)")
                .line()
                .append("	buf := buffer.GetIoBuffer(frameLen)")
                .line();

        if (option.isXmlCodec()) {
            buffer.append("	// 2. write fixed byte length + body")
                    .append("	proto.appendPrefix(buf, request.Content.Len())")
                    .line()
                    .append("	if request.ContentLen > 0 {")
                    .append("\t\tif request.RequestId == \"\" {")
                    .append("			// try query business id from payload.")
                    .append("			payload := request.Content.Bytes()")
                    .append("			request.RequestId = fetchId(payload)")
                    .append("		}")
                    .line()
                    .append("		// 3. write payload bytes")
                    .append("		buf.Write(request.Content.Bytes())")
                    .append("	}")
                    .line();
        } else {

            buffer.line()
                    .append("	if request.ContentLen > 0 {")
                    .append("\t\tif request.RequestId == \"\" {")
                    .append("			// try query business id from payload.")
                    .append("			payload := request.Content.Bytes()")
                    .append("			request.RequestId = fetchId(payload)")
                    .append("		}")
                    .append("	}")
                    .line();

            buffer.append("	// 2 encode: header, content")
                    .append("\tpanic(\"实现: 严格按照私有协议规范字段顺序，依次写入buf中\")")
                    .line()
                    .append("	// TODO: 删除panic以及以下注释，按照私有协议规范字段顺序，依次写入buf中:")
                    .append("	// example: 假设私有协议规范要求依次写入RequestId、HeaderLen和ContentLen")
                    .append("	// buf.WriteString(request.RequestId)")
                    .append("	// buf.WriteUint16(request.HeaderLen)")
                    .append("	// buf.WriteUint32(request.ContentLen)");


            buffer.line()
                    .append("	if request.HeaderLen > 0 {")
                    .append("		// TODO: 根据实际协议，决定是否需要编码协议头部键值对，依次写入buf中:")
                    .append("		// 如果需要编码键值对，请反注释encodeHeader实现，并删除TODO")
                    .append("		// encodeHeader(buf, &request.ProtocolHeader)")
                    .append("	}")
                    .line()
                    .append("	if request.ContentLen > 0 {")
                    .append("		// TODO: 根据实际协议，决定是否需要将消息体写入buf中:")
                    .append("		// 如果需要写入报文体，请反注释buf.Write，并删除TODO")
                    .append("		// buf.Write(request.Content.Bytes())")
                    .append("	}");

        }

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.append("	// If sidecar replaces the ID, we associate the ID with the business ID")
                    .append("	// When the response is received, streamId is restored correctly.")
                    .append("	if request.SteamId != nil {")
                    .append("		proto.PutStreamId(ctx, request.RequestId, request.SteamId.(uint64))")
                    .append("		// record debug mapping stream info.")
                    .append("		if log.DefaultLogger.GetLogLevel() >= log.DEBUG {")
                    .with("\t\t\tlog.DefaultLogger.Debugf(\"").with(proto).append(" proto mapping streamId: %d -> %d\", request.RequestId, request.SteamId.(uint64))")
                    .append("		}")
                    .append("	}");
        }

        buffer.append("	return buf, nil")
                .append("}").line();


        buffer.with("func (proto *").with(p).with("Protocol) encodeResponse(ctx context.Context, response *Response) (api.IoBuffer, error) {")
                .append("	// 1. slow-path, construct buffer from scratch")
                .append("	if len(response.Header.Kvs) != 0 {")
                .append("		response.HeaderLen = getEncodeHeaderLength(&response.Header)")
                .append("	}")
                .append("	if response.Content != nil {")
                .append("		response.ContentLen = uint32(response.Content.Len())")
                .append("	}")
                .append("	frameLen := ResponseHeaderLen + int(response.HeaderLen) + int(response.ContentLen)")
                .line()
                .append("	buf := buffer.GetIoBuffer(frameLen)")
                .line();

        if (option.isXmlCodec()) {
            buffer.append("	// 2. write fixed byte length + body")
                    .append("	proto.appendPrefix(buf, response.Content.Len())")
                    .line()
                    .append("	if response.ContentLen > 0 {")
                    .append("\t\tif response.RequestId == \"\" {")
                    .append("			// try query business id from payload.")
                    .append("			payload := response.Content.Bytes()")
                    .append("			response.RequestId = fetchId(payload)")
                    .append("		}")
                    .line()
                    .append("		// 3. write payload bytes")
                    .append("		buf.Write(response.Content.Bytes())")
                    .append("	}");
        } else {

            buffer.line()
                    .append("	if response.ContentLen > 0 {")
                    .append("\t\tif response.RequestId == \"\" {")
                    .append("			// try query business id from payload.")
                    .append("			payload := response.Content.Bytes()")
                    .append("			response.RequestId = fetchId(payload)")
                    .append("		}")
                    .append("	}")
                    .line();

            buffer.append("	// 2 encode: header, content")
                    .append("\tpanic(\"实现: 严格按照私有协议规范字段顺序，依次写入buf中\")")
                    .line()
                    .append("	// TODO: 删除panic以及以下注释，按照私有协议规范字段顺序，依次写入buf中:")
                    .append("	// example: 假设私有协议规范要求依次写入RequestId、HeaderLen和ContentLen")
                    .append("	// buf.WriteString(response.RequestId)")
                    .append("	// buf.WriteUint16(response.HeaderLen)")
                    .append("	// buf.WriteUint32(response.ContentLen)");


            buffer.line()
                    .append("	if response.HeaderLen > 0 {")
                    .append("		// TODO: 根据实际协议，决定是否需要编码协议头部键值对，依次写入buf中:")
                    .append("		// 如果需要编码键值对，请反注释encodeHeader实现，并删除TODO")
                    .append("		// encodeHeader(buf, &response.ProtocolHeader)")
                    .append("	}")
                    .line()
                    .append("	if response.ContentLen > 0 {")
                    .append("		// TODO: 根据实际协议，决定是否需要将消息体写入buf中:")
                    .append("		// 如果需要写入报文体，请反注释buf.Write，并删除TODO")
                    .append("		// buf.Write(response.Content.Bytes())")
                    .append("	}");
        }

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.append("	// remove associate the ID with the business ID if exists.")
                    .append("	if _, found := proto.StreamId(ctx, response.RequestId); found {")
                    .append("		// remove stream id, help gc")
                    .append("		proto.RemoveStreamId(ctx, response.RequestId)")
                    .append("	}");
        }

        buffer.line()
                .append("	return buf, nil")
                .append("}");


        if (option.isXmlCodec()) {
            buffer.line()
                    .with("func (proto *").with(p).append("Protocol) appendPrefix(buf buffer.IoBuffer, payloadLen int) {")
                    .append("	rayLen := strconv.Itoa(payloadLen)")
                    .append("	if count := RequestHeaderLen - len(rayLen); count > 0 {")
                    .append("		for i := 0; i < count; i++ {")
                    .with("\t\t\tbuf.WriteString(\"").with(option.getCodecOption().prefix).append("\")")
                    .append("		}")
                    .append("	}")
                    .append("	buf.WriteString(rayLen)")
                    .append("}");
        }

        return new Source(name, path, buffer.toString());

    }

    @Override
    public Source mapping(ProtocolOption option) {
        String name = "mapping.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "pkg/protocol/" + proto;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.with("package ").append(proto)
                .line()
                .append("import (")
                .append("\t\"context\"")
                .append("\t\"errors\"")
                .line()
                .append("\t\"mosn.io/api\"")
                .append(")")
                .line()
                .append("type StatusMapping struct{}")
                .line()
                .append("func (m StatusMapping) MappingHeaderStatusCode(ctx context.Context, headers api.HeaderMap) (int, error) {")
                .append("	cmd, ok := headers.(api.XRespFrame)")
                .append("	if !ok {")
                .append("\t\treturn 0, errors.New(\"no response status in headers\")")
                .append("	}")
                .append("	code := cmd.GetStatusCode()")
                .append("	// TODO:  删除panic以及以下注释，实现MappingHeaderStatusCode方法:")
                .append("	// example 假设私有协议状态码0代表成功，则返回http.StatusOK")
                .append("	// 假设私有协议状态码4（ResponseStatusServerThreadPoolBusy）代表线程池忙，则返回http.StatusServiceUnavailable")
                .append("	// switch code {")
                .append("	// case ResponseStatusSuccess:")
                .append("	// 	return http.StatusOK, nil")
                .append("	// case ResponseStatusServerThreadPoolBusy:")
                .append("	// 	return http.StatusServiceUnavailable, nil")
                .append("	// case ResponseStatusTimeout:")
                .append("	// 	return http.StatusGatewayTimeout, nil")
                .append("	// case ResponseStatusConnectionClosed:")
                .append("	// 	return http.StatusBadGateway, nil")
                .append("	// default:")
                .append("	// 	return http.StatusInternalServerError, nil")
                .append("	// }")
                .line()
                .append("	return int(code), nil")
                .append("}");

        return new Source(name, path, buffer.toString());
    }

    @Override
    public Source matcher(ProtocolOption option) {
        String name = "matcher.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "pkg/protocol/" + proto;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.with("package ").append(proto)
                .line()
                .append("import (")
                .append("\t\"mosn.io/api\"");

        if (option.isXmlCodec()) {
            buffer.append("\t\"strconv\"")
                    .append("\t\"strings\"");
        }

        buffer.append(")").line();

        buffer.append("func Matcher(data []byte) api.MatchResult {")
                .append("	if len(data) < RequestHeaderLen {")
                .append("		return api.MatchAgain")
                .append("	}")
                .line();

        if (option.isXmlCodec()) {
            buffer.with("	rawLen := strings.TrimLeft(string(data[0:").with(String.valueOf(option.getCodecOption().length)).with("]), ").with("\"").with(option.getCodecOption().prefix).append("\")")
                    .append("\tif rawLen == \"\" {")
                    .append("		return api.MatchFailed")
                    .append("	}")
                    .line()
                    .append("	packetLen, err := strconv.Atoi(rawLen)")
                    .append("	// invalid packet length or not number")
                    .append("	if packetLen <= 0 || err != nil {")
                    .append("		return api.MatchFailed")
                    .append("	}")
                    .line()
                    .append("	return api.MatchSuccess")
                    .append("}");
        } else {
            buffer.append("\tpanic(\"实现: 识别当前私有协议是否匹配成功\")")
                    .append("\t// TODO: 删除panic以及以下注释，实现Matcher方法:")
                    .append("\t// example 比如可以根据协议0、1下标是否是0xbc、0xbc判断是否是可以处理的协议")
                    .append("\t// if length >= 2 && data[0] == 0xbc && data[1] == 0xbc {")
                    .append("\t// \treturn api.MatchSuccess")
                    .append("\t// }")
                    .line()
                    .append("\treturn api.MatchFailed")
                    .append("}");
        }

        return new Source(name, path, buffer.toString());
    }

    @Override
    public Source protocol(ProtocolOption option) {
        String name = "protocol.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "pkg/protocol/" + proto;
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.with("package ").append(proto)
                .line()
                .append("import (")
                .append("\t\"context\"")
                .append("\t\"errors\"")
                .append("\t\"fmt\"")
                .with("\t\"").with(option.context().getModule()).append("/pkg/common\"")
                .with("\t\"").with(option.context().getModule()).append("/pkg/common/safe\"")
                .append("\t\"mosn.io/api\"")
                .append("\t\"mosn.io/pkg/log\"");

        if (option.isXmlCodec()) {
            buffer.append("\t\"strconv\"")
                    .append("\t\"strings\"");
        }

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.append("\t\"sync/atomic\"");
        }

        buffer.append(")");

        String p = option.Alias();

        buffer.line()
                .with("type ").with(p).append("Protocol struct {")
                .append("	streams safe.IntMap")
                .append("}")
                .line()
                .with("func (proto *").with(p).append("Protocol) Name() api.ProtocolName {")
                .append("	return ProtocolName")
                .append("}")
                .line()
                .with("func (proto *").with(p).append("Protocol) Encode(ctx context.Context, model interface{}) (api.IoBuffer, error) {")
                .append("	switch frame := model.(type) {")
                .append("	case *Request:")
                .append("		return proto.encodeRequest(ctx, frame)")
                .append("	case *Response:")
                .append("		return proto.encodeResponse(ctx, frame)")
                .append("	default:")
                .with("\t\tlog.DefaultLogger.Errorf(\"[protocol][").with(proto).append("] encode with unknown command : %+v\", model)")
                .append("\t\treturn nil, errors.New(\"unknown command type\")")
                .append("	}")
                .append("}")
                .line();

        buffer.with("func (proto *").with(p).append("Protocol) Decode(ctx context.Context, buf api.IoBuffer) (interface{}, error) {")
                .line()
                .append("	bLen := buf.Len()")
                .append("	data := buf.Bytes()")
                .line()
                .append("	if bLen < RequestHeaderLen {")
                .append("		return nil, nil")
                .append("	}")
                .line()
                .append("	var (")
                .append("		headerLen  int")
                .append("		contentLen int")
                .append("	)")
                .line();

        if (option.isXmlCodec()) {
            buffer.with("	rawLen := strings.TrimLeft(string(data[0:").with(String.valueOf(option.getCodecOption().length)).with("]),").with("\"").with(option.getCodecOption().prefix).append("\")")
                    .append("\tif rawLen != \"\" {")
                    .append("		var err error")
                    .append("		headerLen = 0 // 定长header + body 没有键值对")
                    .append("		contentLen, err = strconv.Atoi(rawLen)")
                    .append("		if err != nil {")
                    .append("			return nil, errors.New(fmt.Sprintf(\"failed to decode package len %d, err: %v\", contentLen, err))")
                    .append("		}")
                    .append("	}")
                    .line();
        } else {
            buffer.append("	panic(\"实现: 给headerLen和contentLen字段赋值, headerLen代表键值对编码长度，contentLen代表报文体长度\")")
                    .append("	// TODO: 删除panic以及以下注释，给headerLen和contentLen字段赋值:")
                    .append("	// example 如何读取协议头部字段信息:")
                    .append("	// 假设从索引12:14代表协议头部key-value编码后的长度,")
                    .append("	// headerLen = binary.BigEndian.Uint16(bytes[12:14])")
                    .append("	// contentLen = binary.BigEndian.Uint16(bytes[14:16])");
        }

        buffer.append("	// expected full message length");
        if (option.isXmlCodec()) {
            buffer.with("	totalLen := headerLen + contentLen").with(" + ").with(String.valueOf(option.getCodecOption().length)).append(" // fixed length");
        } else {
            buffer.append("	totalLen := /** 固定协议头部字节长度，如果有需要算进去， eg: 固定22字节 */ /**  22 +  */ headerLen + contentLen");
        }
        buffer.append("	if bLen < totalLen {")
                .append("		return nil, nil")
                .append("	}")
                .line()
                .append("	rpcHeader := common.Header{}")
                .append("	injectHeaders(data[:totalLen], &rpcHeader, headerLen, contentLen, totalLen)")
                .line()
                .append("	frameType, _ := rpcHeader.Get(requestTypeKey)")
                .append("	switch frameType {")
                .append("	case requestFlag:")
                .append("		return proto.decodeRequest(ctx, buf, &rpcHeader, headerLen, contentLen)")
                .append("	case responseFlag:")
                .append("		return proto.decodeResponse(ctx, buf, &rpcHeader, headerLen, contentLen)")
                .append("	default:")
                .with("\t\treturn nil, fmt.Errorf(\"decode ").with(proto).append(" Error, unknown request type = %s\", frameType)")
                .append("	}")
                .append("}")
                .line();

        buffer.append("// Trigger heartbeat detect.")
                .with("func (proto *").with(p).append("Protocol) Trigger(context context.Context, requestId uint64) api.XFrame {")
                .append("	// TODO: 构造心跳请求，如果不支持心跳能力，删除注释并返回nil")
                .append("	return nil")
                .append("}")
                .line()
                .with("func (proto *").with(p).append("Protocol) Reply(context context.Context, request api.XFrame) api.XRespFrame {")
                .append("	// TODO: 构造心跳响应，如果不支持心跳能力，删除注释并返回nil")
                .append("	return nil")
                .append("}");

        buffer.append("// Hijack hijack request, maybe timeout")
                .with("func (proto *").with(p).append("Protocol) Hijack(context context.Context, request api.XFrame, statusCode uint32) api.XRespFrame {")
                .append("resp := proto.hijackResponse(request, statusCode)")
                .append("	return resp")
                .append("}")
                .line();

        buffer.with("func (proto *").with(p).append("Protocol) Mapping(httpStatusCode uint32) uint32 {")
                .append("	// TODO 将http状态码映射成私有协议状态码，返回值用于Hijack的statusCode参数")
                .append("	// 参考状态码：https://github.com/mosn/extensions/blob/master/go-plugin/doc/api/1.%20xprotocol-api.md#114-%E8%AF%B7%E6%B1%82%E5%8A%AB%E6%8C%81")
                .append("	// bolt 映射实现示例：https://github.com/mosn/extensions/blob/bdd06f879a77c7f795df05bf904e4854c1e6034f/go-plugin/pkg/protocol/bolt/protocol.go#L150")
                .append("	return httpStatusCode")
                .append("}");


        buffer.append("// PoolMode returns whether ping-pong or multiplex")
                .with("func (proto *").with(p).append("Protocol) PoolMode() api.PoolMode {");
        if (option.getPoolMode() == PoolMode.PingPong) {
            buffer.append("	return api.PingPong");
        } else {
            buffer.append("	return api.Multiplex");
        }
        buffer.append("}");

        buffer.line()
                .with("func (proto *").with(p).append("Protocol) EnableWorkerPool() bool {")
                .append(option.getPoolMode() == PoolMode.PingPong ? "	return false" : "	return true")
                .append("}").line();

        buffer.with("func (proto *").with(p).append("Protocol) GenerateRequestID(streamID *uint64) uint64 {")
                .append(option.getPoolMode() == PoolMode.PingPong ? "	return 0" : "	return atomic.AddUint64(streamID, 1)")
                .append("}").line();

        buffer.append("// hijackResponse build hijack response")
                .with("func (proto *").with(p).append("Protocol) hijackResponse(request api.XFrame, statusCode uint32) *Response {")
                .append("\tpanic(\"实现: 根据原始请求和私有协议异常码，构造请求被mosn劫持的响应报文\")")
                .append("}");

        return new Source(name, path, buffer.toString());
    }

    @Override
    public Source types(ProtocolOption option) {
        String name = "types.go";
        String proto = option.getPluginName().toLowerCase();
        String path = "pkg/protocol/" + proto;

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        appendLicense(buffer);

        buffer.with("package ").append(proto)
                .line()
                .append("import (")
                .append("\t\"bytes\"");
        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.append("\t\"context\"");
        }
        buffer.append("\t\"encoding/xml\"")
                .with("\t\"").with(option.context().getModule()).append("/pkg/common\"")
                .append("\t\"io\"")
                .append("\t\"mosn.io/api\"");

        if (!option.isXmlCodec()) {
            buffer.append("\t\"mosn.io/pkg/buffer\"");
        }

        buffer.append("\t\"strings\"")
                .append(")")
                .line()
                .with("var ProtocolName api.ProtocolName = \"").with(proto).append("\" // protocol")
                .line()
                .append("const (")
                .append("\tstartHeader          = \"<Header>\"")
                .append("\tendHeader            = \"</Header>\"")
                .append("\texternalReferenceKey = \"RpcRequestId\" // injected request id")
                .append("\tserviceCodeKey       = \"ServiceCode\"")
                .append("\tservice              = \"service\" // service key")
                .line()
                .append("\txmlRequestIdKey = \"ExternalReferenceId\" // xml request or response id")
                .line()
                .append("	// request or response")
                .append("\trequestTypeKey = \"RequestFlag\"")
                .append("\trequestFlag    = \"0\" // 0 request")
                .append("\tresponseFlag   = \"1\" // 1 response")
                .line()
                .append("	ResponseStatusSuccess uint32 = 0 // 0 response status")
                .append(")")
                .line()
                .append("var (")
                .append("	RequestHeaderLen  int = getMinimumRequestLength()")
                .append("	ResponseHeaderLen int = getMinimumResponseLength()")
                .append(")")
                .line();

        String p = option.Alias();

        if (option.getPoolMode() != PoolMode.PingPong) {
            buffer.append("// StreamId query mapping stream id")
                    .with("func (proto *").with(p).append("Protocol) StreamId(ctx context.Context, key string) (val uint64, found bool) {")
                    .append("	val, found = proto.streams.Get(key)")
                    .append("	return")
                    .append("}")
                    .line()
                    .append("// PutStreamId put mapping stream id")
                    .with("func (proto *").with(p).append("Protocol) PutStreamId(ctx context.Context, key string, val uint64) (err error) {")
                    .append("	err = proto.streams.Put(key, val)")
                    .append("	return err")
                    .append("}")
                    .line()
                    .with("func (proto *").with(p).append("Protocol) RemoveStreamId(ctx context.Context, key string) {")
                    .append("	proto.streams.Remove(key)")
                    .append("	return")
                    .append("}")
                    .line();
        }

        if (option.isXmlCodec()) {
            buffer.append("func getMinimumRequestLength() int {")
                    .with("	return ").append(String.valueOf(option.getCodecOption().length))
                    .append("}")
                    .line()
                    .append("func getMinimumResponseLength() int {")
                    .with("	return ").append(String.valueOf(option.getCodecOption().length))
                    .append("}")
                    .line()
                    .append("func getEncodeHeaderLength(h *common.Header) uint32 {")
                    .append("	return 0 // fixed length + body 没有key-value")
                    .append("}")
                    .line();
        } else {
            buffer.append("func getMinimumRequestLength() int {")
                    .append("\tpanic(\"实现: 请求协议头部最小长度，用于读取最小协议头部可以继续解码\")")
                    .line()
                    .append("	// TODO: 删除panic以及以下注释，实现getMinimumRequestLength方法:")
                    .append("	// 假设协议头部最小长度16，即可从中计算出完整协议长度")
                    .append("	// return 16")
                    .append("}")
                    .line()
                    .append("func getMinimumResponseLength() int {")
                    .append("\tpanic(\"实现: 响应协议头部最小长度，用于读取最小协议头部可以继续解码\")")
                    .line()
                    .append("	// TODO: 删除panic以及以下注释，实现getMinimumResponseLength方法:")
                    .append("	// 假设协议头部最小长度16，即可从中计算出完整协议长度")
                    .append("	// return 16")
                    .append("}")
                    .line()
                    .append("func getEncodeHeaderLength(h *common.Header) uint32 {")
                    .append("\tpanic(\"实现: getEncodeHeaderLength计算编码header占用长度\")")
                    .append("	// TODO: 删除panic以及以下注释，实现getEncodeHeaderLength方法:")
                    .append("	// 假设key/value编码后的格式： key=value&other_key=other_value&...=...")
                    .append("	//")
                    .append("	// total := int(h.ByteSize())     // 计算所有key、value的字节长度")
                    .append("	// total += len(h.Header.Kvs) - 1 // 加上所有&符号长度")
                    .append("	// total += len(h.Header.Kvs)     // 加上所有=符号长度")
                    .append("	// return uint32(total)           // 返回最终编码后长度")
                    .append("}")
                    .line();
        }

        buffer.append("func fetchId(data []byte) string {")
                .append("	h := common.Header{}")
                .append("	injectHeaders(data, &h, 0, len(data), len(data))")
                .append("	id, _ := h.Get(externalReferenceKey)")
                .append("	return id")
                .append("}")
                .line()
                .append("func injectHeaders(data []byte, h *common.Header, headerLen int, contentLen int, totalLen int) error {")
                .append("	if len(data) <= 0 {")
                .append("		return nil")
                .append("	}")
                .line()
                .append("	decodedHeader, err := parseHeader(data[headerLen:totalLen])")
                .append("	if err != nil {")
                .append("		return err")
                .append("	}")
                .line()
                .append("\tpanic(\"实现：从decodedHeader中读取请求id、请求/响应类型和服务标识信息\")")
                .append("	// example // TODO 从decodedHeader读取原报文属性, 删除以下注释")
                .append("	// code := decodedHeader[serviceCodeKey]")
                .append("	// flag := decodedHeader[requestTypeKey]")
                .append("	// id := decodedHeader[xmlRequestIdKey]")
                .append("	_ = decodedHeader")
                .line()
                .append("\tpanic(\"实现：往rpc头部添加请求id、请求/响应类型和服务标识信息\")")
                .append("	// example TODO 塞进rpc的请求头部h中, 删除以下注释")
                .append("	// 注意：服务标识service这个key需要和metadata.json中配置一致, 其他服务治理的属性类似方法填进rpc头部即可")
                .append("	// inject service 服务标识")
                .append("	// h.Set(service, code)")
                .append("	// inject request flag")
                .append("	// h.Set(requestTypeKey, flag)")
                .append("	// inject request id")
                .append("	// h.Set(externalReferenceKey, id)")
                .line()
                .append("	// update decodedHeader unchanged, avoid encode again.")
                .append("	h.Changed = false")
                .line()
                .append("	return nil")
                .append("}")
                .line()
                .append("// parseHeader decode xml header")
                .append("func parseHeader(data []byte) (XmlHeader, error) {")
                .append("	xmlBody := string(data)")
                .append("	index := strings.Index(xmlBody, startHeader)")
                .append("	// more xml example: https://pkg.go.dev/encoding/xml")
                .append("	header := XmlHeader{}")
                .append("	// parse header key value")
                .append("	if index >= 0 {")
                .append("		headerEndIndex := strings.Index(xmlBody, endHeader)")
                .append("		xmlHeader := xmlBody[index : headerEndIndex+len(endHeader)]")
                .append("\t\tif xmlHeader != \"\" {")
                .append("			xmlDecoder := xml.NewDecoder(bytes.NewBufferString(xmlHeader))")
                .append("			if err := xmlDecoder.Decode(&header); err != nil {")
                .append("				return nil, err")
                .append("			}")
                .append("		}")
                .append("	}")
                .append("	return header, nil")
                .append("}")
                .line()
                .append("// XmlHeader xml key value pair.")
                .append("// Protocol-specific, depending on")
                .append("// traditional protocol data structures")
                .append("type XmlHeader map[string]string")
                .line()
                .append("type KeyValueEntry struct {")
                .append("	XMLName xml.Name")
                .append("\tValue   string `xml:\",chardata\"`")
                .append("}")
                .line()
                .append("func (m XmlHeader) MarshalXML(e *xml.Encoder, start xml.StartElement) error {")
                .append("	if len(m) == 0 {")
                .append("		return nil")
                .append("	}")
                .line()
                .append("	if err := e.EncodeToken(start); err != nil {")
                .append("		return err")
                .append("	}")
                .line()
                .append("	for k, v := range m {")
                .append("		e.Encode(KeyValueEntry{XMLName: xml.Name{Local: k}, Value: v})")
                .append("	}")
                .line()
                .append("	return e.EncodeToken(start.End())")
                .append("}")
                .line()
                .append("func (m *XmlHeader) UnmarshalXML(d *xml.Decoder, start xml.StartElement) error {")
                .append("	*m = XmlHeader{}")
                .append("	for {")
                .append("		var e KeyValueEntry")
                .line()
                .append("		err := d.Decode(&e)")
                .append("		if err == io.EOF {")
                .append("			break")
                .append("		} else if err != nil {")
                .append("			return err")
                .append("		}")
                .line()
                .append("		(*m)[e.XMLName.Local] = e.Value")
                .append("	}")
                .line()
                .append("	return nil")
                .append("}").line();

        if (!option.isXmlCodec()) {
            buffer.append("func encodeHeader(buf buffer.IoBuffer, h *common.Header) {")
                    .append("	ioBuf := bytes.Buffer{}")
                    .append("	h.Range(func(Key, Value string) bool {")
                    .line()
                    .append("\t\tpanic(\"实现: 编码协议头部\")")
                    .line()
                    .append("		// TODO: 删除panic以及以下注释，实现encodeHeader方法:")
                    .append("		// 假设key/value待编码的格式： key=value&other_key=other_value&...=...")
                    .append("		// example ")
                    .append("		// 除第一个key=value之外都需要追加 &")
                    .append("		// if ioBuf.Len() > 0 {")
                    .append("\t\t//\t ioBuf.WriteString(\"&\")")
                    .append("		// }")
                    .append("		// 写入临时变量ioBuf")
                    .append("		// ioBuf.WriteString(Key)")
                    .append("\t\t// ioBuf.WriteString(\"=\")")
                    .append("		// ioBuf.WriteString(Value)")
                    .line()
                    .append("		return true")
                    .append("	})")
                    .line()
                    .append("	// ioBuf变量刷新到编码buf")
                    .append("	buf.WriteString(ioBuf.String())")
                    .append("}");
        }

        return new Source(name, path, buffer.toString());
    }

    @Override
    public Source buffer(ProtocolOption option) {
        return null;
    }
}
