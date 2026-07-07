//usr/bin/env jbang "$0" "$0" "$@" ; exit $?
//JAVA 25
//DEPS org.springframework.boot:spring-boot-starter:4.1.0
//SOURCES utils.java

import org.springframework.util.Assert;
import java.io.* ;

/**
 * NOTE: the shebang line passes the script name as a parameter and is
 * thus different to 99% of the other JBang.dev scripts
 */
void main(String[] args) throws Exception {
    var fqnScriptName = args[0];
    var bad = (fqnScriptName == null || fqnScriptName.isEmpty());
    Assert.state(!bad, "what is the name of the current script?");
    var directoryOfScript = new File(new File(fqnScriptName).getParent())
            .getAbsoluteFile();
    var findChangesScript = new File(directoryOfScript, "find_changes.java").getAbsolutePath();
    var commmitChangesScript =   new File (directoryOfScript ,"commit_changes.java").getAbsolutePath();
    
    for (var fqn: new String []{findChangesScript, commmitChangesScript})
        Assert.state (new File(fqn).exists(), fqn +" exists");

    var cwd = new File (".").getAbsoluteFile().toPath();
    var lines = Runner.runAndReturnOutputLines(cwd, findChangesScript);
    
    for (var changedDirectory : lines) {
        invoke(commmitChangesScript, changedDirectory);
    }
}

static void invoke (String program, String line) throws Exception {
    IO.println ("invoking " +program + " for line " + line);
    var dir = new File(".").getAbsoluteFile();
    // stdin must be a PIPE so we can feed the changed directory to the child;
    // stdout/stderr INHERIT so the child's output streams straight to our console.
    // (Runner.runButDontWait uses inheritIO() for all three, which would leave
    //  getOutputStream() disconnected and the child reading EOF on stdin.)
    var process = new ProcessBuilder(program)
            .directory(dir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
    IO.println("line: " + line);
    try (
        var pos = process.getOutputStream();
        var osw = new OutputStreamWriter(pos);
        var stdin = new BufferedWriter(osw)
    ) {
        stdin.write(line.trim());
        stdin.newLine();
        stdin.flush();
    }
    var exit = process.waitFor();
    Assert.state(exit == 0, program + " failed for '" + line + "' (exit " + exit + ")");
}