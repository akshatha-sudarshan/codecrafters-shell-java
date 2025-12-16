import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ShellState {
    private Path currentDirectory;

    public ShellState() {
        // Initialize with the absolute, normalized path of the JVM's starting directory.
        this.currentDirectory = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    public String getCurrentPath() {
        // Returns the path string for the PWD command
        return this.currentDirectory.toAbsolutePath().toString();
    }

    /**
     * Simulates the 'cd' command by attempting to change the internal directory state.
     *
     * @param targetPathString The user input path (e.g., "..", "/home/user").
     * @return true if the directory was successfully changed, false otherwise.
     */
    public boolean changeDirectory(String targetPathString) {
        Path resolvedPath;

        // Handle cd with no arguments: typically goes to the home directory.
        if (targetPathString == null || targetPathString.isEmpty() || targetPathString.equals("~")) {
            resolvedPath = Paths.get(System.getProperty("user.home")).normalize();
        } else {
            // 1. Resolve the input path against the current internal directory.
            resolvedPath = currentDirectory.resolve(targetPathString).normalize();
        }

        try {
            // 2. toRealPath() checks existence, resolves ".." and ".", and follows symlinks.
            Path newPath = resolvedPath.toRealPath();

            // 3. Verify the target is a directory before updating the state.
            if (Files.isDirectory(newPath)) {
                this.currentDirectory = newPath;
                return true; // Success
            } else {
                // If the path is valid but is a file (not a directory)
                return false;
            }
        } catch (IOException e) {
            // Catches exceptions like path not found, or access denied.
            return false;
        }
    }
}
