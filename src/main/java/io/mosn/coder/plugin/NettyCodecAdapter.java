package io.mosn.coder.plugin;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.List;

import static io.mosn.coder.plugin.Protocol.*;

/**
 * @author yiji@apache.org
 */
public class NettyCodecAdapter {

    private final ChannelHandler encoder = new InternalEncoder();

    private final ChannelHandler decoder = new InternalDecoder();

    public ChannelHandler getEncoder() {
        return encoder;
    }

    public ChannelHandler getDecoder() {
        return decoder;
    }

    private class InternalEncoder extends MessageToByteEncoder {

        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf buf) throws Exception {
            if (msg instanceof Request) {
                encodeRequest((Request) msg, buf);
            } else if (msg instanceof Response) {
                encodeResponse((Response) msg, buf);
            }
        }

        private void encodeRequest(Request msg, ByteBuf buf) {
            Request request = msg;

            /**
             * write magic、request type and request id
             */
            buf.writeByte(request.magic);
            buf.writeShort(request.flag);
            buf.writeShort(request.requestId);

            /**
             * skip 4 byte length, will be set when encode header and body
             */
            int lenIndex = buf.writerIndex();
            // skip length
            buf.writerIndex(lenIndex + 4);


            /**
             * write timeout and eof flag
             */
            buf.writeShort(request.timeout);
            buf.writeByte(request.eof);


            /**
             * skip 4 byte attr length, will be set when encode header and body
             */
            int attrIndex = buf.writerIndex();
            // skip attr length
            buf.writerIndex(attrIndex + 4);

            if (request.headers != null && !request.headers.isEmpty()) {
                request.attrLength = encodeHeader(buf, request.headers);
            }

            int contentLength = request.contents != null ? request.contents.length : 0;
            if (contentLength > 0) {
                buf.writeBytes(request.contents);
            }

            // fill length and attr length
            request.length = request.attrLength + contentLength;
            buf.setInt(lenIndex, request.length);
            buf.setInt(attrIndex, request.attrLength);
        }

        private void encodeResponse(Response msg, ByteBuf buf) {
            Response response = msg;

            /**
             * write magic、request type and request id
             */
            buf.writeByte(response.magic);
            buf.writeShort(response.flag);
            buf.writeShort(response.requestId);

            /**
             * skip 4 byte length, will be set when encode header and body
             */
            int lenIndex = buf.writerIndex();
            // skip length
            buf.writerIndex(lenIndex + 4);


            /**
             * write status and eof flag
             */
            buf.writeShort(response.status);
            buf.writeByte(response.eof);


            /**
             * skip 4 byte attr length, will be set when encode header and body
             */
            int attrIndex = buf.writerIndex();
            // skip attr length
            buf.writerIndex(attrIndex + 4);

            if (response.headers != null && !response.headers.isEmpty()) {
                response.attrLength = encodeHeader(buf, response.headers);
            }

            int contentLength = response.contents != null ? response.contents.length : 0;
            if (contentLength > 0) {
                buf.writeBytes(response.contents);
            }

            // fill length and attr length
            response.length = response.attrLength + contentLength;
            buf.setInt(lenIndex, response.length);
            buf.setInt(attrIndex, response.attrLength);
        }
    }

    private class InternalDecoder extends ByteToMessageDecoder {

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {

            try {
                do {
                    int offset = buf.readerIndex();
                    if (!packetReceived(buf, offset)) {
                        /**
                         * we break loop and waiting next read loop
                         */
                        return;
                    }

                    /***
                     * decode remote rpc packet
                     */
                    decodePacket(buf, out, offset);

                } while (buf.readableBytes() > 0);
            } catch (Exception e) {
                throw new RuntimeException("Decode failed", e);
            }
        }

        private boolean packetReceived(ByteBuf buf, int offset) {
            /** we expect at least 16 byte proto header length */
            if (buf.readableBytes() < RequestHeaderLen) {
                return false;
            }

            int headerLength = buf.getInt(offset + HeaderLengthIndex);
            int frameLength = RequestHeaderLen + headerLength;
            /** now we check all packet bytes already received? */
            if (buf.readableBytes() < frameLength) {
                return false;
            }
            return true;
        }

        private void decodePacket(ByteBuf buf, List<Object> out, int offset) {
            short flag = buf.getShort(offset + FlagIndex);
            switch (flag) {
                case CmdRequest:
                case CmdRequestHeartbeat: {
                    decodeRequest(buf, out);
                    break;
                }
                case CmdResponse:
                case CmdResponseHeartbeat: {
                    decodeResponse(buf, out);
                    break;
                }
                default: {
                    throw new RuntimeException("Bad protocol frame, flag '" + flag + "', only support 1,2,3,4 flag type");
                }
            }
        }

        private void decodeRequest(ByteBuf buf, List<Object> out) {
            int saved = buf.readerIndex();

            Request request = new Request();
            request.magic = buf.readByte();
            request.flag = buf.readShort();
            request.requestId = buf.readShort();
            request.length = buf.readInt();
            request.timeout = buf.readShort();
            request.eof = buf.readByte();
            request.attrLength = buf.readInt();

            int offset = buf.readerIndex();
            if (request.attrLength > 0) {
                /**
                 * avoid memory copy, don't forget call release.
                 */
                request.wrapHeader = buf.slice(offset, request.attrLength);
                decodeHeader(request.wrapHeader, request.headers);
            }

            int contentLength = request.length - request.attrLength;
            if (contentLength > 0) {
                /**
                 * avoid memory copy, don't forget call release.
                 */
                request.wrapContent = buf.slice(offset + request.attrLength, contentLength);
                fillContent(request, request.wrapContent);
            }

            // update reader index
            buf.readerIndex(saved + RequestHeaderLen + request.length);

            out.add(request);
        }

        private void decodeResponse(ByteBuf buf, List<Object> out) {
            int saved = buf.readerIndex();

            Response response = new Response();
            response.magic = buf.readByte();
            response.flag = buf.readShort();
            response.requestId = buf.readShort();
            response.length = buf.readInt();
            response.status = buf.readShort();
            response.eof = buf.readByte();
            response.attrLength = buf.readInt();

            int offset = buf.readerIndex();
            if (response.attrLength > 0) {
                /**
                 * avoid memory copy, don't forget call release.
                 */
                response.wrapHeader = buf.slice(offset, response.attrLength);
                decodeHeader(response.wrapHeader, response.headers);
            }

            int contentLength = response.length - response.attrLength;
            if (contentLength > 0) {
                /**
                 * avoid memory copy, don't forget call release.
                 */
                response.wrapContent = buf.slice(offset + response.attrLength, contentLength);
                fillContent(response, response.wrapContent);
            }

            // update reader index
            buf.readerIndex(saved + RequestHeaderLen + response.length);

            out.add(response);
        }
    }

}