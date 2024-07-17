package io.mosn.coder.registry;

import io.mosn.coder.common.URL;

import java.util.List;

/**
 * @author yiji@apache.org
 */
public interface NotifyListener {

    List<URL> filter(List<String> dataSet);

    void notify(List<URL> urls);

}
