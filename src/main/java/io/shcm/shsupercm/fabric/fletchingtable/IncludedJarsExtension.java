package io.shcm.shsupercm.fabric.fletchingtable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
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
    private final Configuration configuration;

    public IncludedJarsExtension(Project project, FletchingTableExtension fletchingTableExtension) {
        this.fletchingTableExtension = fletchingTableExtension;
        includedJarsCache = new File(project.getProjectDir(), ".gradle/fletchingtable/includedJarsCache");

        project.getRepositories().flatDir(repository -> {
            repository.dirs(includedJarsCache);
            repository.content(content ->
                    content.includeGroup("includedJars")
            );
        });

        this.dependencies = project.getDependencies();
        this.configuration = project.getConfigurations().create("includedJarsInternalConfiguration");
        this.configuration.setTransitive(false);
    }

    public void from(String dependencyString) {
        this.dependencies.add("includedJarsInternalConfiguration", dependencyString);
    }

    public void extractAll() {
        if (includedJarsCache.exists()) {
            for (File file : Objects.requireNonNull(includedJarsCache.listFiles()))
                if (file.isFile())
                    file.delete();
        } else
            includedJarsCache.mkdirs();

        if (fletchingTableExtension.getEnableIncludedJars().get())
            for (File parentJarFile : this.configuration.resolve())
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
    }
}
