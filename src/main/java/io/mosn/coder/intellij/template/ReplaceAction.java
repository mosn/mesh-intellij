package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;

/**
 * @author yiji@apache.org
 */
public interface ReplaceAction {

    TextLine replace(String line, PluginOption... options);

}
