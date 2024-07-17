package io.mosn.coder.plugin.handler;


import io.mosn.coder.plugin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.mosn.coder.plugin.Protocol.Response;

/**
 * @author yiji@apache.org
 */
public class UploadFileFactory implements HandlerFactory {

    public static final String action = "sys_upload_file";

    private static final Logger LOG = LoggerFactory.getLogger(UploadFileFactory.class.getName());

    public UploadFileFactory() {
    }

    public void init() {
        HandlerAdapter.registerFactory(action, this);
    }

    @Override
    public Handler createHandler() {
        return new UploadFileHandler();
    }

    public class UploadFileHandler extends AbstractHandler {
    }
}