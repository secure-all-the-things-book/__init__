void main() throws Exception {
    var organization = "secure-all-the-things-book";
    try (var executor = Executors.newVirtualThreadPerTaskExecutor();) {
        var start = Paths.get(".").resolve("../").toAbsolutePath().normalize().toString();
        IO.println("initializing from " + start);
        var uri = new URI("https://raw.githubusercontent.com/" + organization + "/__init__/main/repositories.txt");
        var url = uri.toURL();
        var callables = new ArrayList<Callable<Void>>();
        try (var reader = new BufferedReader(new InputStreamReader(url.openStream()))) {
            for (var line : reader.lines().toList()) {
                var dirToCreate = new File(start, line);
                var gitRepo = "git@github.com:" + organization + "/" + line + ".git";
                callables.add(() -> {
                    if (!dirToCreate.exists())
                        runCommand(start, "git", "clone", gitRepo, dirToCreate.getAbsolutePath());
                    else
                        runCommand(dirToCreate.getAbsolutePath(), "git", "pull");
                    return null;
                });
            }
        }
        executor.invokeAll(callables);
    }
}

private void runCommand(String workingDir, String... command) throws Exception {
    var pb = new ProcessBuilder(command);
    pb.directory(new File(workingDir));
    pb.inheritIO();
    var process = pb.start();
    var exitCode = process.waitFor();
    if (exitCode != 0) IO.println("Command failed with exit code: " + exitCode);
}