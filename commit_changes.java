//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 27
//SOURCES utils.java
//DEPS org.springframework.boot:spring-boot-starter:4.1.0

import org.springframework.util.Assert;

static void git(Path dir, String... args) throws Exception {
    var cmd = new ArrayList<String>(List.of("git"));
    cmd.addAll(List.of(args));
    var exit = Runner.run(dir, cmd.toArray(String[]::new));
    Assert.state(exit == 0, String.join(" ", cmd) + " failed in " + dir + " (exit " + exit + ")");
}

void main() throws Exception {
    var when = Instant.now() + "";
    try (var br = new BufferedReader(new InputStreamReader(System.in), 1 << 16)) {
        for (var line : br.readAllLines()) {
            if (!line.isBlank()) {
                var dir = Paths.get(line.trim());
                if (Files.isDirectory(dir) && Files.isDirectory(dir.resolve(".git"))) {
                    var changes = Runner.runAndReturnOutputLines(dir, "git", "status", "--porcelain");
                    if (changes.stream().allMatch(String::isBlank)) {
                        IO.println("nothing to commit in " + dir + ", skipping");
                        continue;
                    }
                    git(dir, "add", ".");
                    git(dir, "commit", "-am", "automatic save @ " + when);
                    git(dir, "push");
                }
            }
        }
    }
}

