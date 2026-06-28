//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0

void main() throws IOException {
    var start = Paths.get("").toAbsolutePath();
    try (var stream = Files.walk(start)) {
        stream.forEach(path -> {
            if (Files.isDirectory(path) && path.getFileName().toString().equalsIgnoreCase(".git")) {
                try {
                    process(path);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}

private void process(Path gitDir) throws IOException, InterruptedException {
    var repo = gitDir.getParent().toAbsolutePath().normalize();
    var p = new ProcessBuilder("git", "status", "--porcelain")
            .directory(repo.toFile()).redirectErrorStream(true)
            .start();
    p.waitFor();
    try (var in = p.getInputStream()) {
        if (!new String(in.readAllBytes()).isBlank())
            IO.println(repo);
    }
}