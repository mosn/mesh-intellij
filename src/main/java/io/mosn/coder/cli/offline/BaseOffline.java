package io.mosn.coder.cli.offline;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.deserializer.MapDeserializer;
import io.mosn.coder.cli.BaseDeploy;
import io.mosn.coder.cli.CommandLine;
import io.mosn.coder.common.DirUtils;
import io.mosn.coder.common.StringUtils;
import io.mosn.coder.compiler.TerminalCompiler;
import io.mosn.coder.intellij.option.PluginType;
import io.mosn.coder.plugin.model.PluginBundle;
import io.mosn.coder.plugin.model.PluginMetadata;
import io.mosn.coder.registry.SubscribeConsoleAddress;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static io.mosn.coder.common.DirUtils.isPluginTypeDir;
import static io.mosn.coder.common.DirUtils.pluginTypeOf;
import static io.mosn.coder.registry.SubscribeConsoleAddress.ENDPOINT_KEY;
import static io.mosn.coder.registry.SubscribeConsoleAddress.INSTANCE_KEY;

/**
 * support offline deploy or upgrade
 *
 * @author yiji@apache.org
 */
public abstract class BaseOffline extends BaseDeploy {


    @CommandLine.Option(names = {"--conf", "-c"}, paramLabel = "<file>", description = "Set plugin conf file, including instance id and etc")
    String conf;

    @CommandLine.Option(required = true, names = {"--project-dir", "-d"}, paramLabel = "<dir>", description = "Set the path to the project, if the init argument exists, the directory will be initialized")
    String path;

    @CommandLine.Option(names = {"--init", "-i"}, paramLabel = "<init>", description = "init plugin struct")
    String init;

