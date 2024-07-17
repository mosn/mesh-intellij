package io.mosn.coder.task;

import io.mosn.coder.compiler.Command;
import io.mosn.coder.plugin.model.PluginStatus;

import java.util.List;

public class Task {

    private boolean parallel;

    private String name;

    private String alias;

    private String prefix;

    private Command cmd;

    private List<Command> commands;

    private Long sleepSeconds;

    private PodInfo pod;

    private String kind;

    private String namespace;

    public boolean isParallel() {
        return parallel;
    }

    public void setParallel(boolean parallel) {
        this.parallel = parallel;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Command getCmd() {
        return cmd;
    }

    public void setCmd(Command cmd) {
        this.cmd = cmd;
    }

    public boolean isRunning() {
        return cmd != null && (cmd.start != 0 && cmd.stop <= 0);
    }

    public boolean isStarted() {
        return cmd != null && (cmd.start != 0);
    }

    public boolean isComplete() {
        return isStarted() && !isRunning();
    }


    public PodInfo getPod() {
        return pod;
    }

    public void setPod(PodInfo pod) {
        this.pod = pod;
    }

    public void cancel() {


        Thread thread = getCmd().getRunningThread();
        getCmd().setRunningThread(null);
        getCmd().setStatus(PluginStatus.CANCEL);

        if (isRunning()) {
            try {
                if (thread != null) {
                    thread.interrupt();
                }

            } catch (Exception ignored) {
            }
        }
    }

    public List<Command> getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    public Long getSleepSeconds() {
        return sleepSeconds;
    }

    public void setSleepSeconds(Long sleepSeconds) {
        this.sleepSeconds = sleepSeconds;
    }

    public String getOutput(){
        return cmd.getStdout();
    }

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }
}
