package io.mosn.coder.cli.minimesh;

import io.mosn.coder.cli.Cli;
import io.mosn.coder.cli.CommandLine;
import io.mosn.coder.common.TimerHolder;
import io.mosn.coder.plugin.model.PluginStatus;
import io.mosn.coder.task.*;
import io.netty.util.Timeout;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class BaseMeshCli {

    @CommandLine.ParentCommand
    Cli parent;

    @CommandLine.Option(names = {"--bundle-dir", "-d"}, paramLabel = "<dir>", description = "Set the path to the mini mesh bundle")
    String path;

    @CommandLine.Option(names = {"--debug"}, paramLabel = "<debug>", description = "deploy mini mesh with debug info, default: true", defaultValue = "true")
    boolean debug = true;

    @CommandLine.Option(names = {"--meshserver", "-m"}, paramLabel = "<meshserver>", description = "deploy mini mesh including mesh server, default: true", defaultValue = "true")
    boolean meshServer;

    @CommandLine.Option(names = {"--operator", "-o"}, paramLabel = "<operator>", description = "deploy mini mesh including operator, default: true", defaultValue = "true")
    boolean operator;

    @CommandLine.Option(names = {"--mosn"}, paramLabel = "<mosn>", description = "deploy mini mesh including mosn, default: true", defaultValue = "true")
    boolean mosn;

    @CommandLine.Option(names = {"--mysql", "-s"}, paramLabel = "<mysql>", description = "deploy mini mesh including mysql component, default: true", defaultValue = "true")
    boolean mysql;

    @CommandLine.Option(names = {"--nacos", "-n"}, paramLabel = "<nacos>", description = "deploy mini mesh including nacos component, default: true", defaultValue = "true")
    boolean nacos;

    @CommandLine.Option(names = {"--prometheus", "-p"}, paramLabel = "<prometheus>", description = "deploy mini mesh including prometheus component, default: true", defaultValue = "true")
    boolean prometheus;

    @CommandLine.Option(names = {"--citadel", "-c"}, paramLabel = "<citadel>", description = "deploy mini mesh including citadel component, default: false", defaultValue = "false")
    boolean citadel;

    @CommandLine.Option(names = {"--dubbo"}, paramLabel = "<dubbo>", description = "deploy mini mesh including dubbo example, default: false", defaultValue = "false")
    boolean dubbo;

    @CommandLine.Option(names = {"--keep-start", "-k"}, paramLabel = "<keep-start>", description = "deploy mini mesh keep cluster start, default: false", defaultValue = "false")
    boolean keepStart;


    @CommandLine.Option(names = {"--k8s-version"}, paramLabel = "<k8s-version>", description = "Set mini mesh k8s version, default: v1.23.8", defaultValue = "v1.23.8")
    String k8s;

    @CommandLine.Option(names = {"--cpu"}, paramLabel = "<cpu>", description = "Set mini mesh cpu cores, format: 4c|8c|16c, default: max", defaultValue = "max")
    String cpu;

    @CommandLine.Option(names = {"--mem"}, paramLabel = "<mem>", description = "Set mini mesh memory size(g), format: 4g|8g|16g, default: max", defaultValue = "max")
    String mem;

    @CommandLine.Option(names = {"--disk"}, paramLabel = "<disk>", description = "Set mini mesh disk size(g), format: 60g|90g|120g", defaultValue = "90g")
    String disk;

    @CommandLine.Option(names = {"--only"}, paramLabel = "<only>", description = "deploy mini mesh including specific components, eg: mysql,nacos", defaultValue = "")
    String only;

    @CommandLine.Option(names = {"--quit", "-q"}, paramLabel = "<quit>", description = "quit and remove mini mesh cluster", defaultValue = "false")
    boolean exit;

    protected CountDownLatch signal = new CountDownLatch(1);

    protected LinkedBlockingDeque<Runnable> queue = new LinkedBlockingDeque<Runnable>();

    protected Runnable quit = () -> {
    };

    protected MiniMeshConfig config = new MiniMeshConfig();

    protected PrettyTask prettyTask;

    protected AtomicReference<Timeout> runningTimeout = new AtomicReference<>();

    protected String checkDeployConfig() {

        if (path == null) {

            String dir = System.getenv("MESH_BUNDLE_DIR");
            if (dir != null && !dir.isEmpty()) {
                path = dir;
            } else {
                return "mini mesh directory is required";
            }

        }

        this.config.setRootDir(new File(path));

        String dir = this.config.getRootDir().getPath();
        File rootDir = new File(dir);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return "mini mesh directory is invalid";
        }

        File release = new File(dir, "release");
        if (!release.exists() || !release.isFile()) {
            return "mini mesh directory is not mini mesh bundle root directory";
        }

        String userDir = System.getProperty("user.home");
        File miniDir = new File(userDir, ".minimesh");

        if (!miniDir.exists()) {
            miniDir.mkdir();
        }

        File mysqlData = new File(miniDir, "mysql/data");
        if (!mysqlData.exists()) {
            mysqlData.mkdirs();
        }

        File hostDir = new File(userDir, ".minikube");
        if (!hostDir.exists()) hostDir.mkdirs();

        File etc = new File(hostDir, "files/etc");
        if (!etc.exists()) etc.mkdirs();

        File hosts = new File(etc, "hosts");
        if (hosts.exists()) hosts.delete();

        try {
            Files.write(hosts.toPath(), "192.168.200.200 sidecar-operator-service".getBytes());
        } catch (IOException e) {
            return "failed to write minikube hosts, err: " + e.getMessage();
        }

        this.config.setProject(miniDir.getPath());
        this.config.setMysql(mysqlData.getPath());

        this.config.setDebug(debug);

//        if (customConfigurationCheckBox.isSelected()) {
//            File config = this.config.getProperty();
//            if (config == null || !config.exists()) {
//
//            }
//
//        }

        this.config.setKeepStart(this.keepStart);

        config.setProperty(new File(config.getRootDir(), "cloudmesh-env.properties"));

        if (config.getProperty().exists()) {
            Properties properties = new Properties();
            try (FileInputStream in = new FileInputStream(config.getProperty())) {
                properties.load(in);

                for (String key : properties.stringPropertyNames()) {
                    config.getDefaults().put(key, properties.getProperty(key));
                }
            } catch (Exception ignored) {
            }
        }

        // check cpu
        String cpu = this.cpu;
        String checkCpu = null;
        if (cpu != null && cpu.endsWith("c")) {
            checkCpu = cpu.replace("c", "");
        } else {
            cpu = "max";
        }

        if (checkCpu != null) {
            try {
                Integer.parseInt(checkCpu);
            } catch (Exception ignore) {
                return "cpu is invalid";
            }
        }

        if (checkCpu != null) {
            this.config.setCpu(checkCpu);
        } else {
            this.config.setCpu(cpu);
        }

        String version = this.k8s;
        if (version == null || version.trim().length() == 0) {
            version = "v1.23.8";
        }
        this.config.setK8sVersion(version);

        // check memory
        String memory = this.mem;
        String checkMem = null;
        if (memory != null && memory.endsWith("g")) {
            checkMem = memory.replace("g", "");
        } else {
            memory = "max";
        }

        if (checkMem != null) {
            try {
                Integer.parseInt(checkMem);
            } catch (Exception ignore) {
                return "memory is invalid";
            }
        }
        this.config.setMemory(memory);

        // check disk
        String disk = this.disk;
        String checkDisk = null;
        if (disk != null && disk.endsWith("g")) {
            checkDisk = disk.replace("g", "");
        } else {
            disk = "90g";
        }

        if (checkDisk != null) {
            try {
                Integer.parseInt(checkDisk);
            } catch (Exception ignore) {
                return "disk is invalid";
            }
        }
        this.config.setDisk(disk);


        if (only != null && !only.isEmpty()) {
            String[] components = only.split(",");

            if (components.length == 1) {
                if (!keepStart)
                    keepStart = true;

            }

            if (components.length == 2) {
                if ("dubbo".equals(components[0]) && "mosn".equals(components[1])
                        || "dubbo".equals(components[1]) && "mosn".equals(components[0])) {
                    if (!keepStart)
                        keepStart = true;
                }
            }

            for (String name : components) {
                String val = name.trim();
                switch (val) {
                    case "meshserver": {
                        this.config.getDeploy().put("meshserver", "meshserver");
                        break;
                    }
                    case "operator": {
                        this.config.getDeploy().put("operator", "operator");
                        break;
                    }
                    case "mosn": {
                        this.config.getDeploy().put("mosn", "mosn");
                        break;
                    }
                    case "mysql": {
                        this.config.getDeploy().put("mysql", "mysql");
                        break;
                    }
                    case "nacos": {
                        this.config.getDeploy().put("nacos", "nacos");
                        break;
                    }
                    case "prometheus": {
                        this.config.getDeploy().put("prometheus", "prometheus");
                        break;
                    }
                    case "citadel": {
                        this.config.getDeploy().put("citadel", "citadel");
                        break;
                    }
                    case "dubbo": {
                        this.config.getDeploy().put("dubbo", "dubbo");
                        break;
                    }
                }
            }
        } else {
            if (meshServer) {
                this.config.getDeploy().put("meshserver", "meshserver");
            }

            if (operator) {
                this.config.getDeploy().put("operator", "operator");
            }

            if (mosn) {
                this.config.getDeploy().put("mosn", "mosn");
            }

            if (mysql) {
                this.config.getDeploy().put("mysql", "mysql");
            }

            if (nacos) {
                this.config.getDeploy().put("nacos", "nacos");
            }

            if (prometheus) {
                this.config.getDeploy().put("prometheus", "prometheus");
            }

            if (citadel) {
                this.config.getDeploy().put("citadel", "citadel");
            }

            if (dubbo) {
                this.config.getDeploy().put("dubbo", "dubbo");
            }
        }

        initPrettyTask();

        return null;
    }

    protected void initPrettyTask() {
        prettyTask = new PrettyTask();

        prettyTask.setConfig(config);

        prettyTask.setRunSteps(new ArrayList<>());

        if (!keepStart) {
            prettyTask.getRunSteps().add(createMinikubeStep());
        }
        prettyTask.getRunSteps().add(createBuildImageStep());
        prettyTask.getRunSteps().add(createDeployStep());

        // first time to update ui
        wrapTextArea(prettyTask.render(), false);
    }

    public String startDeployMiniMesh() {
        String message = checkDeployConfig();
        if (message != null) {
            return message;
        }
        initDeployTask();
        return null;
    }

    protected RunStep createMinikubeStep() {

        MinikubeRunStep step = new MinikubeRunStep(this.config);
        step.setName("prepare minikube");
        step.setOnTaskSuccess(task -> {
            //application.invokeLater(() -> wrapTextArea(prettyTask.render(), true));

            this.queue.offerLast(() -> {
                wrapTextArea(prettyTask.render(), true);
            });
        });
//
//        step.setOnTaskFailure(task -> {
//
//
//            application.invokeLater(() -> infoTextArea.setText(prettyTask.render()));
//        });
//
//        // all task complete
//        step.setOnSuccess(() -> {
////            Timeout timeout = runningTimeout.get();
////            if (timeout != null) timeout.cancel();
//
//            application.invokeLater(() -> infoTextArea.setText(prettyTask.render()));
//        });

        return step;
    }

    protected RunStep createBuildImageStep() {
        BuildImageStep step = new BuildImageStep(this.config);
        step.setName("build image");
        step.setOnTaskSuccess(task -> {
            //application.invokeLater(() -> wrapTextArea(prettyTask.render(), true));

            this.queue.offerLast(() -> {
                wrapTextArea(prettyTask.render(), true);
            });
        });

        return step;
    }

    protected RunStep createDeployStep() {
        DeployStep step = new DeployStep(this.config);
        step.setName("deploy mesh");
        step.setOnTaskSuccess(task -> {
            //application.invokeLater(() -> wrapTextArea(prettyTask.render(), true));

            this.queue.offerLast(() -> {
                wrapTextArea(prettyTask.render(), true);
            });
        });

        return step;
    }

    protected void wrapTextArea(String content, boolean wrap) {
//        int start = infoTextArea.getSelectionStart();
//        int end = infoTextArea.getSelectionEnd();

        clearConsole();
//        infoTextArea.setText(content);
        System.out.println(content);

//        if (content.length() > end && wrap) {
//            infoTextArea.select(start, end);
//        }
    }

    protected static void clearConsole() {
        System.out.println("\033[H\033[2J");
        System.out.flush();
    }

    protected void initDeployTask() {

        prettyTask.setOnStepFailure(step -> {

            String output = step.getFailedOutput();

            this.queue.offerLast(() -> {


                Timeout timeout = runningTimeout.get();
                if (timeout != null) timeout.cancel();

                StringBuilder buf = new StringBuilder(prettyTask.render());

                String appendText = "\n" + step.getName() + " error:\n";
                buf.append(appendText);
                buf.append(output);


                if (config.getLogFile() != null && config.getLogFile().exists()) {
                    try {
                        String err = Files.readString(config.getLogFile().toPath());
                        buf.append("\n logs:\n").append(err);
                    } catch (IOException ignored) {
                    }
                }

                wrapTextArea(buf.toString(), true);

                destroy();

            });

        });

        prettyTask.setOnStepSuccess(step -> {

            this.queue.offerLast(() -> {

                wrapTextArea(prettyTask.render(), true);

                // check all tasks complete but any failed
                if (step.isComplete()) {
                    for (Task task : step.getTasks()) {

                        if (!PluginStatus.SUCCESS.equals(task.getCmd().getStatus())) {
                            String buf = prettyTask.render() + "\n" +
                                    step.getFailedOutput();
                            wrapTextArea(buf, true);

                            return;
                        }
                    }
                }

            });


        });

        prettyTask.setOnSuccess(() -> {
//            Timeout timeout = runningTimeout.get();
//            if (timeout != null) timeout.cancel();


            this.queue.offerLast(() -> {
                wrapTextArea(prettyTask.render(), true);

                String text = prettyTask.render();

                if (text != null) {
                    String foundText = "minikube kubectl -- port-forward service/meshserver-service 7080:80 -n sofamesh";

                    int start = text.indexOf(foundText);

                    if (start < 0) {
                        foundText = "minikube kubectl -- port-forward service/nacos-service 8848 9848 -n sofamesh";
                        start = text.indexOf(foundText);
                    }

                    if (start > 0) {
                        try {
                            String copyEnabled = config.getDefaults().getOrDefault("runtime.copy.forward.enable", "true");
                            if ("true".equals(copyEnabled)) {
                                Toolkit.getDefaultToolkit()
                                        .getSystemClipboard()
                                        .setContents(new StringSelection(foundText), null);
                            }
                        } catch (Exception ignored) {

                        }

                    }
                }
            });

        });

        // start ui update
        startUpdateNotify();

        // debug enable
        // env required SIDECAR_DLV_DEBUG
        config.getDefaults().put("runtime.debug.enable", String.valueOf(this.config.isDebug()));

        // start task step
        new Thread(() -> prettyTask.execute()).start();
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

    public void destroy() {

        if (runningTimeout.get() != null
                && !runningTimeout.get().isCancelled()) {
            runningTimeout.get().cancel();
        }

        prettyTask.cancel();

        // clean pool task
    }

    private void startUpdateNotify() {
        Timeout timeout = TimerHolder.getTimer().newTimeout(tt -> {
            if (tt.isCancelled()) {
                return;
            }

//            boolean complete = true;
//            for (RunStep step : prettyTask.getRunSteps()) {
//                if (!step.isComplete()) {
//                    complete = false;
//                }
//            }
//
//            if (complete) return;

            this.queue.offerLast(() -> {
                wrapTextArea(prettyTask.render(), true);
            });

            /**
             * schedule next time
             *
             */
            runningTimeout.set(tt.timer().newTimeout(tt.task(), 1, TimeUnit.SECONDS));

        }, 1, TimeUnit.SECONDS);
        runningTimeout.set(timeout);
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
