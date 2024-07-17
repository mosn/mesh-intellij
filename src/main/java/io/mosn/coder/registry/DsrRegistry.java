package io.mosn.coder.registry;

import io.mosn.coder.common.URL;

/**
 * @author yiji@apache.org
 */
public class DsrRegistry extends AbstractDsrRegistry {

    private long defaultTimeout = 500;

    public DsrRegistry(URL url) {
        super(url);
    }

    @Override
    protected long getWaitAddressTimeout() {
        return defaultTimeout;
    }

}
