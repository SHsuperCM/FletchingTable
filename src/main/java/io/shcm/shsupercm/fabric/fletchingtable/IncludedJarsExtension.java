package io.shcm.shsupercm.fabric.fletchingtable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.DependencyHandler;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public abstract class IncludedJarsExtension {
    private final FletchingTableExtension fletchingTableExtension;
    private final File includedJarsCache;
    private final DependencyHandler dependencies;
    private final ConfigurationContainer configurations;

    public IncludedJarsExtension(Project project, FletchingTableExtension fletchingTableExtension) {
        this.fletchingTableExtension = fletchingTableExtension;

        this.includedJarsCache = new File(project.getProjectDir(), ".gradle/fletchingtable/includedJarsCache");

        if (includedJarsCache.exists()) {
            for (File file : Objects.requireNonNull(includedJarsCache.listFiles()))
                if (file.isFile())
                    file.delete();
        } else
            includedJarsCache.mkdirs();

        project.getRepositories().flatDir(repository -> {
            repository.dirs(includedJarsCache);
            repository.content(content ->
                    content.includeGroup("includedJars")
            );
        });

        this.dependencies = project.getDependencies();
        this.configurations = project.getConfigurations();
    }

    public void from(String dependencyString) {
        Configuration configuration = configurations.create("includedJarsInternalConfiguration");
        configuration.setTransitive(false);

        this.dependencies.add(configuration.getName(), dependencyString);

        for (File parentJarFile : configuration.resolve())
            try {
                try (ZipFile parentJarZip = new ZipFile(parentJarFile)) {
                    ZipEntry modJsonEntry = parentJarZip.getEntry("fabric.mod.json");
                    if (modJsonEntry != null) {
                        JsonObject modJson = JsonParser.parseReader(new InputStreamReader(parentJarZip.getInputStream(modJsonEntry), StandardCharsets.UTF_8)).getAsJsonObject();
                        JsonArray jars = modJson.getAsJsonArray("jars");
                        if (jars != null)
                            for (JsonElement jar : jars) {
                                String jarPath = jar.getAsJsonObject().get("file").getAsString();
                                Files.copy(parentJarZip.getInputStream(parentJarZip.getEntry(jarPath)), includedJarsCache.toPath().resolve(jarPath.substring(jarPath.lastIndexOf('/') + 1)), StandardCopyOption.REPLACE_EXISTING);
                            }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        configurations.remove(configuration);
    }
}
