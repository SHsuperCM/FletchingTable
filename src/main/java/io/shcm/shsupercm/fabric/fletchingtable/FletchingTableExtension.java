package io.shcm.shsupercm.fabric.fletchingtable;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Settings for Fletching Table
 */
public abstract class FletchingTableExtension {
    private final Project project;

    /**
     * Controls whether the annotation processor should run. When false, enableEntrypoints and enableMixins are ignored.
     */
    public abstract Property<Boolean> getEnableAnnotationProcessor();

    /**
     * Enables annotation processing for @Entrypoint.
     */
    public abstract Property<Boolean> getEnableEntrypoints();

    /**
     * Enables annotation processing for @Mixin.
     */
    public abstract Property<Boolean> getEnableMixins();

    /**
     * Sets the default mixin environment when not overridden by @MixinEnvironment.<br>
     * "mixins" will process mixins for both sides, "client" will process mixins as client mixins, "server" will process mixins as server mixins.
     * "none"(default) will ignore mixins entirely. "auto" will put everything in "mixins" unless one of the targets matches a client/server prefix.
     */
    public abstract Property<String> getDefaultMixinEnvironment();

    /**
     * Sets the prefix required for the "auto" environment to be replaced with "client".
     */
    public abstract Property<String> getAutoMixinEnvironmentClientPrefix();
    /**
     * Sets the prefix required for the "auto" environment to be replaced with "server".
     */
    public abstract Property<String> getAutoMixinEnvironmentServerPrefix();

    public FletchingTableExtension(Project project) {
        this.project = project;
        getEnableAnnotationProcessor().convention(true);

        getEnableEntrypoints().convention(true);
        getEnableMixins().convention(true);

        getDefaultMixinEnvironment().convention("none");
        getAutoMixinEnvironmentClientPrefix().convention("net.minecraft.client");
        getAutoMixinEnvironmentServerPrefix().convention("null");
    }

    protected boolean writeAPSettings(File file) {
        Properties settings = new Properties(), oldSettings = new Properties();
        file.getParentFile().mkdirs();
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                oldSettings.load(fr);
            } catch (Exception ignored) {
                oldSettings = null;
            }
            file.delete();
        }

        settings.put("entrypoints", getEnableEntrypoints().get().toString());
        settings.put("mixins", getEnableMixins().get().toString());
        settings.put("mixins-default", getDefaultMixinEnvironment().get());
        settings.put("mixins-prefix-client", getAutoMixinEnvironmentClientPrefix().get());
        settings.put("mixins-prefix-server", getAutoMixinEnvironmentServerPrefix().get());

        try (FileWriter fw = new FileWriter(file)) {
            settings.store(fw, null);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return !settings.equals(oldSettings);
    }

    /**
     * Makes the Yggdrasil exception not print out a stacktrace.<br>
     * Note that this was only tested with 1.18, it might work for other versions but it might not.
     */
    public void shutUpDrasil18() {
        project.getDependencies().add("modRuntimeOnly", project.files(new File(project.getProjectDir(), ".gradle/fletchingtable/jars/fletchingtable-shutupdrasil-1.18.jar")));
    }
}
