package io.mosn.coder.task;

import io.mosn.coder.compiler.Command;
import io.mosn.coder.plugin.model.PluginStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractRunTask implements Runnable {

    private Command cmd;

    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public AbstractRunTask(Command cmd) {
        this.cmd = cmd;
    }

    @Override
    public void run() {
        int status = 0;
        try {
            cmd.start = System.currentTimeMillis();
            doRun();

            if (cmd.getStatus() == null) {
                cmd.setStatus(PluginStatus.SUCCESS);
            }
        } catch (Exception e) {
            cmd.setStatus(PluginStatus.FAIL);
            status = -1;

            cmd.output = new CopyOnWriteArrayList<>();
            cmd.output.add(e.getMessage());

            if (e.getCause() != null) {
                cmd.output.add(e.getCause().getMessage());
            }

            String command = cmd.toString();
            logger.error(command == null ? "" : command, e);
        } finally {
            cmd.stop = System.currentTimeMillis();

            if (cmd.getCallback() != null) {
                cmd.getCallback().terminated(status);
            }
        }
    }

    public abstract void doRun() throws Exception;
}
