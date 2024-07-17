package io.mosn.coder.intellij.util;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * @author yiji@apache.org
 */
public class FileReader {

    protected static final Logger LOG = Logger.getLogger(FileReader.class.getName());

    public static String plainText(String path) {
        byte[] content = plainBytes(path);
        return content == null ? null : new String(content);
    }

    public static byte[] plainBytes(String path) {
        byte[] content = findFromClassLoader(path);
        if (content == null) {
            content = findFromClass(path);
        }
        return content;
    }

    public static byte[] findFromClassLoader(String path) {

        try {

            final PluginId pluginId = PluginId.getId("io.mosn.coder.intellij");
            final IdeaPluginDescriptor pluginDescriptor = PluginManagerCore.getPlugin(pluginId);
            if (pluginDescriptor == null) {
                return null;
            }

            final ClassLoader classLoader = pluginDescriptor.getPluginClassLoader();

            String search = path;
            if (search.startsWith("/") && search.length() > 1) {
                search = search.substring(1);
            }

            InputStream resource = classLoader.getResourceAsStream(search);

            if (resource != null) {
                try (BufferedInputStream input = new BufferedInputStream(resource)) {
                    byte[] content = input.readAllBytes();
                    return content;
                } catch (Exception ignored) {
                    LOG.warning("Failed to read file '" + search + "' from classloader '" + classLoader.getName() + "'" + " err: " + ignored);
                }
            }
        } catch (Throwable ignored) {

        }

        return null;
    }

    public static byte[] findFromClass(String path) {
        // try read from current class resource
        InputStream resource = FileReader.class.getResourceAsStream(path);
        if (resource != null) {
            try (BufferedInputStream input = new BufferedInputStream(resource)) {
                byte[] content = input.readAllBytes();
                return content;
            } catch (Exception ignored) {
                LOG.warning("Failed to read file '" + path + "'" + " err: " + ignored);
            }
        }

        return null;
    }

}
