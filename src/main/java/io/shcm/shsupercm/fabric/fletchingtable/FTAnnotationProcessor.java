package io.shcm.shsupercm.fabric.fletchingtable;

import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;
import io.shcm.shsupercm.fabric.fletchingtable.api.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixin;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

public class FTAnnotationProcessor extends AbstractProcessor {
    private Writer writerEntrypoints = null, writerMixins = null;
    private String defaultMixinEnvironment = null;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            Properties settings = new Properties();
            try (InputStream is = processingEnv.getFiler().getResource(StandardLocation.SOURCE_OUTPUT, "fletchingtable", "ap.properties").openInputStream()) {
                settings.load(is);
            }
            defaultMixinEnvironment = settings.getProperty("mixins-default", "none");

            FileObject file;
            if (settings.getProperty("entrypoints", "true").equalsIgnoreCase("true")) {
                file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "fletchingtable", "entrypoints.txt");
                file.delete();
                writerEntrypoints = file.openWriter();
            }
            if (settings.getProperty("mixins", "true").equalsIgnoreCase("true")) {
                file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "fletchingtable", "mixins.txt");
                file.delete();
                writerMixins = file.openWriter();
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

                writerEntrypoints.flush();
            }

            if (writerMixins != null) {
                for (Element element : roundEnv.getElementsAnnotatedWith(Mixin.class)) {
                    MixinEnvironment environmentOverride = element.getAnnotation(MixinEnvironment.class);
                    String environment = environmentOverride == null ? defaultMixinEnvironment : environmentOverride.value().configName;

                    if (environment.equals("auto")) {
                        environment = "mixins";
                        for (AnnotationMirror mixinMirror : element.getAnnotationMirrors())
                            if (mixinMirror.getAnnotationType().toString().equals("org.spongepowered.asm.mixin.Mixin") && mixinMirror.toString().contains("net.minecraft.client")) {
                                environment = "client";
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

                writerMixins.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
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
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Got annotated @Entrypoint of unexpected type " + element.getKind() + "!");
        }
        writerEntrypoints
                .append(" ")
                .append(entrypoint)
                .append("\n");
    }
}
