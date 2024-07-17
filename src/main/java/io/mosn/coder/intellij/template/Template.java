package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;

import java.util.List;

/**
 * @author yiji@apache.org
 */
public interface Template<T extends PluginOption> {

    List<Source> create(T option);
}
