package io.mosn.coder.task;

import io.mosn.coder.compiler.AbstractCompiler;
import io.mosn.coder.compiler.TerminalCompiler;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PrettyTask {

    private List<RunStep> runSteps;

    private StepCallback onStepSuccess;

    private StepCallback onStepFailure;

    private Runnable onSuccess;

    private final AtomicInteger current = new AtomicInteger();

    private MiniMeshConfig config;

    public List<RunStep> getRunSteps() {
        return runSteps;
    }

    public void setRunSteps(List<RunStep> runSteps) {
        this.runSteps = runSteps;
    }

    public void execute() {

        int step = current.get();
        if (step < runSteps.size()) {
            RunStep next = this.runSteps.get(step);

            Runnable backupSuccess = next.getOnSuccess();
            next.setOnSuccess(() -> {
                current.incrementAndGet();

                next.setOnSuccess(backupSuccess);

                // execute impl callback
                if (backupSuccess != null) {
                    backupSuccess.run();
                }

                execute();
            });


            TaskCallback backupTaskSuccess = next.getOnTaskSuccess();
            next.setOnTaskSuccess(task -> {
                next.setOnTaskSuccess(backupTaskSuccess);

                // execute impl success callback
                if (backupTaskSuccess != null) {
                    backupTaskSuccess.onComplete(task);
                }

                // check all task complete
                boolean allComplete = true;
                for (Task _task : next.getTasks()) {
                    if (!_task.isComplete()) {
                        allComplete = false;
                        break;
                    }
                }

                if (allComplete && getOnStepSuccess() != null) {
                    getOnStepSuccess().onComplete(next);
                }


            });

            TaskCallback backupFailure = next.getOnTaskFailure();
            next.setOnTaskFailure(task -> {
                next.setOnTaskFailure(backupFailure);


                // write filed task to log
                if (config != null){
                    config.appendLog(next.getFailedOutput());
                }

                // execute impl failure callback
                if (backupFailure != null) {
                    backupFailure.onComplete(task);
                }

                if (getOnStepFailure() != null) {
                    getOnStepFailure().onComplete(next);
                }

            });

            // trigger task execute
            next.execute();
        } else {
            if (getOnSuccess() != null) {
                getOnSuccess().run();
            }
        }
    }

    public String render() {
        if (runSteps != null && !runSteps.isEmpty()) {

            StringBuilder buf = new StringBuilder();
            int stepOrder = 1;
            // append header
            for (RunStep step : runSteps) {
                if (step.isComplete()) {
                    buf.append("✅  ").append(stepOrder).append(". ").append(step.getName()).append("    ");
                } else {
                    if (!step.isStarted()) {
                        buf.append("🏄  ").append(stepOrder).append(". ").append(step.getName()).append("    ");
                    } else {
                        buf.append("🔥  ").append(stepOrder).append(". ").append(step.getName()).append("    ");
                    }

                }
                stepOrder++;
            }
            buf.append("\n\n");

            // append current
            int index = current.get();

            if (index >= runSteps.size()) {
                index = runSteps.size() - 1;
            }

            RunStep step = runSteps.get(index);

            buf.append(step.render());

            return buf.toString();
        }

        return "";
    }

    public StepCallback getOnStepSuccess() {
        return onStepSuccess;
    }

    public void setOnStepSuccess(StepCallback onStepSuccess) {
        this.onStepSuccess = onStepSuccess;
    }

    public StepCallback getOnStepFailure() {
        return onStepFailure;
    }

    public void setOnStepFailure(StepCallback onStepFailure) {
        this.onStepFailure = onStepFailure;
    }

    public Runnable getOnSuccess() {
        return onSuccess;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }


    public void cancel() {
        if (runSteps != null) {
            for (RunStep step : runSteps) {
                step.cancel();
            }
        }

        // clear thread pool tasks
        AbstractCompiler.clearTask();
    }

    public MiniMeshConfig getConfig() {
        return config;
    }

    public void setConfig(MiniMeshConfig config) {
        this.config = config;
    }
}
