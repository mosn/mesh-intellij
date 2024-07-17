package io.mosn.coder.task;

import io.mosn.coder.plugin.model.PluginStatus;

import java.util.List;

public abstract class RunStep {

    private String name;

    List<Task> tasks;

    private Runnable onSuccess;

    private TaskCallback onTaskSuccess;

    private TaskCallback onTaskFailure;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isComplete() {
        if (tasks == null) return false;

        for (Task task : tasks) {
            if (task.isStarted() && !task.isRunning()) continue;
            return false;
        }

        return true;
    }

    public boolean isStarted() {
        if (tasks == null) return false;

        for (Task task : tasks) {
            if (task.isStarted()) return true;
        }

        return false;
    }

    public boolean isRunning() {
        if (tasks == null) return false;

        for (Task task : tasks) {
            if (task.isRunning()) return true;
        }

        return false;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public abstract void execute();

    public abstract String render();


    public Runnable getOnSuccess() {
        return onSuccess;
    }

    public void setOnSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
    }

    public TaskCallback getOnTaskSuccess() {
        return onTaskSuccess;
    }

    public void setOnTaskSuccess(TaskCallback onTaskSuccess) {
        this.onTaskSuccess = onTaskSuccess;
    }

    public TaskCallback getOnTaskFailure() {
        return onTaskFailure;
    }

    public void setOnTaskFailure(TaskCallback onTaskFailure) {
        this.onTaskFailure = onTaskFailure;
    }

    public void cancel() {
        if (tasks != null) {
            for (Task task : tasks) {
                task.cancel();
            }
        }
    }

    public String getFailedOutput() {
        StringBuilder buf = new StringBuilder();
        for (Task task : tasks) {
            if (PluginStatus.FAIL.equals(task.getCmd().getStatus())) {
                buf.append(task.getOutput()).append("\n");
            }
        }

        return buf.toString();
    }
}
