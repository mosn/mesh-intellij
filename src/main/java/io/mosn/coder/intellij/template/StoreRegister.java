package io.mosn.coder.intellij.template;

import org.bouncycastle.util.Store;

public class StoreRegister {

    private static boolean registered = false;

    public static void register() {

        if (registered) return;

        synchronized (StoreRegister.class) {
            if (registered) return;

            registered = true;

            // register static template from here.
            StoreTemplate.register(new CompileFilterTemplate());
            StoreTemplate.register(new CompileTemplate());
            StoreTemplate.register(new CompileCodecTemplate());
            StoreTemplate.register(new CompileTranscoderTemplate());
            StoreTemplate.register(new CompileTraceTemplate());

            StoreTemplate.register(new BinaryDirectoryTemplate());
            StoreTemplate.register(new DockerfileTemplate());

            StoreTemplate.register(new PackageAntTemplate());
            StoreTemplate.register(new PackageCodecTemplate());
            StoreTemplate.register(new PackageFilterTemplate());
            StoreTemplate.register(new PackageTranscoderTemplate());
            StoreTemplate.register(new PackageTraceTemplate());

            StoreTemplate.register(new RunTemplate());
            StoreTemplate.register(new StopTemplate());

            StoreTemplate.register(new HashMapTemplate());
            StoreTemplate.register(new HeaderTemplate());
            StoreTemplate.register(new DynamicConfigTemplate());

            StoreTemplate.register(new IpTemplate());
            StoreTemplate.register(new VariableTemplate());

            StoreTemplate.register(new InternalTemplate());

            StoreTemplate.register(new GitIgnoreTemplate());
            StoreTemplate.register(new MakefileTemplate());
            StoreTemplate.register(new GoModTemplate());

            // application.properties
            StoreTemplate.register(new ApplicationPropertyTemplate());
            StoreTemplate.register(new VersionTemplate());

            // vscode debug
            StoreTemplate.register(new VsCodeLaunchTemplate());

            StoreTemplate.register(new RegistryShellTemplate());
        }

    }

}
