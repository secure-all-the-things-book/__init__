//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//SOURCES Runner.java

void main() throws Exception {
    var when = Instant.now() + "";
    try (var br = new BufferedReader(new InputStreamReader(System.in), 1 << 16)) {
        for (var line : br.readAllLines()) {
            if (!line.isBlank()) {
                var dir = Paths.get(line.trim());
                if (Files.isDirectory(dir) && Files.isDirectory(dir.resolve(".git"))) {
                    var c = Runner.run(dir, "git", "commit", "-am", "automatic save @ " + when);
                    if (c == 0) {
                        Runner.run(dir, "git", "push");
                    } //
                    else {
                        IO.println("commit returned " + c + " (nothing to commit?), skipping push");
                    }
                }
            }
        }
    }
}

