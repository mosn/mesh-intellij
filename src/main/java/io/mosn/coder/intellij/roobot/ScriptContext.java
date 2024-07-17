package io.mosn.coder.intellij.roobot;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.openapi.vfs.VirtualFile;
import io.mosn.coder.intellij.option.*;
import io.mosn.coder.intellij.template.*;
import io.mosn.coder.plugin.model.PluginSimpleMetadata;
import io.mosn.coder.upgrade.ProjectMod;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yiji@apache.org
 */
public class ScriptContext extends AbstractContext {

    public ScriptContext(PluginOption option, File dir) {
        super(option, dir);
    }

    public ScriptContext(PluginOption option, VirtualFile dir) {
        super(option, dir);
    }

    @Override
    public void createTemplateCode() {
        // create static project shell、makefile etc
        prepareProjectIfRequired();

        // update start shell mapping port
        createStartShellIfRequired();

        // update mosn env config
        createEnvConfigIfRequired();

        // create readme
        createReadmeIfRequired();
    }

    private void prepareProjectIfRequired() {
        /**
         * create static template code.
         */

        StoreRegister.register();

        List<Source> files = new ArrayList<>();
        StoreTemplate.forEach(template -> {
            List<Source> code = template.create(option);

            /**
             * Update mod configuration
             */
            if (template instanceof GoModTemplate) {
                Source source = code.get(0);
                String path = getPath();

                ProjectMod pluginMod = new ProjectMod(path, "go.mod");
                pluginMod.readFile(new ByteArrayInputStream(source.getContent().getBytes()));

                /**
                 * local.mod exist
                 */
                ProjectMod current = new ProjectMod(path, "go.mod");
                ProjectMod upgrade = new ProjectMod(path, "build/sidecar/binary/local.mod");
                /**
                 * refresh project dependencies
                 */
                try {

                    if (! current.readFile() && !current.validFile()) {
                        /**
                         *  first time to create project
                         */
                        files.addAll(code);
                        return;
                    }

                    /**
                     * add new dependency to go
                     */
                    if (option instanceof TraceOption) {
                        String traceType;
                        pluginMod.getRequired().add(new ProjectMod.Line("github.com/google/uuid", "v1.3.0"));
                        if ((traceType = ((TraceOption) option).getRealTraceType()) != null) {
                            switch (traceType) {
                                case TraceOption.SKY_WALKING: {
                                    pluginMod.getRequired().add(new ProjectMod.Line("github.com/SkyAPM/go2sky", "v0.5.0"));
                                    pluginMod.getRequired().add(new ProjectMod.Line("google.golang.org/grpc", "v1.49.0"));
                                    pluginMod.getRequired().add(new ProjectMod.Line("google.golang.org/protobuf", "v1.28.0"));
                                    pluginMod.getRequired().add(new ProjectMod.Line("github.com/golang/protobuf", "v1.4.3"));
                                    break;
                                }
                                case TraceOption.ZIP_KIN: {
                                    pluginMod.getRequired().add(new ProjectMod.Line("github.com/openzipkin/zipkin-go", "v0.4.1"));
                                    break;
                                }
                            }
                        }
                    }


                    if (pluginMod.getRequired() != null) {
                        for (ProjectMod.Line pr : pluginMod.getRequired()) {

                            boolean found = false;
                            for (ProjectMod.Line cr : current.getRequired()) {
                                /**
                                 * same repo with current
                                 */
                                if (cr.getRepo().equals(pr.getRepo())) {
                                    found = true;
                                    break;
                                }
                            }

                            if (!found) {
                                current.getRequired().add(pr);
                            }
                        }
                    }

                    if (pluginMod.getReplaced() != null) {
                        for (ProjectMod.Line[] pr : pluginMod.getReplaced()) {
                            ProjectMod.Line old = pr[0];

                            boolean found = false;
                            for (ProjectMod.Line[] cp : current.getReplaced()) {
                                /**
                                 * same repo with current
                                 */
                                if (cp[0].getRepo().equals(old.getRepo())) {
                                    found = true;
                                    break;
                                }

                            }

                            if (!found) {
                                current.getReplaced().add(pr);
                            }
                        }
                    }

                    if (new File(path, "build/sidecar/binary/local.mod").exists()) {
                        current.merge(upgrade);
                    }else {
                        current.prepareFlush(null);
                    }

                    /**
                     * flush mod dependency
                     */
                    current.flush();


                } catch (Exception e) {
                    System.err.println("\nfailed update project go.mod");
                    return;
                }
            }

            if (code != null) {
                files.addAll(code);
            }
        });

        // write static template code
        flush(files);
    }

