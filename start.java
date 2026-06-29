//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0

import org.springframework.core.io.UrlResource;

void main(String[] args) throws Exception {
    var organization = args.length > 0 ? args[0] : "secure-all-the-things-book";
    try (var executor = Executors.newVirtualThreadPerTaskExecutor();) {
        var start = Paths.get(".").toAbsolutePath().normalize().toString();
        IO.println("initializing from " + start);
        var uriResource = new UrlResource(new URI("https://raw.githubusercontent.com/" + organization +
                "/pipeline/refs/heads/main/src/main/resources/application.properties?cb=" + System.currentTimeMillis()));
        var callables = new ArrayList<Callable<Void>>();
        var content =repositories( uriResource.getContentAsString(Charset.defaultCharset()));
        for (var repoName : content)
            callables.add(buildCallable(start, organization, repoName));
        executor.invokeAll(callables);
    }
}

private List<String> repositories(String propertiesBuffer) throws IOException {
    var properties = new Properties();
    var totalRepositories = new ArrayList<String>();
    totalRepositories.add("__init__");
    totalRepositories.add("design-assets");
    try (var bin = new ByteArrayInputStream(propertiesBuffer.getBytes())) {
        properties.load(bin);
        if (properties.get("pipeline.job.code-repositories") instanceof String repositories) {
            totalRepositories.addAll(Arrays
                    .stream(repositories.split(",")) //
                    .filter(l -> !l.isBlank()) //
                    .map(uri -> {
                        var arrImAPirate = uri.split("/");
                        return arrImAPirate[arrImAPirate.length - 1]
                                .replace(".git", "")
                                .trim();
                    }) //
                    .toList());
        }
    }
    return totalRepositories;
}

private Callable<Void> buildCallable(String start, String organization, String repoName) {
    return () -> {
        var dirToCreate = new File(start, repoName);
        var absolutePath = dirToCreate.getAbsolutePath();
        if (!dirToCreate.exists())
            runCommand(start, "git", "clone", "git@github.com:" + organization + "/" + repoName + ".git", absolutePath);
        else runCommand(absolutePath, "git", "pull");
        return null;
    };
}

private void runCommand(String workingDir, String... command) throws Exception {
    var pb = new ProcessBuilder(command);
    pb.directory(new File(workingDir));
    pb.inheritIO();
    var process = pb.start();
    if (process.waitFor() != 0) IO.println("Command failed");
}