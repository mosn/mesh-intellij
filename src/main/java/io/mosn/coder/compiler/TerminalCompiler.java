package io.mosn.coder.compiler;

import io.mosn.coder.plugin.model.PluginBundle;
import io.mosn.coder.task.MiniMeshConfig;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author yiji@apache.org
 */
public class TerminalCompiler extends AbstractCompiler {

    public static final String OS_NAME = System.getProperty("os.name");
    private static final String _OS_NAME = OS_NAME.toLowerCase(Locale.ENGLISH);
    public static final boolean isWindows = _OS_NAME.startsWith("windows");

    public static final String LOCAL_BIN = "/usr/local/bin";
    public static final String BREW_BIN = "/opt/homebrew/bin";

    private static final AtomicReference<CompletableFuture<Map<String, String>>> ourEnvGetter = new AtomicReference<>();

    public static @NotNull Map<String, String> getEnvironmentMap() {
        CompletableFuture<Map<String, String>> getter = ourEnvGetter.get();
        if (getter == null) {
            getter = CompletableFuture.completedFuture(System.getenv());
            if (!ourEnvGetter.compareAndSet(null, getter)) {
                getter = ourEnvGetter.get();
            }
        }
        try {
            return getter.join();
        } catch (Throwable t) {
            // unknown state; is not expected to happen
            throw new AssertionError(t);
        }
    }

    static {
        // fill outer env
        getEnvironmentMap();
    }

    /**
     * terminal compiler
     */
    public static void compile(String project, PluginBundle.Plugin plugin) {

        pool.execute(() -> {

            for (Command command : plugin.getCommands()) {

                if (command.fastQuit) continue;

                runCommand(project, command);

            }

        });
    }

    public static void compile(String project, Command command) {

        if (command != null) {
            pool.execute(() -> {
                runCommand( project, command);
            });
        }
    }

    public static void compile(MiniMeshConfig config, String project, Command command) {

        if (command != null) {
            pool.execute(() -> {
                runCommand(config, project, command);
            });
        }
    }

    public static boolean runCommand(String project, Command command) {
        return runCommand(null, project, command);
    }

    public static boolean runCommand(MiniMeshConfig config, String project, Command command) {

        int exitCode = 0;
        boolean quitNotified = false;

        try {

            command.setRunningThread(Thread.currentThread());

            command.start = System.currentTimeMillis();

            String exePath = command.exec.get(0);
            //String systemPath = System.getenv("PATH");
            String envPath = getEnvironmentMap().get("PATH");

            if (exePath.indexOf(File.separatorChar) == -1) {

//                System.out.println("sys path=>" + systemPath);
//                System.out.println("env path=>" + envPath);

                if (!isWindows) {

                    if (!envPath.contains(LOCAL_BIN)) {
                        File localBin = new File(LOCAL_BIN);
                        if (localBin.exists() && localBin.isDirectory()) {
                            envPath += ":" + LOCAL_BIN;
                        }
                    }

                    if (!envPath.contains(BREW_BIN)) {
                        File localBin = new File(BREW_BIN);
                        if (localBin.exists() && localBin.isDirectory()) {
                            envPath += ":" + BREW_BIN;
                        }
                    }
                }

                if (config != null) {
                    String path = config.getDefaults().get("mesh.runtime.path.include");
                    StringBuilder builder = new StringBuilder();
                    if (path != null && path.length() > 0) {
                        for (String p : path.split(",")){
                            if (!envPath.contains(p)) {
                                File localBin = new File(p);
                                if (localBin.exists() && localBin.isDirectory()) {
                                    builder.append(":").append(p);
                                }
                            }
                        }

                        envPath += builder.toString();
                    }
                }


                File exeFile = findInPath(exePath, envPath, null);
                if (exeFile != null) {
                    exePath = exeFile.getPath();
                    command.exec.set(0, exePath);

//                    System.out.println("exec file path=>" + exeFile);
                }

            }

            ProcessBuilder processBuilder = new ProcessBuilder(command.exec);
            processBuilder.environment().put("PATH", envPath);
            processBuilder.directory(new File(project));


            Process process = processBuilder.start();

            List<String> output = new CopyOnWriteArrayList<>();
            command.output = output;

            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.add(line);
                }
            }

