package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.internal.DubboOptionImpl;
import io.mosn.coder.intellij.internal.SpringCloudOptionImpl;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.ProtocolOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class VsCodeLaunchTemplate implements Template {

    @Override
    public List<Source> create(PluginOption option) {
        String name = "launch.json";
        String path = ".vscode";

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        if (option != null) {
            buffer.with("{")
                    .line()
                    .append("    // Use IntelliSense to learn about possible attributes.")
                    .append("    // Hover to view descriptions of existing attributes.")
                    .append("    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387")
                    .append("    \"version\": \"0.2.0\",")
                    .append("    \"configurations\": [")
                    .append("        {")
                    .append("            \"name\": \"debug\",")
                    .append("            \"type\": \"go\",")
                    .append("            \"debugAdapter\": \"dlv-dap\", ")
                    .append("            \"request\": \"attach\",")
                    .append("            \"trace\": \"verbose\",")
                    .append("            \"showLog\": true,")
                    .append("            \"mode\": \"remote\",")
                    .append("            \"port\": 2345,")
                    .append("            \"host\": \"127.0.0.1\",")
                    .append("            \"substitutePath\": [");

            // user.home
            String module = option.context().getModule();

            // pkg/mod
            buffer.append("                { \"from\": \"${userHome}/go/pkg/mod\", \"to\": \"/go/pkg/mod\" },");

            // plugin project
            buffer.with("                { \"from\": \"${workspaceFolder}\", \"to\": \"")
                    .with("/go/src/").with(module).append("\" },");

            // enterprise mosn
            buffer.append("                { \"from\": \"${userHome}/go/src/gitlab.alipay-inc.com/ant-mesh/mosn\", \"to\": \"/go/src/gitlab.alipay-inc.com/ant-mesh/mosn\" }");

            buffer.append("            ]")
                    .append("        }")
                    .append("    ]")
                    .append("}");
        }

        return Arrays.asList(new Source(name, path, buffer.toString()));
    }
}