    protected void setupPluginConf() {

        File pf = conf != null ? new File(conf) : new File(project, "env_conf");

        /**
         * init project ?
         */
        if (init != null && init.length() > 0 && init.equals("true")) {

            /**
             * create plugin deploy directory
             */

            if (project != null && project.length() > 0) {
                File dir = new File(project);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                if (dir.isDirectory() && dir.exists()) {
                    // create filters
                    File filters = new File(dir, DirUtils.STREAM_FILTERS_DIR);
                    if (!filters.exists()) filters.mkdir();

                    // create transcoders
                    File transcoders = new File(dir, DirUtils.TRANSCODER_DIR);
                    if (!transcoders.exists()) transcoders.mkdir();

                    // create codecs
                    File codecs = new File(dir, DirUtils.CODECS_DIR);
                    if (!codecs.exists()) codecs.mkdir();

                    // create tracers
                    File tracers = new File(dir, DirUtils.TRACE_DIR);
                    if (!tracers.exists()) tracers.mkdir();
                }
            }
            System.out.println("init plugin directory success");
            System.exit(0);
        }

        if (!pf.exists() || !pf.isFile()) {
            System.err.println("env_conf not found, plugin --conf is required.");
            System.exit(0);
        }

        /**
         * @see SubscribeConsoleAddress#getProjectNotify(String)
         */
        System.setProperty(SubscribeConsoleAddress.PLUGIN_CONF, pf.getAbsolutePath());

        // check parameter
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(pf)) {
            properties.load(in);

            /***
             * check instance id、access key、secret key
             */
            String endpoint = properties.getProperty(ENDPOINT_KEY);
            String instanceId = properties.getProperty(INSTANCE_KEY);

            /**
             String access = properties.getProperty(ACCESS_KEY);
             String secret = properties.getProperty(SECRET_KEY);
             **/

            if (instanceId == null || instanceId.length() == 0) {
                System.err.println(INSTANCE_KEY + " is required from '" + pf.getAbsolutePath() + "'");
                System.exit(0);
            }

            if (endpoint == null || endpoint.length() == 0) {
                System.err.println(ENDPOINT_KEY + " is required from '" + pf.getAbsolutePath() + "'");
                System.exit(0);
            }

        } catch (Exception e) {
            System.err.println("resolve conf error: " + e.getMessage());
            System.exit(0);
        }
    }

    @Override
    protected String checkUpgradeVersion(String updateVersion, List<PluginBundle.Plugin> plugins, PluginBundle localBundle) {
        /**
         * offline upgrade
         */
        if (localBundle != null
                && localBundle.getBundles() != null
                && !localBundle.getBundles().isEmpty()) {

            /**
             * we should update server plugins.
             *
             * flat plugin kind -> plugins
             */

            Map<String, List<PluginBundle.Plugin>> kinds = new HashMap<>();
            for (PluginBundle.Plugin plugin : localBundle.getBundles()) {
                List<PluginBundle.Plugin> pf = kinds.get(plugin.getKind());
                if (pf == null) {
                    pf = new ArrayList<>();
                    kinds.put(plugin.getKind(), pf);
                }
                pf.add(plugin);
            }

            for (PluginBundle.Plugin p : plugins) {
                List<PluginBundle.Plugin> pf = kinds.get(p.getKind());
                if (pf == null) {
                    // skip current plugin
                    p.setOwner(false);
                    continue;
                }

                boolean found = false;
                for (PluginBundle.Plugin lp : pf) {
                    if (lp.getName().equals(p.getName())) {

                        found = true;

                        // local plugin is found
                        p.setVersion(lp.getVersion());
                        p.setOwner(true);

                        p.setFullName(p.getName() + (
                                p.getVersion() == null
                                        ? ".zip" : ("-" + p.getVersion())) + ".zip");

                        p.setDependency(lp.getDependency());
                        p.setMetadata(lp.getMetadata());

                        if (p.getVersion() != null && p.getVersion().equals(p.getRevision())) {
                            return "The upgraded version '" + p.getVersion() + "' must be different from the older version '" + p.getRevision() + "'";
                        }

                        break; // next plugin
                    }
                }

                if (!found) {
                    p.setOwner(false);
                }
            }
        }

        return null;
    }

    @Override
    protected void executePluginUpload(PluginBundle bundle, PluginBundle localBundle) {
        deployPlugins(bundle);
    }

    protected void createAllPlugins(String project, PluginBundle bundle) {
        File[] files = new File(project).listFiles();
        if (files != null) {
            for (File file : files) {
                // same kind plugin
                if (file.isDirectory() && isPluginTypeDir(file.getName())) {
                    createSameKindPlugins(file.getPath(), pluginTypeOf(file.getName()), bundle);
                }
            }
        }

        if (bundle.getBundles() == null || bundle.getBundles().isEmpty()) {
            System.err.println("no plugin found.");
            System.exit(0);
        }
    }

    protected void createSameKindPlugins(String path, PluginType type, PluginBundle bundle) {
        File[] files = new File(path).listFiles();
        if (files != null) {
            for (File file : files) {
                // plugin name with version
                if (file.isFile() && file.getName().endsWith("zip")) {
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
        String version = "1.0.0";

        int index = name.indexOf("-");
        if (index > 0) {
            plugin.setName(name.substring(0, index));
            version = name.substring(index + 1 /** skip '-' */, name.lastIndexOf("."));
        } else {
            // name format : xxx.zip
            plugin.setName(name.substring(0, name.indexOf(".")));
        }

        // offline: we don't current revision
        // just same as local version
        plugin.setVersion(version);
        plugin.setRevision((version));

        plugin.setFullName(name);

        if (plugin.getKind() != null)
            bundle.getBundles().add(plugin);
    }

    @Override
    public String deployOrUpgradePlugins(PluginBundle deployBundle) {

        // check bundle is ready
        String error = checkPluginBundle(deployBundle);
        if (error != null) return error;

        try {
            signal.await();
        } catch (Exception ignored) {
            return ignored.getMessage();
        }

        if (this.mode == DeployMode.Upgrade) {
            return upgradePlugins(null, deployBundle);
        }

        deployBundle.setMeshServer(this.address);

        deployPlugins(deployBundle);

        return null;

    }

    @Override
    protected void readyDeployPlugin(PluginBundle bundle) {

        System.out.println("\n\nPlease wait upload plugins...");

        TerminalCompiler.submit(() -> {

            /**
             * upload plugins
             */

            uploadPlugins(bundle);

        });

    }

    String checkPluginBundle(PluginBundle deployBundle) {
        /**
         * check plugin is valid:
         * 1. Multiple versions of the same plugin are not allowed
         * 2. The plugin name must conform to the specification
         */

        List<PluginBundle.Plugin> filters = new ArrayList<>();
        List<PluginBundle.Plugin> transcoders = new ArrayList<>();
        List<PluginBundle.Plugin> codecs = new ArrayList<>();
        List<PluginBundle.Plugin> traces = new ArrayList<>();

        for (PluginBundle.Plugin plugin : deployBundle.getBundles()) {

            /**
             * skip not current project plugin
             */
            if (!plugin.getOwner()) continue;

            switch (plugin.getKind()) {
                case PluginBundle.KIND_FILTER:
                    filters.add(plugin);
                    break;
                case PluginBundle.KIND_TRANSCODER:
                    transcoders.add(plugin);
                    break;
                case PluginBundle.KIND_TRACE:
                    traces.add(plugin);
                    break;
                case PluginBundle.KIND_PROTOCOL:
                    codecs.add(plugin);
                    break;
            }
        }

        try {

            checkPluginValid(filters);
            checkPluginValid(transcoders);
            checkPluginValid(codecs);
            checkPluginValid(traces);

            parsePluginConfig(filters);
            parsePluginConfig(transcoders);
            parsePluginConfig(codecs);
            parsePluginConfig(traces);

        } catch (Exception e) {
            return e.getMessage();
        }

        return null;
    }

    private void checkPluginValid(List<PluginBundle.Plugin> plugins) {
        // name -> version
        Map<String, String> pv = new HashMap<>();

        for (PluginBundle.Plugin plugin : plugins) {
            String version = pv.get(plugin.getName());
            if (version != null) {
                throw new RuntimeException("Plugin '" + plugin.getName() + "' contain duplicate version '" + version + "', current '" + plugin.getVersion() + "', kind " + plugin.getKind());
            }

            // check plugin version
            String err = StringUtils.checkPluginVersion(plugin.getVersion());
            if (err != null) {
                throw new RuntimeException("Plugin '" + plugin.getName() + "' version is invalid: " + err);
            }

            // put pv cache
            pv.put(plugin.getName(), plugin.getVersion());
        }
    }

    @Override
    protected String getZipDir() {
        return "";
    }

    private void parsePluginConfig(List<PluginBundle.Plugin> plugins) throws IOException {

        for (PluginBundle.Plugin plugin : plugins) {

            File file = detectPluginFile(plugin);
            if (file != null) {

                ZipFile zipFile = new ZipFile(file);
                try {
                    Enumeration<? extends ZipEntry> entries = zipFile.entries();
                    while (entries.hasMoreElements()) {

                        ZipEntry entry = entries.nextElement();
                        /**
                         * read plugin metadata
                         */
                        if (entry.getName().endsWith("metadata.json")) {
                            try (InputStream in = zipFile.getInputStream(entry)) {
                                byte[] bytes = in.readAllBytes();
                                if (bytes != null) {
                                    /**
                                     * parse plugin metadata config
                                     */
                                    parseMetadata(plugin, bytes);

                                    /**
                                     * insert protocol port
                                     */
                                    if (PluginBundle.KIND_PROTOCOL.equals(plugin.getKind())) {
                                        parseListenerConf(plugin, zipFile);
                                    }
                                }
                            } catch (Exception e) {
                                // dump console ?
                                e.printStackTrace();
                                throw new RuntimeException("failed read plugin '" + plugin.getName() + "'", e);
                            }

                            /**
                             * break while loop, parse next plugin
                             */
                            break;
                        }
                    }
                } finally {
                    if (zipFile != null) zipFile.close();
                }
            }

        }

    }

    protected File detectPluginFile(PluginBundle.Plugin plugin) {

        File file = null;

        switch (plugin.getKind()) {
            case PluginBundle.KIND_FILTER: {
                file = new File(project, "stream_filters/" + plugin.getFullName());
                break;
            }
            case PluginBundle.KIND_TRANSCODER: {
                file = new File(project, "transcoders/" + plugin.getFullName());
                break;
            }
            case PluginBundle.KIND_TRACE: {
                file = new File(project, "traces/" + plugin.getFullName());
                break;
            }
            case PluginBundle.KIND_PROTOCOL: {
                file = new File(project, "codecs/" + plugin.getFullName());
                break;
            }
        }
        return file;
    }

    private static void parseMetadata(PluginBundle.Plugin plugin, byte[] bytes) throws IOException {
        /**
         *
         * fastjson hack:
         * Array objects have only one element
         * and are automatically converted to map objects
         *
         * @see MapDeserializer#parseMap(DefaultJSONParser, Map, Type, Object, int)
         *
         */
        PluginMetadata metadata = JSON.parseObject(new ByteArrayInputStream(bytes), PluginMetadata.class);
        plugin.setMetadata(metadata);

        /**
         * check plugin is valid
         */
        if (metadata.getName() == null || !metadata.getName().equals(plugin.getName())) {
            throw new RuntimeException("plugin metadata name is invalid. ");
        }

        if (metadata.getKind() == null || !metadata.getKind().equals(plugin.getKind())) {
            throw new RuntimeException("plugin metadata kind is invalid. ");
        }

        /**
         * api or pkg changed, update metadata.json
         */
        if (plugin.getDependency() == null) {
            plugin.setDependency(new HashMap<>());
            plugin.getDependency().putAll(metadata.getDependencies());
        }
    }

    private void parseListenerConf(PluginBundle.Plugin plugin, ZipFile zipFile) throws IOException {

        ZipEntry entry;

        PluginMetadata metadata = plugin.getMetadata();
        if (metadata.getExtension() == null) {
            metadata.setExtension(new HashMap<>());
        }

        String protoConf = null;
        if ("X".equals(metadata.getFramework())) {
            protoConf = plugin.getName() + "/egress_" + plugin.getName() + ".json";
        } else if ("HTTP1".equals(metadata.getFramework())) {
            protoConf = plugin.getName() + "/egress_" + plugin.getName() + ".json";

            /**
             * failover egress_http format
             */
            if (zipFile.getEntry(protoConf) == null) {
                protoConf = plugin.getName() + "/egress_http.json";
            }
        }

        if (protoConf != null && ((entry = zipFile.getEntry(protoConf)) != null)) {

            try (InputStream fin = zipFile.getInputStream(entry)) {
                Object object = JSON.parseObject(fin, Map.class);
                if (object instanceof Map && ((Map) object).containsKey("address")) {
                    Object address = ((Map) object).get("address");
                    String ingressPort = String.valueOf(address);
                    int index = ingressPort.indexOf(":");
                    if (index > 0) {
                        ingressPort = ingressPort.substring(index + 1);
                    }

                    metadata.getExtension().put("ingressPort", ingressPort);
                }
            }
        }

        if ("X".equals(metadata.getFramework())) {
            protoConf = plugin.getName() + "/ingress_" + plugin.getName() + ".json";
        } else if ("HTTP1".equals(metadata.getFramework())) {
            protoConf = plugin.getName() + "/ingress_" + plugin.getName() + ".json";

            /**
             * failover ingress_http format
             */
            if (zipFile.getEntry(protoConf) == null) {
                protoConf = plugin.getName() + "/ingress_http.json";
            }
        }

        if (protoConf != null && ((entry = zipFile.getEntry(protoConf)) != null)) {

            try (InputStream fin = zipFile.getInputStream(entry)) {
                Object object = JSON.parseObject(fin, Map.class);
                if (object instanceof Map && ((Map) object).containsKey("address")) {
                    Object address = ((Map) object).get("address");
                    String egressPort = String.valueOf(address);
                    int index = egressPort.indexOf(":");
                    if (index > 0) {
                        egressPort = egressPort.substring(index + 1);
                    }

                    metadata.getExtension().put("egressPort", egressPort);
                }
            }
        }

    }


}
