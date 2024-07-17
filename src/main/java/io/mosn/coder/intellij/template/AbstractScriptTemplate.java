package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.util.CodeBuilder;

/**
 * @author yiji@apache.org
 */
public abstract class AbstractScriptTemplate implements Template {

    protected static void appendHeader(CodeBuilder buffer) {

        buffer.append2("#!/bin/bash").line()
                .append2("SHELL=/bin/bash");

        buffer.append("go env -w GO111MODULE=on")
                .append("go env -w GOPROXY=https://goproxy.cn,direct")
                .append("go env -w GOPRIVATE=gitlab.alipay-inc.com,code.alipay.com").line();

        buffer.append("# update mod")
                .append2("go mod tidy");
    }

}
