package io.mosn.coder.compiler;

import java.util.ArrayList;
import java.util.List;

public class Command {

    public volatile long start;

    public volatile long stop;

    public String title;

    public String shortAlias;

    volatile public String status;

    public ArrayList<String> exec;

    public boolean clearConsole;

    public CallBack callback;

    public volatile boolean fastQuit;

    public volatile List<String> output;

    public volatile List<String> failedOutput;

    private String prettyValue;

    private Thread runningThread;

    public Runnable runnable;

    protected boolean fatalQuit;


    public String getPrettyValue() {
        return prettyValue;
    }

    public List<String> getFailedOutputFirst() {
        if (failedOutput != null && !failedOutput.isEmpty()) return failedOutput;
        return output;
    }

    public List<String> getStdOutput() {
        return output;
    }

    public void setOutput(List<String> output) {
        this.output = output;
    }

    public void addOutputMessage(String message) {
        if (output == null) {
            output = new ArrayList<>();
        }
        output.add(message);
    }

    public void addOutputMessage(String message, boolean clear) {
        if (output == null) {
            output = new ArrayList<>();
        }

        if (clear) output.clear();

        output.add(message);
    }


    public void setPrettyValue(String prettyValue) {
        this.prettyValue = prettyValue;
    }

    public String getShortAlias() {
        return shortAlias;
    }

    public void setShortAlias(String shortAlias) {
        this.shortAlias = shortAlias;
    }

    public CallBack getCallback() {
        return callback;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void resetStatus() {
        setStatus(null);

        this.start = 0;
        this.stop = 0;
    }

    public String getStdout() {
        if (getFailedOutputFirst() != null) {
            StringBuilder buf = new StringBuilder();
            for (String line : getFailedOutputFirst()) {

                if (line != null && line.length() > 0) {
                    buf.append(line);

                    if (!line.endsWith("\n")) {
                        buf.append("\n");
                    }
                }
            }

            return buf.toString();
        }

        return "";
    }

    public Runnable getRunnable() {
        return runnable;
    }

    public void setRunnable(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public String toString() {

        if (this.exec != null) {
            StringBuilder buff = new StringBuilder();
            for (String cmd : this.exec) {
                if (buff.length() > 0) buff.append(" ");
                buff.append(cmd);
            }

            return buff.toString();
        }

        return this.shortAlias;
    }

    public interface CallBack {

        void terminated(int status);

    }

    public Thread getRunningThread() {
        return runningThread;
    }

    public void setRunningThread(Thread runningThread) {
        this.runningThread = runningThread;
    }

    public boolean isFatalQuit() {
        return fatalQuit;
    }

    public void setFatalQuit(boolean fatalQuit) {
        this.fatalQuit = fatalQuit;
    }
}
