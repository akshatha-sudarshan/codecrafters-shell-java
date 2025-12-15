import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        // TODO: Uncomment the code below to pass the first stage
        for (; ; ) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();
            if (input.equals("exit")) {
                break;
            }
            String[] inputArray = input.split(" ");
            if (inputArray[0].equals("type") && inputArray.length == 2) {
                if (inputArray[1].equals("echo") || inputArray[1].equals("exit") || inputArray[1].equals("type")
                        || inputArray[1].equals("pwd")||inputArray[1].equals("cd")) {
                    System.out.println(inputArray[1] + " is a shell builtin");
//                    continue;
            } else {
                String executablePath = findExecutableOnPath(inputArray[1]);
                if (executablePath != null) {
                    System.out.println(inputArray[1] + " is " + executablePath);
//                        continue;
                } else {
                    System.out.println(inputArray[1] + ": not found");
//                        continue;
                }
            }

//                System.out.println("This is a simple echo program.");
//                continue;
        } else if (inputArray[0].equals("echo")) {
            for (int i = 1; i < inputArray.length; i++) {
                System.out.print(inputArray[i]);
                if (i != inputArray.length - 1) {
                    System.out.print(" ");
                }
            }
            System.out.println();
        } else if (inputArray[0].equals("pwd")) {
            Path absolutePath = Paths.get(".").toAbsolutePath().normalize();
//                System.out.println(System.getProperty("user.dir"));
            System.out.println(absolutePath.toString());
        } else if (inputArray[0].equals("cd")) {
            if (inputArray.length == 1) {
                String userHome = System.getProperty("user.home");
                System.setProperty("user.dir", userHome);
                System.out.println(userHome);
            } else if (inputArray.length == 2) {
                File dir = new File(inputArray[1]);
                if (dir.exists() && dir.isDirectory()) {
                    String newPath = dir.getCanonicalPath();
                    System.setProperty("user.dir", newPath);
//                    System.out.println(newPath);
                } else {
                    System.out.println("cd: " + inputArray[1] + ": No such file or directory");
                }
            } else {
                System.out.println("cd: too many arguments");}
            } else {
            String command = inputArray[0];
            String executablePath = findExecutableOnPath(command);
            if (executablePath != null) {
                String response = invokeExecutable(command, java.util.Arrays.copyOfRange(inputArray, 1, inputArray.length));
                if (response != null) {
                    System.out.println(response);
//                        System.out.println();
                }
            } else {
                System.out.println(command + ": command not found");
            }

        }

    }

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

    public static String invokeExecutable(String executablePath, String... arguments) {
        try {
            // 1. Create a ProcessBuilder instance
            // The first element is the command/executable path, followed by its arguments.
            // We use a List/Array to handle arguments correctly.
            java.util.List<String> command = new java.util.ArrayList<>();
            command.add(executablePath);
            for (String arg : arguments) {
                command.add(arg);
            }

            ProcessBuilder builder = new ProcessBuilder(command);

            // Optional: Redirect standard error to standard output
            // This is useful so you don't miss error messages.
            builder.redirectErrorStream(true);


            // 2. Start the process
            Process process = builder.start();

            // 3. Read the output (stdout and stderr combined due to redirectErrorStream(true))
            // We use a BufferedReader for efficient line-by-line reading.
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            StringBuilder output = new StringBuilder();

            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
            }

            // 4. Wait for the process to exit and get the exit code
            int exitCode = process.waitFor();
            String response = output.toString().trim();
            return response;
//            System.out.println(output.toString());
//            System.out.println("Process exited with code: " + exitCode);

        } catch (IOException e) {
            System.err.println("Error executing command: " + e.getMessage());
        } catch (InterruptedException e) {
            // This occurs if the current thread is interrupted while waiting for the process
            Thread.currentThread().interrupt();
            System.err.println("Process waiting interrupted: " + e.getMessage());
        }
        return null;
    }
}
