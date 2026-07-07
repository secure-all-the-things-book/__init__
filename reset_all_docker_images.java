//usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 25
//SOURCES utils.java

void main() throws Exception {
    var f = new File(".").getAbsoluteFile().toPath();
    var lines = Runner.runAndReturnOutputLines(f, "docker", "ps", "-aq");
    for (var l : lines) {
        var trimLine = l.trim();
        if (!trimLine.isEmpty())
            Runner.runAndReturnProcess(f, "docker", "rm", "-f", trimLine);
    }
}