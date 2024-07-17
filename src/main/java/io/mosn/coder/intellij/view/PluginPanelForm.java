package io.mosn.coder.intellij.view;

import com.alibaba.fastjson.JSON;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import io.mosn.coder.common.NetUtils;
import io.mosn.coder.common.StringUtils;
import io.mosn.coder.common.TimerHolder;
import io.mosn.coder.common.URL;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.CommandBuilder;
import io.mosn.coder.compiler.PluginCompiler;
import io.mosn.coder.console.CustomCommandUtil;
import io.mosn.coder.intellij.template.VersionTemplate;
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PluginPanelForm {

    private volatile int state = STATE_NOT_READY;

    public static int STATE_NOT_READY = 0;

    public static int STATE_READY = STATE_NOT_READY + 1;

    public static int STATE_RUNNING = STATE_READY + 1;

    public static int STATE_DONE = STATE_RUNNING + 1;

    public static int STATE_FAILED = STATE_DONE + 1;

    private JComboBox serverAddress;
    private JLabel serverAddressTip;
    private JComboBox sidecarInjectRule;
    private JLabel sidecarInjectRuleTip;
    private JComboBox sidecarUpgradeImage;
    private JLabel serverAddressLabel;
    private JLabel sidecarInjectRuleLabel;
    private JLabel sidecarUpgradeImageLabel;
    private JTextArea pluginInfo;
    private JPanel rootContentPanel;
    private JLabel pluginInfoLabel;
    private JTextField pluginVersionText;
    private JLabel pluginVersionLabel;

    private DeployMode mode;

    private Project project;

    private Application application;

    private PluginAction action;

    private volatile String address;

    private String version;

    protected AtomicBoolean addrNotified = new AtomicBoolean();

    public PluginPanelForm(Project project) {
        this(project, DeployMode.Upgrade);
    }


    public PluginPanelForm(Project project, DeployMode mode) {
        this.mode = mode;

        this.project = project;

        $$$setupUI$$$();
        updateSidecarComponent(this.mode == DeployMode.Upgrade);
        updateSidecarImage(this.mode == DeployMode.Upgrade);

        readLocalPluginVersion();

        registerCallBack(project.getBasePath());

    }

    /**
     * return null if success else return error message
     */
    public String deployOrUpgradePlugins(PluginBundle deployBundle) {

        state = STATE_RUNNING;

        String version = this.pluginVersionText.getText();
        if (version == null) {
            return "upgrade plugin version is required";
        }

        version = version.trim();

        String error = StringUtils.checkPluginVersion(version);
        if (error != null) return error;

        final String updateVersion = version;

        /**
         * must update the version first because the packaging reads the version number locally
         */
        updateLocalPluginVersion();

        if (this.mode == DeployMode.Upgrade) {
            return upgradePlugins(updateVersion);
        }

        deployPlugins(deployBundle);

        return null;
    }

    @Nullable
    private String upgradePlugins(String updateVersion) {
        PluginBundle bundle = new PluginBundle();

        /**
         * build current upgrade bundle
         */

        PluginBundleRule.PluginRule rule = (PluginBundleRule.PluginRule) this.sidecarInjectRule.getSelectedItem();
        if (rule == null
                || rule.getPluginTask() == null) {
            return "sidecar rule not found";
        }

        if (rule.getPluginTask().getPluginBundle() == null
                || rule.getPluginTask().getPluginBundle().isEmpty()) {
            return "no plugin need to update";
        }

        PluginBundleRule.PluginRule image = (PluginBundleRule.PluginRule) this.sidecarUpgradeImage.getSelectedItem();
        if (image == null || image.getImage() == null) {
            return "please update image from mesh console first";
        }

        /**
         * build bundle task
         */

        PluginBundleRule.PluginTask task = rule.getPluginTask();
        List<PluginBundle.Plugin> plugins = task.getPluginBundle();

        bundle.setRuleId(rule.getId().toString());
        bundle.setUpgradeId(image.getId().toString());

        bundle.setBundles(plugins);

        /**
         * check upgrade version
         */
        for (PluginBundle.Plugin plugin : plugins) {
            if (plugin.getRevision() != null
                    && plugin.getRevision().equals(updateVersion)) {
                return "The upgraded version '" + updateVersion + "' must be different from the older version '" + plugin.getRevision() + "'";
            }
        }

        // disable action first
        this.disablePluginAction();

        bundle.resetTaskStatus(PluginStatus.COMMAND_RUNNING);

        bundle.setOldImage(rule.getImage());
        bundle.setRuleName(rule.getName());

        bundle.setUpgradeImage(image.getImage());
        /**
         * update plugin dependencies first
         */

        long start = System.currentTimeMillis();
        AtomicReference<Timeout> runningTimeout = new AtomicReference<>();
        Timeout timeout = TimerHolder.getTimer().newTimeout(tt -> {
            if (tt.isCancelled()) {
                return;
            }

            this.pluginInfo.setText(bundle.renderWithCommand(false));
            this.pluginInfo.append("\n\nWait until the remote image is pulled for the first time.");
            this.pluginInfo.append("\nPlease wait copy upgrade dependency... ");
            this.pluginInfo.append(String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start)));
            this.pluginInfo.append("s");

            /**
             * schedule next time
             *
             */
            runningTimeout.set(tt.timer().newTimeout(tt.task(), 1, TimeUnit.SECONDS));

        }, 1, TimeUnit.SECONDS);
        runningTimeout.set(timeout);

        Command command = CommandBuilder.createCopyUpgradeCommand(project.getBasePath(), image);
        command.callback = status -> {
            runningTimeout.get().cancel();

            if (status != 0) {
                application.invokeLater(() -> {
                    this.pluginInfo.append("\n\nInit container failed, exit code '" + status + "', maybe image is old, please check goland console message");
                });

                return;
            }

            /**
             * read container id
             */
            String last, containerId = null;
            File container = new File(project.getBasePath(), "build/upgrade/container.id");
            if (!container.exists()) {
                application.invokeLater(() -> {
                    this.pluginInfo.append("\n\nbuild/upgrade/container.id file is missing, please try again.");
                });
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
                application.invokeLater(() -> {
                    this.pluginInfo.append("\n\nbuild/upgrade/container.id is valid: " + e.getMessage());
                });
            }

            if (containerId != null && containerId.length() > 0) {

                /**
                 * copy sidecar mod file
                 */
                Command cp = new Command();

                File remote = new File(project.getBasePath(), "build/upgrade/remote.mod");
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
                    File updateMod = new File(project.getBasePath(), "build/upgrade/remote.mod");
                    try {
                        if (s != 0 || !updateMod.exists()) {
                            /**
                             * copy file failed.
                             */

                            application.invokeLater(() -> {
                                this.pluginInfo.append("\ncopy upgrade dependency failed, please try again.");
                                this.retryPluginAction();
                            });

                            return;
                        }

                        application.invokeLater(() -> {
                            this.pluginInfo.append("\ncopy upgrade dependency complete");

                            /**
                             * ready to upgrade
                             */

                            try {
                                bundle.resetTaskStatus(PluginStatus.COMMAND_RUNNING);
                                readyDeployPlugin(bundle);
                            } catch (Exception e) {
                                this.pluginInfo.append("exception caught: " + e.getMessage());

                                this.retryPluginAction();
                            }
                        });
                    } finally {
                        removeContainer(exec, finalContainerId);
                    }
                };

                application.invokeLater(() -> {
                    PluginCompiler.compile(project, cp);
                });
            }
        };
        PluginCompiler.compile(project, command);

        return null;
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
        application.invokeLater(() -> {
            PluginCompiler.compile(project, rm);
        });
    }

    private void deployPlugins(PluginBundle deployBundle) {
        try {
            deployBundle.resetTaskStatus(PluginStatus.COMMAND_RUNNING);
            readyDeployPlugin(deployBundle);
        } catch (Exception e) {
            this.pluginInfo.append("exception caught: " + e.getMessage());

            this.retryPluginAction();
        }
    }

    private void registerCallBack(String project) {

        this.sidecarInjectRule.setEditable(false);
        this.sidecarInjectRule.setModel(new SidecarInjectRuleModel());

        this.sidecarUpgradeImage.setEditable(false);
        this.sidecarUpgradeImage.setModel(new SidecarInjectRuleModel());

        Application application = ApplicationManager.getApplication();

        this.application = application;

        /**
         * initialize mesh server address
         */
        registerPluginRegistryCallback(project, application);


        if (this.mode == DeployMode.Upgrade) {

            /**
             * upgrade model, should fetch sidecar rule and sidecar image first
             */

            String address = (String) this.serverAddress.getSelectedItem();
            if (address != null && address.length() > 0) {
                querySidecarRule(project, application, address, null);
                querySidecarVersion(project, application, address);
            }

        }

    }

    private void registerPluginRegistryCallback(String project, Application application) {
        SubscribeConsoleAddress.DefaultNotify defaultNotify = SubscribeConsoleAddress.getProjectNotify(project);
        if (defaultNotify != null) {
            List<URL> urls = defaultNotify.getUrls();
            if (!urls.isEmpty()) {
                // clear tips first
                this.serverAddressTip.setText("");

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

                this.serverAddress.addItem(addr);
                this.address = addr;
            } else {

                this.serverAddressTip.setText("fetching address, please wait...");

                /**
                 * register registry callback
                 */

                defaultNotify.setCallback(() -> application.invokeLater(() -> {

                    String address = (String) this.serverAddress.getSelectedItem();

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

                    this.serverAddress.removeAll();

                    List<URL> notifyUrls = defaultNotify.getUrls();

                    String host = NetUtils.getLocalHost();
                    address = notifyUrls.get(0).getAddress();
                    for (URL u : notifyUrls) {
                        if (u.getHost().equals(host)) {
                            address = u.getAddress();
                            break;
                        }
                    }

                    this.serverAddress.addItem(address);

                    this.address = address;

                    this.serverAddressTip.setText("");
                    this.serverAddress.setSelectedIndex(0);

                    if (addrNotified.compareAndSet(false, true)) {

                        if (this.mode == DeployMode.Upgrade) {

                            /**
                             * registry callback trigger fetch api first time.
                             */
                            querySidecarRule(project, application, address, null);

                            /**
                             * query sidecar version
                             */
                            querySidecarVersion(project, application, address);
                        }
                        
                    }


                }));
            }
        }
    }


    private void querySidecarRule(String project, Application application, String address, Long id) {
        RpcClient rpc = RpcClient.getClient(address);

        Protocol.Request request = RequestBuilder.newSidecarRuleRequest(project);

        if (id != null) {
            /**
             * query sidecar rule detail with id
             */
            request.appendHead(Protocol.WrapCommand.RULE_ID, id.toString());
        }

        rpc.AsyncRequest(request).whenComplete((future) -> {
            Protocol.Response response = future.getResult();
            if (response.isSuccess()) {
                try {
                    PluginBundleRule bundle = JSON.parseObject(new ByteArrayInputStream(response.getContents()), PluginBundleRule.class);

                    if (id == null) {

                        /**
                         * query sidecar rule information
                         */
                        if (bundle != null && bundle.getPluginRules() != null
                                && !bundle.getPluginRules().isEmpty()) {
                            application.invokeLater(() -> {

                                ItemListener listener = e -> {
                                    if (e.getStateChange() == ItemEvent.SELECTED) {
                                        PluginBundleRule.PluginRule rule = (PluginBundleRule.PluginRule) e.getItem();
                                        if (rule != null) {
                                            querySidecarRule(project, application, address, rule.getId());
                                        }
                                    }
                                };

                                this.sidecarInjectRuleTip.setText("");

                                this.sidecarInjectRule.removeItemListener(listener);
                                this.sidecarInjectRule.removeAllItems();

                                for (PluginBundleRule.PluginRule rule : bundle.getPluginRules()) {
                                    this.sidecarInjectRule.addItem(new SidecarInjectRuleModel.InjectRule(rule));
                                }

                                this.sidecarInjectRule.addItemListener(listener);
                                this.sidecarInjectRule.setSelectedIndex(0);

                                PluginBundleRule.PluginRule rule = (PluginBundleRule.PluginRule) this.sidecarInjectRule.getSelectedItem();
                                if (rule != null && rule.getId() != null) {
                                    querySidecarRule(project, application, address, rule.getId());
                                }

                            });
                        } else {
                            application.invokeLater(() -> {
                                this.sidecarInjectRuleTip.setText("sidecar rule is empty");
                            });
                        }

                        return;

                    }

                    /**
                     * query sidecar rule detail information
                     */
                    if (bundle != null && bundle.getBindingTask() != null) {
                        application.invokeLater(() -> {
                            this.sidecarInjectRuleTip.setText("");

                            for (int i = 0; i < this.sidecarInjectRule.getItemCount(); i++) {
                                PluginBundleRule.PluginRule rule = (PluginBundleRule.PluginRule) this.sidecarInjectRule.getItemAt(i);

                                if (rule.getId() == id) {
                                    rule.setPluginTask(bundle.getBindingTask());
                                    break;
                                }
                            }

                            /**
                             * render plugin bundle
                             */
                            PluginBundleRule.PluginRule rule = (PluginBundleRule.PluginRule) this.sidecarInjectRule.getSelectedItem();
                            if (rule != null && rule.getPluginTask() != null
                                    && rule.getPluginTask().getPluginBundle() != null) {
                                PluginBundle bd = new PluginBundle();
                                bd.setBundles(rule.getPluginTask().getPluginBundle());

                                File file = null;
                                for (PluginBundle.Plugin plugin : rule.getPluginTask().getPluginBundle()) {
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

                                    if (file != null) {
                                        plugin.setOwner(file.exists());
                                    }
                                }

                                this.pluginInfo.setText(bd.renderAllTasks(false));
                            }

                        });
                    } else {
                        application.invokeLater(() -> {
                            this.sidecarInjectRuleTip.setText("fetch sidecar rule detail failed");
                        });
                    }

                } catch (IOException e) {
                    application.invokeLater(() -> {
                        this.sidecarInjectRuleTip.setText("fetch sidecar rule failed");

                        /**
                         * clear text first.
                         */
                        this.pluginInfo.setText("");

                        this.pluginInfo.append("fetch sidecar rule failed:\n");
                        this.pluginInfo.append(e.getMessage());
                    });
                }

                return;
            }

            /**
             * rpc request failed.
             */

            displayErrorStack(application, response);
        });
    }

    private void querySidecarVersion(String project, Application application, String address) {
        RpcClient rpc = RpcClient.getClient(address);

        Protocol.Request request = RequestBuilder.newSidecarVersionRequest(project);


        rpc.AsyncRequest(request).whenComplete((future) -> {
            Protocol.Response response = future.getResult();
            if (response.isSuccess()) {
                try {
                    PluginBundleRule bundle = JSON.parseObject(new ByteArrayInputStream(response.getContents()), PluginBundleRule.class);

                    /**
                     * query sidecar version information
                     */
                    if (bundle != null && bundle.getPluginRules() != null
                            && !bundle.getPluginRules().isEmpty()) {
                        application.invokeLater(() -> {
                            this.sidecarUpgradeImage.removeAllItems();
                            for (PluginBundleRule.PluginRule rule : bundle.getPluginRules()) {
                                this.sidecarUpgradeImage.addItem(new SidecarInjectRuleModel.UpgradeImage(rule));
                            }
                        });
                    } else {
                        application.invokeLater(() -> {
                            this.sidecarInjectRuleTip.setText("sidecar version is empty");
                        });
                    }
                } catch (IOException e) {
                    application.invokeLater(() -> {
                        this.sidecarInjectRuleTip.setText("fetch sidecar version failed");

                        /**
                         * clear text first.
                         */
                        this.pluginInfo.setText("");

                        this.pluginInfo.append("fetch sidecar version failed:\n");
                        this.pluginInfo.append(e.getMessage());
                    });
                }

                return;
            }

            displayErrorStack(application, response);
        });
    }

    private void displayErrorStack(Application application, Protocol.Response response) {
        /**
         * rpc request failed.
         */

        String desc = response.getHeaders().get(Protocol.WrapCommand.DESCRIPTION);
        String stack = response.getHeaders().get(Protocol.WrapCommand.STACK);

        if (desc != null) {
            application.invokeLater(() -> {
                this.pluginInfo.append("\nmessage: " + desc);

                if (stack != null) {
                    this.pluginInfo.append("\nstack: ");
                    this.pluginInfo.append("\n" + stack);
                }
            });
        }
    }

    private void readyDeployPlugin(PluginBundle bundle) {

        /**
         *
         * current running in the awt thread.
         *
         * update dependencies first.
         */

        ProjectMod current;

        if (this.mode == DeployMode.Upgrade) {
            File upgradeMod = new File(project.getBasePath(), "build/upgrade/remote.mod");
            if (!upgradeMod.exists()) {
                this.pluginInfo.append("missing upgrade file 'build/upgrade/remote.mod'");
                return;
            }

            File currentMod = new File(project.getBasePath(), "go.mod");
            if (!currentMod.exists()) {
                return;
            }

            current = new ProjectMod(project.getBasePath(), "go.mod");
            ProjectMod upgrade = new ProjectMod(project.getBasePath(), "build/upgrade/remote.mod");
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
                this.pluginInfo.append("\nfailed update project go.mod");
                return;
            }
        } else {
            current = new ProjectMod(project.getBasePath(), "go.mod");
            current.readFile();
        }

        int ownerPlugins = 0;

        for (PluginBundle.Plugin plugin : bundle.getBundles()) {

            if (plugin.getStatus() == null
                    || PluginStatus.INIT.equals(plugin.getStatus())) {
                plugin.setStatus(PluginStatus.COMMAND_RUNNING);
            }

            plugin.setVersion(this.pluginVersionText.getText().trim());
            plugin.setFullName(plugin.getName() + (
                    plugin.getVersion() == null
                            ? "" : ("-" + plugin.getVersion())) + ".zip");

            File file = null;
            switch (plugin.getKind()) {
                case PluginBundle.KIND_FILTER: {
                    file = new File(project.getBasePath(), "configs/stream_filters/" + plugin.getName() + "/metadata.json");
                    break;
                }
                case PluginBundle.KIND_TRANSCODER: {
                    file = new File(project.getBasePath(), "configs/transcoders/" + plugin.getName() + "/metadata.json");
                    break;
                }
                case PluginBundle.KIND_TRACE: {
                    file = new File(project.getBasePath(), "configs/traces/" + plugin.getName() + "/metadata.json");
                    break;
                }
                case PluginBundle.KIND_PROTOCOL: {
                    file = new File(project.getBasePath(), "configs/codecs/" + plugin.getName() + "/metadata.json");
                    break;
                }
            }

            if (file != null) {

                plugin.setOwner(file.exists());

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
                                proto = new File(project.getBasePath(), "configs/codecs/" + plugin.getName() + "/egress_" + plugin.getName() + ".json");
                            } else if ("HTTP1".equals(metadata.getFramework())) {
                                proto = new File(project.getBasePath(), "configs/codecs/" + plugin.getName() + "/egress_" + plugin.getName() + ".json");
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
                                proto = new File(project.getBasePath(), "configs/codecs/" + plugin.getName() + "/ingress_" + plugin.getName() + ".json");
                            } else if ("HTTP1".equals(metadata.getFramework())) {
                                proto = new File(project.getBasePath(), "configs/codecs/" + plugin.getName() + "/ingress_" + plugin.getName() + ".json");
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
                    plugin.getCommands().add(CommandBuilder.createPackageCommand(project.getBasePath(), plugin));
                    waitingCommands.incrementAndGet();

                } else {
                    waitingCommands.addAndGet(plugin.getCommands().size());
                }

            }
        }

        if (ownerPlugins > 0) {
            this.disablePluginAction();
            this.pluginInfo.setText(bundle.renderWithCommand(false));
            this.pluginInfo.append("\n\nplease wait build or package plugins...");
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

            this.pluginInfo.setText(bundle.renderWithCommand(false));
            this.pluginInfo.append("\n\nPlease wait build or package plugins...");

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
        for (PluginBundle.Plugin plugin : bundle.getBundles()) {
            if (plugin.getOwner()) {
                for (Command command : plugin.getCommands()) {

                    command.resetStatus();

                    if (command.getCallback() == null) {
                        command.callback = status -> {

                            if (status != 0) {
                                plugin.setStatus(PluginStatus.FAIL);
                            }

                            command.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                            /**
                             * notify update bundle status
                             */

                            application.invokeLater(() -> {
                                this.pluginInfo.setText(bundle.renderWithCommand(false));
                                this.pluginInfo.append("\n\nplease wait build or package plugins...");
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

                                application.invokeLater(() -> {
                                    this.pluginInfo.setText(bundle.renderWithCommand(true));

                                    /**
                                     * task already failed
                                     */
                                    if (!bundle.commandComplete()) {
                                        this.pluginInfo.append("\n\nThe current plugin failed to compile or package. Please try again");
                                        this.retryPluginAction();

                                        return;
                                    }

                                    this.pluginInfo.append("\n\nPlease wait upload plugins...");

                                    PluginCompiler.submit(() -> {

                                        /**
                                         * upload plugins
                                         */

                                        uploadPlugins(bundle);

                                    });

                                });

                            }
                        };
                    }
                }

                PluginCompiler.compile(project, plugin);
            }
        }
    }

    private void uploadPlugins(PluginBundle bundle) {

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
                case PluginBundle.KIND_TRACE:
                    traces.add(plugin);
                    break;
                case PluginBundle.KIND_PROTOCOL:
                    codecs.add(plugin);
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

            application.invokeLater(() -> {
                pluginInfo.setText(bundle.renderAllTasks(false));
                this.pluginInfo.append("\n\nPlease wait upload plugins...");
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
        Protocol.Request initTaskRequest = RequestBuilder.newInitTaskRequest(project.getBasePath());
        initTaskRequest.appendHead(Protocol.WrapCommand.RULE_ID, bundle.getRuleId());
        initTaskRequest.appendHead(Protocol.WrapCommand.UPGRADE_ID, bundle.getUpgradeId());
        initTaskRequest.setContents(initTask.getBytes());

        RpcClient rpc = null;

        try {

            rpc = RpcClient.getClient(this.address);

            /**
             * register disconnect callback
             */
            rpc.addCloseCallback(() -> application.invokeLater(() -> {
                taskTimeout.get().cancel();
                //pluginInfo.setText(bundle.renderAllTasks(true));
            }));

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

                application.invokeLater(() -> {

                    this.retryPluginAction();

                    pluginInfo.setText(bundle.renderAllTasks(true));

                    pluginInfo.append("\n");
                    pluginInfo.append("init task failed:\n");

                    displayErrorStack(application, initTaskResponse);

                });

                return;
            }

            bundle.setTaskId(initTaskResponse.getTaskId());

            /**
             * invoke sys_upload_file
             * @see UploadFileFactory
             */
            String root = project.getBasePath();

            for (PluginBundle.Plugin plugin : sortedPlugins) {

                plugin.setStart(System.currentTimeMillis());

                /**
                 * check file exist first
                 */

                File pluginFile = null;
                String prefix = "build/target/";

                switch (plugin.getKind()) {
                    case PluginBundle.KIND_FILTER:
                        pluginFile = new File(root, prefix + "stream_filters/" + plugin.getFullName());
                        break;
                    case PluginBundle.KIND_TRANSCODER:
                        pluginFile = new File(root, prefix + "transcoders/" + plugin.getFullName());
                        break;
                    case PluginBundle.KIND_TRACE:
                        pluginFile = new File(root, prefix + "traces/" + plugin.getFullName());
                        break;
                    case PluginBundle.KIND_PROTOCOL:
                        pluginFile = new File(root, prefix + "codecs/" + plugin.getFullName());
                        break;
                }

                if (pluginFile == null || !pluginFile.exists()) {
                    plugin.setStatus(PluginStatus.FAIL);
                    taskTimeout.get().cancel();
                    application.invokeLater(() -> {
                        this.retryPluginAction();
                        pluginInfo.setText(bundle.renderAllTasks(true));

                        pluginInfo.append("\n");
                        pluginInfo.append("plugin file '" + plugin.getFullName() + "' not found\n");

                    });

                    return;
                }

                /**
                 * build plugin upload request
                 */
                long fileLength = pluginFile.length();
                Protocol.Request uploadRequest = RequestBuilder.newUploadTaskRequest(project.getBasePath());

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
                    application.invokeLater(() -> {
                        this.retryPluginAction();

                        pluginInfo.setText(bundle.renderAllTasks(true));

                        pluginInfo.append("\n");
                        pluginInfo.append("Upload file failed:\n");

                        displayErrorStack(application, uploadResponse);

                    });

                    return;
                }
            }

            /**
             * sys_commit_task
             *
             * @see CommitTaskFactory
             */

            Protocol.Request commitRequest = RequestBuilder.newCommitTaskRequest(project.getBasePath());
            Protocol.Response commitResponse = rpc.request(commitRequest);

            updateTaskStatus(bundle, commitResponse);
            taskTimeout.get().cancel();

            if (!commitResponse.isSuccess()) {
                bundle.updateTaskStatus(PluginStatus.FAIL);
                taskTimeout.get().cancel();
                application.invokeLater(() -> {
                    this.retryPluginAction();

                    pluginInfo.setText(bundle.renderAllTasks(true));

                    pluginInfo.append("\n");
                    pluginInfo.append("commit task failed:\n");

                    displayErrorStack(application, commitResponse);

                });

                return;
            }

            application.invokeLater(() -> {
                pluginInfo.setText(bundle.renderAllTasks(true));
            });
        } catch (Exception e) {
            application.invokeLater(() -> {
                this.retryPluginAction();

                pluginInfo.setText(bundle.renderAllTasks(true));

                pluginInfo.append("\n");
                pluginInfo.append("deploy failed:\n");

                pluginInfo.append(e.getMessage());
            });
        } finally {
            taskTimeout.get().cancel();
            if (rpc != null) {
                rpc.destroy();
            }
        }

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
                application.invokeLater(() -> {
                    this.pluginInfo.append("\nerror:\n");
                    this.pluginInfo.append(e.getMessage());
                });

                return false;
            }

        }

        return true;
    }

    private void updateSidecarImage(boolean enable) {
        this.sidecarUpgradeImageLabel.setVisible(enable);
        this.sidecarUpgradeImage.setVisible(enable);
    }

    private void updateSidecarComponent(boolean enable) {
        this.sidecarInjectRuleLabel.setVisible(enable);
        this.sidecarInjectRule.setVisible(enable);
        this.sidecarInjectRuleTip.setVisible(enable);
    }

    public void destroy() {

        SubscribeConsoleAddress.DefaultNotify defaultNotify = SubscribeConsoleAddress.getProjectNotify(this.project.getBasePath());
        if (defaultNotify != null) {

            // clear server address
            this.serverAddress.removeAll();

            defaultNotify.setCallback(null);
        }

        if (this.mode == DeployMode.Upgrade) {

            /**
             * unregister event
             */
            ItemListener[] listeners = this.sidecarInjectRule.getItemListeners();
            if (listeners != null) {
                for (ItemListener listener : listeners) {
                    this.sidecarInjectRule.removeItemListener(listener);
                }
            }

            listeners = this.sidecarUpgradeImage.getItemListeners();
            if (listeners != null) {
                for (ItemListener listener : listeners) {
                    this.sidecarUpgradeImage.removeItemListener(listener);
                }
            }
        }
    }

    private void readLocalPluginVersion() {
        /**
         * update plugin version
         */
        File version = new File(this.project.getBasePath(), VersionTemplate.Name);
        if (version.exists()) {
            try (FileInputStream in = new FileInputStream(version)) {
                byte[] bytes = in.readAllBytes();
                if (bytes != null) {
                    this.pluginVersionText.setText(new String(bytes).trim());
                    this.version = this.pluginVersionText.getText();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void updateLocalPluginVersion() {
        /**
         * update plugin version
         */
        File version = new File(this.project.getBasePath(), VersionTemplate.Name);
        if (version.exists()) {
            try (FileOutputStream out = new FileOutputStream(version)) {
                this.version = this.pluginVersionText.getText();
                if (this.version != null && this.version.length() > 0) {
                    out.write(this.version.getBytes());
                    out.flush();
                }
            } catch (Exception ignored) {
            }
        }
    }

    public void renderBundle(PluginBundle bundle) {
        if (bundle != null) {
            pluginInfo.setText(bundle.renderAllTasks(false));
        }
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     *
     * @noinspection ALL
     */
    private void $$$setupUI$$$() {
        rootContentPanel = new JPanel();
        rootContentPanel.setLayout(new GridLayoutManager(6, 4, new Insets(0, 0, 0, 0), -1, -1));
        serverAddressLabel = new JLabel();
        serverAddressLabel.setText("Mesh Server Address:");
        rootContentPanel.add(serverAddressLabel, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        serverAddress = new JComboBox();
        rootContentPanel.add(serverAddress, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer1 = new Spacer();
        rootContentPanel.add(spacer1, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        serverAddressTip = new JLabel();
        serverAddressTip.setText("");
        rootContentPanel.add(serverAddressTip, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer2 = new Spacer();
        rootContentPanel.add(spacer2, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        sidecarInjectRuleLabel = new JLabel();
        sidecarInjectRuleLabel.setText("Sdiecar Inject Rule:");
        rootContentPanel.add(sidecarInjectRuleLabel, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sidecarInjectRule = new JComboBox();
        sidecarInjectRule.setEditable(false);
        rootContentPanel.add(sidecarInjectRule, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sidecarInjectRuleTip = new JLabel();
        sidecarInjectRuleTip.setText("");
        rootContentPanel.add(sidecarInjectRuleTip, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sidecarUpgradeImageLabel = new JLabel();
        sidecarUpgradeImageLabel.setText("Sidecar Upgrade Image：");
        rootContentPanel.add(sidecarUpgradeImageLabel, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        sidecarUpgradeImage = new JComboBox();
        sidecarUpgradeImage.setEditable(false);
        rootContentPanel.add(sidecarUpgradeImage, new GridConstraints(3, 1, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 5, new Insets(0, 0, 0, 0), -1, -1));
        rootContentPanel.add(panel1, new GridConstraints(4, 0, 1, 4, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null, 0, false));
        pluginInfo = new JTextArea();
        pluginInfo.setEditable(false);
        panel1.add(pluginInfo, new GridConstraints(1, 0, 1, 5, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(600, 400), new Dimension(150, 50), null, 0, false));
        pluginInfoLabel = new JLabel();
        pluginInfoLabel.setText("Plugin Information:");
        panel1.add(pluginInfoLabel, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        final Spacer spacer3 = new Spacer();
        panel1.add(spacer3, new GridConstraints(0, 2, 1, 3, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null, 0, false));
        pluginVersionLabel = new JLabel();
        pluginVersionLabel.setText("Update Plugin Version:");
        rootContentPanel.add(pluginVersionLabel, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null, 0, false));
        pluginVersionText = new JTextField();
        pluginVersionText.setEditable(true);
        pluginVersionText.setToolTipText("format: {major}.{minor}.{revise}[-dev | -release | -bugfix ], eg: 1.0.0-dev or 1.0.0");
        rootContentPanel.add(pluginVersionText, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null, 0, false));
    }

    /**
     * @noinspection ALL
     */
    public JComponent $$$getRootComponent$$$() {
        return rootContentPanel;
    }

    public enum DeployMode {
        Deploy, Upgrade;
    }

    public JPanel getContent() {
        return rootContentPanel;
    }

    public PluginAction getAction() {
        return action;
    }

    public void setAction(PluginAction action) {
        this.action = action;
    }

    public void retryPluginAction() {
        if (this.action != null) {
            this.action.retry();
        }
    }

    public void disablePluginAction() {
        if (this.action != null) {
            this.action.disable();
        }
    }
}
