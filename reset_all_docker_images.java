//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 27
//SOURCES utils.java

void main() throws Exception {
    var f = new File(".").getAbsoluteFile().toPath();
    var lines = Runner.runAndReturnOutputLines(f, "docker", "ps", "-aq");
    for (var l : lines) {
        var trimLine = l.trim();
        if (!trimLine.isEmpty())
            Runner.runAndReturnOutputLines(f, "docker", "rm", "-f", trimLine);
    }
}