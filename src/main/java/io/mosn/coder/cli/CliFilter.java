package io.mosn.coder.cli;


import io.mosn.coder.intellij.option.AbstractOption;
import io.mosn.coder.intellij.option.FilterOption;
import io.mosn.coder.intellij.option.PluginType;
import io.mosn.coder.intellij.roobot.CodeGenerator;

import java.io.File;

/**
 * @author yiji@apache.org
 */

@CommandLine.Command(name = "filter",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Generate filter plugin code.",
        description = "Automatically generate stream filter extension code.")
public class CliFilter extends BaseCli implements Runnable {

    @CommandLine.Option(required = true, names = {"--apply-side", "-s"}, description = "Set the effective mode, eg: client,server|all")
    String side;

    @Override
    public void run() {

        check();

        FilterOption option = new FilterOption();
        option.setPluginName(this.plugin);
        option.setOrganization(this.organization);

        option.setActiveMode(
                this.side.contains("all") ? AbstractOption.ActiveMode.ALL : (
                        this.side.contains("client")
                                ? AbstractOption.ActiveMode.Client : AbstractOption.ActiveMode.Server));

        if (api != null) {
            option.setApi(this.api);
        }

        if (pkg != null) {
            option.setPkg(this.pkg);
        }

        CodeGenerator.createCliApplication(new File(path), PluginType.Filter, option);

    }
}