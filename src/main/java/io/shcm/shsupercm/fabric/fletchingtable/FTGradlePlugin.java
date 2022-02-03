/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package io.shcm.shsupercm.fabric.fletchingtable;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;
import io.shcm.shsupercm.fabric.fletchingtable.api.MixinEnvironment;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

public class FTGradlePlugin implements Plugin<Project> {
    private FletchingTableExtension fletchingTableExtension;
    private File jarsDir;

    @Override
    public void apply(Project project) {
        fletchingTableExtension = project.getExtensions().create("fletchingTable", FletchingTableExtension.class, project);

        jarsDir = new File(project.getProjectDir(), ".gradle/fletchingtable/jars");
        jarsDir.mkdirs();
        for (String name : new String[] { "api.jar", "shutupdrasil-1.18.jar" })
            try (InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/jars/" + name)) {
                Files.copy(Objects.requireNonNull(is), new File(jarsDir, "fletchingtable-" + name).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException | NullPointerException e) {
                System.err.println("Could not extract FletchingTable dependency " + name + "!");
                e.printStackTrace();
            }

        project.afterEvaluate(this::afterEvaluate);

        project.getDependencies().getExtensions().create("includedJars", IncludedJarsExtension.class, project, fletchingTableExtension);
    }

    private void afterEvaluate(Project project) {
        FileCollection thisJar = project.files(getClass().getProtectionDomain().getCodeSource().getLocation());

        if (fletchingTableExtension.getEnableAnnotationProcessor().get()) {
            project.getDependencies().add(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME, thisJar);
            project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, project.files(new File(jarsDir, "fletchingtable-api.jar")));

            for (SourceSet sourceSet : project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets())
                for (File resourcesDir : sourceSet.getResources().getSrcDirs()) {
                    File jsonFile = new File(resourcesDir, "fabric.mod.json");
                    if (!jsonFile.exists())
                        continue;

                    for (File generatedSourcesDir : sourceSet.getOutput().getGeneratedSourcesDirs())
                        if (fletchingTableExtension.writeAPSettings(new File(generatedSourcesDir, "fletchingtable/ap.properties")))
                            for (Task classes : project.getTasksByName("compileJava", false))
                                classes.getOutputs().upToDateWhen(task -> false);
                }


            if (fletchingTableExtension.getEnableEntrypoints().get())
                for (Task classes : project.getTasksByName("classes", false))
                    classes.doLast("ftEntrypoints", this::entrypoints);

            if (fletchingTableExtension.getEnableMixins().get())
                for (Task classes : project.getTasksByName("classes", false))
                    classes.doLast("ftMixins", this::mixins);
        }
    }

    private void entrypoints(Task task) {
        for (SourceSet sourceSet : task.getProject().getExtensions().getByType(JavaPluginExtension.class).getSourceSets())
            for (File resourcesDir : sourceSet.getResources().getSrcDirs()) {
                File jsonFile = new File(resourcesDir, "fabric.mod.json");
                if (!jsonFile.exists())
                    continue;

                for (File generatedSourcesDir : sourceSet.getOutput().getGeneratedSourcesDirs()) {
                    File entrypointsFile = new File(generatedSourcesDir, "fletchingtable/entrypoints.txt");
                    if (!entrypointsFile.exists())
                        continue;

                    final Map<String, Set<String>> entrypoints = new HashMap<>();

                    try {
                        JsonObject modJson;
                        try (FileReader fileReader = new FileReader(jsonFile)) {
                            modJson = JsonParser.parseReader(fileReader).getAsJsonObject();
                        }

                        JsonObject jEntrypoints = modJson.getAsJsonObject("entrypoints");
                        if (jEntrypoints == null)
                            jEntrypoints = new JsonObject();

                        for (Map.Entry<String, JsonElement> entry : jEntrypoints.entrySet()) {
                            Set<String> ep = entrypoints.computeIfAbsent(entry.getKey(), s -> new LinkedHashSet<>());
                            for (JsonElement element : entry.getValue().getAsJsonArray())
                                ep.add(element.getAsString());
                        }

                        Files.lines(entrypointsFile.toPath(), StandardCharsets.UTF_8)
                                .forEachOrdered(line -> {
                                    if (!line.isEmpty()) {
                                        int i = line.indexOf(' ');
                                        entrypoints.computeIfAbsent(line.substring(i + 1), s -> new LinkedHashSet<>()).add(line.substring(0, i));
                                    }
                                });

                        for (Map.Entry<String, Set<String>> entry : entrypoints.entrySet()) {
                            JsonArray ep = new JsonArray();

                            for (String s : entry.getValue())
                                ep.add(s);

                            jEntrypoints.add(entry.getKey(), ep);
                        }

                        try (FileReader fileReader = new FileReader(jsonFile = new File(sourceSet.getOutput().getResourcesDir(), "fabric.mod.json"))) {
                            modJson = JsonParser.parseReader(fileReader).getAsJsonObject();
                        }

                        if (jEntrypoints.size() == 0)
                            modJson.remove("entrypoints");
                        else
                            modJson.add("entrypoints", jEntrypoints);

                        Gson gson = new Gson();
                        try (FileWriter fileWriter = new FileWriter(jsonFile); JsonWriter writer = gson.newJsonWriter(fileWriter)) {
                            writer.setIndent("    ");
                            gson.toJson(modJson, writer);
                        }
                    } catch (IOException e) {
                        task.getProject().getLogger().debug("Errored while inserting entrypoints.", e);
                    }
                }
            }
    }

