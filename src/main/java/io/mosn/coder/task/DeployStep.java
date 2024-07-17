package io.mosn.coder.task;


import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.StatefulSet;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.ExecListener;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import io.fabric8.kubernetes.client.dsl.Resource;
import io.fabric8.kubernetes.client.utils.PodStatusUtil;
import io.mosn.coder.compiler.Command;
import io.mosn.coder.compiler.TerminalCompiler;
import io.mosn.coder.plugin.model.PluginStatus;
import io.mosn.coder.plugin.model.PluginTable;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class DeployStep extends RunStep {

    static final Map<String, String> icons = new HashMap<>();

    private MiniMeshConfig config;

    private AtomicInteger current = new AtomicInteger();

    private KubernetesClient client;

    private volatile boolean initialized;

    private Task waitingTask = null;

    private Task dubboClientTask = null;

    public DeployStep(MiniMeshConfig config) {
        this.config = config;
    }

    private List<Task> prepareTasks() {
        List<Task> tasks = new ArrayList<>();

        tasks.add(createPrepareResourceCommand());

        if (config.deployNacos()) {
            tasks.add(createDeployNacosCommand());
        }

        if (config.deployMysql()) {
            tasks.add(createDeployMySqlCommand());
        }

        if (config.deployMeshServer()) {
            tasks.add(createDeployMeshServerCommand());
        }

        if (config.deployOperator()) {
            tasks.add(createDeployOperatorCommand());
        }

        if (config.deployCitadel()) {
            tasks.add(createDeployCitadelCommand());
        }

        if (config.deployPrometheus()) {
            tasks.add(createDeployPrometheusCommand());
        }

        if (config.deployDubbo()) {
            tasks.add(createDeployDubboServerCommand());
            tasks.add(dubboClientTask = createDeployDubboClientCommand());
        }

        tasks.add(waitingTask = createWaitingMeshReadyCommand());

        return tasks;
    }

    private Task createPrepareResourceCommand() {
        Task task = new Task();

        task.setPrefix("🔥  ");
        task.setAlias("Copy the resources required for the installation...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {
                // copy or generate yaml resource
                String miniDir = config.getProject();
                File target = new File(miniDir, "meshserver");
                if (!target.exists()) {
                    target.mkdir();
                }

                File source = new File(config.getRootDir().getPath(), "meshserver/deployment");

                // copy mesh server yaml
                Files.walkFileTree(Path.of(source.getPath()), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.copy(file, Path.of(target.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });

                // copy nacos deployment...
                File nacosTarget = new File(miniDir, "nacos");
                if (!nacosTarget.exists()) {
                    nacosTarget.mkdir();
                }


                File nacos = new File(config.getRootDir().getPath(), "nacos/deployment");
                Files.walkFileTree(Path.of(nacos.getPath()), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.copy(file, Path.of(nacosTarget.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });


                // copy mysql deployment...
                File mysqlTarget = new File(miniDir, "mysql");
                if (!mysqlTarget.exists()) {
                    mysqlTarget.mkdir();
                }
                File mysql = new File(config.getRootDir().getPath(), "mysql/deployment");
                Files.walkFileTree(Path.of(mysql.getPath()), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.copy(file, Path.of(mysqlTarget.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });

                // copy operator deployment...
//                if (config.deployOperator()) {
                File operatorTarget = new File(miniDir, "operator");
                if (!operatorTarget.exists()) {
                    operatorTarget.mkdir();
                }
                File operator = new File(config.getRootDir().getPath(), "operator/deployment");
                Files.walkFileTree(Path.of(operator.getPath()), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.copy(file, Path.of(operatorTarget.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
//                }

                // copy citadel deployment...
//                if (config.deployCitadel()){
                File citadelTarget = new File(miniDir, "citadel");
                if (!citadelTarget.exists()) {
                    citadelTarget.mkdir();
                }
                File citadel = new File(config.getRootDir().getPath(), "citadel/deployment");
                Files.walkFileTree(Path.of(citadel.getPath()), new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.copy(file, Path.of(citadelTarget.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                        return FileVisitResult.CONTINUE;
                    }
                });
//                }


                if (config.deployDubbo()) {
                    File dubboTarget = new File(miniDir, "dubbo");
                    if (!dubboTarget.exists()) {
                        dubboTarget.mkdir();
                    }
                    File dubboServer = new File(config.getRootDir().getPath(), "example/dubbo/server/deployment");
                    Files.walkFileTree(Path.of(dubboServer.getPath()), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.copy(file, Path.of(dubboTarget.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });

                    File dubboClient = new File(config.getRootDir().getPath(), "example/dubbo/client/deployment");
                    Files.walkFileTree(Path.of(dubboClient.getPath()), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.copy(file, Path.of(dubboTarget.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }

                if (config.deployPrometheus()) {
                    // copy mysql deployment...
                    File prometheusTarget = new File(miniDir, "prometheus");
                    if (!prometheusTarget.exists()) {
                        prometheusTarget.mkdir();
                    }
                    File prometheus = new File(config.getRootDir().getPath(), "prometheus/deployment");
                    Files.walkFileTree(Path.of(prometheus.getPath()), new SimpleFileVisitor<>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.copy(file, Path.of(prometheusTarget.getPath(), file.toFile().getName()), StandardCopyOption.REPLACE_EXISTING);
                            return FileVisitResult.CONTINUE;
                        }
                    });
                }

                if (!config.isKeepStart()) {

                    Namespace ns = new NamespaceBuilder().withNewMetadata().withName("sofamesh")
                            .endMetadata().build();
                    Namespace query = client.namespaces().withName("sofamesh").get();
                    if (query == null) {
                        client.namespaces().resource(ns).create();
                    }


                    Namespace ns_proxy = new NamespaceBuilder().withNewMetadata().withName("sofamesh-proxy")
                            .endMetadata().build();
                    Namespace ns_query = client.namespaces().withName("sofamesh-proxy").get();
                    if (ns_query == null) {
                        client.namespaces().resource(ns_proxy).create();
                    }


                    createServiceAccount();

                }


                task.setPrefix("👍  ");
            }
        });

        task.setCmd(cmd);

        return task;
    }

    private Task createDeployNacosCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh");

        task.setName("nacos");
        task.setPrefix("🔥  ");
        task.setAlias("Install the nacos service registry and configuration center...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "nacos/nacos.yaml"))
                                .item();


                StatefulSet sts = client.apps().statefulSets()
                        .inNamespace("sofamesh").withName("nacos").get();
                if (sts != null) {
                    client.apps().statefulSets().inNamespace("sofamesh").withName("nacos").delete();

                    // waiting sts quit
                    while (true) {
                        sts = client.apps().statefulSets()
                                .inNamespace("sofamesh").withName("nacos").get();
                        if (sts == null) break;
                        Thread.sleep(1000);
                    }
                }

                deployment = client.apps().statefulSets().inNamespace("sofamesh").resource(deployment).createOrReplace();


                // create service
                Service service = client.services().load(new File(config.getProject(), "nacos/nacos-service.yaml"))
                        .item();

                service = client.services().inNamespace("sofamesh").resource(service).createOrReplace();

                String serviceURL = client.services().inNamespace("sofamesh").withName(service.getMetadata().getName())
                        .getURL("nacos-port");

                //System.out.println(serviceURL);

                task.setPrefix("🎉  ");
            }
        });
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createDeployMySqlCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh");

        task.setName("mysql");
        task.setPrefix("🔥  ");
        task.setAlias("Start the control plane mysql database...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "mysql/mysql.yaml"))
                                .item();

                deployment = client.apps().statefulSets().inNamespace("sofamesh").resource(deployment).createOrReplace();


                // create service
                Service service = client.services().load(new File(config.getProject(), "mysql/mysql-service.yaml"))
                        .item();

                service = client.services().inNamespace("sofamesh").resource(service).createOrReplace();

                String serviceURL = client.services().inNamespace("sofamesh").withName(service.getMetadata().getName())
                        .getURL("mysql-port");

                // waiting pod start
                task.setPrefix("😄  ");
            }
        });
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createDeployMeshServerCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh");

        task.setName("meshserver");
        task.setPrefix("🔥  ");
        task.setAlias("Deploy the mesh server console...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                for (Command run : task.getCommands()) {
                    run.runnable.run();

                    if (!PluginStatus.SUCCESS.equals(run.getStatus())) {
                        task.setPrefix("❗  ");

                        task.getCmd().setStatus(PluginStatus.FAIL);

                        if (run.getFailedOutputFirst() != null) {
                            task.getCmd().setOutput(run.getFailedOutputFirst());
                        }

                        return;
                    }
                }

                // deploy mesh server

                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "meshserver/meshserver.yaml"))
                                .item();

                StatefulSet sts = client.apps().statefulSets()
                        .inNamespace("sofamesh").withName("meshserver").get();
                if (sts != null) {
                    client.apps().statefulSets().inNamespace("sofamesh").withName("meshserver").delete();

                    // waiting sts quit
                    while (true) {
                        sts = client.apps().statefulSets()
                                .inNamespace("sofamesh").withName("meshserver").get();
                        if (sts == null) break;
                        Thread.sleep(1000);
                    }
                }

                deployment = client.apps().statefulSets().inNamespace("sofamesh").resource(deployment).createOrReplace();


                // create mesh server service
                Service service = client.services().load(new File(config.getProject(), "meshserver/meshserver-service.yaml"))
                        .item();

                service = client.services().inNamespace("sofamesh").resource(service).createOrReplace();

                // start deploy now.
                task.setPrefix("🌱  ");
            }
        });
        task.setCmd(cmd);

        // create sub task
        task.setCommands(new CopyOnWriteArrayList<>());

        Command checkReady = new Command();
        checkReady.setPrettyValue("Check whether the nacos registry and mysql service are ready ...");
        checkReady.setRunnable(new AbstractRunTask(checkReady) {
            @Override
            public void doRun() throws Exception {

                waitNacosStartAlready(checkReady);
                waitMysqlStartAlready(checkReady);


            }
        });

        Command writeMysql = new Command();
        writeMysql.setPrettyValue("Initialize the control plane database schema ...");
        writeMysql.setRunnable(new AbstractRunTask(writeMysql) {
            @Override
            public void doRun() throws Exception {

                Map<String, String> conf = config.getDefaults();

                String exec = conf.get("mesh.server.mysql.pod.exec");
                if (exec != null && exec.equals("off")) return;

                if (exec == null || exec.equals("true")) {
                    execPodMysql(conf, writeMysql);
                } else {
                    initRemoteMysql(conf, writeMysql);
                }
            }
        });

        Command writeConfigMap = new Command();
        writeConfigMap.setPrettyValue("Create control plane configuration ...");
        writeConfigMap.setRunnable(new AbstractRunTask(writeConfigMap) {
            @Override
            public void doRun() throws Exception {

                Secret secret = client.secrets()
                        .load(new File(config.getProject(), "meshserver/mesh-license-publickey.yaml")).item();

                // create license secrete
                client.secrets().inNamespace("sofamesh").resource(secret).createOrReplace();


                // create mesh agent
                ConfigMap meshAgent = client.configMaps().load(new File(config.getProject(), "meshserver/mesh-agent.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(meshAgent).createOrReplace();

                // create mesh agent cm
                ConfigMap meshAgentCm = client.configMaps().load(new File(config.getProject(), "meshserver/mesh-agent-cm.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(meshAgentCm).createOrReplace();

                // create dsr-ca-security.yaml
                ConfigMap dsrCaCm = client.configMaps().load(new File(config.getProject(), "meshserver/dsr-ca-security.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(dsrCaCm).createOrReplace();

                // citadel-mcp template
                ConfigMap citadelMcpTemplate = client.configMaps().load(new File(config.getProject(), "citadel/citadel-mcp.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(citadelMcpTemplate).createOrReplace();

                ConfigMap istioCMTemplate = client.configMaps().load(new File(config.getProject(), "citadel/istio-cm.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(istioCMTemplate).createOrReplace();

                // waiting pod start
                task.setPrefix("😄  ");
            }
        });

        task.getCommands().add(checkReady);
        task.getCommands().add(writeMysql);
        task.getCommands().add(writeConfigMap);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                if (cmd.getStatus() != null) return;
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createDeployOperatorCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh");

        task.setName("sidecar-operator");
        task.setPrefix("🔥  ");
        task.setAlias("Start the sidecar operator injection service...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                Secret secret = client.secrets()
                        .load(new File(config.getProject(), "operator/tls-cert.yaml")).item();

                // create tls cert
                client.secrets().inNamespace("sofamesh").resource(secret).createOrReplace();


                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "operator/operator.yaml"))
                                .item();

                StatefulSet sts = client.apps().statefulSets()
                        .inNamespace("sofamesh").withName("sidecar-operator").get();
                if (sts != null) {
                    client.apps().statefulSets().inNamespace("sofamesh").withName("sidecar-operator").delete();

                    // wait sts deleted
                    while (true) {
                        sts = client.apps().statefulSets()
                                .inNamespace("sofamesh").withName("sidecar-operator").get();
                        if (sts == null) break;
                        Thread.sleep(1000);
                    }
                }

                deployment = client.apps().statefulSets().inNamespace("sofamesh").resource(deployment).createOrReplace();


                // create service
                Service service = client.services().load(new File(config.getProject(), "operator/operator-service.yaml"))
                        .item();

                Service ss = client.services().inNamespace("sofamesh").withName("sidecar-operator-service").get();
                if (ss != null) {
                    client.services().inNamespace("sofamesh").withName("sidecar-operator-service").delete();

                    while (true) {
                        ss = client.services()
                                .inNamespace("sofamesh").withName("sidecar-operator-service").get();
                        if (ss == null) break;
                        Thread.sleep(1000);
                    }
                }

                service = client.services().inNamespace("sofamesh").resource(service).createOrReplace();

//                Service serviceInternal = client.services().load(new File(config.getProject(), "operator/operator-service-internal.yaml"))
//                        .item();
//
//                service = client.services().inNamespace("sofamesh").resource(serviceInternal).createOrReplace();
//
//                String serviceURL = client.services().inNamespace("sofamesh").withName(service.getMetadata().getName())
//                        .getURL("mysql-port");

                // mosn-template
                ConfigMap mosnTemplate = client.configMaps().load(new File(config.getProject(), "operator/mosn-template.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(mosnTemplate).createOrReplace();

                // mesh-clusters
                ConfigMap meshCLusterTemplate = client.configMaps().load(new File(config.getProject(), "operator/mesh-clusters.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(meshCLusterTemplate).createOrReplace();

                // mosn default template
                ConfigMap mosnDefaultTemplate = client.configMaps().load(new File(config.getProject(), "operator/mosn-default-template.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(mosnDefaultTemplate).createOrReplace();

                // mesh pod inject config
                ConfigMap mosnInjectTemplate = client.configMaps().load(new File(config.getProject(), "operator/mesh-pod-inject-config.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(mosnInjectTemplate).createOrReplace();

                ConfigMap mosnGwTemplate = client.configMaps().load(new File(config.getProject(), "operator/mosngw-configmap.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(mosnGwTemplate).createOrReplace();

                // create webhook
                ConfigMap webhook = client.configMaps().load(new File(config.getProject(), "operator/webhook.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(webhook).createOrReplace();

                // waiting pod start
                task.setPrefix("🎉  ");
            }
        });
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createDeployCitadelCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh");

        task.setName("citadel");
        task.setPrefix("🔥  ");
        task.setAlias("Start the citadel service component...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                // create configmap

                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "citadel/citadel.yaml"))
                                .item();

                StatefulSet sts = client.apps().statefulSets()
                        .inNamespace("sofamesh").withName("citadel").get();
                if (sts != null) {
                    client.apps().statefulSets().inNamespace("sofamesh").withName("citadel").delete();

                    while (true) {
                        sts = client.apps().statefulSets()
                                .inNamespace("sofamesh").withName("citadel").get();
                        if (sts == null) break;
                        Thread.sleep(1000);
                    }
                }

                deployment = client.apps().statefulSets().inNamespace("sofamesh").resource(deployment).createOrReplace();


                // create service
                Service service = client.services().load(new File(config.getProject(), "citadel/citadel-service.yaml"))
                        .item();

                service = client.services().inNamespace("sofamesh").resource(service).createOrReplace();

                // waiting pod start
                task.setPrefix("😄  ");
            }
        });
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createDeployPrometheusCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh");

        task.setName("prometheus");
        task.setPrefix("🔥  ");
        task.setAlias("Start the prometheus component...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                // create configmap
                ConfigMap serverConfigTemplate = client.configMaps().load(new File(config.getProject(), "prometheus/prometheus-server-config.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(serverConfigTemplate).createOrReplace();

                ConfigMap ruleConfigTemplate = client.configMaps().load(new File(config.getProject(), "prometheus/prometheus-server-rule-config.yaml"))
                        .item();
                client.configMaps().inNamespace("sofamesh").resource(ruleConfigTemplate).createOrReplace();

                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "prometheus/prometheus.yaml"))
                                .item();

                StatefulSet sts = client.apps().statefulSets()
                        .inNamespace("sofamesh").withName("prometheus").get();
                if (sts != null) {
                    client.apps().statefulSets().inNamespace("sofamesh").withName("prometheus").delete();

                    while (true) {
                        sts = client.apps().statefulSets()
                                .inNamespace("sofamesh").withName("prometheus").get();
                        if (sts == null) break;
                        Thread.sleep(1000);
                    }
                }

                deployment = client.apps().statefulSets().inNamespace("sofamesh").resource(deployment).createOrReplace();


                // create service
                Service service = client.services().load(new File(config.getProject(), "prometheus/prometheus-service.yaml"))
                        .item();

                service = client.services().inNamespace("sofamesh").resource(service).createOrReplace();

                // waiting pod start
                task.setPrefix("😄  ");
            }
        });
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createDeployDubboServerCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh-proxy");

        task.setName("dubbo-server");
        task.setPrefix("🔥  ");
        task.setAlias("Start dubbo server test application...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                for (Command run : task.getCommands()) {
                    run.runnable.run();

                    if (!PluginStatus.SUCCESS.equals(run.getStatus())) {
                        task.setPrefix("❗  ");

                        task.getCmd().setStatus(PluginStatus.FAIL);

                        if (run.getFailedOutputFirst() != null) {
                            task.getCmd().setOutput(run.getFailedOutputFirst());
                        }

                        return;
                    }
                }

                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "dubbo/dubbo-server.yaml"))
                                .item();

                StatefulSet sts = client.apps().statefulSets()
                        .inNamespace("sofamesh-proxy").withName("dubbo-server").get();
                if (sts != null) {
                    client.apps().statefulSets().inNamespace("sofamesh-proxy").withName("dubbo-server").delete();

                    while (true) {
                        sts = client.apps().statefulSets()
                                .inNamespace("sofamesh-proxy").withName("dubbo-server").get();
                        if (sts == null) {

                            // wait pod quit
//                            List<Pod> pods = client.pods()
//                                    .inNamespace("sofamesh-proxy")
//                                    .withLabel("app", "dubbo-server")
//                                    .list().getItems();
//
//                            if (pods == null || pods.isEmpty())
                            break;
                        }
                        Thread.sleep(1000);
                    }
                }

                deployment = client.apps().statefulSets().inNamespace("sofamesh-proxy").resource(deployment).createOrReplace();

                // waiting pod start
                task.setPrefix("😄  ");

            }
        });
        task.setCmd(cmd);

        // create sub task
        task.setCommands(new CopyOnWriteArrayList<>());

        Command checkReady = new Command();
        checkReady.setPrettyValue("Check whether the nacos registry are ready ...");
        checkReady.setRunnable(new AbstractRunTask(checkReady) {
            @Override
            public void doRun() throws Exception {
                waitNacosStartAlready(checkReady);
            }
        });

        task.getCommands().add(checkReady);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                if (cmd.getStatus() != null) return;
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createDeployDubboClientCommand() {
        Task task = new Task();

        task.setNamespace("sofamesh-proxy");

        task.setName("dubbo-client");
        task.setPrefix("🔥  ");
        task.setAlias("Start dubbo client test application...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                for (Command run : task.getCommands()) {
                    run.runnable.run();

                    if (!PluginStatus.SUCCESS.equals(run.getStatus())) {
                        task.setPrefix("❗  ");

                        task.getCmd().setStatus(PluginStatus.FAIL);

                        if (run.getFailedOutputFirst() != null) {
                            task.getCmd().setOutput(run.getFailedOutputFirst());
                        }

                        return;
                    }
                }

                StatefulSet deployment =
                        client.apps().statefulSets()
                                .load(new File(config.getProject(), "dubbo/dubbo-client.yaml"))
                                .item();

                StatefulSet sts = client.apps().statefulSets()
                        .inNamespace("sofamesh-proxy").withName("dubbo-client").get();
                if (sts != null) {
                    client.apps().statefulSets().inNamespace("sofamesh-proxy").withName("dubbo-client").delete();

                    while (true) {
                        sts = client.apps().statefulSets()
                                .inNamespace("sofamesh-proxy").withName("dubbo-client").get();
                        if (sts == null) {
                            // wait pod quit
//                            List<Pod> pods = client.pods()
//                                    .inNamespace("sofamesh-proxy")
//                                    .withLabel("app", "dubbo-client")
//                                    .list().getItems();
//
//                            if (pods == null || pods.isEmpty())
                            break;
                        }
                        Thread.sleep(1000);
                    }
                }

                deployment = client.apps().statefulSets().inNamespace("sofamesh-proxy").resource(deployment).createOrReplace();

                // waiting pod start
                task.setPrefix("😄  ");

            }
        });
        task.setCmd(cmd);

        // create sub task
        task.setCommands(new CopyOnWriteArrayList<>());

        Command checkReady = new Command();
        checkReady.setPrettyValue("Check whether the dubbo client  is ready ...");
        checkReady.setRunnable(new AbstractRunTask(checkReady) {
            @Override
            public void doRun() throws Exception {
                waitDubboServerStartAlready(checkReady);
            }
        });

        task.getCommands().add(checkReady);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                if (cmd.getStatus() != null) return;
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private Task createWaitingMeshReadyCommand() {
        Task task = new Task();

        //task.setName("wait");
        task.setPrefix("🔥  ");
        task.setAlias("Waiting for all components to start successfully...");

        Command cmd = new Command();
        cmd.setRunnable(new AbstractRunTask(cmd) {
            @Override
            public void doRun() throws Exception {

                // start thread to update pod info
                TerminalCompiler.submit(() -> scheduleAndUpdatePodInfo());

                for (Task waitTask : tasks) {
                    if (waitTask.getName() == null || waitTask.getName().length() == 0) continue;

                    // mesh server 120 seconds
                    waitComponentAlready(waitTask, "meshserver".equals(waitTask.getName()) ? 120 : 60);
                }

                boolean allSuccess = true;

                Task failed = null;
                for (Task waitTask : tasks) {
                    if (waitTask.getName() == null || waitTask.getName().length() == 0) continue;

                    if (!PluginStatus.SUCCESS.equals(waitTask.getCmd().getStatus())) {
                        allSuccess = false;

                        failed = waitTask;
                        break;
                    }
                }

                // waiting pod start
                if (allSuccess) {
                    String message = "Done! Run `minikube kubectl -- port-forward service/meshserver-service 7080:80 -n sofamesh` \n" +
                            "Service Mesh is now available at http://localhost:7080/index.html";

                    if (config.deployNacosOnly()) {
                        message = "Done! Run `minikube kubectl -- port-forward service/nacos-service 8848 9848 -n sofamesh` \n" +
                                "Nacos is now available at http://localhost:8848/nacos/index.html";
                    }

                    task.setAlias(message);
                    task.setPrefix("👍  ");
                } else {
                    task.setAlias("Fatal! Run `minikube kubectl -- describe pod " + failed.getPod().getName() + " -n sofamesh` check error.");
                    task.setPrefix("❗  ");
                }
            }
        });
        task.setCmd(cmd);

        cmd.callback = new Command.CallBack() {
            @Override
            public void terminated(int status) {
                cmd.setStatus(status == 0 ? PluginStatus.SUCCESS : PluginStatus.FAIL);
            }
        };

        return task;
    }

    private void execPodMysql(Map<String, String> conf, Command writeMysql) throws IOException {
        // write sql to database
        List<Pod> pods = client.pods()
                .inNamespace("sofamesh")
                .withLabel("app", "mysql")
                .list().getItems();


        String user = conf.get("mesh.server.mysql.user");
        if (user == null) user = "root";
        String pwd = conf.get("mesh.server.mysql.password");
        if (pwd == null) pwd = "root123456";
        String database = conf.get("mesh.server.mysql.database.name");
        if (database == null) database = "meshdb";

        String cmd = "mysql" +
                " -u" + user +
                " -p" + pwd +
                " -h 127.0.0.1 ";

        File sql = new File(config.getProject(), "meshserver/meshserver_table.sql");
        if (sql.exists()) {
            if (pods != null) {
                for (Pod pod : pods) {
                    if (PodStatusUtil.isRunning(pod)) {
                        CompletableFuture<String> data = new CompletableFuture<>();
                        try (ExecWatch watch = execCmd(writeMysql, pod, sql, data, cmd.split(" "))) {
//                            String message = data.get(10, TimeUnit.SECONDS);
//                            if (message != null && message.length() > 0) {
//                                writeMysql.addOutputMessage(message);
//                            }

                            // close is needed when we're reading from stdin to terminate
                            watch.close();

                            // wait for the process to exit
                            watch.exitCode().join();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }

    }

    private ExecWatch execCmd(Command writeMysql, Pod pod, File sql, CompletableFuture<String> data, String... command) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        RunSqlListener listener = new RunSqlListener(writeMysql, data, outputStream);
        ExecWatch watch = client.pods()
                .inNamespace(pod.getMetadata().getNamespace())
                .withName(pod.getMetadata().getName())
                .redirectingInput()//.readingInput(new FileInputStream(sql))
                .writingOutput(outputStream)
                .writingError(outputStream)
                //.withTTY()
                .usingListener(new RunSqlListener(writeMysql, data, outputStream))
                .exec(command);

        // FIXME: This is a bit dodgy, but we need the listener to be able to close the ExecWatch in failure conditions,
        // because it doesn't cleanup properly and deadlocks.
        // Needs bugs fixed inside kubernetes-client.
        listener.setWatch(watch);

        OutputStream input = watch.getInput();
        input.write(Files.readAllBytes(sql.toPath()));

        input.flush();

        return watch;
    }

    static class RunSqlListener implements ExecListener {

        private final CompletableFuture<String> data;
        private final ByteArrayOutputStream output;

        private final Command writeMysql;

        ExecWatch watch;

        public RunSqlListener(Command writeMysql, CompletableFuture<String> data, ByteArrayOutputStream output) {
            this.data = data;
            this.output = output;
            this.writeMysql = writeMysql;
        }

        @Override
        public void onOpen() {
            ExecListener.super.onOpen();
        }

        @Override
        public void onFailure(Throwable t, Response failureResponse) {

            data.completeExceptionally(t);

            this.writeMysql.setStatus(PluginStatus.FAIL);
            this.writeMysql.addOutputMessage("init sql failure: " + t.getMessage() + "\n");
        }

        @Override
        public void onClose(int code, String reason) {
            data.complete(output.toString());

            if (reason != null && reason.length() > 0) {
                this.writeMysql.setStatus(PluginStatus.FAIL);
                this.writeMysql.addOutputMessage("init sql err: " + output + "\nreason: " + reason + " code: " + code + "\n");
            }
        }

        @Override
        public void onExit(int code, Status status) {
            ExecListener.super.onExit(code, status);
        }

        public ExecWatch getWatch() {
            return watch;
        }

        public void setWatch(ExecWatch watch) {
            this.watch = watch;
        }
    }


    private void initRemoteMysql(Map<String, String> conf, Command writeMysql) throws SQLException {
        // write sql to database
        Connection conn = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");

            String url = conf.get("mesh.server.mysql.url");
            if (url == null) url = "jdbc:mysql://mysql-service:3306/meshdb";
            String user = conf.get("mesh.server.mysql.user");
            if (user == null) user = "root";
            String pwd = conf.get("mesh.server.mysql.password");
            if (pwd == null) pwd = "root123456";

            conn = DriverManager.getConnection(url, user, pwd);

            execute(writeMysql, conn, "meshserver/meshserver_table.sql");
            execute(writeMysql, conn, "meshserver/drmdata_table.sql");
        } catch (Exception e) {
            writeMysql.setStatus(PluginStatus.FAIL);
            writeMysql.addOutputMessage("init mesh server table error:\n" + e.getMessage());
        } finally {
            if (conn != null) conn.close();
        }
    }

    private void execute(Command writeMysql, Connection conn, String sqlFile) throws Exception {
        try (Statement stmt = conn.createStatement()) {
            List<String> sqlList = loadSql(writeMysql, sqlFile);
            for (String sql : sqlList) {
                stmt.execute(sql);
            }
        }
    }

    private List<String> loadSql(Command writeMysql, String sqlFile) throws Exception {
        List<String> sqlList = new ArrayList<>();
        InputStream sqlFileIn = null;
        try {
            File file = new File(config.getProject(), sqlFile);

            if (!file.exists()) return sqlList;

            sqlFileIn = new FileInputStream(file);
            StringBuilder sqlSb = new StringBuilder();
            byte[] buff = new byte[1024];
            int byteRead = 0;
            while ((byteRead = sqlFileIn.read(buff)) != -1) {
                sqlSb.append(new String(buff, 0, byteRead));
            }

            String[] sqlArr = sqlSb.toString().split(";");
            for (int i = 0; i < sqlArr.length; i++) {
                String sql = sqlArr[i].replaceAll("--.*", "").trim();
                if (sql.length() > 0) {
                    sqlList.add(sql);
                }
            }
            return sqlList;
        } catch (Exception ex) {
            throw new Exception(ex.getMessage());
        } finally {
            if (sqlFileIn != null) sqlFileIn.close();
        }
    }

    private void waitMysqlStartAlready(Command checkReady) throws InterruptedException {
        int maxRetry;
        int i;


        i = 0;
        maxRetry = 60;
        boolean quit = false;

        while (true) {

            // check mysql
            List<Pod> pods = client.pods()
                    .inNamespace("sofamesh")
                    .withLabel("app", "mysql")
                    .list().getItems();

            if (pods != null) {

                for (Pod pod : pods) {

                    for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        ContainerState myState = cs.getState();
                        if (myState.getRunning() != null) {

                            if (cs.getReady() != null && cs.getReady()) {
                                quit = true;
                                break;
                            }

                        }
                    }

                    PodStatus status = pod.getStatus();
                    if (status != null) {

                        String message = status.getReason();
                        if (message == null) message = status.getMessage();

                        if (message != null) {
                            checkReady.setStatus(PluginStatus.FAIL);
                            checkReady.addOutputMessage("\nmysql: \n" + message);
                            break;
                        }
                    }
                }
            }


            if (quit) {
                break;
            }

            if (i++ < maxRetry) {
                // wait random 1 seconds and retry
                Thread.sleep(1000L);
            } else {
                checkReady.setStatus(PluginStatus.FAIL);
                checkReady.addOutputMessage("waiting mysql start timeout", true);
            }

        }
    }

    private void waitNacosStartAlready(Command checkReady) throws InterruptedException {
        int i = 0;
        int maxRetry = 60;

        boolean quit = false;

        while (true) {

            List<Pod> pods = client.pods()
                    .inNamespace("sofamesh")
                    .withLabel("app", "nacos")
                    .list().getItems();

            if (pods != null) {

                for (Pod pod : pods) {

                    for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        ContainerState myState = cs.getState();
                        if (myState.getRunning() != null) {

                            if (cs.getReady() != null && cs.getReady()) {
                                quit = true;
                                break;
                            }

                        }
                    }

                    PodStatus status = pod.getStatus();
                    if (status != null) {

                        String message = status.getReason();
                        if (message == null) message = status.getMessage();

                        if (message != null) {
                            checkReady.setStatus(PluginStatus.FAIL);
                            checkReady.addOutputMessage("\nnacos: \n" + message);
                            break;
                        }
                    }
                }
            }

            if (quit) {
                break;
            }

            if (i++ < maxRetry) {
                // wait random 1 seconds and retry
                Thread.sleep(1000L);
            } else {
                checkReady.setStatus(PluginStatus.FAIL);
                checkReady.addOutputMessage("waiting nacos start timeout", true);
            }

        }
    }

    private void waitDubboServerStartAlready(Command checkReady) throws InterruptedException {
        int i = 0;
        int maxRetry = 60;

        boolean quit = false;

        while (true) {

            List<Pod> pods = client.pods()
                    .inNamespace("sofamesh-proxy")
                    .withLabel("app", "dubbo-server")
                    .list().getItems();

            if (pods != null) {

                for (Pod pod : pods) {

                    for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        ContainerState myState = cs.getState();
                        if (myState.getRunning() != null) {

                            if (cs.getReady() != null && cs.getReady()) {
                                quit = true;
                                break;
                            }

                        }
                    }

                    PodStatus status = pod.getStatus();
                    if (status != null) {

                        String message = status.getReason();
                        if (message == null) message = status.getMessage();

                        if (message != null) {
                            checkReady.setStatus(PluginStatus.FAIL);
                            checkReady.addOutputMessage("\ndubbo-server: \n" + message);
                            break;
                        }
                    }
                }
            }

            if (quit) {
                break;
            }

            if (i++ < maxRetry) {
                // wait random 1 seconds and retry
                Thread.sleep(1000L);
            } else {
                checkReady.setStatus(PluginStatus.FAIL);
                checkReady.addOutputMessage("waiting dubbo-server start timeout", true);
            }

        }
    }

    private void waitComponentAlready(Task checkReady, Integer retry) throws InterruptedException {


        boolean quit = false;

        while (true) {

            // task was cancel
            if (PluginStatus.CANCEL.equals(checkReady.getCmd().getStatus())) break;

            // check mysql
            List<Pod> pods = client.pods()
                    .inNamespace(checkReady.getNamespace())
                    .withLabel("app", checkReady.getName())
                    .list().getItems();

            if (pods != null) {

                int count = 0;

                for (Pod pod : pods) {

                    if (checkReady.getPod() == null) {
                        checkReady.setPod(new PodInfo());
                    }

                    checkReady.getPod().setName(pod.getMetadata().getName());
                    checkReady.getPod().setNamespace(pod.getMetadata().getNamespace());

                    // query resource
                    StatefulSet sts = client.apps().statefulSets()
                            .inNamespace(checkReady.getNamespace()).withName(checkReady.getName()).get();

                    if (sts != null) {
                        checkReady.getPod().setAvailable(count);
                        checkReady.getPod().setReplicas(sts.getStatus().getReplicas());
                        checkReady.getPod().setReady(count + "/" + sts.getStatus().getReplicas());
                    }

                    String lastMessage = null;

                    for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                        ContainerState myState = cs.getState();

                        checkReady.getPod().setRestarts(cs.getRestartCount());
                        if (myState.getWaiting() != null) {
                            checkReady.getPod().setStatus(myState.getWaiting().getReason());

                            lastMessage = myState.getWaiting().getMessage();
                        } else if (myState.getRunning() != null) {
                            checkReady.getPod().setStatus("Running");

                            if (cs.getReady() != null && cs.getReady()) {
                                count++;

                                quit = true;
                            }


                            if (sts != null) {
                                checkReady.getPod().setAvailable(count);
                                checkReady.getPod().setReplicas(sts.getStatus().getReplicas());
                                checkReady.getPod().setReady(count + "/" + sts.getStatus().getReplicas());
                            }
                        } else {
                            checkReady.getPod().setStatus(myState.getTerminated().getReason());

                            lastMessage = myState.getWaiting().getMessage();
                        }

                    }

                    PodStatus status = pod.getStatus();
                    if (status != null) {

                        String message = status.getReason();
                        if (message == null) message = status.getMessage();

                        if (message != null) {
                            checkReady.getCmd().setStatus(PluginStatus.FAIL);
                            checkReady.getCmd().addOutputMessage("\n" + checkReady.getName() + ": \n" + message, true);
                            quit = true;
                            break;
                        } else {
                            // check pod message
                            if (checkReady.getPod().getStatus() != null
                                    && (
                                    !"Running".equals(checkReady.getPod().getStatus()) && checkReady.getPod().getRestarts() != null
                                            && checkReady.getPod().getRestarts() > 0
                            )) {
                                checkReady.getCmd().setStatus(PluginStatus.FAIL);
                                checkReady.getCmd().addOutputMessage("\n" + checkReady.getName() + ": \n" + lastMessage, true);
                                quit = true;
                                break;
                            }
                        }
                    }
                }
            }

            if (quit) break;

            Thread.sleep(1000L);


        }
    }

    private void scheduleAndUpdatePodInfo() {

        for (; ; ) {

            boolean shouldQuit = false;
            for (Task task : getTasks()) {

                if (task.getName() == null || task.getName().length() == 0) continue;

                // task was cancel
                if (PluginStatus.CANCEL.equals(task.getCmd().getStatus())) {
                    shouldQuit = true;
                    break;
                }

                // check mysql
                List<Pod> pods = client.pods()
                        .inNamespace(task.getNamespace())
                        .withLabel("app", task.getName())
                        .list().getItems();

                if (pods != null) {


                    int count = 0;

                    for (Pod pod : pods) {

                        if (task.getPod() == null) {
                            task.setPod(new PodInfo());
                        }

                        task.getPod().setName(pod.getMetadata().getName());
                        task.getPod().setNamespace(pod.getMetadata().getNamespace());

                        // query resource
                        StatefulSet sts = client.apps().statefulSets()
                                .inNamespace(task.getNamespace()).withName(task.getName()).get();

                        if (sts != null) {
                            task.getPod().setAvailable(count);
                            task.getPod().setReplicas(sts.getStatus().getReplicas());
                            task.getPod().setReady(sts.getStatus().getAvailableReplicas() + "/" + sts.getStatus().getReplicas());
                        }

                        for (ContainerStatus cs : pod.getStatus().getContainerStatuses()) {
                            ContainerState myState = cs.getState();

                            task.getPod().setRestarts(cs.getRestartCount());
                            if (myState.getWaiting() != null) {
                                task.getPod().setStatus(myState.getWaiting().getReason());
                            } else if (myState.getRunning() != null) {
                                task.getPod().setStatus("Running");

                                if (cs.getReady() != null && cs.getReady()) {
                                    count++;
                                }


                                if (sts != null) {
                                    task.getPod().setAvailable(count);
                                    task.getPod().setReplicas(sts.getStatus().getReplicas());
                                    task.getPod().setReady(count + "/" + sts.getStatus().getReplicas());
                                }

                            } else {
                                task.getPod().setStatus(myState.getTerminated().getReason());
                            }

                        }

                    }
                }

            }

            if (shouldQuit) break;

        }


    }

    @Override
    public void execute() {

        if (!initialized) {
            this.tasks = prepareTasks();
            prepareMiniK8sClient();

            initialized = true;
        }

        int run = current.get();
        if (run < this.tasks.size()) {
            Task task = this.tasks.get(run);

            Command.CallBack backup = task.getCmd().getCallback();
            task.getCmd().callback = status -> {

                // rollback callback
                task.getCmd().callback = backup;

                current.incrementAndGet();

                if (backup != null) backup.terminated(status);

                if (status == 0) {

                    // check cmd status if success ?
                    if (task.getCmd().getStatus() != null
                            && !task.getCmd().getStatus().equals(PluginStatus.SUCCESS)) {
                        if (getOnTaskFailure() != null) {
                            getOnTaskFailure().onComplete(task);

                            return;
                        }
                    }

                    if (getOnTaskSuccess() != null) {
                        getOnTaskSuccess().onComplete(task);
                    }

                    if (task.getSleepSeconds() != null) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(task.getSleepSeconds()));
                        } catch (Exception ignored) {
                        }
                    }

                    // take next task to execute
                    execute();
                } else {
                    // task execute failed
                    if (getOnTaskFailure() != null) {
                        getOnTaskFailure().onComplete(task);
                    }
                }
            };

            TerminalCompiler.submit(task.getCmd().getRunnable());
        } else {
            // all task executed
            if (getOnSuccess() != null) {
                getOnSuccess().run();
            }
        }
    }

    private void prepareMiniK8sClient() {
        try {

//            System.setProperty(KUBERNETES_TRUST_CERT_SYSTEM_PROPERTY, "true");
//            System.setProperty(KUBERNETES_DISABLE_HOSTNAME_VERIFICATION_SYSTEM_PROPERTY, "true");

            client = new KubernetesClientBuilder().build();
        } catch (Exception e) {

            if (getOnTaskFailure() != null) {
                Task mockTask = this.tasks.get(0);
                mockTask.getCmd().output = new CopyOnWriteArrayList<>();
                mockTask.getCmd().output.add(e.getMessage());
                mockTask.getCmd().setStatus(PluginStatus.FAIL);

                getOnTaskFailure().onComplete(mockTask);
            }
        }
    }

    @Override
    public String render() {

        StringBuilder buf = new StringBuilder();

        boolean allTaskSuccess = true;

        if (this.getTasks() != null) {
            for (Task task : getTasks()) {

                String status = task.getCmd().getStatus();
                if (task != waitingTask && !PluginStatus.SUCCESS.equals(status)) {
                    allTaskSuccess = false;
                }

                if (task.isStarted()) {
                    buf.append(task.getPrefix()).append(task.getAlias()).append("\n");

                    if (task.getCommands() != null) {
                        for (Command cmd : task.getCommands()) {
                            if (cmd.start != 0 || cmd.stop != 0) {
                                buf.append("    ▪ ").append(cmd.getPrettyValue()).append("\n");
                            }
                        }
                    }
                }
            }
        }

        if (waitingTask != null
                && waitingTask.isStarted()
                || (dubboClientTask != null && dubboClientTask.isStarted())) {

            if (this.getTasks() != null) {
                buf.append("\n");

                PluginTable table = new PluginTable();

                table.addHeader("POD NAME")
                        .addHeader("NAMESPACE")
                        .addHeader("READY")
                        .addHeader("STATUS")
                        .addHeader("RESTARTS")
                        .addHeader("AGE")
                        .addHeader("HEALTHY");

                long now = System.currentTimeMillis();
                for (Task task : getTasks()) {
                    if (task.getPod() != null) {
                        PodInfo pod = task.getPod();
                        PluginTable.Row row = new PluginTable.Row();
                        row.appendColumn(pod.getName(), false)
                                .appendColumn(pod.getNamespace())
                                .appendColumn(pod.getReady())
                                .appendColumn(pod.getStatus())
                                .appendColumn(pod.getRestarts() == null ? "0" : String.valueOf(pod.getRestarts().intValue()));

                        long seconds = TimeUnit.MILLISECONDS.toSeconds(now - task.getCmd().start);
                        if (seconds < 60) {
                            row.appendColumn(seconds + "s");
                        } else {

                            long milliseconds = now - task.getCmd().start;
                            long minutes = TimeUnit.MILLISECONDS.toMinutes(now - task.getCmd().start);

                            String secondsString = "";
                            long hour = 0;
                            if (minutes > 60) {

                                hour = TimeUnit.MILLISECONDS.toHours(milliseconds);
                                secondsString = hour + "h";

                                minutes = milliseconds - hour * 60 * 60 * 1000;
                                minutes = TimeUnit.MILLISECONDS.toMinutes(minutes);
                            }

                            secondsString += minutes + "min";
                            long leftSeconds = milliseconds - hour * 60 * 60 * 1000 - minutes * 60 * 1000;
                            if (leftSeconds > 0) {
                                secondsString += TimeUnit.MILLISECONDS.toSeconds(leftSeconds) + "s";
                            }

                            row.appendColumn(secondsString);
                        }

                        row.appendColumn(pod.getAvailable() > 0 && pod.getAvailable() == pod.getAvailable() ? "✅" : "🔥");

                        table.addRow(row);
                    }
                }

                buf.append(table.pretty());
            }

        }

//        if (isComplete()){
//            String appendText = "\n👍👍👍 Mini mesh has been successfully deployed !\n";
//            buf.append(appendText);
//        }

        return buf.toString();
    }


    private String childDirectory() {
        String path = this.config.getRootDir().getPath();
        if (path.endsWith("/")) {
            path = path.substring(0, path.length() - 1);
        }

        return path;
    }

    private void createServiceAccount() {
        // create account
        String name = "env-config";

        Resource<ConfigMap> configMapResource = client.configMaps().inNamespace("sofamesh")
                .resource(new ConfigMapBuilder().withNewMetadata().withName(name)
                        .endMetadata().addToData(config.getDefaults())
                        .build());

        ConfigMap configMap = configMapResource.createOrReplace();


        ServiceAccount account = client.serviceAccounts()
                .load(new File(config.getProject(), "meshserver/service-account.yaml"))
                .item();
        // create service account
        client.serviceAccounts().inNamespace("sofamesh").resource(account).createOrReplace();

        ClusterRole role = client.rbac().clusterRoles()
                .load(new File(config.getProject(), "meshserver/cluster-role.yaml"))
                .item();
        // create cluster role
        client.rbac().clusterRoles().resource(role).createOrReplace();

        ClusterRoleBinding binding = client.rbac().clusterRoleBindings()
                .load(new File(config.getProject(), "meshserver/cluster-role-binding.yaml"))
                .item();
        client.rbac().clusterRoleBindings().resource(binding).createOrReplace();
    }
}
