package io.shcm.shsupercm.fabric.fletchingtable;

import org.gradle.api.Project;
import org.gradle.api.provider.Property;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

public abstract class FletchingTableExtension {
    private final Project project;

    public abstract Property<Boolean> getEnableEntrypoints();

    public abstract Property<Boolean> getEnableMixins();

    public abstract Property<Boolean> getEnableIncludedJars();

    public abstract Property<Boolean> getEnableShutUpDrasil();

    public abstract Property<Boolean> getEnableAnnotationProcessor();

    public abstract Property<String> getDefaultMixinEnvironment();

    public abstract Property<String> getAutoMixinEnvironmentClientPrefix();

    public abstract Property<String> getAutoMixinEnvironmentServerPrefix();

    public FletchingTableExtension(Project project) {
        this.project = project;
        getEnableEntrypoints().convention(true);
        getEnableMixins().convention(true);
        getEnableIncludedJars().convention(true);
        getEnableShutUpDrasil().convention(true);

        getEnableAnnotationProcessor().convention(true);

        getDefaultMixinEnvironment().convention("none");
        getAutoMixinEnvironmentClientPrefix().convention("net.minecraft.client");
        getAutoMixinEnvironmentServerPrefix().convention("null");
    }

    protected boolean writeAPSettings(File file) {
        Properties settings = new Properties(), oldSettings = null;
        file.getParentFile().mkdirs();
        if (file.exists()) {
            try (FileReader fr = new FileReader(file)) {
                (oldSettings = new Properties()).load(fr);
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

    public void shutUpDrasil18() {
        project.getDependencies().add("modRuntimeOnly", project.files(new File(project.getProjectDir(), ".gradle/fletchingtable/jars/fletchingtable-shutupdrasil-1.18.jar")));
    }
}
