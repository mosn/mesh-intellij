package io.mosn.coder.cli.offline;


import io.mosn.coder.cli.CommandLine;
import io.mosn.coder.plugin.model.PluginBundle;

import java.io.File;
import java.util.ArrayList;

@CommandLine.Command(name = "offline-upgrade",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Offline upgrade plugin package.",
        description = "Automatically offline upgrade plugin package.")
public class CliOfflineUpgrade extends BaseOffline implements Runnable {

    private ThreadLocal<PluginBundle> localBundle = new InheritableThreadLocal<>() {
        @Override
        protected PluginBundle initialValue() {
            PluginBundle bundle = new PluginBundle();
            bundle.setBundles(new ArrayList<>());

            return bundle;
        }
    };

    @Override
    public void run() {
        this.project = path;
        this.mode = DeployMode.Upgrade;

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
        localBundle.set(bundle);

        try {
            this.registerCallBack(project);

            String message = this.deployOrUpgradePlugins(bundle);
            if (message != null) {
                System.err.println(message);
                return;
            }
        } finally {
            localBundle.remove();
        }

        waitQuit();
    }

    @Override
    protected File getPluginFile(String project, PluginBundle.Plugin plugin) {

        if (plugin.getFullName() == null && localBundle.get() != null) {
            for (PluginBundle.Plugin p : localBundle.get().getBundles()) {
                if (p.getKind().equals(plugin.getKind())
                        && p.getName().equals(plugin.getName())) {
                    /**
                     * update server plugin full name
                     */
                    plugin.setFullName(p.getFullName());
                }
            }
        }

        /**
         * Check whether the local plugin package exists
         */
        return detectPluginFile(plugin);
    }
}
