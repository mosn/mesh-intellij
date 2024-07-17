package io.mosn.coder.plugin.model;

import com.alibaba.fastjson.annotation.JSONField;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.intellij.view.SidecarInjectRuleModel;
import io.netty.buffer.ByteBuf;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author yiji@apache.org
 */
public class PluginBundle extends PluginBundleBase {

    public static final String TaskMeta = "_task_meta";

    public static final int CustomProtocol = 11;

    public static final int InternalProtocol = 10;

    public static final int StreamFilter = 4;

    public static final int Transcoder = 3;

    public static final String KIND_PROTOCOL = "protocol";

    public static final String KIND_FILTER = "stream_filter";
    public static final String KIND_TRANSCODER = "transcoder";

    public static final String KIND_TRACE = "trace";

    public static final String FILTER_ALIAS = "filter";

    private String status;

    private String ruleName;

    private String oldImage;

    private String upgradeImage;

    private List<Plugin> bundles;

    private Long start;

    private Long stop;

    private String indent = "    "; // 4 space

    private boolean terminal;

    private String meshServer;

    private String deployVersion;

    private SidecarInjectRuleModel.InjectRule selectedRule;

    private SidecarInjectRuleModel.UpgradeImage selectedImage;

    public List<Plugin> getBundles() {
        return bundles;
    }

