package io.shcm.shsupercm.fabric.fletchingtable;

import io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({ "io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint.Repeated", "io.shcm.shsupercm.fabric.fletchingtable.api.Entrypoint" })
public class FTAnnotationProcessor extends AbstractProcessor {
    private Writer writer;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        try {
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "fletchingtable", "entrypoints.txt");
            file.delete();
            writer = file.openWriter();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try {
            for (Element element : roundEnv.getElementsAnnotatedWith(Entrypoint.class))
                process(element, element.getAnnotation(Entrypoint.class).value());

            for (Element element : roundEnv.getElementsAnnotatedWith(Entrypoint.Repeated.class))
                for (Entrypoint entrypoint : element.getAnnotation(Entrypoint.Repeated.class).value())
                    process(element, entrypoint.value());

            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    public void process(Element element, String entrypoint) throws IOException {
        switch (element.getKind()) {
            case CLASS:
                writer.append(element.toString());
                break;
            case METHOD:
            case FIELD:
                writer.append(element.getEnclosingElement().toString())
                        .append("::")
                        .append(element.getSimpleName());
                break;
            default:
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Got annotated @Entrypoint of unexpected type " + element.getKind() + "!");
        }
        writer.append(" ")
                .append(entrypoint)
                .append("\n");
    }
}
