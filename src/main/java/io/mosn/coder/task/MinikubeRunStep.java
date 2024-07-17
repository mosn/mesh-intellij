package io.mosn.coder.task;

import com.alibaba.fastjson.JSON;
import io.mosn.coder.common.NetUtils;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.FatalCommand;
import io.mosn.coder.compiler.TerminalCompiler;
import io.mosn.coder.plugin.model.PluginStatus;
import io.mosn.coder.task.model.KubeStatus;
import io.mosn.coder.task.model.KubeStep;
import io.mosn.coder.task.model.KubeVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

public class MinikubeRunStep extends RunStep {

    public static Logger logger = LoggerFactory.getLogger(MinikubeRunStep.class);

    public static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}");

    static final int checkMinikube = 0;
    static final int removeMinikube = 1;

    static final int startMinikube = 2;

    volatile int state = checkMinikube;

    public static final Map<String, String> icons = new HashMap<>();

    private MiniMeshConfig config;

    private AtomicInteger current = new AtomicInteger();

    static {
        icons.put("Initial Minikube Setup", "😄  ");
        icons.put("Selecting Driver", "✨  ");
        icons.put("Starting Node", "👍  ");
        icons.put("Pulling Base Image", "🚜  ");
        icons.put("Creating Container", "🔥  ");
        icons.put("Preparing Kubernetes", "🐳  ");
        icons.put("Generating certificates", "    ▪ ");
        icons.put("Booting control plane", "    ▪ ");
        icons.put("Configuring RBAC rules", "    ▪ ");
        icons.put("Verifying Kubernetes", "🔎  ");
        icons.put("Enabling Addons", "🌟  ");
        icons.put("Done", "🏄  ");
    }

    public MinikubeRunStep(MiniMeshConfig config) {
        this.config = config;
        this.tasks = prepareTasks();
    }

    private List<Task> prepareTasks() {
        List<Task> tasks = new ArrayList<>();

        tasks.add(createCheckMinikubeCommand());
        tasks.add(createRemoveMinikubeCommand());

        tasks.add(createStartMinikubeCommand());

        //tasks.add(createCheckStartedCommand());

        if (config != null) {
            String preloads = config.getDefaults().get("mesh.docker.image.preload");
            if (preloads != null && preloads.length() > 0) {
                for (String image : preloads.split(",")) {
                    if (image != null && (image = image.trim()).length() > 0) {
                        tasks.add(createPreloadImageCommand(image));
                    }
                }
            }
        }

        return tasks;
    }

    private Task createCheckMinikubeCommand() {
        Task task = new Task();

        Command cmd = new FatalCommand();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("version");
        exec.add("-o");
        exec.add("json");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {

                String version = cmd.getStdout();
                try {
                    if (version.contains("minikubeVersion")) {
                        KubeVersion kv = JSON.parseObject(version, KubeVersion.class);
                        cmd.setPrettyValue(kv.getMinikubeVersion());
                        cmd.setStatus(PluginStatus.SUCCESS);
                    } else {
                        cmd.setPrettyValue("not install");
                        cmd.setStatus(PluginStatus.FAIL);
                    }
                } finally {
                    config.appendLog("minikube info:" + version);

                    // dump

                }

                logger.info(cmd.toString());
            }
        };

        return task;
    }

    private Task createRemoveMinikubeCommand() {
        Task task = new Task();

        Command cmd = new FatalCommand();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("delete");
//        exec.add("--all=true");
//        exec.add("--purge=true");
        exec.add("-o");
        exec.add("json");

        cmd.exec = exec;

        // when task complete, sleep 3s for next task
        // task.setSleepSeconds(3L);

        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
//                if (cmd.getOutput() != null) {
//                    List<KubeStep> steps = new ArrayList<>();
//                    Iterator<String> e = cmd.getOutput().iterator();
//                    while (e.hasNext()) {
//                        try {
//                            String line = e.next();
//                            KubeStep step = JSON.parseObject(line, KubeStep.class);
//                            steps.add(step);
//                        } catch (Exception ignored) {
//                        }
//                    }
//                }

                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                logger.info(cmd.toString());
            }
        };

        return task;
    }

    private Task createStartMinikubeCommand() {
        Task task = new Task();

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("start");
//        exec.add("--force");
//        exec.add("--extra-config=kubeadm.skip-phases=preflight");

        // get host address
        String host = NetUtils.getLocalHost();

        exec.add("--apiserver-ips=127.0.0.1");
        exec.add("--apiserver-ips=" + host);

        exec.add("--apiserver-names=127.0.0.1");
        exec.add("--apiserver-names=" + host);

        exec.add("--embed-certs=true");

        exec.add("--driver=docker");

        String opts = config.getDefaults().get("minikube.start.opts");
        if (opts != null && opts.length() > 0) {
            exec.add(config.getDefaults().get("minikube.start.opts"));
        }

        if (config.getMysql() != null
                && new File(config.getMysql()).exists()) {
            exec.add("--mount=true");
            exec.add("--mount-string=" + config.getMysql() + ":/minikube-host/mysql/data");
        }

        if (opts == null || !opts.contains("--image-mirror-country")) {
            exec.add("--image-mirror-country=cn");
        }

        if (opts == null || !opts.contains("--listen-address")) {
            exec.add("--listen-address=0.0.0.0");
        }

        if (opts == null || !opts.contains("--ports")) {
            exec.add("--ports=8443:8443");
        }

        exec.add("--static-ip=192.168.200.200");
//        exec.add("--subnet=192.168.49.0");

        if (opts == null || !opts.contains("--addons")) {
            exec.add("--addons=metrics-server");
        }

//        if (opts == null || !opts.contains("--extra-config")) {
////            exec.add("--extra-config=apiserver.address=" + host);
//        }

        if (config.getK8sVersion() != null) {
            exec.add("--kubernetes-version=" + config.getK8sVersion());
        }

        if (config.getMemory() != null) {
            exec.add("--memory=" + config.getMemory());
        }

        if (config.getCpu() != null) {
            exec.add("--cpus=" + config.getCpu());
        }

        if (config.getDisk() != null) {
            exec.add("--disk-size=" + config.getDisk());
        }

        exec.add("-o");
        exec.add("json");

        cmd.exec = exec;
        task.setCmd(cmd);

        // when task complete, sleep 3s for next task
        task.setSleepSeconds(3L);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                logger.info(cmd.toString());

                if (status == 0) {
                    String userDir = System.getProperty("user.home");
                    File miniDir = new File(userDir, ".kube/config");
                    if (miniDir.exists()) {

                        try {
                            String kubeConfig = Files.readString(miniDir.toPath());
                            config.appendLog("local address :" + host + "\n");
                            if (kubeConfig.contains("minikube")) {
                                kubeConfig = kubeConfig.replaceFirst(IP_PATTERN.pattern(), host);
                                Files.writeString(miniDir.toPath(), kubeConfig);
                            }
                        } catch (IOException e) {
                            logger.error("update minikube config", e);
                        }
                    }
                }else {
                    // notify render quit
                    cmd.setFatalQuit(true);
                }

            }
        };

        return task;
    }

    private Task createPreloadImageCommand(String image) {
        Task task = new Task();

        task.setAlias(image);

        Command cmd = new Command();

        // fast quit if load image failed
        cmd.setFatalQuit(true);

        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");

        exec.add("load");
        exec.add(image);

        cmd.exec = exec;
        task.setCmd(cmd);


        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
                checkImageSuccess(cmd, task);

                logger.info(cmd.toString());
            }
        };

        return task;
    }

    private static void checkImageSuccess(Command cmd, Task task) {
        if (cmd.getFailedOutputFirst() != null
                && !cmd.getFailedOutputFirst().isEmpty()) {
            // expect Successfully tagged <image>:<tag>
            boolean foundImage = false;
            for (int i = cmd.getFailedOutputFirst().size() - 1; i >= 0; i--) {
                if (cmd.getFailedOutputFirst().get(i).contains(task.getAlias())) {
                    foundImage = true;
                    break;
                }
            }

            if (!foundImage) {
                cmd.setStatus(PluginStatus.FAIL);
            }
        }
    }

    private Task createCheckStartedCommand() {
        Task task = new Task();

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("status");
        exec.add("-o");
        exec.add("json");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = status -> {
            if (cmd.getStdOutput() != null) {

                // {"Name":"minikube","Host":"Running","Kubelet":"Running","APIServer":"Running","Kubeconfig":"Configured","Worker":false}
                if (cmd.getStdOutput().size() == 1) {

                    KubeStatus kubeStatus = JSON.parseObject(cmd.getStdOutput().get(0), KubeStatus.class);
                    String expect = "Running";
                    if (expect.equalsIgnoreCase(kubeStatus.getHost())
                            && expect.equalsIgnoreCase(kubeStatus.getKubelet())
                            && expect.equalsIgnoreCase(kubeStatus.getaPIServer())
                    ) {
                        cmd.setStatus(PluginStatus.SUCCESS);
                    }
                } else {
                    cmd.setStatus(PluginStatus.FAIL);
                }

            }
        };

        return task;
    }

    @Override
    public void execute() {

        int run = current.get();
        if (run < this.tasks.size()) {
            Task task = this.tasks.get(run);

            Command.CallBack backup = task.getCmd().getCallback();
            task.getCmd().callback = status -> {

                // rollback callback
                task.getCmd().callback = backup;

                current.incrementAndGet();

                if (backup != null) backup.terminated(status);

                if (status == 0) {

                    // check cmd status if success ?
                    if (task.getCmd().getStatus() != null
                            && !task.getCmd().getStatus().equals(PluginStatus.SUCCESS)) {
                        if (getOnTaskFailure() != null) {
                            getOnTaskFailure().onComplete(task);

                            return;
                        }
                    }

                    if (getOnTaskSuccess() != null) {
                        getOnTaskSuccess().onComplete(task);
                    }

                    if (task.getSleepSeconds() != null) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(task.getSleepSeconds()));
                        } catch (Exception ignored) {
                        }
                    }

                    state = current.get();
                    if (state >= startMinikube) {

                        String preloads = config.getDefaults().get("mesh.docker.image.preload");
                        if (preloads == null || preloads.length() == 0) {
                            state = startMinikube;
                        }

                    }

                    // take next task to execute
                    execute();
                } else {
                    // task execute failed
                    if (getOnTaskFailure() != null) {
                        getOnTaskFailure().onComplete(task);
                    }
                }
            };


            TerminalCompiler.compile(config.getProject(), task.getCmd());
        } else {
            // all task executed
            if (getOnSuccess() != null) {
                getOnSuccess().run();
            }
        }
    }

    @Override
    public String render() {
        StringBuilder buf = new StringBuilder();

        Task checkTask = this.tasks.get(0);
        String status = checkTask.getCmd().getStatus();
        if (!checkTask.isStarted()) {
            buf.append("🔥  ").append("select minimesh directory to start deploying...").append("\n");
            return buf.toString();
        }

        if (checkTask.isRunning()) {
            buf.append("🚀  ").append("checking minikube...").append("\n");
            return buf.toString();
        }

        if (checkTask.isComplete()) {
            if (PluginStatus.SUCCESS.equals(status)) {
                buf.append("👍  ");
            } else {
                buf.append("❗  ");
            }
            buf.append("minikube ").append(checkTask.getCmd().getPrettyValue()).append("\n");
        }

        switch (state) {
            case removeMinikube: {
                Task removeKubeTask = this.tasks.get(1);
//
//                if (!removeKubeTask.isStarted()) {
//                    buf.append("🌱  ").append("ready to remove minikube cluster...").append("\n");
//                    return buf.toString();
//                }

                if (removeKubeTask.isStarted() && removeKubeTask.isRunning()) {
                    buf.append("🌱  ").append("removing minikube cluster...").append("\n");
                }

                // append running or completed task
                if (removeKubeTask.getCmd().getStdOutput() != null) {
                    for (String line : removeKubeTask.getCmd().getStdOutput()) {
                        try {
                            KubeStep step = JSON.parseObject(line, KubeStep.class);
                            String prefix = "";
                            if (step.getData().getName() == null) {
                                if (step.getData().getMessage().contains("Using Docker Desktop driver")) {
                                    prefix = "📌  ";
                                } else if (step.getData().getMessage().contains("Removed all")) {
                                    prefix = "💀  ";
                                } else {
                                    prefix = "🔥  ";
                                }
                            } else {
                                prefix = icons.get(step.getName());
                                if (prefix == null) {
                                    prefix = "🔥  ";
                                }
                            }

                            buf.append(prefix).append(step.getData().getMessage()).append("\n");
                        } catch (Exception ignored) {
                        }
                    }
                }

                break;
            }
            case startMinikube: {
                Task startMinikubeTask = this.tasks.get(2);

//                if (!startMinikubeTask.isStarted()) {
//                    buf.append("🌱  ").append("ready to start minikube cluster...").append("\n");
//                    return buf.toString();
//                }

                if (startMinikubeTask.isStarted() && startMinikubeTask.isRunning()) {
                    buf.append("🌱  ").append("starting minikube cluster...").append("\n");
                }

                // append running or completed task
                if (startMinikubeTask.getCmd().getFailedOutputFirst() != null) {
                    for (String line : startMinikubeTask.getCmd().getFailedOutputFirst()) {
                        try {

                            KubeStep step = JSON.parseObject(line, KubeStep.class);
                            String prefix = "";
                            if (step.getData().getName() == null) {
                                if (step.getData().getMessage().contains("Using Docker Desktop driver")) {
                                    prefix = "📌  ";
                                } else if (step.getData().getMessage().contains("Using image")) {
                                    prefix = "    ▪ ";
                                } else {

                                    if (step.getData().getMessage().contains("0.0.0.0")) continue;
                                    if (step.getData().getMessage().contains("kubeadm.skip-phases")) continue;

                                    prefix = "🎉  ";
                                }
                            } else {

                                if (step.getData().getName().equals("Creating Container")) {
                                    if (step.getData().getMessage().contains("production")
                                            || step.getData().getMessage().contains("不适用于生产环境")) continue;
                                }

                                prefix = icons.get(step.getData().getName());
                                if (prefix == null) {
                                    prefix = "🎉  ";
                                }
                            }

                            buf.append(prefix).append(step.getData().getMessage()).append("\n");
                        } catch (Exception ignored) {
                        }
                    }

                    if (startMinikubeTask.getCmd().isFatalQuit()) {
                        System.exit(0);
                    }
                }
                break;
            }
            default: {

                String preloads = config.getDefaults().get("mesh.docker.image.preload");
                if (preloads != null && preloads.length() > 0) {
                    for (int i = 3; i < tasks.size(); i++) {
                        Task task = tasks.get(i);

                        if (task.isRunning()) {
                            buf.append("🚀  ").append("preload image ").append(task.getAlias()).append(" ...").append("\n");
                            return buf.toString();
                        }

                        if (task.isComplete()) {
                            if (PluginStatus.SUCCESS.equals(status)) {
                                buf.append("👍  ");
                            } else {
                                buf.append("❗  ");
                            }
                            buf.append("preload image ").append(task.getAlias()).append(" ...").append("\n");
                        }
                    }
                }

            }
        }

        return buf.toString();
    }

}
