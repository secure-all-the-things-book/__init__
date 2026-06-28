//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0

void main() throws IOException {
    var start = Paths.get("").toAbsolutePath();
    try (var stream = Files.walk(start)) {
        stream
                .filter(Files::isDirectory)
                .filter(p -> p.getFileName() != null
                        && p.getFileName().toString().equalsIgnoreCase(".git"))
                .forEach(path -> {
                    try {
                        process(path);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}

private void process(Path gitDir)
        throws IOException, InterruptedException {
    var repo = gitDir.getParent().toAbsolutePath().normalize();
    var p = new ProcessBuilder("git", "status", "--porcelain")
            .directory(repo.toFile())
            .redirectErrorStream(true)
            .start();
    var out = (String) null;
    try (var in = p.getInputStream()) {
        out = new String(in.readAllBytes());
    }
    p.waitFor();
    if (!out.isBlank()) {
        IO.println("there are changes in " + repo);
    }
}