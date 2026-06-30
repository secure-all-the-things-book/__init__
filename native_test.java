//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//SOURCES Runner.java

import org.springframework.util.FileSystemUtils;

void main() throws Exception {
    var target = Path.of("target");
    var cwd = Path.of(".").toAbsolutePath();
    if (Files.exists(target)) FileSystemUtils.deleteRecursively(target.toFile());
    Runner.run(cwd, "mvn", "-DskipTests", "spring-javaformat:apply", "clean", "package");
    Runner.run(cwd, "mvn", "-Pnative", "spring-boot:process-aot");
    Runner.run(cwd, "mvn", "-PnativeTest", "test");
}