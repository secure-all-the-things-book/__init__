//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
void main() throws Exception {
    var ps = new ProcessBuilder("docker", "ps", "-aq").start();
    ps.waitFor();
    try (var r = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
        for (var l : r.readAllLines())
            if (!l.trim().isEmpty())
                new ProcessBuilder("docker", "rm", "-f", l.trim())
                        .inheritIO().start().waitFor();
    }
}