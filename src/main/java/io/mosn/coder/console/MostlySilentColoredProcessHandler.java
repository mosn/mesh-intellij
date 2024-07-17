package io.mosn.coder.console;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.UnixProcessManager;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author yiji@apache.org
 */
public class MostlySilentColoredProcessHandler extends ColoredProcessHandler {

    private Consumer<String> onTextAvailable;

    public MostlySilentColoredProcessHandler(@NotNull GeneralCommandLine commandLine)
            throws ExecutionException {
        this(commandLine, null);
    }

    public MostlySilentColoredProcessHandler(@NotNull GeneralCommandLine commandLine, Consumer<String> onTextAvailable)
            throws ExecutionException {
        super(commandLine);
        this.onTextAvailable = onTextAvailable;
    }

    @NotNull
    @Override
    protected BaseOutputReader.Options readerOptions() {
        return BaseOutputReader.Options.forMostlySilentProcess();
    }

    @Override
    protected void doDestroyProcess() {
        final Process process = getProcess();
        if (SystemInfo.isUnix && shouldDestroyProcessRecursively() && processCanBeKilledByOS(process)) {
            final boolean result = UnixProcessManager.sendSigIntToProcessTree(process);
            if (!result) {
                // sendEvent("process", "process kill failed");
                super.doDestroyProcess();
            }
        } else {
            super.doDestroyProcess();
        }
    }

    @Override
    public void coloredTextAvailable(@NotNull String text, @NotNull Key outputType) {
        super.coloredTextAvailable(text, outputType);

        if (onTextAvailable != null) {
            onTextAvailable.accept(text);
        }
    }
}
