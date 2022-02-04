package io.shcm.shsupercm.fabric.fletchingtable;

import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import io.shcm.shsupercm.fabric.fletchingtable.api.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FTAnnotationProcessor extends AbstractProcessor {
    private Writer writerEntrypoints = null, writerMixins = null;
    private String defaultMixinEnvironment, autoMixinClientPrefix, autoMixinServerPrefix;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            FileObject file;
            if (processingEnv.getOptions().get("fletchingtable.entrypoints").equalsIgnoreCase("true")) {
                file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "fletchingtable", "entrypoints.txt");
                file.delete();
                writerEntrypoints = file.openWriter();
            }
            if (processingEnv.getOptions().get("fletchingtable.mixins").equalsIgnoreCase("true")) {
                file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "fletchingtable", "mixins.txt");
                file.delete();
                writerMixins = file.openWriter();

                defaultMixinEnvironment = processingEnv.getOptions().get("fletchingtable.mixins.default");

                autoMixinClientPrefix = processingEnv.getOptions().get("fletchingtable.mixins.prefix.client");
                if (autoMixinClientPrefix.equals("null"))
                    autoMixinClientPrefix = null;
                autoMixinServerPrefix = processingEnv.getOptions().get("fletchingtable.mixins.prefix.server");
                if (autoMixinServerPrefix.equals("null"))
                    autoMixinServerPrefix = null;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new HashSet<>();

        if (writerEntrypoints != null) {
            types.add("io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint.Repeated");
            types.add("io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint");
        }

        if (writerMixins != null)
            types.add("org.spongepowered.asm.mixin.Mixin");

        return types;
    }

    @Override
    public Set<String> getSupportedOptions() {
        HashSet<String> options = new HashSet<>();

        options.add("fletchingtable.entrypoints");
        options.add("fletchingtable.mixins");
        options.add("fletchingtable.mixins.default");
        options.add("fletchingtable.mixins.prefix.client");
        options.add("fletchingtable.mixins.prefix.server");

        return options;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            if (writerEntrypoints != null) {
                for (Element element : roundEnv.getElementsAnnotatedWith(Entrypoint.class))
                    processEntrypoint(element, element.getAnnotation(Entrypoint.class).value());

                for (Element element : roundEnv.getElementsAnnotatedWith(Entrypoint.Repeated.class))
                    for (Entrypoint entrypoint : element.getAnnotation(Entrypoint.Repeated.class).value())
                        processEntrypoint(element, entrypoint.value());

                if (roundEnv.processingOver()) {
                    writerEntrypoints.close();
                    writerEntrypoints = null;
                } else
                    writerEntrypoints.flush();
            }

            if (writerMixins != null) {
                for (Element element : roundEnv.getElementsAnnotatedWith(Mixin.class))
                    processMixin(element);

                if (roundEnv.processingOver()) {
                    writerMixins.close();
                    writerMixins = null;
                } else
                    writerMixins.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void processMixin(Element element) throws IOException {
        if (!element.getKind().isClass() && !element.getKind().isInterface())
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Got annotated @Mixin on unexpected type " + element.getKind() + "!");

        MixinEnvironment environmentOverride = element.getAnnotation(MixinEnvironment.class);
        String environment = environmentOverride == null ? defaultMixinEnvironment : environmentOverride.value().configName;

        if (environment.equals("auto")) {
            environment = "mixins";
            annotations: for (AnnotationMirror mixinMirror : element.getAnnotationMirrors())
                if (mixinMirror.getAnnotationType().toString().equals("org.spongepowered.asm.mixin.Mixin")) {
                    for (ExecutableElement key : mixinMirror.getElementValues().keySet())
                        if (key.toString().equals("value()") || key.toString().equals("targets()"))
                            //noinspection unchecked
                            for (Object targetObj : ((List<Object>) mixinMirror.getElementValues().get(key).getValue())) {
                                String target = targetObj.toString(); // safe for both Attribute.Class and String

                                if (autoMixinClientPrefix != null && target.startsWith(autoMixinClientPrefix)) {
                                    environment = "client";
                                    break annotations;
                                }
                                if (autoMixinServerPrefix != null && target.startsWith(autoMixinServerPrefix)) {
                                    environment = "server";
                                    break annotations;
                                }
                            }
                    break;
                }
        }

        if (!environment.equals("none"))
            writerMixins
                    .append(element.toString())
                    .append(" ")
                    .append(environment)
                    .append("\n");
    }

    public void processEntrypoint(Element element, String entrypoint) throws IOException {
        switch (element.getKind()) {
            case CLASS:
                writerEntrypoints.append(element.toString());
                break;
            case METHOD:
            case FIELD:
                writerEntrypoints
                        .append(element.getEnclosingElement().toString())
                        .append("::")
                        .append(element.getSimpleName());
                break;
            default:
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Got annotated @Entrypoint on unexpected type " + element.getKind() + "!");
                return;
        }
        writerEntrypoints
                .append(" ")
                .append(entrypoint)
                .append("\n");
    }
}
