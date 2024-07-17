package io.mosn.coder.cli;


import io.mosn.coder.intellij.option.AbstractOption;
import io.mosn.coder.intellij.option.PluginType;
import io.mosn.coder.intellij.option.TranscoderOption;
import io.mosn.coder.intellij.roobot.CodeGenerator;

import java.io.File;

/**
 * @author yiji@apache.org
 */

@CommandLine.Command(name = "trans",
        aliases = {"transcoder"},
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Generate transcoder plugin code.",
        description = "Automatically generate transcoder extension code.")
public class CliTranscoder extends BaseCli implements Runnable {

    @CommandLine.Option(required = true, names = {"--apply-side", "-s"}, description = "Set the effective mode, eg: client|server")
    String side;

    @CommandLine.Option(required = true, names = {"--src-protocol"}, description = "Set the source protocol name")
    String srcProtocol;

    @CommandLine.Option(required = true, names = {"--dst-protocol"}, description = "Set the destination protocol name")
    String dstProtocol;

    @Override
    public void run() {

        check();

        TranscoderOption option = new TranscoderOption();
        option.setPluginName(this.plugin);
        option.setOrganization(this.organization);

        option.setActiveMode(
                this.side.contains("client")
                        ? AbstractOption.ActiveMode.Client : AbstractOption.ActiveMode.Server);

        option.setSrcProtocol(this.srcProtocol);
        option.setDstProtocol(this.dstProtocol);

        if (api != null) {
            option.setApi(this.api);
        }

        if (pkg != null) {
            option.setPkg(this.pkg);
        }


        CodeGenerator.createCliApplication(new File(path), PluginType.Transcoder, option);

    }
}