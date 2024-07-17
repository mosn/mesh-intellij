package io.mosn.coder.plugin;

import io.mosn.coder.registry.SubscribeConsoleAddress;
import io.netty.buffer.ByteBuf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * @author yiji@apache.org
 *
 * <pre> {@code
 * 0      1         3     5           9                11    12          16
 * +------+----+----+--+--+--+--+--+--+-------+--------+-----+--+--+--+--+
 * | 0x88 | req/rsp |  id |  length   | timeout/status | eof |  attr len |
 * +------+----+----+--+--+--+--+--+--+-------+--------+-----+--+--+--+--+
 * | encoded attribute bytes ...   content bytes ...                     |
 * +---------------------------------------------------------------------+
 *
 * attributes:
 *     // file_offset -> file offset
 *     // file_name -> plugin file name
 *     // file_length -> plugin file length (byte)
 *     // file_version -> plugin package version
 *     // kind -> plugin file type
 *     // check_sum -> plugin file md5
 *     //
 *     // task_id -> upgrading task id (uuid)
 *     //
 *     // server:
 *     // action => init_task, upload_file, commit_task
 *     //
 *     // client:
 *     // action => event_notify
 *     //
 *     // returned with response:
 *     //
 *     // plugin_id -> saved plugin primary key (row id), only phrase == successed
 *     // phrase: init -> uploading -> uploaded -> (waiting -> success) | fail
 *     // description -> error message
 *
 * eof:
 * 	// means end of stream
 * 	// important: If the eof bit is false,
 * 	//  developers need to ensure that multiple request ids are the same
 *
 *
 * request eof:
 *     // server
 *     //   1. append random file complete
 *     //   2. write file bytes to pugin table
 *     //   3. send response with eof flag
 *     //   4. destroy response resource, eg: temp file
 *     //
 *     // client
 *     //   1. update plugin file upload status
 *
 *
 * response eof:
 *     // client
 *     //   1. update plugin file upload status
 *     //   2. destroy client resource
 *
 * }</pre>
 */
public class Protocol {

    static final byte Magic = (byte) 0x88;

    static final Integer Port = 7760;

    static final int RequestHeaderLen = 16;

    static final int FlagIndex = 1;
    static final int HeaderLengthIndex = 5;

    static final byte EOF = 1;
    static final short CmdRequest = 1;
    static final short CmdResponse = 2;
    static final short CmdRequestHeartbeat = 3;
    static final short CmdResponseHeartbeat = 4;


    // error code
    public static final short SUCCESS = (short) 0x00;
    // maybe recover
    public static final short ERROR_BAD_CHECKSUM = (short) 0x64;
    public static final short ERROR_TIMEOUT = (short) 0x65;
    // fatal error
    public static final short ERROR_NO_ACTION = (short) 0xc8;
    public static final short ERROR_WRITE_FILE = (short) 0xc9;
    public static final short ERROR_WRITE_DB = (short) 0xca;
    // biz error: 250, smile :-)
    public static final short ERROR_BIZ = (short) 0xfa;
    // unknown error
    public static final short ERROR_INTERNAL = (short) 0xff;

    public static AtomicInteger id = new AtomicInteger();

    public static Request request(String project) {
        return request(null, null, true, project);
    }

    public static Request request(
            Map<String, String> headers
            , byte[] content) {
        return request(headers, content, true);
    }

    public static Request request(
            Map<String, String> headers
            , byte[] content
            , boolean eof) {
        return request(headers, content, eof, null);
    }

    public static Request request(
            Map<String, String> headers
            , byte[] content
            , boolean eof
            , String project) {

        Request request = new Request();
        if (headers != null)
            request.headers.putAll(headers);

        if (content != null) {
            request.contents = content;
        }

        if (eof) {
            request.eof = EOF;
        }

        request.requestId = (short) id.incrementAndGet();

        /**
         * append default parameters
         */
        if (project != null) {

            SubscribeConsoleAddress.Tenant tenant = SubscribeConsoleAddress.getCurrentTenant(project);

            request.headers.put(WrapCommand.INSTANCE_ID, tenant.getInstanceId());
            long timeout = tenant.getDebugTimeout() != null ? tenant.getDebugTimeout() : RpcClient.defaultTimeout;
            if (timeout >= Short.MAX_VALUE) {
                request.timeout = -1;
                request.headers.put(Request.TIMEOUT_KEY, String.valueOf(timeout));
            } else {
                request.timeout = (short) timeout;
            }
        }

        return request;
    }