            List<String> failedOutput = new CopyOnWriteArrayList<>();
            try (BufferedReader reader =
                         new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    failedOutput.add(line);
                }
            }
            command.failedOutput = failedOutput;

            exitCode = process.waitFor();

            if (exitCode != 0) {

                try (BufferedReader reader =
                             new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.add(line);
                    }
                }

            }

            command.stop = System.currentTimeMillis();

            if (command.getCallback() != null) {
                command.getCallback().terminated(exitCode);
                quitNotified = true;
            }
        } catch (Exception ex) {
            command.stop = System.currentTimeMillis();

            if (!quitNotified) {
                if (command.getCallback() != null) {
                    command.getCallback().terminated(exitCode);
                }
            }

            // dump stack trace
            ex.printStackTrace();

            if (command.isFatalQuit()) {
                System.exit(1);
            }

            return false;
        }

        return true;
    }

    public static void submit(Runnable runnable) {
        if (runnable != null) {
            pool.execute(runnable);
        }
    }

    public static File findInPath(String fileBaseName, String pathVariableValue, FileFilter filter) {
        List<File> exeFiles = findExeFilesInPath(filter, pathVariableValue, fileBaseName);
        return getFirstItem(exeFiles);
    }

    public static <T> T getFirstItem(List<? extends T> items) {
        return items == null || items.isEmpty() ? null : items.get(0);
    }

    private static List<File> findExeFilesInPath(FileFilter filter,
                                                 String pathEnvVarValue,
                                                 String... fileBaseNames) {
        if (pathEnvVarValue == null) {
            return Collections.emptyList();
        }
        List<File> result = new ArrayList<>();
        List<String> dirPaths = getPathDirs(pathEnvVarValue);
        for (String dirPath : dirPaths) {
            File dir = new File(dirPath);
            if (dir.isAbsolute() && dir.isDirectory()) {
                for (String fileBaseName : fileBaseNames) {
                    File exeFile = new File(dir, fileBaseName);
                    if (exeFile.isFile() && exeFile.canExecute()) {
                        if (filter == null || filter.accept(exeFile)) {
                            result.add(exeFile);
                            return result;
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<String> getPathDirs(String pathEnvVarValue) {
        return split(pathEnvVarValue, File.pathSeparator, true, true);
    }

    public static List<String> split(String s, String separator, boolean excludeSeparator, boolean excludeEmptyStrings) {
        //noinspection unchecked
        return (List) split((CharSequence) s, separator, excludeSeparator, excludeEmptyStrings);
    }

    public static List<CharSequence> split(CharSequence s, CharSequence separator, boolean excludeSeparator, boolean excludeEmptyStrings) {
        if (separator.length() == 0) {
            return Collections.singletonList(s);
        }
        List<CharSequence> result = new ArrayList<>();
        int pos = 0;
        while (true) {
            int index = indexOf(s, separator, pos);
            if (index == -1) break;
            final int nextPos = index + separator.length();
            CharSequence token = s.subSequence(pos, excludeSeparator ? index : nextPos);
            if (token.length() != 0 || !excludeEmptyStrings) {
                result.add(token);
            }
            pos = nextPos;
        }
        if (pos < s.length() || !excludeEmptyStrings && pos == s.length()) {
            result.add(s.subSequence(pos, s.length()));
        }
        return result;
    }

    public static int indexOf(CharSequence sequence, CharSequence infix, int start) {
        return indexOf(sequence, infix, start, sequence.length());
    }

    public static int indexOf(CharSequence sequence, CharSequence infix, int start, int end) {
        for (int i = start; i <= end - infix.length(); i++) {
            if (startsWith(sequence, i, infix)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean startsWith(CharSequence text, int startIndex, CharSequence prefix) {
        int tl = text.length();
        if (startIndex < 0 || startIndex > tl) {
            throw new IllegalArgumentException("Index is out of bounds: " + startIndex + ", length: " + tl);
        }
        int l1 = tl - startIndex;
        int l2 = prefix.length();
        if (l1 < l2) return false;

        for (int i = 0; i < l2; i++) {
            if (text.charAt(i + startIndex) != prefix.charAt(i)) return false;
        }
        return true;
    }

}