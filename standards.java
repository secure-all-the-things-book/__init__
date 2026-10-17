//usr/bin/env jbang "$0" "$0" "$@" ; exit $?
//JAVA 27
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//SOURCES utils.java

import org.springframework.util.Assert;

static void removeUnwantedFiles(Path root) throws IOException {
    var remove = Set.of(".DS_Store");
    try (var paths = Files.walk(root)) {
        var unwanted = paths
                .filter(Files::isRegularFile)
                .filter(p -> remove.contains(p.getFileName().toString()))
                .toList();
        for (var p : unwanted) {
            IO.println("deleting " + p.toAbsolutePath().normalize());
            Files.delete(p);
        }
    }
}

/**
 * NOTE: the shebang line passes the script name as a parameter and is
 * thus different to 99% of the other JBang.dev scripts
 */
void main(String[] args) throws Exception {
    Assert.state(args.length > 0 && !args[0].isBlank(), "what is the name of the current script?");
    var here = Path.of(args[0]).toAbsolutePath().normalize().getParent();
    var cwd = Path.of(".");
    removeUnwantedFiles(cwd);
    for (var scriptName : new String[]{"adoc_standards.java", "code_standards.java"}) {
        var script = here.resolve(scriptName);
        Assert.state(Files.exists(script), "the script named " + script + " doesn't exist!");
        IO.println("invoking " + script);
        var exitCode = Runner.run(cwd, "jbang", script.toString());
        Assert.state(exitCode == 0, "the script " + script + " failed with exit code " + exitCode);
    }
}
