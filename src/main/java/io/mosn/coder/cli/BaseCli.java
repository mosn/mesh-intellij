package io.mosn.coder.cli;

import io.mosn.coder.upgrade.ProjectMod;

import java.io.File;

/**
 * @author yiji@apache.org
 */
public abstract class BaseCli {

    @CommandLine.ParentCommand
    Cli parent;

    @CommandLine.Option(required = true, names = {"--plugin", "-p"}, description = "Set the plugin name")
    String plugin;

    @CommandLine.Option(names = {"--api-version"}, description = "Set api version, default read from go.mod")
    String api;

    @CommandLine.Option(names = {"--pkg-version"}, description = "Set pkg version, default read from go.mod")
    String pkg;

    @CommandLine.Option(required = true, names = {"--project-dir", "-d"}, paramLabel = "<dir>", description = "Set the path to the project including project name")
    String path;

    @CommandLine.Option(names = {"--organization", "-o"}, paramLabel = "<organization>", description = "New project module prefix name, if the project already exists, this value does not need to be provided. example: github.com/zonghaishang")
    String organization;

    void check() {
        if (parent != null) {

            File file = new File(/*parent.*/path);
            if (file == null || !file.exists()) {
                if (/*parent.*/organization == null || /*parent.*/organization.length() <= 0) {
                    System.err.println("--organization is required, example: github.com/zonghaishang");
                    System.exit(0);
                }
            }

            /**
             * update api and pkg
             */

            File upgradeMod = new File(path, "build/upgrade/remote.mod");
            if (upgradeMod.exists()) {
                ProjectMod current = new ProjectMod(path, "build/upgrade/remote.mod");
                current.readFile();

                if (current.getApi() != null) {
                    api = current.getApi();
                }

                if (current.getPkg() != null) {
                    pkg = current.getPkg();
                }

                return;
            }

            File mod = new File(path, "go.mod");
            if (mod.exists()
                    /**
                     * go.mod is required and user not reset api or pkg
                     */
                    && ((api == null || api.length() == 0) || (pkg == null || pkg.length() == 0))
            ) {
                /**
                 * read default go.mod
                 */
                ProjectMod current = new ProjectMod(path, "go.mod");
                current.readFile();

                if (current.getApi() != null) {
                    api = current.getApi();
                }

                if (current.getPkg() != null) {
                    pkg = current.getPkg();
                }
            }

        }
    }
}
