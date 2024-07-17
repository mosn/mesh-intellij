package io.mosn.coder.intellij.template;

import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.util.CodeBuilder;

import java.util.Arrays;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class IpTemplate implements Template {

    public static final String Name = "ip.go";

    public static final String Path = "pkg/common";

    public static final String Content;

    static {
        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.header();

        // write package and import

        buffer.line()
                .append("package common").line()
                .append("import \"net\"").line();

        // write code
        buffer.append("var IpV4 = \"\"").line();

        buffer.append("func init() {")
                .append("	IpV4 = IPV4()")
                .append("}").line();

        buffer.append("func IPV4() string {")
                .append("	ipv4s := AllIPV4()")
                .append("	if len(ipv4s) > 0 {")
                .append("		return ipv4s[0]")
                .append("	}")
                .append("\treturn \"no-hostname\"")
                .append("}").line();

        buffer.append("func AllIPV4() (ipv4s []string) {")
                .append("	addresses, err := net.InterfaceAddrs()")
                .append("	if err != nil {")
                .append("		return")
                .append("	}")
                .line()
                .append("	for _, addr := range addresses {")
                .append("		if ipNet, ok := addr.(*net.IPNet); ok && !ipNet.IP.IsLoopback() {")
                .append("			if ipNet.IP.To4() != nil {")
                .append("				ipv4 := ipNet.IP.String()")
                .append("\t\t\t\tif ipv4 == \"127.0.0.1\" || ipv4 == \"localhost\" {")
                .append("					continue")
                .append("				}")
                .append("				ipv4s = append(ipv4s, ipv4)")
                .append("			}")
                .append("		}")
                .append("	}")
                .append("	return")
                .append("}").line();

        Content = buffer.toString();
    }

    public static Source header() {
        return new Source(Name, Path, Content);
    }

    @Override
    public List<Source> create(PluginOption option) {
        return Arrays.asList(header());
    }
}