    public static class WrapCommand {

        public static final String FILE_OFFSET = "file_offset";
        public static final String FILE_NAME = "file_name";
        public static final String FILE_LENGTH = "file_length";
        public static final String FILE_VERSION = "file_version";
        public static final String KIND = "kind";
        public static final String CHECK_SUM = "check_sum";
        //
        public static final String TASK_ID = "task_id";

        // source inject rule id
        public static final String RULE_ID = "rule_id";
        // upgrade sidecar version
        public static final String UPGRADE_ID = "upgrade_id";

        public static final String INSTANCE_ID = "instance_id";

        // server & client
        public static final String ACTION = "sys_action";

        //
        public static final String PLUGIN_ID = "plugin_id";
        public static final String PHRASE = "phrase";
        public static final String DESCRIPTION = "description";

        public static final String STACK = "error_stack";

        public static final String METADATA = "metadata";

        public void setContents(byte[] contents) {
            this.contents = contents;
        }

        Map<String, String> headers = new HashMap<>();


        ByteBuf wrapHeader;

        ByteBuf wrapContent;

        byte[] contents;

        public String header(String name) {
            return headers.get(name);
        }

        // request action
        public String getAction() {
            return header(ACTION);
        }

        public String getKind() {
            return header(KIND);
        }

        public String getTaskId() {
            return header(TASK_ID);
        }

        public String getRuleId() {
            return header(RULE_ID);
        }

        public String getUpgradeId() {
            return header(UPGRADE_ID);
        }

        public String getInstanceId() {
            return header(INSTANCE_ID);
        }

        public ByteBuf getWrapContent() {
            return wrapContent;
        }

        AtomicBoolean destroy = new AtomicBoolean();

        public Map<String, String> getHeaders() {
            return headers;
        }

        public byte[] getContents() {

            if (contents == null && this.wrapContent != null
                    && this.wrapContent.refCnt() > 0 && this.wrapContent.readableBytes() > 0) {
                // readSliceContent();
            }

            return contents;
        }

        static void appendHead(Map<String, String> dst, Map<String, String> src, String... keys) {
            for (String key : keys) {
                String content = src.get(key);
                if (content != null) {
                    dst.put(key, content);
                }
            }
        }

        static void appendDefault(Map<String, String> dst, Map<String, String> src) {
            appendHead(dst, src
                    , FILE_OFFSET
                    , FILE_NAME
                    , FILE_LENGTH
                    , FILE_VERSION
                    , KIND
                    , CHECK_SUM
                    , TASK_ID
                    , ACTION
                    , PLUGIN_ID
                    , PHRASE
            );
        }

        static void appendHead(Map<String, String> dst, String key, String content) {
            if (key != null && content != null) {
                dst.put(key, content);
            }
        }

        /**
         * must invoke when complete
         */
        public void release() {

            if (destroy.compareAndSet(false, true)) {

                // release wrap header
                if (this.wrapHeader != null && this.wrapHeader.refCnt() > 0) {
                    // this.wrapHeader.release();
                }

                // release wrap content
                if (this.wrapContent != null && this.wrapContent.refCnt() > 0) {

                    /**
                     * response maybe not in io thread, auto copy data first
                     */
                    if (this.wrapContent.readableBytes() > 0
                            && this.contents == null) {
                        // readSliceContent();
                    }

                    // this.wrapContent.release();
                }
            }
        }

        private void readSliceContent() {
            ByteBuf buf = this.wrapContent;

            int len = buf.readableBytes();
            this.contents = new byte[len];

            if (buf.hasArray()) {
                int offset = buf.arrayOffset() + buf.readerIndex();
                System.arraycopy(buf.array(), offset, this.contents, 0, len);
            } else {
                buf.getBytes(buf.readerIndex(), this.contents);
            }
        }
    }

    /**
     * @author yiji@apache.org
     */
    public static class Request extends WrapCommand {

        public static final String TIMEOUT_KEY = "_rpc_timeout_";

        byte magic = Magic;
        short flag = CmdRequest;
        short requestId;

        short timeout;

        int length;

        int attrLength;

        byte eof;

        public boolean isHeartbeat() {
            return this.flag == Protocol.CmdRequestHeartbeat;
        }

        public boolean isEndOfStream() {
            return this.eof == EOF;
        }

        public Request appendHead(String key, String content) {
            if (key != null && content != null) {
                this.headers.put(key, content);
            }

            return this;
        }

