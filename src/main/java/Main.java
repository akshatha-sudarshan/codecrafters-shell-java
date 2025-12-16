import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Main {

    // Instantiate the persistent state manager once
    private final ShellState shellState = new ShellState();

    public static void main(String[] args) throws Exception {
        new Main().startShell();
    }

    public void startShell() throws Exception {
        Scanner scanner = new Scanner(System.in);
        String input;

        for (; ; ) {
            // The prompt uses the current directory from the persistent state
            System.out.print(shellState.getCurrentPath() + "$ ");

            if (!scanner.hasNextLine()) break;
            input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            }

            // --- Command Parsing ---
            String[] inputArray = input.trim().split("\\s+");
            if (inputArray.length == 0 || inputArray[0].isEmpty()) continue;

            String command = inputArray[0];
            String args = inputArray.length > 1 ? inputArray[1] : "";
            String[] commandArgs = Arrays.copyOfRange(inputArray, 1, inputArray.length);

            // --- Command Execution ---
            switch (command) {
                case "echo":
                    // Your original echo logic
                    for (int i = 1; i < inputArray.length; i++) {
                        System.out.print(inputArray[i]);
                        if (i != inputArray.length - 1) {
                            System.out.print(" ");
                        }
                    }
                    System.out.println();
                    break;
                case "pwd":
                    // NEW PWD: Uses the persistent state
                    System.out.println(shellState.getCurrentPath());
                    break;
                case "cd":
                    // NEW CD: Uses the persistent state manager
                    if (inputArray.length <= 2) {
                        String targetPath = inputArray.length == 1 ? "" : inputArray[1]; // Empty for `cd` with no args
                        System.out.println("Changing directory to: " + targetPath);
                        if (!shellState.changeDirectory(targetPath)) {
                            System.out.println("cd: " + targetPath + ": No such file or directory");
                        }
                    } else {
                        System.out.println("cd: too many arguments");
                    }
                    break;
                case "type":
                    // Your original type logic (modified slightly for clean break)
                    if (inputArray.length == 2) {
                        String target = inputArray[1];
                        if (target.equals("echo") || target.equals("exit") || target.equals("type")
                                || target.equals("pwd") || target.equals("cd")) {
                            System.out.println(target + " is a shell builtin");
                        } else {
                            String executablePath = findExecutableOnPath(target);
                            if (executablePath != null) {
                                System.out.println(target + " is " + executablePath);
                            } else {
                                System.out.println(target + ": not found");
                            }
                        }
                    } else {
                        System.out.println("type: too many arguments");
                    }
                    break;
                default:
                    // External Command Execution
                    String executablePath = findExecutableOnPath(command);
                    if (executablePath != null) {
                        // Pass the current working directory to the invoke method
                        String response = invokeExecutable(executablePath, shellState.getCurrentPath(), commandArgs);
                        if (response != null && !response.isEmpty()) {
                            System.out.println(response);
                        }
                    } else {
                        System.out.println(command + ": command not found");
                    }
                    break;
            }
        }
        scanner.close();
    }

    public static String findExecutableOnPath(String executableName) {
        // 1. Retrieve the PATH environment variable
        String pathEnv = System.getenv("PATH");

        // Determine the path separator based on the operating system
        // Windows uses semicolon (;), Linux/macOS use colon (:)
        String pathSeparator = System.getProperty("path.separator");

        // 2. Split the PATH value into individual directory paths
        String[] pathDirectories = pathEnv.split(pathSeparator);

        // 3. Iterate through each directory
        for (String dirPath : pathDirectories) {
            // Create a File object for the directory
            File directory = new File(dirPath);

            // Check if the directory exists and is actually a directory
            if (!directory.exists() || !directory.isDirectory()) {
                continue; // Skip invalid or non-existent paths
            }

            // Construct the full path to the potential executable
            File potentialExecutable = new File(directory, executableName);

            // On Windows, executables often need a file extension (.exe, .bat, etc.)
            // The standard check for File.canExecute() handles this, but for a more
            // explicit search on Windows, you might need to check common extensions.
            // For cross-platform simplicity, we rely on File.canExecute() first.
            if (potentialExecutable.exists() && potentialExecutable.isFile() && potentialExecutable.canExecute()) {
                return potentialExecutable.getAbsolutePath();
            }

            // --- Windows Specific Check ---
            // If the system is Windows, executables often don't include the .exe extension
            // when typed, so we explicitly check for the common extensions.
            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                String[] windowsExtensions = {".exe", ".bat", ".cmd"};
                for (String ext : windowsExtensions) {
                    File potentialWindowsExecutable = new File(directory, executableName + ext);
                    if (potentialWindowsExecutable.exists() && potentialWindowsExecutable.isFile() && potentialWindowsExecutable.canExecute()) {
                        return potentialWindowsExecutable.getAbsolutePath();
                    }
                }
            }
        }

        return null; // Executable not found in any PATH directory
    }

    public String invokeExecutable(String executablePath, String workingDirectory, String... arguments) {
        try {
            // ... (ProcessBuilder command list creation remains the same)
            java.util.List<String> command = new ArrayList<>();
            command.add(executablePath);
            command.addAll(Arrays.asList(arguments));

            ProcessBuilder builder = new ProcessBuilder(command);

            // CRITICAL CHANGE: Set the working directory for the external process
            builder.directory(new File(workingDirectory));
            builder.redirectErrorStream(true);

            // ... (rest of the process start and output reading logic remains the same)
            Process process = builder.start();
            // ... (output reading and process.waitFor() logic)

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            int exitCode = process.waitFor();
            String response = output.toString().trim();
            return response;

        } catch (IOException | InterruptedException e) {
            // Handle error (e.g., if the file cannot be executed)
            return null;
        }
    }
}