    private void updateMetadata(File metadata, PluginSimpleMetadata pm) throws IOException {
        String mApi = pm.getDependencies().get("mosn_api");
        String mPkg = pm.getDependencies().get("mosn_pkg");

        if (mApi != null && !mApi.equals(option.getApi()) ||
                mPkg != null && !mPkg.equals(option.getPkg())) {
            pm.getDependencies().put("mosn_api", option.getApi());
            pm.getDependencies().put("mosn_pkg", option.getPkg());
            /**
             * flush metadata
             */
            try (FileOutputStream out = new FileOutputStream(metadata)) {
                out.write(JSON.toJSONString(pm, true).getBytes());
                out.flush();
            }
        }
    }

    private void createStartShellIfRequired() {
        /**
         * StartTemplate:
         * The public external port needs to be modified.
         */
        StartTemplate template = new StartTemplate();
        List<Source> code = template.create(option);
        Source start = code.get(0);

        File shell = findFileByRelativePath(start.getPath() + "/" + start.getName());
        if (shell == null || !shell.exists()) {
            /**
             * Create a new startup container script
             */
            flush(code);
        } else {

            /**
             * ports need to be updated
             */

            List<PluginOption> options = new ArrayList<>();
            options.add(option);
            if (option instanceof ProtocolOption) {
                ProtocolOption opt = (ProtocolOption) option;
                if (opt.getEmbedded() != null && !opt.getEmbedded().isEmpty()) {
                    for (ProtocolOption o : opt.getEmbedded()) {
                        options.add(o);
                    }
                }
            }

            replaceAndFlush(start, (line) -> line.contains("BIZ_PORTS=")
                    , appendListenerPorts(), options.toArray(new PluginOption[0]));
        }
    }


