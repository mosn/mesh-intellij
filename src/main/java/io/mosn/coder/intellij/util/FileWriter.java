package io.mosn.coder.intellij.util;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.template.ReplaceAction;
import io.mosn.coder.intellij.template.TextLine;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * @author yiji@apache.org
 */
public class FileWriter {

    protected static final Logger LOG = Logger.getLogger(FileWriter.class.getName());

    public static void writeAndFlush(@NotNull VirtualFile baseDir, List<Source> code) {

        if (baseDir == null || code == null || code.isEmpty()) {
            return;
        }

        try {
            WriteAction.compute(() -> {
                for (Source source : code) {
                    try {
                        VirtualFile path = VfsUtil.createDirectoryIfMissing(baseDir, source.getPath());

                        // support for create directory
                        if (source.getName() == null
                                || source.getName().length() <= 0
                                || (source.getContent() == null && source.getRawBytes() == null)) {
                            continue;
                        }

                        VirtualFile file = VfsUtil.refreshAndFindChild(path, source.getName());
                        if (file != null) {
                            /**
                             * file exist already, skip now.
                             */
                            continue;
                        }

                        file = path.createChildData(source, source.getName());
                        if (source.getContent() != null) {
                            VfsUtil.saveText(file, source.getContent());
                        } else {
                            // write binary raw bytes.
                            try (OutputStream stream = file.getOutputStream(file)) {
                                stream.write(source.getRawBytes());
                            }
                        }
                    } catch (IOException e) {
                        LOG.warning("write file '" + source.getPath() + "/" + source.getName() + "' failed" + " err: " + e);
                    }
                }
                return null;
            });
        } catch (Exception ignored) {
        }
    }

    public static void replaceAndFlush(@NotNull VirtualFile baseDir, Source source, Predicate<String> predicate, ReplaceAction action, PluginOption... options) {

        try {
            WriteAction.compute(() -> {
                try {
                    VirtualFile path = VfsUtil.createDirectoryIfMissing(baseDir, source.getPath());

                    // support for create directory
                    if (source.getName() == null
                            || source.getName().length() <= 0
                            || (source.getContent() == null && source.getRawBytes() == null)) {
                        return null;
                    }

                    VirtualFile file = VfsUtil.refreshAndFindChild(path, source.getName());
                    if (file == null) {
                        /**
                         * file not exist, skip now.
                         */
                        return null;
                    }

                    boolean terminate = false;

                    StringBuilder buffer = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {

                        String line;

                        do {
                            line = reader.readLine();
                            if (!terminate && line != null && predicate.test(line)) {
                                TextLine textLine = action.replace(line, options);
                                if (textLine.isTerminate()) {
                                    // stop next action notify
                                    terminate = true;

                                    // append replaced line
                                    if (textLine.text() != null) {
                                        line = textLine.text();
                                    }
                                }
                            }

                            /**
                             * append to buffer, when complete replaced file will be refreshed
                             */
                            if (line != null) {
                                buffer.append(line).append("\n");
                            }

                        } while (line != null);

                    } catch (Exception e) {
                        LOG.warning("read file '" + source.getPath() + "/" + source.getName() + "' failed" + " err: " + e);
                    }

                    if (buffer.length() > 0) {
                        VfsUtil.saveText(file, buffer.toString());
                    }
                } catch (IOException e) {
                    LOG.warning("replace file '" + source.getPath() + "/" + source.getName() + "' failed" + " err: " + e);
                }
                return null;
            });
        } catch (Exception ignored) {
        }
    }

    public static void writeAndFlush(@NotNull File baseDir, List<Source> code) {

        if (baseDir == null || code == null || code.isEmpty()) {
            return;
        }

        for (Source source : code) {
            try {

                if (baseDir != null && !baseDir.exists()) {
                    // create parent dir if required.
                    baseDir.mkdirs();
                }

                File path = new File(baseDir, source.getPath());
                if (path != null && !path.exists()) {
                    path.mkdirs();
                }

                // support for create directory
                if (source.getName() == null
                        || source.getName().length() <= 0
                        || (source.getContent() == null && source.getRawBytes() == null)) {
                    continue;
                }

                File file = new File(path, source.getName());
                if (file != null && file.exists()) {
                    /**
                     * file exist already, skip now.
                     */
                    continue;
                }

                if (file.createNewFile()) {
                    byte[] content = source.getContent() != null ? source.getContent().getBytes() : source.getRawBytes();
                    try (OutputStream stream = new FileOutputStream(file)) {
                        stream.write(content);
                    }
                }

            } catch (IOException e) {
                LOG.warning("write file '" + source.getPath() + "/" + source.getName() + "' failed" + " err: " + e);
            }
        }
    }

    public static void replaceAndFlush(@NotNull File baseDir, Source source, Predicate<String> predicate, ReplaceAction action, PluginOption... options) {

        try {

            if (baseDir != null && !baseDir.exists()) {
                // create parent dir if required.
                baseDir.mkdirs();
            }

            File path = new File(baseDir, source.getPath());

            // support for create directory
            if (source.getName() == null
                    || source.getName().length() <= 0
                    || (source.getContent() == null && source.getRawBytes() == null)) {
                return;
            }

            File file = new File(path, source.getName());
            if (file == null || !file.exists()) {
                /**
                 * file exist already, skip now.
                 */
                return;
            }

            boolean terminate = false;

            StringBuilder buffer = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {

                String line;

                do {
                    line = reader.readLine();
                    if (!terminate && line != null && predicate.test(line)) {
                        TextLine textLine = action.replace(line, options);
                        if (textLine.isTerminate()) {
                            // stop next action notify
                            terminate = true;

                            // append replaced line
                            if (textLine.text() != null) {
                                line = textLine.text();
                            }
                        }
                    }

                    /**
                     * append to buffer, when complete replaced file will be refreshed
                     */
                    if (line != null) {
                        buffer.append(line).append("\n");
                    }

                } while (line != null);

            } catch (Exception e) {
                LOG.warning("read file '" + source.getPath() + "/" + source.getName() + "' failed" + " err: " + e);
            }

            if (buffer.length() > 0) {
                byte[] content = buffer.toString().getBytes();
                try (OutputStream stream = new FileOutputStream(file)) {
                    stream.write(content);
                }
            }
        } catch (IOException e) {
            LOG.warning("replace file '" + source.getPath() + "/" + source.getName() + "' failed" + " err: " + e);
        }
    }

}
