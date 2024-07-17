package io.mosn.coder.cli.offline;

import io.mosn.coder.cli.CommandLine;
import io.mosn.coder.plugin.model.PluginBundle;

import java.util.ArrayList;

@CommandLine.Command(name = "offline-deploy",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Offline deploy plugin package.",
        description = "Automatically offline deploy plugin package.")
public class CliOfflineDeploy extends BaseOffline implements Runnable {



    @Override
    public void run() {
        this.project = path;
        this.mode = DeployMode.Deploy;

        setupPluginConf();

        /**
         * prepare bundle
         */

        PluginBundle bundle = new PluginBundle();
        bundle.setBundles(new ArrayList<>());

        /**
         * create all deploy plugins
         */
        createAllPlugins(project, bundle);

        this.registerCallBack(project);

        String message = this.deployOrUpgradePlugins(bundle);
        if (message != null) {
            System.err.println(message);
            return;
        }

        waitQuit();
    }
}