        @Override
        public String toString() {
            return "Request{" + "magic=" + magic + ", flag=" + flag + (flag == CmdRequestHeartbeat ? " heartbeat" : "") + ", requestId=" + requestId + ", timeout=" + timeout + ", length=" + length + ", attrLength=" + attrLength + ", eof=" + eof + ", headers=" + headers + '}';
        }

    }

    /**
     * @author yiji@apache.org
     */
    public static class Response extends WrapCommand {

        byte magic = Magic;
        short flag = CmdResponse;
        short requestId;

        short status;
        int length;

        int attrLength;

        byte eof;

        public boolean isHeartbeat() {
            return this.flag == Protocol.CmdResponseHeartbeat;
        }

        static Response heartbeat(short id) {
            Response response = new Response();
            response.requestId = id;
            response.flag = CmdResponseHeartbeat;
            return response;
        }

        static Response unknownAction(Request request) {
            Response response = create(request).withStatus(ERROR_NO_ACTION);

            // append headers
            appendDefault(response.headers, request.headers);

            // append description
            String message = "action '" + request.headers.get(ACTION) + "' not support yet.";
            response.appendEof(DESCRIPTION, message);

            return response;
        }

        public Response failed(short status, String message) {
            this.eof = EOF;
            this.status = status;
            return this.append(DESCRIPTION, message);
        }

        public Response success(String message) {
            this.status = SUCCESS;
            this.eof = EOF;
            return this.append(DESCRIPTION, message);
        }

        public Response payload(String content) {
            return this.payload(content.getBytes());
        }

        public Response payload(byte[] content) {
            return this.payload(content, SUCCESS, null);
        }

        public Response payload(byte[] content, short status, String message) {
            this.contents = content;
            return status == SUCCESS ? this.success(null) : this.failed(status, message);
        }

        public static Response create(Request request) {
            Response response = new Response();
            response.requestId = request.requestId;
            response.eof = request.eof;
            response.status = SUCCESS;
            // append headers
            appendDefault(response.headers, request.headers);

            return response;
        }

        public Response append(String key, String content) {
            appendHead(this.headers, key, content);
            return this;
        }

        public Response appendEof(String key, String content) {
            this.eof = EOF;
            return append(key, content);
        }

        public Response withStatus(short status) {
            this.status = status;
            return this;
        }

        public Response withStack(String stack) {
            return this.append(STACK, stack);
        }

        public boolean isSuccess() {
            return this.status == SUCCESS;
        }

        public boolean isEndOfStream() {
            return this.eof == EOF;
        }

        @Override
        public String toString() {
            return "Response{" + "magic=" + magic + ", flag=" + flag + (flag == CmdResponseHeartbeat ? " heartbeat" : "") + ", requestId=" + requestId + ", status=" + status + ", length=" + length + ", attrLength=" + attrLength + ", eof=" + eof + ", headers=" + headers + '}';
        }
    }

    static void decodeHeader(ByteBuf bytes, Map<String, String> h) {
        if (bytes != null && bytes.readableBytes() > 0) {
            String str = convertByteBufToString(bytes);
            String[] items = str.split("&");
            for (String item : items) {
                String[] pair = item.split("=");
                if (pair.length == 2) {
                    h.put(pair[0], pair[1]);
                }
            }
        }
    }

    private static String convertByteBufToString(ByteBuf buf) {

        int len = buf.readableBytes();
        if (buf.hasArray()) {
            return new String(buf.array(), buf.arrayOffset() + buf.readerIndex(), len);
        }

        byte[] bytes = new byte[len];
        buf.getBytes(buf.readerIndex(), bytes);
        return new String(bytes, 0, len);
    }

    public static void fillContent(WrapCommand command, ByteBuf buf) {
        int len = buf.readableBytes();
        if (command.contents == null) {
            command.contents = new byte[len];
        }

        if (buf.hasArray()) {
            System.arraycopy(buf.array(), buf.arrayOffset() + buf.readerIndex(), command.contents, 0, len);
        } else {
            buf.getBytes(buf.readerIndex(), command.contents);
        }
    }

    static int encodeHeader(ByteBuf buf, Map<String, String> h) {
        StringBuffer sb = new StringBuffer();

        for (String key : h.keySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }

            sb.append(key);
            sb.append("=");
            sb.append(h.get(key));
        }

        byte[] bytes = sb.toString().getBytes();
        if (buf != null) {
            buf.writeBytes(bytes);
        }

        return bytes.length;
    }

}