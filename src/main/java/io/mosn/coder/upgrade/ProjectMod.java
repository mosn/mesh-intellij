package io.mosn.coder.upgrade;

import io.mosn.coder.intellij.util.CodeBuilder;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public class ProjectMod {

    protected static final Logger LOG = Logger.getLogger(ProjectMod.class.getName());

    private String projectDir;

    private String modFile;

    private String module;

    private String goVersion;

    private String api;

    private String pkg;

    private boolean shouldFlush;

    private boolean readAlready;

    /**
     * flush buffer
     */
    private CodeBuilder buffer;

    private LinkedList<Line> required = new LinkedList<>();

    private LinkedList<Line[]> replaced = new LinkedList<>();

    public String getApi() {
        return api;
    }

    public String getPkg() {
        return pkg;
    }

    public ProjectMod(String projectDir, String modFile) {
        this.projectDir = projectDir;
        this.modFile = modFile;
    }

    /**
     * merge projectMod to current.
     */
    public ProjectMod merge(ProjectMod projectMod) {
        this.readFile();
        projectMod.readFile();

        /**
         * create required and replace map
         */

        Map<String, Line> requiredMap = new HashMap<>();
        Map<String, Line[]> replacedMap = new HashMap<>();

        for (Line require : projectMod.required) {
            requiredMap.put(require.repo, require);
        }

        for (Line[] replace : projectMod.replaced) {
            replacedMap.put(replace[0].repo, replace);
        }

        List<Line[]> appendReplace = new ArrayList<>();

        for (Line require : this.required) {
            Line rep = requiredMap.get(require.repo);
            if (rep != null) {
                require.setVersion(rep.getVersion());
            }

            /**
             * runtime use replace, plugin not use yet:
             * eg:
             * plugin:  require  abc.efg 1.0
             * remote:  require  abc.efg 1.0
             *          replace  abc.efg => abc.efg 2.0
             */
            Line[] needReplace = replacedMap.get(require.repo);
            if (needReplace != null) {
                boolean already = false;
                for (Line[] lp : this.replaced) {
                    if (lp[0].getRepo().equals(needReplace[0].getRepo())) {
                        already = true;
                        break;
                    }
                }

                if (!already) {
                    appendReplace.add(needReplace);
                }
            }
        }

        /**
         * append golang.org/x
         */
        for (Line[] repo : projectMod.getReplaced()) {
            boolean already = false;
            if (repo[0].getRepo() != null
                    && repo[0].getRepo().startsWith("golang.org/x")) {
                for (Line[] lp : this.replaced) {
                    if (lp[0].getRepo().equals(repo[0].getRepo())) {
                        already = true;
                        break;
                    }
                }

                if (!already) {
                    appendReplace.add(repo);
                }
            }
        }

        for (Line[] replace : this.replaced) {

            Line line = replace[0];

            /**
             * set version same with proj mod
             */
            Line rep = requiredMap.get(line.repo);
            if (rep != null) {
                replace[1].setRepo(rep.getRepo());
                replace[1].setVersion(rep.getVersion());
            }


            /**
             * old replace => new replace mod
             */
            Line[] repo = replacedMap.get(line.repo);
            if (repo != null) {
                replace[1].setRepo(repo[1].getRepo());
                replace[1].setVersion(repo[1].getVersion());
            }
        }

        if (projectMod.api != null) {
            this.api = projectMod.api;
        }

        if (projectMod.pkg != null) {
            this.pkg = projectMod.pkg;
        }

        if (projectMod.goVersion != null) {
            this.goVersion = projectMod.goVersion;
        }

        prepareFlush(appendReplace);

        return this;

    }

    public void prepareFlush(List<Line[]> appendReplace) {
        /**
         * dump go.mod
         */

        CodeBuilder buffer = new CodeBuilder(new StringBuilder());

        buffer.append(this.module)
                .line()
                .append(this.goVersion)
                .line()
                .append("require (");


        /**
         * split redirect
         */
        List<Line> required = new ArrayList<>();
        List<Line> redirect = new ArrayList<>();

        for (Line require : this.required) {
            if (require.isIndirect()) {
                redirect.add(require);
            } else {
                required.add(require);
            }
        }

        for (Line require : required) {
            buffer.with("\t").with(require.repo).with(" ").append(require.getVersion());
        }

        buffer.append(")");


        if (!redirect.isEmpty()) {
            buffer.line();

            buffer.append("require (");
            for (Line require : redirect) {
                buffer.with("\t").with(require.repo).with(" ").with(require.getVersion()).append(" // indirect");
            }
            buffer.append(")");
        }

        if (!this.replaced.isEmpty()) {
            buffer.line()
                    .append("replace (");

            for (Line[] replace : this.replaced) {
                buffer.with("\t")
                        .with(replace[0].getRepo())
                        .with(" => ")
                        .with(replace[1].getRepo());

                if (replace[1].getVersion() != null) {
                    buffer.with(" ").append(replace[1].getVersion());
                }

            }

            if (appendReplace != null) {
                // appendReplace
                for (Line[] replace : appendReplace) {
                    buffer.with("\t")
                            .with(replace[0].getRepo())
                            .with(" => ")
                            .with(replace[1].getRepo());

                    if (replace[1].getVersion() != null) {
                        buffer.with(" ").append(replace[1].getVersion());
                    }

                }
            }


            buffer.append(")");
        }

        this.buffer = buffer;
        this.shouldFlush = true;
    }

    public void flush() {
        if (this.shouldFlush && this.buffer != null) {
            try (FileOutputStream out =
                         new FileOutputStream(new File(this.projectDir, this.modFile))) {
                out.write(buffer.toString().getBytes());
                out.flush();
            } catch (Exception e) {
                throw new RuntimeException("failed to update go.mod", e);
            }
        }
    }

    public String getBufferText() {
        if (this.buffer == null) {
            return "";
        }

        return this.buffer.toString();
    }

    public boolean readFile() {
        try {
            if (readAlready) return true;

            return this.readFile(new FileInputStream(new File(this.projectDir, this.modFile)));
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean validFile(){
        File file = new File(this.projectDir, this.modFile);
        return file.exists();
    }

    public boolean readFile(InputStream inputStream) {

        if (readAlready) return true;

        this.readAlready = true;

        this.required.clear();
        this.replaced.clear();

        this.module = null;
        this.goVersion = null;

        File file = new File(this.projectDir, this.modFile);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {

            String line;

            boolean isRequired = false;
            boolean isReplaced = false;

            do {
                line = reader.readLine();
                if (line == null) break;

                line = line.trim();
                if (line.isEmpty()) continue;

                if (this.module == null && line.startsWith("module")) {
                    this.module = line;
                }

                if (this.goVersion == null && line.startsWith("go")) {
                    this.goVersion = line;
                }

                if (line.startsWith("require")) {
                    isRequired = true;
                    if (line.equals("require (")) continue;
                }

                if (isRequired && line.startsWith(")")) {
                    isRequired = false;
                }

                if (isRequired) {

                    /**
                     * single require we should set isRequired to false
                     */
                    if (line.startsWith("require") && !line.contains("(")) {
                        isRequired = false;
                    }

                    boolean indirect = false;
                    int index = line.indexOf("//");
                    if (index > 0) {
                        indirect = line.endsWith("indirect");
                        line = line.substring(0, index);
                    }
                    String[] items = line.trim().split(" ");
                    if (items.length >= 2) {
                        Line req = new Line("require".equals(items[0]) ? items[1] : items[0], items[items.length - 1]);
                        req.setIndirect(indirect);

                        this.required.add(req);

                        if ("mosn.io/api".equals(req.repo)) {
                            this.api = items[items.length - 1];
                        } else if ("mosn.io/pkg".equals(req.repo)) {
                            this.pkg = items[items.length - 1];
                        }
                    }
                }

                if (line.startsWith("replace")) {
                    isReplaced = true;
                }

                if (isReplaced && line.startsWith(")")) {
                    isReplaced = false;
                }

                if (isReplaced) {

                    int index = line.indexOf("//");
                    if (index > 0) {
                        line = line.substring(0, index);
                    }

                    /**
                     * starts with replace and repo
                     */
                    index = line.indexOf("=>");
                    if (index > 0) {

                        String[] items = line.split("=>");

                        String old = items[0];
                        String replace = items[1];

                        Line[] lines = new Line[2];

                        String[] oldItems = old.trim().split(" ");
                        lines[0] = new Line("replace".equals(oldItems[0]) ? oldItems[1] : oldItems[0], null);

                        String[] replaceItems = replace.trim().split(" ");
                        if (replaceItems.length >= 2) {
                            lines[1] = new Line(replaceItems[0], replaceItems[replaceItems.length - 1]);

                            if ("mosn.io/api".equals(items[0])) {
                                this.api = replaceItems[replaceItems.length - 1];
                            } else if ("mosn.io/pkg".equals(items[0])) {
                                this.pkg = replaceItems[replaceItems.length - 1];
                            }
                        }

                        this.replaced.add(lines);
                    }
                }

            } while (line != null);

        } catch (Exception e) {
            LOG.warning("read file '" + file.getAbsolutePath() + "' failed" + " err: " + e);

            return false;
        }

        return true;
    }

    public LinkedList<Line> getRequired() {
        return required;
    }

    public LinkedList<Line[]> getReplaced() {
        return replaced;
    }

    public String getGoVersion() {
        return goVersion;
    }

    public void setGoVersion(String goVersion) {
        this.goVersion = goVersion;
    }

    public static class Line {

        private String repo;

        private String version;

        private boolean indirect;

        public Line(String repo, String version) {
            this.repo = repo;
            this.version = version;
        }

        public String getRepo() {
            return repo;
        }

        public void setRepo(String repo) {
            this.repo = repo;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public boolean isIndirect() {
            return indirect;
        }

        public void setIndirect(boolean indirect) {
            this.indirect = indirect;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Line line = (Line) o;
            return repo.equals(line.repo) && Objects.equals(version, line.version);
        }

        @Override
        public int hashCode() {
            return Objects.hash(repo, version);
        }
    }
}