    private void createEnvConfigIfRequired() {
        /**
         * EnvConfTemplate:
         * The public external PLUGINS_ACTIVE needs to be modified.
         */
        EnvConfTemplate template = new EnvConfTemplate();
        List<Source> code = template.create(option);
        Source env = code.get(0);

        File shell = findFileByRelativePath(env.getPath() + "/" + env.getName());
        if (shell == null || !shell.exists()) {
            /**
             * Create a new env config file
             */
            flush(code);
        } else {

            /**
             * PLUGINS_ACTIVE need to be updated
             */
            replaceAndFlush(env, (line) -> line.contains("PLUGINS_ACTIVE=")
                    , appendActivePlugin(), option);

            /**
             * remove open api
             */
            replaceAndFlush(env, (line) -> line.contains("FEATURE_GATES=") && line.contains("MOSN_FEATURE_OPENAPI_ENABLE")
                    , replaceFutureGate(), option);

            if (option instanceof TraceOption) {

                boolean span = false;
                boolean zipkin = false;
                boolean sky_walking = false;

                StringBuilder buffer = new StringBuilder();
                File file = new File(getPath(), EnvConfTemplate.Path + "/" + EnvConfTemplate.Name);
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
                    String line;
                    do {
                        line = reader.readLine();
                        /**
                         * append to buffer, when complete replaced file will be refreshed
                         */
                        if (line != null) {
                            buffer.append(line).append("\n");

                            /**
                             *  check MOSN_GENERATOR_SAPN_ENABLED exist
                             */
                            if (!span && line.contains("MOSN_GENERATOR_SAPN_ENABLED")) {
                                span = true;
                            } else if (!zipkin && line.contains("ZIPKIN_ADDRESS")) {
                                zipkin = true;
                            } else if (!sky_walking && line.contains("SKY_WALKING_ADDRESS")) {
                                sky_walking = true;
                            }

                        }

                    } while (line != null);

                } catch (Exception ignored) {
                }

                if (!span) {
                    buffer.append("MOSN_GENERATOR_SAPN_ENABLED=true").append("\n");
                }

                switch (((TraceOption) option).getRealTraceType()) {
                    case TraceOption.SKY_WALKING: {
                        if (!sky_walking) {
                            buffer.append("SKY_WALKING_ADDRESS=${PUB_BOLT_LOCAL_IP}:11800").append("\n");
                        }
                        break;
                    }
                    case TraceOption.ZIP_KIN: {
                        if (!zipkin) {
                            buffer.append("ZIPKIN_ADDRESS=http://${PUB_BOLT_LOCAL_IP}:9411/api/v2/spans").append("\n");
                        }
                    }
                }

                try {
                    if (buffer.length() > 0) {
                        byte[] content = buffer.toString().getBytes();
                        try (OutputStream stream = new FileOutputStream(file)) {
                            stream.write(content);
                        }
                    }
                } catch (Exception ignored) {
                }

            }
        }
    }

    private void createReadmeIfRequired() {
        /**
         * EnvConfTemplate:
         * The public external PLUGINS_ACTIVE needs to be modified.
         */
        ReadMeTemplate template = new ReadMeTemplate();
        List<Source> code = template.create(option);
        Source readme = code.get(0);

        File md = findFileByRelativePath(readme.getName());
        if (md == null || !md.exists()) {
            /**
             * Create a new readme file
             */
            flush(code);
        }
    }

    @NotNull
    private ReplaceAction appendListenerPorts() {
        return (line, options) -> {

            if (options != null) {

                int index = line.lastIndexOf("\"");
                StringBuilder buffer = new StringBuilder();
                // append matched line
                buffer.append(line, 0, index);

                for (PluginOption opt : options) {
                    if (opt instanceof ProtocolOption) {
                        ProtocolOption o = (ProtocolOption) opt;

                        // append client port
                        if (o.getClientPort() != null
                                && o.getClientPort() > 0) {

                            String port = String.valueOf(o.getClientPort());
                            if (!line.contains(port)) {
                                buffer.append(" -p ").append(port).append(":").append(port);
                            }
                        }

                        // append server port
                        if (o.getServerPort() != null
                                && o.getServerPort() > 0) {

                            String port = String.valueOf(o.getServerPort());
                            if (!line.contains(port)) {
                                buffer.append(" -p ").append(port).append(":").append(port);
                            }
                        }
                    }
                }

                buffer.append("\"");

                // return replaced text line
                return TextLine.Terminate.with(buffer.toString());
            }

            return TextLine.Terminate;
        };
    }

    @NotNull
    private ReplaceAction appendActivePlugin() {
        return (line, options) -> {

            if (options != null) {

                String text = "PLUGINS_ACTIVE=";
                int index = line.indexOf(text);

                // format: [{"kind":"","plugins":[{}]]
                String pluginText = line.substring(index + text.length());

                Gson gson = new Gson();
                JsonArray pluginArray = JsonParser.parseString(pluginText).getAsJsonArray();

                // unmarshal plugin kind array elements
                ArrayList<PluginKind> kinds = new ArrayList<>();
                for (JsonElement plugin : pluginArray) {
                    PluginKind kind = gson.fromJson(plugin, PluginKind.class);
                    if (kind.plugins == null) {
                        // avoid null pointer
                        kind.plugins = new Plugin[0];
                    }
                    kinds.add(kind);
                }

                StringBuilder buffer = new StringBuilder();
                for (PluginOption opt : options) {

                    boolean matched = false;
                    String pluginName = opt.getPluginName().toLowerCase();

                    for (PluginKind kind : kinds) {
                        if (opt.pluginTypeDescriptor().equals(kind.kind)) {
                            boolean append = kind.appendPlugin(pluginName);
                            matched = true;
                        }

                        if (buffer.length() > 0) {
                            buffer.append(",");
                        }

                        // append exists kind
                        buffer.append(kind);
                    }

                    if (!matched) {
                        // no current plugin kind exist
                        PluginKind kind = new PluginKind(opt.pluginTypeDescriptor());
                        kind.appendPlugin(opt.getPluginName().toLowerCase());

                        if (buffer.length() > 0) {
                            buffer.append(",");
                        }
                        buffer.append(kind);
                    }
                }

                // return replaced text line
                return TextLine.Terminate.with("PLUGINS_ACTIVE=[" + buffer + "]");
            }

            return TextLine.Terminate;
        };
    }

    @NotNull
    private ReplaceAction replaceFutureGate() {
        return (line, options) -> {

            String override = line.replace("MOSN_FEATURE_OPENAPI_ENABLE=false,", "");
            // return replaced text line
            return TextLine.Terminate.with(override);
        };
    }
}
