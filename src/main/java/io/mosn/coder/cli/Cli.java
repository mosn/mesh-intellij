package io.mosn.coder.cli;


import io.mosn.coder.cli.minimesh.CliMiniMesh;
import io.mosn.coder.cli.offline.CliOfflineDeploy;
import io.mosn.coder.cli.offline.CliOfflineUpgrade;

/**
 * @author yiji@apache.org
 */
@CommandLine.Command(name = "sofactl", mixinStandardHelpOptions = true, sortOptions = false,
        version = "sofactl version 1.0.13",
        description = "mosn plugin code generator",
        commandListHeading = "%nCommands:%n%nThe most commonly used mecha commands are:%n",
        footer = "%nSee 'sofactl help <command>' to read about a specific subcommand or concept.",
        subcommands = {
                CliProtocol.class,
                CliFilter.class,
                CliTranscoder.class,
                CliDeploy.class,
                CliUpgrade.class,
                CliRefresh.class,
                CliOfflineDeploy.class,
                CliOfflineUpgrade.class,
                CliMiniMesh.class,
                CommandLine.HelpCommand.class
        })
public class Cli implements Runnable {

    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        // if the command was invoked without subcommand, show the usage help
        spec.commandLine().usage(System.err);
    }

    public static void main(String[] args) {
        System.exit(new CommandLine(new Cli()).execute(args));
    }

}
