package io.mosn.coder.console;

import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.MessageView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static io.mosn.coder.console.CustomCommandUtil.CUSTOM_CONSOLE;

/**
 * @author yiji@apache.org
 */
public class PluginConsole {

    // static final Key<PluginConsole> KEY = Key.create(CUSTOM_CONSOLE);

    static final Map<String, Key<PluginConsole>> keys = new HashMap<>();

    @NotNull
    final ConsoleView view;
    @NotNull
    final Content content;
    @NotNull
    final Project project;


    @Nullable
    private Runnable cancelProcessSubscription;

    private PluginConsole(@NotNull ConsoleView view, @NotNull Content content, @NotNull Project project) {
        this.view = view;
        this.content = content;
        this.project = project;
    }

    @NotNull
    public static synchronized PluginConsole findOrCreate(@NotNull Project project, String key) {

        Key<PluginConsole> cacheKey = keys.get(key);
        if (cacheKey == null) {
            cacheKey = Key.create(key);
            keys.put(key, cacheKey);
        }

        for (Content content : MessageView.SERVICE.getInstance(project).getContentManager().getContents()) {
            final PluginConsole console = content.getUserData(cacheKey);
            if (console != null) {
                assert (project == console.project);
                return console;
            }
        }
        final PluginConsole console = create(project, key);
        console.content.putUserData(cacheKey, console);

        // add content
        MessageView.SERVICE.getInstance(project).getContentManager().addContent(console.content);
        return console;
    }

    @NotNull
    private static PluginConsole create(@NotNull Project project, String key) {
        final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        builder.setViewer(true);

        final ConsoleView view = builder.getConsole();

        final SimpleToolWindowPanel panel = new SimpleToolWindowPanel(false, true);
        panel.setContent(view.getComponent());

        final Content content = ContentFactory.SERVICE.getInstance().createContent(panel.getComponent(), key, true);

        Disposer.register(content, () -> {
            /**
             * remove cache map
             */
            keys.remove(key);
            content.dispose();
        });

        return new PluginConsole(view, content, project);
    }

    /**
     * Starts displaying the output of a different process.
     */
    void watchProcess(@NotNull ColoredProcessHandler process) {
        if (cancelProcessSubscription != null) {
            cancelProcessSubscription.run();
            cancelProcessSubscription = null;
        }

        view.clear();
        view.attachToProcess(process);

        // Print exit code.
        final ProcessAdapter listener = new ProcessAdapter() {
            @Override
            public void processTerminated(final ProcessEvent event) {
                view.print(
                        "Process finished with exit code " + event.getExitCode(),
                        ConsoleViewContentType.SYSTEM_OUTPUT);
            }
        };
        process.addProcessListener(listener);
        cancelProcessSubscription = () -> process.removeProcessListener(listener);
    }

    /**
     * Moves this console to the end of the tool window's tab list, selects it, and shows the tool window.
     */
    public void bringToFront() {
        // Move the tab to be last and select it.
        final MessageView messageView = MessageView.SERVICE.getInstance(project);
        final ContentManager contentManager = messageView.getContentManager();
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);

        // Show the panel.
        final ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId.MESSAGES_WINDOW);
        if (toolWindow != null) {
            toolWindow.activate(null, true);
        }
    }

    /**
     * Shows a process's output on the appropriate console. (Asynchronous.)
     */
    public static void displayProcessLater(@NotNull ColoredProcessHandler process,
                                           @NotNull Project project, String key,
                                           Runnable onReady) {

        // Getting a MessageView has to happen on the UI thread.
        ApplicationManager.getApplication().invokeLater(() -> {
            final MessageView messageView = MessageView.SERVICE.getInstance(project);
            messageView.runWhenInitialized(() -> {
                final PluginConsole console = findOrCreate(project, key);
                console.watchProcess(process);
                console.bringToFront();
                if (onReady != null) {
                    onReady.run();
                }
            });
        });
    }

    public static void displayMessage(@NotNull Project project, String key, @NotNull String message) {
        displayMessage(project, key, message, false);
    }

    public static void displayMessage(@NotNull Project project, String key, @NotNull String message, boolean clearContent) {
        // Getting a MessageView has to happen on the UI thread.
        ApplicationManager.getApplication().invokeLater(() -> {
            final MessageView messageView = MessageView.SERVICE.getInstance(project);
            messageView.runWhenInitialized(() -> {
                final PluginConsole console = findOrCreate(project, key);
                if (clearContent) {
                    console.view.clear();
                }
                console.view.print(message + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
                console.bringToFront();

                // update scroll position
                console.getView().requestScrollingToEnd();
            });
        });
    }

    public ConsoleView getView() {
        return view;
    }
}
