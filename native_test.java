//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0

import org.springframework.util.FileSystemUtils;

void main() throws Exception {
    var target = Path.of("target");
    if (Files.exists(target)) FileSystemUtils.deleteRecursively(target.toFile());
    run("mvn", "-DskipTests", "spring-javaformat:apply", "clean", "package");
    run("mvn", "-Pnative", "spring-boot:process-aot");
    run("mvn", "-PnativeTest", "test");
}

void run(String... command) throws IOException, InterruptedException {
    var exitCode = 0;
    if ((exitCode = new ProcessBuilder(command).inheritIO().start().waitFor()) != 0)
        throw new RuntimeException("Command failed (exit " + exitCode + "): " + List.of(command));
}