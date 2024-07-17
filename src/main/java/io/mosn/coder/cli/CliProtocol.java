package io.mosn.coder.cli;

import io.mosn.coder.intellij.internal.BoltOptionImpl;
import io.mosn.coder.intellij.internal.DubboOptionImpl;
import io.mosn.coder.intellij.internal.SpringCloudOptionImpl;
import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.roobot.CodeGenerator;

import java.io.File;
import java.util.ArrayList;

import static io.mosn.coder.intellij.option.AbstractOption.*;
import static io.mosn.coder.intellij.option.CodecType.FixedLength;
import static io.mosn.coder.intellij.util.Constants.COMMA_SPLIT_PATTERN;

/**
 * @author yiji@apache.org
 */

@CommandLine.Command(name = "protocol",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Generate protocol plugin code.",
        description = "Automatically generate protocol extension code, including TCP and HTTP protocol types.")
public class CliProtocol extends BaseCli implements Runnable {

    @CommandLine.Option(names = {"--http", "-h"}, paramLabel = "<http>", description = "Set http protocol type")
    boolean http;

    @CommandLine.Option(names = {"--inject", "-i"}, paramLabel = "<inject>", description = "Inject decode http body code")
    boolean inject;

    @CommandLine.Option(names = {"--server-port", "-s"}, paramLabel = "<server-port>", description = "Set server port")
    Integer serverPort;

    @CommandLine.Option(names = {"--client-port", "-c"}, required = true, paramLabel = "<client-port>", description = "Set client port")
    Integer clientPort;

    @CommandLine.Option(names = {"--pool-mode"}, required = true, paramLabel = "<pool-mode>", description = "Set connection pool mode")
    String mode;

    @CommandLine.Option(names = {"--request-id-string"}, paramLabel = "<string id>", description = "Set request id is string type")
    boolean requestIdIsStringType;

    @CommandLine.Option(names = {"--standard-protocol"}, description = "Generate standard protocol, eg: dubbo,bolt,springcloud")
    String standardProtocols;

    @CommandLine.Option(names = {"--codec"}, description = "Set protocol codec type, eg: none|FixedLength")
    String codec;

    @CommandLine.Option(names = {"--fixed-length"}, description = "Set fixed byte length")
    int fixedLength;

    @CommandLine.Option(names = {"--fixed-prefix"}, description = "Set codec prefix, default \"0\"")
    String fixedPrefix;

    @CommandLine.Option(names = {"--service-key"}, required = true, defaultValue = "service", prompt = "service", description = "Set service key for sidecar")
    String serviceKey;

    @CommandLine.Option(names = {"--service-method-key"}, description = "Set service method key for sidecar")
    String serviceMethodKey;

    @CommandLine.Option(names = {"--service-trace-key"}, description = "Set service trace key for sidecar")
    String serviceTraceKey;

    @CommandLine.Option(names = {"--service-span-key"}, description = "Set service span key for sidecar")
    String serviceSpanKey;

    private PoolMode poolMode;

    private CodecType codecType;

    @Override
    void check() {
        super.check();

        boolean bad = false;

        // pool mode
        if (!this.mode.equalsIgnoreCase("Multiplex")
                && !this.mode.equalsIgnoreCase("PingPong")) {
            System.err.println("--pool-mode must be Multiplex or PingPong");
            bad = true;
        }

        if (this.mode.equalsIgnoreCase("Multiplex")) {
            poolMode = PoolMode.Multiplex;
        } else if (this.mode.equalsIgnoreCase("PingPong")) {
            poolMode = PoolMode.PingPong;
        }

        // codec
        if (this.codec != null && !"None".equalsIgnoreCase(this.codec)
                && !"FixedLength".equalsIgnoreCase(this.codec)) {
            System.err.println("--pool-mode must be None or FixedLength");
            bad = true;
        }

        if (this.codec == null || this.codec.equalsIgnoreCase("None")) {
            codecType = CodecType.Customize;
        } else if (this.codec.equalsIgnoreCase("FixedLength")) {
            codecType = CodecType.FixedLength;
        }

        if (codecType == FixedLength) {
            if (this.fixedLength <= 0) {
                System.err.println("--fixed-length must be > 0 ");
                bad = true;
            }
        }

        if (bad) {
            System.exit(0);
        }
    }

    @Override
    public void run() {

        /**
         * check required field.
         */
        check();

        /**
         * init protocol parameter
         */
        ProtocolOption option = new ProtocolOption();

        option.setPluginName(this.plugin);
        option.setOrganization(this.organization);

        option.setHttp(this.http);
        option.setInjectHead(this.inject);

        option.setServerPort(this.serverPort);
        option.setClientPort(this.clientPort);

        if (!option.isHttp()) {

            option.setPoolMode(this.poolMode);
            option.setStringRequestId(this.requestIdIsStringType);

            if (codecType == FixedLength) {
                int length = this.fixedLength;
                if (length > 0 && this.fixedPrefix == null) {
                    this.fixedPrefix = "0";
                    option.setCodecOption(
                            new AbstractOption.CodecOption(true, length, this.fixedPrefix));
                }
            }
        }

        // check service key
        String dataId = this.serviceKey;

        // service key
        option.addRequired(X_MOSN_DATA_ID, COMMA_SPLIT_PATTERN.split(dataId));

        if (this.serviceMethodKey != null) {
            option.addOptional(X_MOSN_METHOD, COMMA_SPLIT_PATTERN.split(this.serviceMethodKey));
        }
        if (this.serviceTraceKey != null) {
            option.addOptional(X_MOSN_TRACE_ID, COMMA_SPLIT_PATTERN.split(this.serviceTraceKey));
        }
        if (this.serviceSpanKey != null) {
            option.addOptional(X_MOSN_SPAN_ID, COMMA_SPLIT_PATTERN.split(this.serviceSpanKey));
        }

        ArrayList<ProtocolOption> opts = new ArrayList<>();

        if (this.standardProtocols != null && this.standardProtocols.contains("bolt")) {
            opts.add(new BoltOptionImpl());
        }
        if (this.standardProtocols != null && this.standardProtocols.contains("dubbo")) {
            opts.add(new DubboOptionImpl());
        }
        if (this.standardProtocols != null && this.standardProtocols.contains("springcloud")) {
            opts.add(new SpringCloudOptionImpl());
        }

        option.setEmbedded(opts);

        if (api != null) {
            option.setApi(this.api);
        }

        if (pkg != null) {
            option.setPkg(this.pkg);
        }

        CodeGenerator.createCliApplication(new File(path), PluginType.Protocol, option);

    }
}