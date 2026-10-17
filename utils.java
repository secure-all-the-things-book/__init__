import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.io.*;

class Runner {


    static List<String> runAndReturnOutputLines(Path dir, String... cmd) throws IOException, InterruptedException {
        var ps = runAndReturnProcess(dir, cmd);
        try (var r = new BufferedReader(new InputStreamReader(ps.getInputStream()))) {
            var lines = r.readAllLines();
            ps.waitFor();
            return lines;
        }
    }

    // NB: doesn't wait for the process to exit: callers must drain its output first,
    // otherwise a chatty process fills the pipe buffer and blocks forever
    static Process runAndReturnProcess(Path dir, String... cmd) throws IOException {
        var cwd = dir.toAbsolutePath().normalize();
        return new ProcessBuilder(cmd).directory(cwd.toFile()).redirectErrorStream(true).start();
    }

    static Process runButDontWait(Path dir, String... cmd) throws IOException, InterruptedException {
        return new ProcessBuilder(cmd).directory(dir.toFile()).inheritIO().start();
    }

    static int run(Path dir, String... cmd) throws IOException, InterruptedException {
        return runButDontWait(dir, cmd)
                .waitFor();
    }

}
