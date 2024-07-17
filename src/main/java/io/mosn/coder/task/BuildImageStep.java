package io.mosn.coder.task;

import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.TerminalCompiler;
import io.mosn.coder.plugin.model.PluginStatus;
import io.mosn.coder.plugin.model.PluginTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BuildImageStep extends RunStep {

    static final Map<String, String> icons = new HashMap<>();

    private MiniMeshConfig config;

    private AtomicInteger current = new AtomicInteger();

    public static Logger logger = LoggerFactory.getLogger(BuildImageStep.class);

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

    public BuildImageStep(MiniMeshConfig config) {
        this.config = config;
    }

    private List<Task> prepareTasks() {
        List<Task> tasks = new ArrayList<>();

        if (config.deployMysql()) {
            tasks.add(createBuildMysqlCommand());
        }

        if (config.deployMeshServer()) {
            tasks.add(createBuildMeshServerCommand());
        }

        if (config.deployOperator()){
            tasks.add(createBuildOperatorCommand());
        }

        if (config.deployMosn()){
            tasks.add(createBuildMosnCommand());
        }

        if (config.deployNacos())
            tasks.add(createBuildNacosCommand());

        if (config.deployPrometheus()){
            tasks.add(createBuildPrometheusCommand());
        }

        if (config.deployCitadel()) {
            tasks.add(createBuildCitadelCommand());
            tasks.add(createBuildNodeAgentCommand());
        }

        if (config.deployDubbo()) {
            tasks.add(createBuildDubboServerCommand());
            tasks.add(createBuildDubboClientCommand());
        }

        return tasks;
    }

    private Task createBuildMeshServerCommand() {
        Task task = new Task();
        task.setKind("mesh console");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-meshserver");
        exec.add("-t");
        exec.add("meshserver:v1");

        task.setAlias("meshserver:v1");

        task.setName("meshserver");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildMosnCommand() {
        Task task = new Task();

        task.setKind("sidecar");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-mosn");
        exec.add("-t");
        exec.add("mosn:v1");

        task.setAlias("mosn:v1");

        task.setName("mosn");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildOperatorCommand() {
        Task task = new Task();

        task.setKind("k8s operator");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-operator");
        exec.add("-t");
        exec.add("operator:v1");

        task.setAlias("operator:v1");

        task.setName("operator");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildNacosCommand() {
        Task task = new Task();

        task.setKind("registry");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-nacos");
        exec.add("-t");
        exec.add("nacos:v1");

        task.setAlias("nacos:v1");

        task.setName("nacos");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildCitadelCommand() {
        Task task = new Task();

        task.setKind("security");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-istio-citadel");
        exec.add("-t");
        exec.add("citadel:v1");

        task.setAlias("citadel:v1");

        task.setName("citadel");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildNodeAgentCommand() {
        Task task = new Task();

        task.setKind("security");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-istio-nodeagent");
        exec.add("-t");
        exec.add("nodeagent:v1");

        task.setAlias("nodeagent:v1");

        task.setName("nodeagent");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildMysqlCommand() {
        Task task = new Task();

        task.setKind("database");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-mysql");
        exec.add("-t");
        exec.add("mini-mysql:v1");

        task.setAlias("mini-mysql:v1");

        task.setName("mysql");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildPrometheusCommand() {
        Task task = new Task();

        task.setKind("monitor");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-prometheus");
        exec.add("-t");
        exec.add("prometheus:v1");

        task.setAlias("prometheus:v1");

        task.setName("prometheus");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildDubboServerCommand() {
        Task task = new Task();

        task.setKind("example");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-dubbo-server");
        exec.add("-t");
        exec.add("dubbo-server:v1");

        task.setAlias("dubbo-server:v1");

        task.setName("dubbo-server");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private Task createBuildDubboClientCommand() {
        Task task = new Task();

        task.setKind("example");

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("image");
        exec.add("build");
        exec.add(childDirectory());
        exec.add("-f");
        exec.add("Dockerfile-dubbo-client");
        exec.add("-t");
        exec.add("dubbo-client:v1");

        task.setAlias("dubbo-client:v1");

        task.setName("dubbo-client");

        cmd.exec = exec;
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

                checkImageSuccess(cmd, task);
            }
        };

        return task;
    }

    private static void checkImageSuccess(Command cmd, Task task) {

        logger.info(cmd.toString());

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

                // dump log
                StringBuilder buffer = new StringBuilder();
                for (int i = 0, n = cmd.getFailedOutputFirst().size() - 1; i <= n; i++) {
                    String line = cmd.getFailedOutputFirst().get(i);
                    buffer.append(cmd.getFailedOutputFirst().get(i));
                    if (!line.endsWith("\n")) {
                        buffer.append("\n");
                    }
                }

                logger.error(buffer.toString());
            }
        }

    }

    @Override
    public void execute() {

        this.tasks = prepareTasks();

        for (Task task : this.tasks) {


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

                    // check all task complete
                    boolean allComplete = true;
                    for (Task _task : getTasks()) {
                        if (!_task.isComplete()
                                || _task.getCmd() != null
                                && !PluginStatus.SUCCESS.equals(_task.getCmd().getStatus())) {
                            allComplete = false;
                            break;
                        }
                    }
                    // all task executed
                    if (allComplete && getOnSuccess() != null) {
                        getOnSuccess().run();
                    }

                } else {
                    // task execute failed
                    if (getOnTaskFailure() != null) {
                        getOnTaskFailure().onComplete(task);
                    }
                }
            };

            TerminalCompiler.compile(config.getProject(), task.getCmd());
        }
    }

    @Override
    public String render() {

        PluginTable table = new PluginTable();

        table.addHeader("name")
                .addHeader("kind")
                .addHeader("")  // for =>
                .addHeader("image")
                .addHeader("status")
                .addHeader("time")
                .addHeader("ready");


        if (this.getTasks() == null) {

            PluginTable.Row row = new PluginTable.Row();
            row.appendColumn("-", true)
                    .appendColumn("kind")
                    .appendColumn("=>")
                    .appendColumn("-")
                    .appendColumn("-")
                    .appendColumn("0s")
                    .appendColumn("-");

            table.addRow(row);

            return table.pretty();
        }

        for (Task task : getTasks()) {

            String status = PluginStatus.WAITING;
            Command cmd = task.getCmd();

            long time = 0;
            if (task.isStarted()) {
                time += ((cmd.stop <= 0 ? System.currentTimeMillis() : cmd.stop) - cmd.start);
            }

            if (cmd.getStatus() != null) {
                status = cmd.getStatus();
            } else {
                if (task.isRunning()) {
                    status = PluginStatus.COMMAND_RUNNING;
                }

                if (task.isComplete()) {
                    status = cmd.getStatus();
                }
            }

            PluginTable.Row row = new PluginTable.Row();
            row.appendColumn(task.getName(), true)
                    .appendColumn(task.getKind())
                    .appendColumn("=>")
                    .appendColumn(task.getAlias())
                    .appendColumn(status)
                    .appendColumn(TimeUnit.MILLISECONDS.toSeconds(time) + "s");

            if (PluginStatus.COMMAND_RUNNING.equals(status)) {
                row.appendColumn("🔥");
            } else if (PluginStatus.WAITING.equals(status)) {
                row.appendColumn("⏸️");
            } else if (PluginStatus.SUCCESS.equals(status)) {
                row.appendColumn("✅");
            } else {
                row.appendColumn("❗");
            }

            table.addRow(row);

        }


        return table.pretty();
    }


    private String childDirectory() {
        String path = this.config.getRootDir().getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }
}
