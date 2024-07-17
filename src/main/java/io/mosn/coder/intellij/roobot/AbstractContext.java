package io.mosn.coder.intellij.roobot;

import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.option.PluginOption;
import io.mosn.coder.intellij.option.Source;
import io.mosn.coder.intellij.template.ReplaceAction;
import io.mosn.coder.intellij.util.FileWriter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author yiji@apache.org
 */
public class AbstractContext implements PluginContext {

    protected PluginOption option;

    protected VirtualFile dir;

    protected File cliDir;

    protected String module;

    public AbstractContext(PluginOption option, File dir) {
        this.option = option;
        this.option.setContext(this);
        this.cliDir = dir;
    }

    public AbstractContext(PluginOption option, VirtualFile dir) {
        this.option = option;
        this.option.setContext(this);
        this.dir = dir;
    }

    @Override
    public void createTemplateCode() {
    }

    @Override
    public void destroy() {

    }

    public String getModule() {

        if (module != null && module.length() > 0) {
            return this.module;
        }

        if (this.option != null) {
            String org = this.option.getOrganization();
            if (org != null && org.length() > 0
                    /**
                     * using exists go mod module name
                     */
                    && (findGoMod() == null || !findGoMod().exists())) {
                String project = getPath();
                if (project != null) {
                    int index = project.lastIndexOf("/");
                    if (index >= 0) {
                        if (org.endsWith("/")) {
                            this.module = org + project.substring(index + 1);
                        } else {
                            this.module = org + "/" + project.substring(index + 1);
                        }

                        return module;
                    }
                }
            }
        }

        // read from go.mod
        File mod = findGoMod();
        if (mod != null && mod.exists()) {
            try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(mod)))) {
                String line = reader.readLine();
                while (line.startsWith("#")
                        || !line.contains("module")) {
                    line = reader.readLine();
                }

                line = line.trim();
                String[] items = line.split(" ");
                if (items != null
                        && items.length == 2
                        && "module".equals(items[0])) {
                    this.module = items[1];
                }
            } catch (Exception ignored) {
            }
        }

        return module;
    }

    @Nullable
    private File findGoMod() {
        if (cliDir != null) {
            return new File(cliDir, "go.mod");
        }

        VirtualFile file = dir.findChild("go.mod");
        if (file != null) {
            return new File(file.getPath());
        }

        return null;
    }

    @NotNull
    protected @NonNls String getPath() {
        if (this.cliDir != null) {
            return this.cliDir.getPath();
        }

        return this.dir.getPath();
    }

    public void setModule(String module) {
        this.module = module;
    }

    protected void flush(List<Source> code) {
        /** flush cli code first **/
        if (cliDir != null) {
            FileWriter.writeAndFlush(cliDir, code);
            return;
        }

        /***
         * flush intellij code
         */
        if (dir != null) {
            FileWriter.writeAndFlush(dir, code);
        }
    }

    protected void replaceAndFlush(Source source, Predicate<String> predicate, ReplaceAction action, PluginOption... options) {
        /** flush cli code first **/
        if (cliDir != null) {
            FileWriter.replaceAndFlush(cliDir, source, predicate, action, options);
            return;
        }

        /***
         * flush intellij code
         */
        if (dir != null) {
            FileWriter.replaceAndFlush(dir, source, predicate, action, options);
        }
    }

    protected File findFileByRelativePath(String path) {

        if (cliDir != null) {
            return new File(cliDir, path);
        }

        if (dir != null) {
            return new File(dir.getPath(), path);
        }

        return null;
    }
}