    private void mixins(Task task) {
        for (SourceSet sourceSet : task.getProject().getExtensions().getByType(JavaPluginExtension.class).getSourceSets())
            for (File resourcesDir : sourceSet.getResources().getSrcDirs()) {
                File jsonFile = new File(resourcesDir, "fabric.mod.json");
                if (!jsonFile.exists())
                    continue;

                for (File generatedSourcesDir : sourceSet.getOutput().getGeneratedSourcesDirs()) {
                    File mixinsFile = new File(generatedSourcesDir, "fletchingtable/mixins.txt");
                    if (!mixinsFile.exists())
                        continue;

                    try {
                        final Map<String, MixinEnvironment.Env> mixins = new HashMap<>();

                        Files.lines(mixinsFile.toPath(), StandardCharsets.UTF_8)
                                .forEachOrdered(line -> {
                                    if (!line.isEmpty()) {
                                        int i = line.indexOf(' ');
                                        mixins.put(line.substring(0, i), MixinEnvironment.Env.valueOf(line.substring(i + 1).toUpperCase(Locale.ENGLISH)));
                                    }
                                });

                        JsonObject json;
                        try (FileReader fileReader = new FileReader(jsonFile)) {
                            json = JsonParser.parseReader(fileReader).getAsJsonObject();
                        }

                        MixinEnvironment.Env modEnvironment = MixinEnvironment.Env.MIXINS;
                        JsonElement jEnvironment = json.get("environment");
                        if (jEnvironment != null)
                            switch (jEnvironment.getAsString()) {
                                case "client":
                                    modEnvironment = MixinEnvironment.Env.CLIENT; break;
                                case "server":
                                    modEnvironment = MixinEnvironment.Env.SERVER; break;
                            }

                        JsonArray jMixinConfigs = json.getAsJsonArray("mixins");
                        if (jMixinConfigs == null || jMixinConfigs.isEmpty())
                            continue; //todo generate new mixin config

                        for (JsonElement jMixinConfig : jMixinConfigs) {
                            String configName;
                            MixinEnvironment.Env environment = modEnvironment;
                            if (jMixinConfig.isJsonObject()) {
                                configName = jMixinConfig.getAsJsonObject().get("config").getAsString();

                                if (environment == MixinEnvironment.Env.MIXINS) {
                                    jEnvironment = jMixinConfig.getAsJsonObject().get("environment");
                                    if (jEnvironment != null)
                                        switch (jEnvironment.getAsString()) {
                                            case "client":
                                                environment = MixinEnvironment.Env.CLIENT;
                                                break;
                                            case "server":
                                                environment = MixinEnvironment.Env.SERVER;
                                                break;
                                        }
                                }
                            } else
                                configName = jMixinConfig.getAsString();

                            try (FileReader fileReader = new FileReader(jsonFile = new File(sourceSet.getOutput().getResourcesDir(), configName))) {
                                json = JsonParser.parseReader(fileReader).getAsJsonObject();
                            }

                            JsonElement jMixinRoot = json.get("package");
                            if (jMixinRoot != null) {
                                JsonArray jMixins = new JsonArray(), jClient = new JsonArray(), jServer = new JsonArray();

                                String mixinRoot = jMixinRoot.getAsString();

                                for (Map.Entry<String, MixinEnvironment.Env> entry : mixins.entrySet())
                                    if (entry.getKey().startsWith(mixinRoot)) {
                                        String relMixin = entry.getKey().substring(mixinRoot.length() + 1);
                                        switch (environment == MixinEnvironment.Env.MIXINS ? entry.getValue() : environment) {
                                            case MIXINS: jMixins.add(relMixin); break;
                                            case CLIENT: jClient.add(relMixin); break;
                                            case SERVER: jServer.add(relMixin); break;
                                        }
                                    }

                                json.add("mixins", jMixins.isEmpty() ? null : jMixins);
                                json.add("client", jClient.isEmpty() ? null : jClient);
                                json.add("server", jServer.isEmpty() ? null : jServer);

                                Gson gson = new Gson();
                                try (FileWriter fileWriter = new FileWriter(jsonFile); JsonWriter writer = gson.newJsonWriter(fileWriter)) {
                                    writer.setIndent("    ");
                                    gson.toJson(json, writer);
                                }
                            }
                        }

                    } catch (Exception e) {
                        task.getProject().getLogger().debug("Errored while inserting mixins.", e);
                    }
                }
            }
    }

}
