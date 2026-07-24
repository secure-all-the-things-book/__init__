//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//DEPS org.asciidoctor:asciidoctorj:3.0.0


import org.asciidoctor.Asciidoctor;
import org.asciidoctor.Attributes;
import org.asciidoctor.Options;
import org.asciidoctor.SafeMode;
import org.asciidoctor.ast.Document;
import org.asciidoctor.ast.StructuralNode;
import org.asciidoctor.log.LogHandler;
import org.asciidoctor.log.LogRecord;
import org.asciidoctor.log.Severity;
import org.springframework.util.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

void main() throws IOException {
    var root = new File(".").getCanonicalFile();
    try (var stream = Files.walk(root.toPath())) {
        var adocs = stream.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".adoc")).toList();
        for (var input : adocs)
            Auditor.validateAdoc(input.toFile(), root);
    }
}

class Auditor {

    private static void unresolvedCallout(Errors errors, LogRecord message) {
        if (message.getMessage().contains("no callout found for")) errors.unresolvedCallouts().add(context(message));
    }

    private static String context(LogRecord record) {
        return record.getCursor() + "::" + record.getMessage();
    }

    private static void unresolvedInclude(Errors errors, LogRecord message) {
        if (message.getMessage().contains("include file not found:"))
            errors.unresolvedIncludes().add(context(message));
    }

    private static Errors validate(File adoc, File codeRootFolder) {
        var analysers = List.<ErrorClassifier>of(Auditor::unresolvedCallout, Auditor::unresolvedInclude);
        var errors = new Errors(new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
        IO.println("inspecting " + adoc.getAbsolutePath() + " with {code} value " + codeRootFolder.getAbsolutePath());
        try (var asciidoctor = Asciidoctor.Factory.create()) {
            var handler = (LogHandler) record -> {
                if (record.getSeverity() == Severity.ERROR || record.getSeverity() == Severity.WARN)
                    for (var analyser : analysers)
                        analyser.accept(errors, record);
            };
            asciidoctor.registerLogHandler(handler);
            var attributes = Attributes.builder().attribute("code", codeRootFolder.getAbsolutePath()).build();
            var options = Options //
                    .builder() //
                    .safe(SafeMode.UNSAFE) //
                    .baseDir(adoc.getParentFile()) //
                    .toFile(false)
                    .attributes(attributes).build();
            var adocDocument = asciidoctor.loadFile(adoc, options);
            for (var missingImage : collectBadImages(adoc.getParentFile(), adocDocument)) {
                errors.unresolvedImages().add(missingImage.target());
            }
            asciidoctor.convertFile(adoc, options); // we don't care about the result
        }
        return errors;
    }

    private static List<BadImage> collectBadImages(File baseDir, Document doc) {
        var list = new ArrayList<BadImage>();
        collectBadImages(doc, doc, baseDir, list);
        return list;
    }

    private static void collectBadImages(StructuralNode node, Document doc, File baseDir, List<BadImage> out) {
        if ("image".equals(node.getContext())) {
            var target = (String) node.getAttribute("target");
            if (target != null && !isRemote(target)) {
                var imagesDir = (String) doc.getAttribute("imagesdir", "");
                var resolved = baseDir.toPath().resolve(imagesDir == null ? "" : imagesDir).resolve(target).normalize();
                if (!resolved.toFile().exists()) {
                    var line = node.getSourceLocation() != null ? node.getSourceLocation().getLineNumber() : -1;
                    out.add(new BadImage(target, resolved, line));
                }
            }
        }
        // recurse into children (sections, blocks, list items, etc.)
        for (var child : node.getBlocks()) {
            if (child instanceof StructuralNode sn) {
                collectBadImages(sn, doc, baseDir, out);
            }
        }
    }

    private static boolean isRemote(String target) {
        try {
            var uri = java.net.URI.create(target);
            return uri.getScheme() != null && (uri.getScheme().startsWith("http") || uri.getScheme().equals("data"));
        } catch (Exception e) {
            return false;
        }
    }

    private static void validateAdoc(File input, File root) {
        Assert.state(input.exists(), "input file must exist");
        Assert.state(root.exists(), "code folder must exist");

        var errors = validate(input, root);

        for (var entry : errors.unresolvedImages())
            IO.println("missing image: " + entry);

        for (var entry : errors.unresolvedCallouts())
            IO.println("missing callouts: " + entry);

        for (var entry : errors.unresolvedIncludes())
            IO.println("missing include: " + entry);
    }

    interface ErrorClassifier extends BiConsumer<Errors, LogRecord> {
    }

    public static record Errors(Collection<String> unresolvedIncludes, //
                                Collection<String> unresolvedImages,//
                                Collection<String> unresolvedCallouts //
    ) {
    }

    private static record BadImage(String target, Path resolved, int line) {
    }
}