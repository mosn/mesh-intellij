package io.mosn.coder.cli;

import io.mosn.coder.intellij.option.PluginType;
import io.mosn.coder.intellij.template.VersionTemplate;
import io.mosn.coder.plugin.model.PluginBundle;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;

import static io.mosn.coder.common.DirUtils.*;

/**
 * @author yiji@apache.org
 */

@CommandLine.Command(name = "deploy",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "Deploy plugin package.",
        description = "Automatically deploy plugin package.")
public class CliDeploy extends BaseDeploy implements Runnable {

    @CommandLine.Option(required = true, names = {"--project-dir", "-d"}, paramLabel = "<dir>", description = "Set the path to the project including project name")
    String path;

    @CommandLine.Option(names = {"--kind", "-k"}, description = "deploy single package or all packages, format: (protocol|filter|transcoder)[s]|all")
    String kind;


    @CommandLine.Option(names = {"--plugin", "-p"}, description = "Set the plugin name")
    String plugin;

    @CommandLine.Option(names = {"--version", "-v"}, description = "Set the deploy plugin version")
    String version;

    /**
     * Supports independent zip package deployment or upgrade
     */
    // @CommandLine.Option(names = {"--compile", "-c"}, description = "Set skip compile model, default true")
    Boolean compile;

    @Override
    public void run() {
        this.project = path;
        this.mode = DeployMode.Deploy;

        /**
         * read user deploy version
         */
        this.upgradeVersion = version;

        this.registerCallBack(project);

        /**
         * prepare bundle
         */

        PluginBundle bundle = new PluginBundle();
        bundle.setBundles(new ArrayList<>());

        switch (kind) {

            /**
             * scan local plugins
             */
            case "all": {
                createAllPlugins(project, bundle);
                break;
            }
            /**
             * scan all protocol plugins
             */
            case "protocols": {
                createSameKindPlugins(project + "/" + ROOT_DIR + "/" + CODECS_DIR, PluginType.Protocol, bundle);
                break;
            }
            /**
             * scan all filter plugins
             */
            case "filters": {
                createSameKindPlugins(project + "/" + ROOT_DIR + "/" + STREAM_FILTERS_DIR, PluginType.Filter, bundle);
                break;
            }
            /**
             * scan all transcoder plugins
             */
            case "transcoders": {
                createSameKindPlugins(project + "/" + ROOT_DIR + "/" + TRANSCODER_DIR, PluginType.Transcoder, bundle);
                break;
            }
            case "traces": {
                createSameKindPlugins(project + "/" + ROOT_DIR + "/" + TRACE_DIR, PluginType.Trace, bundle);
                break;
            }
            /**
             * scan single filter plugin
             */
            case PluginBundle.FILTER_ALIAS: {
                createSingleCommand(bundle, PluginType.Filter, this.plugin);
                break;
            }
            /**
             * scan single transcoder plugin
             */
            case PluginBundle.KIND_TRANSCODER: {
                createSingleCommand(bundle, PluginType.Transcoder, this.plugin);
                break;
            }
            case PluginBundle.KIND_TRACE: {
                createSingleCommand(bundle, PluginType.Trace, this.plugin);
                break;
            }
            /**
             * scan single protocol plugin
             */
            case PluginBundle.KIND_PROTOCOL: {
                createSingleCommand(bundle, PluginType.Protocol, this.plugin);
                break;
            }

            default: {
                System.err.println("unsupported kind '" + kind + "'");
                return;
            }
        }

        String message = this.deployOrUpgradePlugins(bundle);
        if (message != null) {
            System.err.println(message);
            return;
        }

        waitQuit();
    }

    protected void createAllPlugins(String project, PluginBundle bundle) {
        File[] files = new File(project, ROOT_DIR).listFiles();
        if (files != null) {
            for (File file : files) {
                // same kind plugin
                if (file.isDirectory() && isPluginDirectory(file.getName(), ROOT_DIR)) {
                    createSameKindPlugins(file.getPath(), pluginTypeOf(file.getName()), bundle);
                }
            }
        }
    }

    protected void createSameKindPlugins(String path, PluginType type, PluginBundle bundle) {
        File[] files = new File(path).listFiles();
        if (files != null) {
            for (File file : files) {
                // plugin name
                if (file.isDirectory()) {
                    createSingleCommand(bundle, type, file.getName());
                }
            }
        }
    }

    protected void createSingleCommand(PluginBundle bundle, PluginType type, String name) {

        /**
         * append plugin to current bundle
         */

        PluginBundle.Plugin plugin = new PluginBundle.Plugin();
        switch (type) {

            case Filter: {
                plugin.setKind(PluginBundle.KIND_FILTER);
                break;
            }
            case Transcoder: {
                plugin.setKind(PluginBundle.KIND_TRANSCODER);
                break;
            }
            case Trace: {
                plugin.setKind(PluginBundle.KIND_TRACE);
                break;
            }
            case Protocol: {
                plugin.setKind(PluginBundle.KIND_PROTOCOL);
                break;
            }
        }

        plugin.setOwner(true);
        plugin.setName(name);

        /**
         * update local plugin version
         */
        File version = new File(path, VersionTemplate.Name);
        if (version.exists()) {
            try (FileInputStream in = new FileInputStream(version)) {
                byte[] bytes = in.readAllBytes();
                if (bytes != null) {
                    String v = new String(bytes);
                    if (this.oldVersion == null) {
                        this.oldVersion = v;
                    }
                    plugin.setRevision(v);
                }
            } catch (Exception ignored) {
            }
        }

        if (plugin.getKind() != null)
            bundle.getBundles().add(plugin);
    }
}
