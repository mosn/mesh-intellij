package io.mosn.coder.task;

import b.h.F;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MiniMeshConfig {

    // expect ~/.minimesh
    private String project;

    // expect ~/.minimesh/mysql/data
    private String mysql;

    private File rootDir;

    private File property;

    private String cpu;

    private String memory;

    private String disk;

    private String k8sVersion;

    private boolean onlyRegistry;

    private boolean debug;

    private boolean keepStart;

    private Map<String, String> defaults = new HashMap<>();

    private Map<String, String> deploy = new HashMap<>();

    private File logFile = null;

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;

        this.logFile = new File(project, "default.log");
        if (this.logFile.exists()) {
            // remove first
            this.logFile.delete();
        }

        File intellijLog = new File(project, "mosn-intellij.log");
        if (intellijLog.exists()) {
            // remove first
            intellijLog.delete();
        }
    }

    public File getRootDir() {
        return rootDir;
    }

    public void setRootDir(File rootDir) {
        this.rootDir = rootDir;
    }

    public String getCpu() {
        return cpu;
    }

    public void setCpu(String cpu) {
        this.cpu = cpu;
    }

    public String getMemory() {
        return memory;
    }

    public void setMemory(String memory) {
        this.memory = memory;
    }

    public String getDisk() {
        return disk;
    }

    public void setDisk(String disk) {
        this.disk = disk;
    }

    public File getProperty() {
        return property;
    }

    public void setProperty(File property) {
        this.property = property;
    }

    public boolean isOnlyRegistry() {
        return onlyRegistry;
    }

    public void setOnlyRegistry(boolean onlyRegistry) {
        this.onlyRegistry = onlyRegistry;
    }

    public String getK8sVersion() {
        return k8sVersion;
    }

    public void setK8sVersion(String k8sVersion) {
        this.k8sVersion = k8sVersion;
    }

    public Map<String, String> getDefaults() {
        return defaults;
    }

    public boolean isDebug() {
        return debug;
    }

    public boolean isKeepStart(){
        return keepStart;
    }

    public void setKeepStart(boolean keepStart){
        this.keepStart = keepStart;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setDefaults(Map<String, String> defaults) {
        this.defaults = defaults;
    }

    public String getMysql() {
        return mysql;
    }

    public void setMysql(String mysql) {
        this.mysql = mysql;
    }

    public synchronized void appendLog(String message) {
        if (this.logFile == null) {
            String userDir = System.getProperty("user.home");
            File miniDir = new File(userDir, ".minimesh");

            this.logFile = new File(miniDir, "default.log");
        }

        try (BufferedWriter br = new BufferedWriter(new FileWriter(this.logFile, true))) {
            br.write(message);
        } catch (IOException ignored) {
        }
    }

    public File getLogFile() {
        return logFile;
    }

    public Map<String, String> getDeploy() {
        return deploy;
    }

    public boolean deployMeshServer() {
        return getDeploy().get("meshserver") != null;
    }

    public boolean deployOperator() {
        return getDeploy().get("operator") != null;
    }

    public boolean deployMosn() {
        return getDeploy().get("mosn") != null;
    }

    public boolean deployMysql() {
        return getDeploy().get("mysql") != null;
    }

    public boolean deployNacos() {
        return getDeploy().get("nacos") != null;
    }

    public boolean deployNacosOnly() {
        return getDeploy().size() == 1 && deployNacos();
    }

    public boolean deployCitadel() {
        return getDeploy().get("citadel") != null;
    }

    public boolean deployDubbo() {
        return getDeploy().get("dubbo") != null;
    }

    public boolean deployPrometheus() {
        return getDeploy().get("prometheus") != null;
    }


}
