package io.mosn.coder.cli;

/**
 * @author yiji@apache.org
 */

@CommandLine.Command(name = "upgrade",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Upgrade plugin.",
        description = "Automatically upgrade plugin package.")
public class CliUpgrade extends BaseDeploy implements Runnable {

    @CommandLine.Option(required = true, names = {"--project-dir", "-d"}, paramLabel = "<dir>", description = "Set the path to the project including project name")
    String path;

    @CommandLine.Option(names = {"--version", "-v"}, description = "Set the deploy plugin version")
    String version;

    @Override
    public void run() {

        this.project = path;
        this.mode = DeployMode.Upgrade;

        /**
         * read user deploy version
         */
        this.upgradeVersion = version;

        this.registerCallBack(project);

        String message = this.deployOrUpgradePlugins(null);
        if (message != null) {
            System.err.println(message);
            return;
        }

        waitQuit();
    }
}
