package io.mosn.coder.cli.minimesh;

import com.alibaba.fastjson.JSON;
import io.mosn.coder.cli.CommandLine;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.TerminalCompiler;
import io.mosn.coder.plugin.model.PluginStatus;
import io.mosn.coder.task.MinikubeRunStep;
import io.mosn.coder.task.Task;
import io.mosn.coder.task.model.KubeStep;

import java.util.ArrayList;

@CommandLine.Command(name = "mesh",
        sortOptions = false,
        headerHeading = "@|bold,underline Usage:|@%n%n",
        synopsisHeading = "%n",
        descriptionHeading = "%n@|bold,underline Description:|@%n%n",
        parameterListHeading = "%n@|bold,underline Parameters:|@%n",
        optionListHeading = "%n@|bold,underline Options:|@%n",
        header = "deploy mesh in minikube.",
        description = "Automatically deploy mesh in minikube. \n\n" +
                "start default mini mesh cluster:\n" +
                "sofactl mesh \n\n" +
                "start mini mesh cluster contains non-default components, eg: dubbo :\n" +
                "sofactl mesh --dubbo\n\n" +
                "deploy a new component in a started cluster:\n" +
                "sofactl mesh --only=dubbo,mosn --keep-start\n\n" +
                "remove mini mesh cluster:\n" +
                "sofactl mesh -q"
)
public class CliMiniMesh extends BaseMeshCli implements Runnable {
    @Override
    public void run() {

        if (exit) {
            removeAndQuit();
        }

        String message = this.startDeployMiniMesh();
        if (message != null) {
            System.err.println(message);
            return;
        }

        waitQuit();
    }

    private Task createRemoveMinikubeCommand() {
        Task task = new Task();

        Command cmd = new Command();
        ArrayList<String> exec = new ArrayList<>();
        exec.add("minikube");
        exec.add("delete");
//        exec.add("--all=true");
//        exec.add("--purge=true");
        exec.add("-o");
        exec.add("json");

        cmd.exec = exec;

        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);

            }
        };

        return task;
    }

    private void removeAndQuit() {
        StringBuilder buf = new StringBuilder();
        Task removeKubeTask = createRemoveMinikubeCommand();

        TerminalCompiler.runCommand("/tmp", removeKubeTask.getCmd());

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
                        prefix = MinikubeRunStep.icons.get(step.getName());
                        if (prefix == null) {
                            prefix = "🔥  ";
                        }
                    }

                    buf.append(prefix).append(step.getData().getMessage()).append("\n");
                } catch (Exception ignored) {
                }
            }
        }

        wrapTextArea(buf.toString(), true);
        System.exit(0);
    }
}
