import java.io.IOException;
import java.nio.file.Path;

class Runner {

    static Process runAndReturnProcess(Path path, String... cmd) throws IOException, InterruptedException {
        var repo = path.getParent().toAbsolutePath().normalize();
        var p = new ProcessBuilder(cmd).directory(repo.toFile()).redirectErrorStream(true).start();
        p.waitFor();
        return p;
    }

    static int run(Path dir, String... cmd) throws IOException, InterruptedException {
        IO.println("current directory is " + dir.toString());
        return new ProcessBuilder(cmd).directory(dir.toFile()).inheritIO().start().waitFor();
    }

}