    public void setBundles(List<Plugin> bundles) {
        this.bundles = bundles;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getOldImage() {
        return oldImage;
    }

    public void setOldImage(String oldImage) {
        this.oldImage = oldImage;
    }

    public String getUpgradeImage() {
        return upgradeImage;
    }

    public void setUpgradeImage(String upgradeImage) {
        this.upgradeImage = upgradeImage;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public Long getStop() {
        return stop;
    }

    public void setStop(Long stop) {
        this.stop = stop;
    }

    public String getMeshServer() {
        return meshServer;
    }

    public void setMeshServer(String meshServer) {
        this.meshServer = meshServer;
    }

    public String getDeployVersion() {
        return deployVersion;
    }

    public void setDeployVersion(String deployVersion) {
        this.deployVersion = deployVersion;
    }

    public String renderInitTask() {

        StringBuilder buffer = new StringBuilder();

        buffer.append("{");

        if (this.getRuleId() != null) {
            buffer.append("\"ruleId\": \"").append(this.getRuleId()).append("\",");
        }

        if (this.getUpgradeId() != null) {
            buffer.append("\"upgradeId\": \"").append(this.getUpgradeId()).append("\",");
        }

        if (this.getInstanceId() != null) {
            buffer.append("\"instanceId\": \"").append(this.getInstanceId()).append("\",");
        }

        if (this.bundles != null) {
            buffer.append("\"bundles\": ").append(this.renderBundlePlugins());
        } else {
            buffer.append("\"bundles\": []");
        }

        buffer.append("}");

        return buffer.toString();
    }

    public String renderWithCommand(boolean allCommandComplete) {
        StringBuilder buffer = new StringBuilder();

        if (this.getBundles() != null) {

            List<PluginBundle.Plugin> failed = null;

            List<PluginBundle.Plugin> filters = new ArrayList<>();
            List<PluginBundle.Plugin> transcoders = new ArrayList<>();
            List<PluginBundle.Plugin> codecs = new ArrayList<>();
            List<PluginBundle.Plugin> traces = new ArrayList<>();
            for (PluginBundle.Plugin plugin : this.getBundles()) {

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

            if (this.terminal) {
                /**
                 * append upgrade and image info
                 */

                if (this.meshServer != null) {
                    buffer.append("mesh server: ").append(this.meshServer).append("\n");
                }

                if (this.deployVersion != null) {
                    buffer.append("deploy version: ").append(this.deployVersion).append("\n");
                }

                if (this.selectedRule != null) {
                    buffer.append("sidecar rule: ").append(this.selectedRule).append("\n");
                }
                if (this.upgradeImage != null) {
                    buffer.append("upgrade image: ").append(this.upgradeImage).append("\n");
                }

                buffer.append("\n");
            }

            PluginTable table = new PluginTable();

            table.addHeader("kind")
                    .addHeader("")  // for =>
                    .addHeader("action")
                    .addHeader("plugin")
                    .addHeader("status")
                    .addHeader("time");

            if (allCommandComplete && failed == null) {
                failed = new ArrayList<>();
            }

            appendWithCommand(buffer, filters, table, failed);
            appendWithCommand(buffer, transcoders, table, failed);
            appendWithCommand(buffer, codecs, table, failed);
            appendWithCommand(buffer, traces, table, failed);

            buffer.append(table.pretty());

            if (allCommandComplete) {
                buffer.append("\n\n");
                if (failed != null && failed.isEmpty()) {
                    buffer.append("All plugin command was successfully executed. Congratulations!\n");
                } else {
                    buffer.append("Some plugin command execute failed. Please try again\n");
                }

                if (failed != null
                        && !failed.isEmpty()) {
                    buffer.append("\n");
                    buffer.append("The following plugin command failed:\n");
                    for (PluginBundle.Plugin p : failed) {
                        buffer.append("\t").append(p.getKind()).append("->").append(p.getName()).append("\n");
                    }
                }
            }
        }

        return buffer.toString();
    }

    public String renderWithTask() {
        StringBuilder buffer = new StringBuilder();

        if (this.getBundles() != null) {

            List<PluginBundle.Plugin> filters = new ArrayList<>();
            List<PluginBundle.Plugin> transcoders = new ArrayList<>();
            List<PluginBundle.Plugin> codecs = new ArrayList<>();
            List<PluginBundle.Plugin> traces = new ArrayList<>();
            for (PluginBundle.Plugin plugin : this.getBundles()) {

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


            PluginTable table = new PluginTable();

            table.addHeader("kind")
                    .addHeader("")  // for =>
                    .addHeader("action")
                    .addHeader("plugin")
                    .addHeader("status")
                    .addHeader("time");

            appendTask(buffer, filters, null, table);
            appendTask(buffer, transcoders, null, table);
            appendTask(buffer, codecs, null, table);
            appendTask(buffer, traces, null, table);

            buffer.append(table.pretty());
        }

        return buffer.toString();
    }

    public String renderAllTasks(boolean allTaskComplete) {
        StringBuilder buffer = new StringBuilder();

        if (this.getBundles() != null) {

            List<PluginBundle.Plugin> remoteFailedPlugins = null;

            List<PluginBundle.Plugin> failed = null;

            if (allTaskComplete) {
                failed = new ArrayList<>();
                remoteFailedPlugins = new ArrayList<>();
            }

            List<PluginBundle.Plugin> filters = new ArrayList<>();
            List<PluginBundle.Plugin> transcoders = new ArrayList<>();
            List<PluginBundle.Plugin> codecs = new ArrayList<>();
            List<PluginBundle.Plugin> traces = new ArrayList<>();
            for (PluginBundle.Plugin plugin : this.getBundles()) {

                /**
                 * skip not current project plugin
                 */
                if (!plugin.getOwner()) {

                    if (allTaskComplete) {
                        if (plugin.getStatus() != null
                                && !PluginStatus.SUCCESS.equals(plugin.getStatus())) {
                            remoteFailedPlugins.add(plugin);
                        }
                    }

                    continue;
                }

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

            if (this.terminal) {
                /**
                 * append upgrade and image info
                 */

                if (this.meshServer != null) {
                    buffer.append("mesh server: ").append(this.meshServer).append("\n");
                }

                if (this.deployVersion != null) {
                    buffer.append("deploy version: ").append(this.deployVersion).append("\n");
                }

                if (this.selectedRule != null) {
                    buffer.append("sidecar rule: ").append(this.selectedRule).append("\n");
                }
                if (this.upgradeImage != null) {
                    buffer.append("upgrade image: ").append(this.upgradeImage).append("\n\n");
                }
            }

            PluginTable table = new PluginTable();

            table.addHeader("kind")
                    .addHeader("")  // for =>
                    .addHeader("action")
                    .addHeader("plugin")
                    .addHeader("status")
                    .addHeader("time");

            appendTask(buffer, filters, failed, table);
            appendTask(buffer, transcoders, failed, table);
            appendTask(buffer, codecs, failed, table);
            appendTask(buffer, traces, failed, table);

            buffer.append(table.pretty());

            if (allTaskComplete) {
                boolean printTaskId = false;
                buffer.append("\n");
                if (failed != null && failed.isEmpty()) {
                    buffer.append("\nCongratulations! All plugins was successfully deployed. ");
                } else {
                    buffer.append("\nSome plugins deploy failed. Please try again. ");
                    if (this.getTaskId() != null) {
                        printTaskId = true;
                        buffer.append("\ntask id: '" + this.getTaskId() + "'\n");
                    }
                }

                if (remoteFailedPlugins != null
                        && !remoteFailedPlugins.isEmpty()) {
                    buffer.append("\n\n");
                    buffer.append("The following plugins are missing to be uploaded:");
                    PluginTable remoteTable = new PluginTable();
                    for (PluginBundle.Plugin missing : remoteFailedPlugins) {
                        PluginTable.Row remoteRow = new PluginTable.Row();
                        remoteRow.appendColumn(missing.getKind())
                                .appendColumn("->")
                                .appendColumn(missing.getName())
                                .appendColumn(missing.getStatus() == null ? PluginStatus.WAITING : missing.getStatus());
                        remoteTable.addRow(remoteRow);
                    }

                    buffer.append(remoteTable.pretty());

                    if (!printTaskId && this.getTaskId() != null) {
                        buffer.append("\ntask id: '" + this.getTaskId() + "'\n");
                    }
                }
            }


        }

        return buffer.toString();
    }

    public boolean commandComplete() {

        if (this.getBundles() != null) {
            for (PluginBundle.Plugin plugin : this.getBundles()) {
                if (!plugin.getOwner()) continue;
                if (plugin.getCommands() != null) {
                    for (Command command : plugin.getCommands()) {
                        if (!PluginStatus.SUCCESS.equals(command.getStatus())) {
                            return false;
                        }
                    }
                }
            }
        }

        return true;
    }

    public boolean bundleComplete() {

        if (this.getBundles() != null) {
            for (PluginBundle.Plugin plugin : this.getBundles()) {
                if (!plugin.getOwner()) continue;
                if (!PluginStatus.SUCCESS.equals(plugin.getStatus())) {
                    return false;
                }
            }
        }

        return true;
    }

    protected void appendWithCommand(StringBuilder buff, List<PluginBundle.Plugin> plugins, PluginTable table, List<PluginBundle.Plugin> failed) {
        for (PluginBundle.Plugin plugin : plugins) {

            /**
             * format:
             *
             * {kind}
             *      {action} {plugin name} {status} {time}
             */

            String action = "";
            long time = 0;
            String status = PluginStatus.INIT;

            if (plugin.getCommands() != null) {

                for (Command command : plugin.getCommands()) {
                    if (action.length() > 0) {
                        action += "&";
                    }
                    if (command.getShortAlias() != null) {
                        action += command.getShortAlias();
                    }

                    if (command.start <= 0) {
                        time = 0;
                        status = PluginStatus.WAITING;
                    } else {
                        time += ((command.stop <= 0 ? System.currentTimeMillis() : command.stop) - command.start);
                        if (command.getStatus() != null) {
                            status = command.getStatus();
                        } else {
                            status = PluginStatus.COMMAND_RUNNING;
                        }
                    }

                    /**
                     * record failed command
                     */
                    if (failed != null && !PluginStatus.SUCCESS.equals(status)) {
                        if (!failed.contains(plugin)) {
                            failed.add(plugin);
                        }
                    }
                }

                PluginTable.Row row = new PluginTable.Row();
                row.appendColumn(plugin.getKind(), true)
                        .appendColumn("=>")
                        .appendColumn(action)
                        .appendColumn(plugin.getName())
                        .appendColumn(plugin.getStatus() == null ? status : plugin.getStatus())
                        .appendColumn(TimeUnit.MILLISECONDS.toSeconds(time) + "s");

                table.addRow(row);
            }

        }
    }

    protected void appendTask(StringBuilder buff, List<PluginBundle.Plugin> plugins, List<PluginBundle.Plugin> failed, PluginTable table) {

        if (plugins == null || plugins.isEmpty()) {
            return;
        }

        for (PluginBundle.Plugin plugin : plugins) {

            /**
             * format:
             *
             * {kind} => {action} {plugin name} {status} {time}
             */

            String action = "deploy";
            long time = 0;
            String status = PluginStatus.INIT;

            if (plugin.start == null || plugin.start <= 0) {
                time = 0;
            } else {
                time += ((plugin.stop == null || plugin.stop <= 0 ? System.currentTimeMillis() : plugin.stop) - plugin.start);
            }

            PluginTable.Row row = new PluginTable.Row();
            row.appendColumn(plugin.getKind(), true)
                    .appendColumn("=>")
                    .appendColumn(action)
                    .appendColumn(plugin.getName())
                    .appendColumn(plugin.getStatus() == null ? status : plugin.getStatus())
                    .appendColumn(TimeUnit.MILLISECONDS.toSeconds(time) + "s");

            table.addRow(row);

            if (failed != null && !PluginStatus.SUCCESS.equals(plugin.getStatus())) {
                failed.add(plugin);
            }

        }
    }

    public String renderBundlePlugins() {
        return render(this.getBundles());
    }

    public void updateTaskStatus(String status) {
        if (this.getBundles() != null) {
            /**
             * update remote bundle status
             */
            for (PluginBundle.Plugin plugin : this.getBundles()) {
                if (plugin.getOwner()) {
                    if (!PluginStatus.SUCCESS.equals(status)) {
                        plugin.setStatus(status);
                    }
                }
            }
        }
    }

    public void resetTaskStatus(String status) {
        if (this.getBundles() != null) {
            /**
             * update remote bundle status
             */
            for (PluginBundle.Plugin plugin : this.getBundles()) {
                if (plugin.getOwner()) {
                    plugin.setStatus(status);

                    /**
                     * reset start & stop
                     */
                    if (PluginStatus.INIT.equals(status) || PluginStatus.COMMAND_RUNNING.equals(status)) {
                        plugin.setStart(null);
                        plugin.setStop(null);

                        if (plugin.getCommands() != null) {
                            for (Command command : plugin.getCommands()) {
                                command.resetStatus();
                            }
                        }
                    }
                }
            }
        }
    }

    protected String printStack(Exception e) {
        if (e != null) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            try (PrintStream out = new PrintStream(stream, true)) {
                e.printStackTrace(out);
            }

            return stream.toString();
        }

        // default empty stack
        return "";
    }

    public boolean isTerminal() {
        return terminal;
    }

    public void setTerminal(boolean terminal) {
        this.terminal = terminal;
    }

    public SidecarInjectRuleModel.InjectRule getSelectedRule() {
        return selectedRule;
    }

    public void setSelectedRule(SidecarInjectRuleModel.InjectRule selectedRule) {
        this.selectedRule = selectedRule;
    }

    public SidecarInjectRuleModel.UpgradeImage getSelectedImage() {
        return selectedImage;
    }

    public void setSelectedImage(SidecarInjectRuleModel.UpgradeImage selectedImage) {
        this.selectedImage = selectedImage;
    }

    /**
     * @author yiji@apache.org
     */
    public static class Plugin {

        private Long id;

        // plugin_edition id
        private Long editionId;

        private Boolean owner;

        private Long pluginFileId;

        private String kind;

        private String name;

        /**
         * plugin version: should be upgraded
         */
        private String version;

        /**
         * plugin version before upgrade.
         */
        private String revision;

        private String fullName;

        private String path;

        private Map<String, String> dependency;

        private PluginMetadata metadata;

        // ======   only for protocol ======
        @JSONField(name = "stream_filters")
        private List<Plugin> filters;

        private List<Plugin> transcoders;

        // ======   end for protocol ======


        // ======  used for notify client =====
        private volatile String status;

        private String description;

        // ======  end notify client =====

        private UploadFile file;

        private List<Command> commands;

        private Long start;

        private Long stop;

        public String getName() {
            if (name == null && this.fullName != null) {
                if (this.fullName.indexOf("-") > 0) {
                    name = this.fullName.substring(0, this.fullName.indexOf("-"));
                }
            }
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getKind() {
            return kind;
        }

        public void setKind(String kind) {
            this.kind = kind;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        // ========   only protocol plugin available ================
        public List<Plugin> getFilters() {
            return filters;
        }

        public void setFilters(List<Plugin> filters) {
            this.filters = filters;
        }

        public List<Plugin> getTranscoders() {
            return transcoders;
        }

        public void setTranscoders(List<Plugin> transcoders) {
            this.transcoders = transcoders;
        }

        // ========   end protocol plugin ================

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Long getPluginFileId() {
            return pluginFileId;
        }

        public void setPluginFileId(Long pluginFileId) {
            this.pluginFileId = pluginFileId;
        }

        public Boolean getOwner() {
            return owner;
        }

        public void setOwner(Boolean owner) {
            this.owner = owner;
        }

        public Long getEditionId() {
            return editionId;
        }

        public void setEditionId(Long editionId) {
            this.editionId = editionId;
        }

        public UploadFile getFile() {
            return file;
        }

        public void setFile(UploadFile file) {
            this.file = file;
        }

        public Map<String, String> getDependency() {
            return dependency;
        }

        public void setDependency(Map<String, String> dependency) {
            this.dependency = dependency;
        }

        public String getRevision() {
            return revision;
        }

        public void setRevision(String revision) {
            this.revision = revision;
        }

        public PluginMetadata getMetadata() {
            return metadata;
        }

        public void setMetadata(PluginMetadata metadata) {
            this.metadata = metadata;
        }

        public List<Command> getCommands() {
            return commands;
        }

        public void setCommands(List<Command> commands) {
            this.commands = commands;
        }

        public Long getStart() {
            return start;
        }

        public void setStart(Long start) {
            this.start = start;
        }

        public Long getStop() {
            return stop;
        }

        public void setStop(Long stop) {
            this.stop = stop;
        }
    }

    public static class UploadFile {

        private PluginBundle bundle;

        private Plugin plugin;

        // write to file
        private File file;

        private RandomAccessFile out;

        private boolean closed;

        public UploadFile(PluginBundle bundle, Plugin plugin) throws IOException {
            this.bundle = bundle;
            this.plugin = plugin;
            this.closed = false;
            this.file = File.createTempFile(this.plugin.getKind(), this.plugin.getFullName());

            this.plugin.setPath(this.file.getAbsolutePath());

            this.out = new RandomAccessFile(this.file, "rw");
        }

        public boolean write(long offset, ByteBuf buf) throws IOException {

            this.out.seek(offset);

            int len = buf.readableBytes();
            if (buf.hasArray()) {
                out.write(buf.array(), buf.arrayOffset() + buf.readerIndex(), len);
                return true;
            }

            byte[] bytes = new byte[len];
            buf.getBytes(buf.readerIndex(), bytes);
            this.out.write(bytes);

            return true;
        }

        void close() throws IOException {
            if (closed) return;

            this.closed = true;
            if (this.out != null) {
                this.out.close();
            }
        }

        public void destroy() throws IOException {
            close();

            if (this.file != null && this.file.exists()) {
                this.file.delete();
            }

            this.file = null;
            this.out = null;
        }

    }

}
