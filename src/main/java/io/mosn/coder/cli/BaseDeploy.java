package io.mosn.coder.cli;

import com.alibaba.fastjson.JSON;
import io.mosn.coder.common.NetUtils;
import io.mosn.coder.common.StringUtils;
import io.mosn.coder.common.TimerHolder;
import io.mosn.coder.common.URL;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.CommandBuilder;
import io.mosn.coder.compiler.TerminalCompiler;
import io.mosn.coder.console.CustomCommandUtil;
import io.mosn.coder.intellij.template.VersionTemplate;
import io.mosn.coder.intellij.view.SidecarInjectRuleModel;
import io.mosn.coder.plugin.Protocol;
import io.mosn.coder.plugin.RequestBuilder;
import io.mosn.coder.plugin.RpcClient;
import io.mosn.coder.plugin.handler.CommitTaskFactory;
import io.mosn.coder.plugin.handler.InitTaskFactory;
import io.mosn.coder.plugin.handler.UploadFileFactory;
import io.mosn.coder.plugin.model.PluginBundle;
import io.mosn.coder.plugin.model.PluginBundleRule;
import io.mosn.coder.plugin.model.PluginMetadata;
import io.mosn.coder.plugin.model.PluginStatus;
import io.mosn.coder.registry.SubscribeConsoleAddress;
import io.mosn.coder.upgrade.ProjectMod;
import io.netty.util.Timeout;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class BaseDeploy {

    protected String project;

    protected DeployMode mode;

    protected String oldVersion;

    protected String upgradeVersion;

    protected volatile String address;

    protected AtomicBoolean addrNotified = new AtomicBoolean();

    protected boolean skipCompile;

    protected List<SidecarInjectRuleModel.InjectRule> rules = new ArrayList<>();

    protected List<SidecarInjectRuleModel.UpgradeImage> images = new ArrayList<>();

    protected SidecarInjectRuleModel.InjectRule selectedRule = null;

    protected SidecarInjectRuleModel.UpgradeImage selectedImage = null;

    protected CountDownLatch signal = new CountDownLatch(1);

    protected LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<Runnable>();

    protected Runnable quit = () -> {
    };

    public enum DeployMode {
        Deploy, Upgrade;
    }

    protected void registerCallBack(String project) {


        /**
         * initialize mesh server address
         */
        registerPluginRegistryCallback(project);


        if (this.mode == DeployMode.Upgrade) {

            /**
             * upgrade model, should fetch sidecar rule and sidecar image first
             */

            String address = this.address;
            if (address != null && address.length() > 0) {
                querySidecarRule(project, address, null);
                querySidecarVersion(project, address);
            }

        }

    }

    private void registerPluginRegistryCallback(String project) {
        SubscribeConsoleAddress.DefaultNotify defaultNotify = SubscribeConsoleAddress.getProjectNotify(project);
        if (defaultNotify != null) {
            List<URL> urls = defaultNotify.getUrls();
            if (!urls.isEmpty()) {

                /**
                 * prefer local address for debug
                 */

                String host = NetUtils.getLocalHost();
                String addr = urls.get(0).getAddress();
                for (URL u : urls) {
                    if (u.getHost().equals(host)) {
                        addr = u.getAddress();
                        break;
                    }
                }

                this.address = addr;

                System.out.println("mesh server address:" + this.address);

                if (this.mode == DeployMode.Deploy) {
                    this.signal.countDown();
                }

            } else {

                System.out.println("fetching address, please wait...");

                /**
                 * register registry callback
                 */

                defaultNotify.setCallback(() -> {

                    String address = this.address;

                    boolean alreadyExist = false;
                    if (address != null && address.length() > 0) {
                        for (URL url : defaultNotify.getUrls()) {
                            if (url.getAddress().equals(address)) {
                                alreadyExist = true;
                                break;
                            }
                        }
                    }

                    if (alreadyExist) return;

                    List<URL> notifyUrls = defaultNotify.getUrls();

                    String host = NetUtils.getLocalHost();
                    address = notifyUrls.get(0).getAddress();
                    for (URL u : notifyUrls) {
                        if (u.getHost().equals(host)) {
                            address = u.getAddress();
                            break;
                        }
                    }

                    this.address = address;

                    if (addrNotified.compareAndSet(false, true)) {

                        System.out.println("mesh server address:" + this.address);

                        if (this.mode == DeployMode.Upgrade) {

                            /**
                             * registry callback trigger fetch api first time.
                             */
                            querySidecarRule(project, address, null);

                            /**
                             * query sidecar version
                             */
                            querySidecarVersion(project, address);
                        } else {
                            /**
                             * notify client thread start deploy
                             */
                            this.signal.countDown();
                        }

                    }

                });
            }
        }
    }

    private void querySidecarRule(String project, String address, Long id) {
        RpcClient rpc = RpcClient.getClient(address);

        Protocol.Request request = RequestBuilder.newSidecarRuleRequest(project);

        if (id != null) {
            /**
             * query sidecar rule detail with id
             */
            request.appendHead(Protocol.WrapCommand.RULE_ID, id.toString());
        }

        Protocol.Response response = rpc.request(request);
        if (response.isSuccess()) {
            try {
                PluginBundleRule bundle = JSON.parseObject(new ByteArrayInputStream(response.getContents()), PluginBundleRule.class);

                if (id == null) {

                    /**
                     * query sidecar rule information
                     */
                    if (bundle != null && bundle.getPluginRules() != null
                            && !bundle.getPluginRules().isEmpty()) {

                        this.rules.clear();

                        int selected = -1;
                        int order = 1;

                        System.out.println("Available sidecar inject rules:");
                        for (PluginBundleRule.PluginRule rule : bundle.getPluginRules()) {
                            this.rules.add(new SidecarInjectRuleModel.InjectRule(rule));
                            System.out.println("    " + order + ") " + this.rules.get(order - 1));
                            order++;
                        }

                        System.out.print("Please select sidecar rule number:");

                        Scanner scan = new Scanner(System.in);
                        if (scan.hasNextInt()) {
                            selected = scan.nextInt();
                        }

                        if (selected > 0 && selected < order) {
                            this.selectedRule = this.rules.get(selected - 1);
                            if (this.selectedRule != null && this.selectedRule.getId() != null) {
                                querySidecarRule(project, address, this.selectedRule.getId());
                            }

                        } else {
                            System.out.println("Bad sidecar rule number.");
                        }

                    } else {
                        System.out.println("sidecar rule is empty");
                    }

                    return;

                }

                /**
                 * query sidecar rule detail information
                 */
                if (bundle != null && bundle.getBindingTask() != null) {

                    for (int i = 0; i < this.rules.size(); i++) {
                        PluginBundleRule.PluginRule rule = this.rules.get(i);
                        if (rule.getId() == id) {
                            rule.setPluginTask(bundle.getBindingTask());
                            break;
                        }
                    }

                    /**
                     * render plugin bundle
                     */
                    PluginBundleRule.PluginRule rule = this.selectedRule;
                    if (rule != null && rule.getPluginTask() != null
                            && rule.getPluginTask().getPluginBundle() != null) {
                        PluginBundle bd = new PluginBundle();
                        bd.setBundles(rule.getPluginTask().getPluginBundle());

                        for (PluginBundle.Plugin plugin : rule.getPluginTask().getPluginBundle()) {
                            File file = getPluginFile(project, plugin);
                            plugin.setOwner(file != null && file.exists());
                        }
                    }

                } else {
                    System.out.println("fetch sidecar rule detail failed");
                }

            } catch (IOException e) {

                System.out.println("fetch sidecar rule failed");

                /**
                 * clear text first.
                 */
                // this.pluginInfo.setText("");

                System.out.println("fetch sidecar rule failed:\n");
                System.out.println(e.getMessage());

            }

            return;
        }

        /**
         * rpc request failed.
         */

        displayErrorStack(response);
    }

    protected File getPluginFile(String project, PluginBundle.Plugin plugin) {
        File file = null;
        switch (plugin.getKind()) {
            case PluginBundle.KIND_FILTER: {
                file = new File(project, "configs/stream_filters/" + plugin.getName() + "/metadata.json");
                break;
            }
            case PluginBundle.KIND_TRANSCODER: {
                file = new File(project, "configs/transcoders/" + plugin.getName() + "/metadata.json");
                break;
            }
            case PluginBundle.KIND_TRACE: {
                file = new File(project, "configs/traces/" + plugin.getName() + "/metadata.json");
                break;
            }
            case PluginBundle.KIND_PROTOCOL: {
                file = new File(project, "configs/codecs/" + plugin.getName() + "/metadata.json");
                break;
            }
        }
        return file;
    }

    private void querySidecarVersion(String project, String address) {
        RpcClient rpc = RpcClient.getClient(address);

        Protocol.Request request = RequestBuilder.newSidecarVersionRequest(project);

        Protocol.Response response = rpc.request(request);
        boolean quit = false;
        if (response.isSuccess()) {
            try {
                PluginBundleRule bundle = JSON.parseObject(new ByteArrayInputStream(response.getContents()), PluginBundleRule.class);

                /**
                 * query sidecar version information
                 */
                if (bundle != null && bundle.getPluginRules() != null
                        && !bundle.getPluginRules().isEmpty()) {
                    this.images.clear();

                    int selected = -1;
                    int order = 1;
                    System.out.println("Available upgrade sidecar images:");
                    for (PluginBundleRule.PluginRule rule : bundle.getPluginRules()) {
                        this.images.add(new SidecarInjectRuleModel.UpgradeImage(rule));
                        System.out.println("    " + order + ") " + this.images.get(order - 1));
                        order++;
                    }

                    System.out.print("Please select upgrade sidecar version number:");

                    Scanner scan = new Scanner(System.in);
                    if (scan.hasNextInt()) {
                        selected = scan.nextInt();
                    }

                    if (selected > 0 && selected < order) {
                        this.selectedImage = this.images.get(selected - 1);

                        /**
                         * after select image, render bundle
                         */

                        this.queue.offerLast(() -> {
                            PluginBundle bd = new PluginBundle();

                            bd.setSelectedRule(this.selectedRule);
                            bd.setSelectedImage(this.selectedImage);
                            bd.setDeployVersion(this.upgradeVersion);
                            bd.setMeshServer(this.address);
                            bd.setTerminal(true);

                            bd.setBundles(this.selectedRule.getPluginTask().getPluginBundle());

                            clearConsole();

                            System.out.println(bd.renderAllTasks(false));
                        });

                    } else {
                        System.out.println("Bad sidecar version number.");
                        quit = true;
                    }

                } else {
                    System.out.println("sidecar version is empty");
                    quit = true;
                }
            } catch (IOException e) {

                System.out.println("fetch sidecar version failed");

                /**
                 * clear text first.
                 */

                System.out.println("fetch sidecar version failed:\n");
                System.out.println(e.getMessage());

                quit = true;
            }
        } else {
            quit = true;
            displayErrorStack(response);
        }

        /**
         * notify main thread to running
         */
        this.signal.countDown();
        if (quit) {
            this.notifyQuit();
        }
    }

    public String deployOrUpgradePlugins(PluginBundle deployBundle) {

        String version = this.upgradeVersion;
        if (version == null) {
            return "upgrade plugin version is required";
        }

        version = version.trim();

        String error = StringUtils.checkPluginVersion(version);
        if (error != null) return error;

        try {
            signal.await();
        } catch (Exception ignored) {
            return ignored.getMessage();
        }

        final String updateVersion = version;

        /**
         * must update the version first because the packaging reads the version number locally
         */
        updateLocalPluginVersion();

        if (this.mode == DeployMode.Upgrade) {
            return upgradePlugins(updateVersion, null);
        }

        deployBundle.setMeshServer(this.address);
        deployBundle.setDeployVersion(updateVersion);

        deployPlugins(deployBundle);

        return null;
    }

    protected void updateLocalPluginVersion() {
        /**
         * update plugin version
         */
        File version = new File(project, VersionTemplate.Name);
        if (version.exists()) {
            try (FileOutputStream out = new FileOutputStream(version)) {
                if (this.upgradeVersion != null && this.upgradeVersion.length() > 0) {
                    out.write(this.upgradeVersion.getBytes());
                    out.flush();
                }
            } catch (Exception ignored) {
            }
        }
    }

    protected String upgradePlugins(String updateVersion, PluginBundle localBundle) {
        PluginBundle bundle = new PluginBundle();
        bundle.setTerminal(true);

        /**
         * build current upgrade bundle
         */

        PluginBundleRule.PluginRule rule = this.selectedRule;
        if (rule == null
                || rule.getPluginTask() == null) {
            return "sidecar rule not found";
        }

        if (rule.getPluginTask().getPluginBundle() == null
                || rule.getPluginTask().getPluginBundle().isEmpty()) {
            return "no plugin need to update";
        }

        PluginBundleRule.PluginRule image = this.selectedImage;
        if (image == null || image.getImage() == null) {
            return "please update image from mesh console first";
        }


        /**
         * pretty terminal banner
         */
        bundle.setSelectedRule(new SidecarInjectRuleModel.InjectRule(rule));
        bundle.setSelectedImage(new SidecarInjectRuleModel.UpgradeImage(image));


        bundle.setMeshServer(this.address);
        bundle.setDeployVersion(updateVersion);

        /**
         * build bundle task
         */
        PluginBundleRule.PluginTask task = rule.getPluginTask();
        List<PluginBundle.Plugin> plugins = task.getPluginBundle();

        bundle.setRuleId(rule.getId().toString());
        bundle.setUpgradeId(image.getId().toString());

        bundle.setBundles(plugins);

        bundle.resetTaskStatus(PluginStatus.COMMAND_RUNNING);

        bundle.setOldImage(rule.getImage());
        bundle.setRuleName(rule.getName());

        bundle.setUpgradeImage(image.getImage());

        String errorVersion = checkUpgradeVersion(updateVersion, plugins, localBundle);
        if (errorVersion != null) return errorVersion;

        executePluginUpload(bundle, localBundle);

        return null;
    }

    protected void executePluginUpload(PluginBundle bundle, PluginBundle localBundle) {

        PluginBundleRule.PluginRule image = this.selectedImage;

        /**
         * update plugin dependencies first
         */

        long start = System.currentTimeMillis();
        AtomicReference<Timeout> runningTimeout = new AtomicReference<>();
        Timeout timeout = TimerHolder.getTimer().newTimeout(tt -> {
            if (tt.isCancelled()) {
                return;
            }

            this.queue.offerLast(() -> {


                /**
                 * clear screen
                 */
                clearConsole();

                System.out.println(bundle.renderWithCommand(false));
                System.out.println("\n\nWait until the remote image is pulled for the first time.");
                System.out.println("\nPlease wait copy upgrade dependency... ");
                System.out.println(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)));
                System.out.println("s");
            });

            /**
             * schedule next time
             *
             */
            runningTimeout.set(tt.timer().newTimeout(tt.task(), 1, TimeUnit.SECONDS));

        }, 1, TimeUnit.SECONDS);
        runningTimeout.set(timeout);

        Command command = CommandBuilder.createCopyUpgradeCommand(this.project, image);
        command.callback = status -> {
            runningTimeout.get().cancel();

            if (status != 0) {
                System.out.println("\n\nInit container failed, exit code '" + status + "', maybe image is old, please check goland console message");
                return;
            }

            /**
             * read container id
             */
            String last, containerId = null;
            File container = new File(project, "build/upgrade/container.id");
            if (!container.exists()) {
                System.out.println("\n\nbuild/upgrade/container.id file is missing, please try again.");
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(container))) {
                containerId = last = reader.readLine();
                while (last != null) {
                    last = reader.readLine();
                    if (last != null && last.length() > 0) {
                        containerId = last;
                    }
                }
            } catch (Exception e) {
                System.out.println("\n\nbuild/upgrade/container.id is valid: " + e.getMessage());
            }

            if (containerId != null && containerId.length() > 0) {

                /**
                 * copy sidecar mod file
                 */
                Command cp = new Command();

                File remote = new File(project, "build/upgrade/remote.mod");
                if (remote.exists()) remote.delete();

                ArrayList<String> exec = new ArrayList<>();
                exec.add("docker");
                exec.add("cp");
                exec.add(containerId + ":/home/admin/mosn/base_conf/mosn.gmd");
                exec.add(remote.getAbsolutePath());

                cp.exec = exec;
                cp.title = CustomCommandUtil.CUSTOM_CONSOLE;

                String finalContainerId = containerId;
                cp.callback = s -> {
                    File updateMod = new File(project, "build/upgrade/remote.mod");
                    try {
                        if (s != 0 || !updateMod.exists()) {
                            /**
                             * copy file failed.
                             */

                            System.out.println("\ncopy upgrade dependency failed, please try again.");
                            this.notifyQuit();

                            return;
                        }


                        System.out.println("\ncopy upgrade dependency complete");

                        /**
                         * ready to upgrade
                         */

                        try {
                            bundle.resetTaskStatus(PluginStatus.COMMAND_RUNNING);
                            readyDeployPlugin(bundle);
                        } catch (Exception e) {
                            System.out.println("exception caught: " + e.getMessage());

                            this.notifyQuit();
                        }

                    } finally {
                        removeContainer(exec, finalContainerId);
                    }
                };


                TerminalCompiler.compile(project, cp);

            }
        };
        TerminalCompiler.compile(project, command);
    }

    @Nullable
    protected String checkUpgradeVersion(String updateVersion, List<PluginBundle.Plugin> plugins, PluginBundle deployBundle) {
        /**
         * check upgrade version
         */
        for (PluginBundle.Plugin plugin : plugins) {
            if (plugin.getRevision() != null
                    && plugin.getRevision().equals(updateVersion)) {
                return "The upgraded version '" + updateVersion + "' must be different from the older version '" + plugin.getRevision() + "'";
            }
        }
        return null;
    }

    protected void deployPlugins(PluginBundle deployBundle) {
        try {
            deployBundle.setTerminal(true);
            deployBundle.resetTaskStatus(PluginStatus.COMMAND_RUNNING);
            readyDeployPlugin(deployBundle);
        } catch (Exception e) {
            System.err.println("exception caught: " + e.getMessage());
            this.retryPluginAction();
        }
    }

    protected void readyDeployPlugin(PluginBundle bundle) {

        /**
         *
         * current running in the awt thread.
         *
         * update dependencies first.
         */

        ProjectMod current;

        if (this.mode == DeployMode.Upgrade) {
            File upgradeMod = new File(project, "build/upgrade/remote.mod");
            if (!upgradeMod.exists()) {
                System.err.println("missing upgrade file 'build/upgrade/remote.mod'");
                notifyQuit();
                return;
            }

            File currentMod = new File(project, "go.mod");
            if (!currentMod.exists()) {
                notifyQuit();
                return;
            }

            current = new ProjectMod(project, "go.mod");
            ProjectMod upgrade = new ProjectMod(project, "build/upgrade/remote.mod");
            /**
             * refresh project dependencies
             */
            try {
                current.merge(upgrade);

                /**
                 * flush mod dependency
                 */
                current.flush();
            } catch (Exception e) {
                System.err.println("\nfailed update project go.mod");
                notifyQuit();
                return;
            }
        } else {
            current = new ProjectMod(project, "go.mod");
            current.readFile();
        }

        int ownerPlugins = 0;

        for (PluginBundle.Plugin plugin : bundle.getBundles()) {

            if (plugin.getStatus() == null
                    || PluginStatus.INIT.equals(plugin.getStatus())) {
                plugin.setStatus(PluginStatus.COMMAND_RUNNING);
            }

            plugin.setVersion(this.upgradeVersion.trim());
            plugin.setFullName(plugin.getName() + (
                    plugin.getVersion() == null
                            ? ".zip" : ("-" + plugin.getVersion())) + ".zip");

            File file = null;
            switch (plugin.getKind()) {
                case PluginBundle.KIND_FILTER: {
                    file = new File(project, "configs/stream_filters/" + plugin.getName() + "/metadata.json");
                    break;
                }
                case PluginBundle.KIND_TRANSCODER: {
                    file = new File(project, "configs/transcoders/" + plugin.getName() + "/metadata.json");
                    break;
                }
                case PluginBundle.KIND_TRACE: {
                    file = new File(project, "configs/traces/" + plugin.getName() + "/metadata.json");
                    break;
                }
                case PluginBundle.KIND_PROTOCOL: {
                    file = new File(project, "configs/codecs/" + plugin.getName() + "/metadata.json");
                    break;
                }
            }

            plugin.setOwner(file != null && file.exists());

            if (file != null) {

                if (!plugin.getOwner()) continue;

                ownerPlugins++;
                boolean shouldUpdate = false;

                /**
                 * update dependencies( api and pkg )
                 */
                try (FileInputStream in = new FileInputStream(file)) {
                    byte[] bytes = in.readAllBytes();
                    if (bytes != null) {
                        PluginMetadata metadata = JSON.parseObject(new ByteArrayInputStream(bytes), PluginMetadata.class);
                        plugin.setMetadata(metadata);

                        String mApi = metadata.getDependencies().get("mosn_api");
                        String mPkg = metadata.getDependencies().get("mosn_pkg");

                        /**
                         * api or pkg changed, update metadata.json
                         */
                        if ((mApi != null && current.getApi() != null && !current.getApi().equals(mApi))
                                || (mPkg != null && current.getPkg() != null && !current.getPkg().equals(mPkg))) {
                            metadata.getDependencies().put("mosn_api", current.getApi());
                            metadata.getDependencies().put("mosn_pkg", current.getPkg());

                            shouldUpdate = true;
                        }

                        if (plugin.getDependency() == null) {
                            plugin.setDependency(new HashMap<>());
                            plugin.getDependency().putAll(metadata.getDependencies());
                        }

                        /**
                         * insert protocol port
                         */
                        if (PluginBundle.KIND_PROTOCOL.equals(plugin.getKind())) {

                            if (metadata.getExtension() == null) {
                                metadata.setExtension(new HashMap<>());
                            }

                            File proto = null;
                            if ("X".equals(metadata.getFramework())) {
                                proto = new File(project, "configs/codecs/" + plugin.getName() + "/egress_" + plugin.getName() + ".json");
                            } else if ("HTTP1".equals(metadata.getFramework())) {
                                proto = new File(project, "configs/codecs/" + plugin.getName() + "/egress_" + plugin.getName() + ".json");

                                /**
                                 * failover egress_http format
                                 */
                                if (!proto.exists()) {
                                    proto = new File(project, "configs/codecs/" + plugin.getName() + "/egress_http.json");
                                }
                            }

                            if (proto != null && proto.exists()) {

                                try (FileInputStream fin = new FileInputStream(proto)) {
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
                                proto = new File(project, "configs/codecs/" + plugin.getName() + "/ingress_" + plugin.getName() + ".json");
                            } else if ("HTTP1".equals(metadata.getFramework())) {
                                proto = new File(project, "configs/codecs/" + plugin.getName() + "/ingress_" + plugin.getName() + ".json");

                                if (!proto.exists()) {
                                    proto = new File(project, "configs/codecs/" + plugin.getName() + "/ingress_http.json");
                                }
                            }

                            if (proto != null && proto.exists()) {

                                try (FileInputStream fin = new FileInputStream(proto)) {
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
                } catch (Exception e) {
                    // dump console ?
                    e.printStackTrace();
                    throw new RuntimeException("failed read plugin '" + plugin.getName() + "' metadata", e);
                }

                if (shouldUpdate) {
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        byte[] bytes = JSON.toJSONBytes(plugin.getMetadata());
                        if (bytes != null) {
                            out.write(bytes);
                        }
                    } catch (Exception e) {
                        // dump console ?
                        e.printStackTrace();

                        throw new RuntimeException("failed flush plugin '" + plugin.getName() + "' metadata", e);
                    }
                }
            }
        }

        /**
         * now we prepare create compile or package plugin command
         */
        AtomicInteger waitingCommands = new AtomicInteger();
        for (PluginBundle.Plugin plugin : bundle.getBundles()) {
            if (plugin.getOwner()) {

                /**
                 * create compile or package command
                 */

                if (plugin.getCommands() == null) {
                    plugin.setCommands(new ArrayList<>());

                    String os = System.getProperty("os.name");
                    if (os != null && !os.toLowerCase().contains("mac")) {
                        plugin.getCommands().add(CommandBuilder.createCompileCommand(plugin));

                        waitingCommands.incrementAndGet();
                    }

                    /**
                     * Mac machines compile amd plugin packages automatically, so skip compilation
                     */
                    plugin.getCommands().add(CommandBuilder.createPackageCommand(project, plugin));
                    waitingCommands.incrementAndGet();

                } else {
                    waitingCommands.addAndGet(plugin.getCommands().size());
                }

            }
        }

        if (ownerPlugins > 0) {
            this.queue.offerLast(() -> {
                System.out.println(bundle.renderWithCommand(false));
                System.out.println("\n\nplease wait build or package plugins...");
            });
        }

        AtomicReference<Timeout> runningTimeout = new AtomicReference<>();
        Timeout timeout = TimerHolder.getTimer().newTimeout(tt -> {
            if (tt.isCancelled()) {
                return;
            }

            if (bundle.commandComplete()) {
                tt.cancel();

                return;
            }

            this.queue.offerLast(() -> {

                /**
                 * clear screen
                 */
                clearConsole();

                System.out.println(bundle.renderWithCommand(false));
                System.out.println("\n\nPlease wait build or package plugins...");
            });

            /**
             * schedule next time
             *
             */
            runningTimeout.set(tt.timer().newTimeout(tt.task(), 1, TimeUnit.SECONDS));

        }, 1, TimeUnit.SECONDS);
        runningTimeout.set(timeout);

        /**
         * ready to run command
         */
        List<Command> failedCmd = new CopyOnWriteArrayList<>();
        for (PluginBundle.Plugin plugin : bundle.getBundles()) {
            if (plugin.getOwner()) {
                for (Command command : plugin.getCommands()) {

                    command.resetStatus();

                    if (command.getCallback() == null) {
                        command.callback = status -> {

                            if (status != 0) {
                                plugin.setStatus(PluginStatus.FAIL);

                                failedCmd.add(command);
                            }

                            command.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                            /**
                             * notify update bundle status
                             */

                            this.queue.offerLast(() -> {

                                /**
                                 * clear screen
                                 */
                                clearConsole();

                                System.out.println(bundle.renderWithCommand(false));
                                System.out.println("\n\nPlease wait build or package plugins...");
                            });

                            /**
                             * notify task complete
                             */
                            int remain = waitingCommands.decrementAndGet();
                            if (remain <= 0) {

                                /**
                                 * cancel scheduled render
                                 */
                                runningTimeout.get().cancel();

                                /**
                                 * update bundle render
                                 *
                                 */

                                this.queue.offerLast(() -> {

                                    /**
                                     * clear screen
                                     */
                                    clearConsole();

                                    System.out.println(bundle.renderWithCommand(true));
                                });

                                /**
                                 * task already failed
                                 */
                                if (!bundle.commandComplete()) {
                                    System.out.println("\n\nThe current plugin failed to compile or package. Please try again");

                                    /**
                                     * dump failed task stack:
                                     */
                                    if (!failedCmd.isEmpty()) {
                                        for (Command cmd : failedCmd) {
                                            if (cmd.output != null && !cmd.output.isEmpty()) {
                                                System.out.println(cmd);
                                                for (String line : cmd.output) {
                                                    System.out.println(line);
                                                }
                                            }
                                        }
                                    }

                                    this.notifyQuit();

                                    return;
                                }

                                System.out.println("\n\nPlease wait upload plugins...");

                                TerminalCompiler.submit(() -> {

                                    /**
                                     * upload plugins
                                     */

                                    uploadPlugins(bundle);

                                });


                            }
                        };
                    }
                }

                TerminalCompiler.compile(project, plugin);
            }
        }
    }

    protected void uploadPlugins(PluginBundle bundle) {

        List<PluginBundle.Plugin> filters = new ArrayList<>();
        List<PluginBundle.Plugin> transcoders = new ArrayList<>();
        List<PluginBundle.Plugin> codecs = new ArrayList<>();
        List<PluginBundle.Plugin> traces = new ArrayList<>();
        for (PluginBundle.Plugin plugin : bundle.getBundles()) {

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
                case PluginBundle.KIND_PROTOCOL:
                    codecs.add(plugin);
                    break;
                case PluginBundle.KIND_TRACE:
                    traces.add(plugin);
                    break;
            }
        }

        List<PluginBundle.Plugin> sortedPlugins = new ArrayList<>();
        sortedPlugins.addAll(filters);
        sortedPlugins.addAll(transcoders);
        sortedPlugins.addAll(codecs);
        sortedPlugins.addAll(traces);

        AtomicReference<Timeout> taskTimeout = new AtomicReference<>();
        Timeout timeout = TimerHolder.getTimer().newTimeout(tt -> {
            if (tt.isCancelled()) {
                return;
            }

            if (bundle.bundleComplete()) {
                tt.cancel();

                return;
            }


            queue.offerLast(() -> {

                /**
                 * clear screen
                 */
                clearConsole();

                System.out.println(bundle.renderAllTasks(false));
                System.out.println("\n\nPlease wait upload plugins...");
            });


            /**
             * schedule next time
             *
             */
            taskTimeout.set(tt.timer().newTimeout(tt.task(), 1, TimeUnit.SECONDS));

        }, 1, TimeUnit.SECONDS);
        taskTimeout.set(timeout);

        /**
         * invoke sys_init_task
         * @see InitTaskFactory
         */

        String initTask = bundle.renderInitTask();
        Protocol.Request initTaskRequest = RequestBuilder.newInitTaskRequest(project);
        initTaskRequest.appendHead(Protocol.WrapCommand.RULE_ID, bundle.getRuleId());
        initTaskRequest.appendHead(Protocol.WrapCommand.UPGRADE_ID, bundle.getUpgradeId());
        initTaskRequest.setContents(initTask.getBytes());

        RpcClient rpc = null;

        try {

            if (initTaskRequest.getInstanceId() == null || initTaskRequest.getInstanceId().length() == 0) {
                throw new IllegalArgumentException("instance id not found");
            }

            rpc = RpcClient.getClient(this.address);

            /**
             * register disconnect callback
             */
            rpc.addCloseCallback(() -> taskTimeout.get().cancel());

            /**
             * set running
             */
            bundle.resetTaskStatus(PluginStatus.COMMAND_RUNNING);

            Protocol.Response initTaskResponse = rpc.request(initTaskRequest);
            if (!updateTaskStatus(bundle, initTaskResponse)) taskTimeout.get().cancel();

            if (!initTaskResponse.isSuccess()) {
                bundle.updateTaskStatus(PluginStatus.FAIL);

                /**
                 * cancel task
                 */
                taskTimeout.get().cancel();

                this.queue.offerLast(() -> {

                    /**
                     * clear screen
                     */
                    clearConsole();

                    System.out.println(bundle.renderAllTasks(true));

                    System.out.println("\n");
                    System.out.println("init task failed:\n");

                    displayErrorStack(initTaskResponse);
                });

                this.retryPluginAction();

                return;
            }

            bundle.setTaskId(initTaskResponse.getTaskId());

            /**
             * invoke sys_upload_file
             * @see UploadFileFactory
             */
            String root = project;

            for (PluginBundle.Plugin plugin : sortedPlugins) {

                plugin.setStart(System.currentTimeMillis());

                /**
                 * check file exist first
                 */

                File pluginFile = null;
                String prefix = getZipDir();

                switch (plugin.getKind()) {
                    case PluginBundle.KIND_FILTER:
                        pluginFile = new File(root, prefix + "stream_filters/" + plugin.getFullName());
                        break;
                    case PluginBundle.KIND_TRANSCODER:
                        pluginFile = new File(root, prefix + "transcoders/" + plugin.getFullName());
                        break;
                    case PluginBundle.KIND_PROTOCOL:
                        pluginFile = new File(root, prefix + "codecs/" + plugin.getFullName());
                        break;
                    case PluginBundle.KIND_TRACE:
                        pluginFile = new File(root, prefix + "traces/" + plugin.getFullName());
                        break;
                }

                if (pluginFile == null || !pluginFile.exists()) {
                    plugin.setStatus(PluginStatus.FAIL);
                    taskTimeout.get().cancel();

                    this.queue.offerLast(() -> {

                        /**
                         * clear screen
                         */
                        clearConsole();

                        System.out.println(bundle.renderAllTasks(true));

                        System.out.println("\n");
                        System.out.println("plugin file '" + plugin.getFullName() + "' not found\n");
                    });

                    this.retryPluginAction();

                    return;
                }

                /**
                 * build plugin upload request
                 */
                long fileLength = pluginFile.length();
                Protocol.Request uploadRequest = RequestBuilder.newUploadTaskRequest(project);

                uploadRequest.appendHead(Protocol.WrapCommand.FILE_OFFSET, "0");
                uploadRequest.appendHead(Protocol.WrapCommand.FILE_LENGTH, String.valueOf(fileLength));
                uploadRequest.appendHead(Protocol.WrapCommand.FILE_NAME, plugin.getFullName());
                uploadRequest.appendHead(Protocol.WrapCommand.FILE_VERSION, plugin.getVersion());
                uploadRequest.appendHead(Protocol.WrapCommand.METADATA
                        , URLEncoder.encode(JSON.toJSONString(plugin.getMetadata())
                                , Charset.defaultCharset()));

                try (FileInputStream in = new FileInputStream(pluginFile)) {
                    byte[] pluginBytes = in.readAllBytes();
                    uploadRequest.setContents(pluginBytes);
                }

                Protocol.Response uploadResponse = rpc.request(uploadRequest);

                if (!updateTaskStatus(bundle, uploadResponse)) taskTimeout.get().cancel();
                plugin.setStop(System.currentTimeMillis());

                if (!uploadResponse.isSuccess()) {
                    bundle.updateTaskStatus(PluginStatus.FAIL);
                    taskTimeout.get().cancel();


                    this.queue.offerLast(() -> {

                        /**
                         * clear screen
                         */
                        clearConsole();

                        System.out.println(bundle.renderAllTasks(true));

                        System.out.println("\n");
                        System.out.println("Upload file failed:\n");

                        displayErrorStack(uploadResponse);
                    });

                    this.retryPluginAction();
                    return;
                }
            }

            /**
             * sys_commit_task
             *
             * @see CommitTaskFactory
             */

            Protocol.Request commitRequest = RequestBuilder.newCommitTaskRequest(project);
            Protocol.Response commitResponse = rpc.request(commitRequest);

            updateTaskStatus(bundle, commitResponse);
            taskTimeout.get().cancel();

            if (!commitResponse.isSuccess()) {
                bundle.updateTaskStatus(PluginStatus.FAIL);
                taskTimeout.get().cancel();


                this.queue.offerLast(() -> {

                    /**
                     * clear screen
                     */
                    clearConsole();

                    System.out.println(bundle.renderAllTasks(true));

                    System.out.println("\n");
                    System.out.println("commit task failed:\n");

                    displayErrorStack(commitResponse);
                });

                this.retryPluginAction();
                return;
            }


            /**
             * clear screen
             */
            clearConsole();

            System.out.println(bundle.renderAllTasks(true));

        } catch (Exception e) {

            this.queue.offerLast(() -> {

                /**
                 * clear screen
                 */
                clearConsole();

                System.out.println(bundle.renderAllTasks(true));

                System.out.println("\n");
                System.out.println("deploy failed:\n");

                System.out.println(e.getMessage());
            });

        } finally {
            taskTimeout.get().cancel();
            if (rpc != null) {
                rpc.destroy();
            }

            this.retryPluginAction();
        }

    }

    protected String getZipDir() {
        return "build/target/";
    }

    private boolean updateTaskStatus(PluginBundle bundle, Protocol.Response response) {
        if (response.getContents() != null) {
            try {
                List<PluginBundle.Plugin> remotePlugins = JSON.parseArray(new String(response.getContents()), PluginBundle.Plugin.class);
                /**
                 * update remote bundle status
                 */
                for (PluginBundle.Plugin plugin : bundle.getBundles()) {
                    for (PluginBundle.Plugin remote : remotePlugins) {
                        if (plugin.getKind().equals(remote.getKind())
                                && plugin.getName().equals(remote.getName())) {
                            plugin.setStatus(remote.getStatus());
                        }
                    }
                }
            } catch (Exception e) {

                System.out.println("\nerror:\n");
                System.out.println(e.getMessage());

                return false;
            }

        }

        return true;
    }

    private void displayErrorStack(Protocol.Response response) {
        /**
         * rpc request failed.
         */

        String desc = response.getHeaders().get(Protocol.WrapCommand.DESCRIPTION);
        String stack = response.getHeaders().get(Protocol.WrapCommand.STACK);

        if (desc != null) {

            System.out.println("\nmessage: " + desc);

            if (stack != null) {
                System.out.println("\nstack: ");
                System.out.println("\n" + stack);
            }

        }
    }

    public void retryPluginAction() {
        /**
         * quit main thread.
         */
        queue.offerLast(quit);
    }

    protected void notifyQuit() {
        this.retryPluginAction();
    }

    private void removeContainer(ArrayList<String> exec, String finalContainerId) {
        /**
         * remove container id first
         */
        Command rm = new Command();

        ArrayList<String> run = new ArrayList<>();
        run.add("docker");
        run.add("container");
        run.add("rm");
        run.add(finalContainerId);
        rm.exec = exec;
        rm.title = CustomCommandUtil.CUSTOM_CONSOLE;
        TerminalCompiler.compile(project, rm);
    }

    private static void clearConsole() {
        System.out.println("\033[H\033[2J");
        System.out.flush();
    }

    protected void waitQuit() {
        /**
         * main thread running task
         */
        try {
            Runnable task;
            while (true) {
                task = queue.takeFirst();

                if (task == quit) {
                    break;
                }

                task.run();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
