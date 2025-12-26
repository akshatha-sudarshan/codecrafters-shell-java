import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
//            System.out.print(shellState.getCurrentPath() + "$ ");
            System.out.print("$ ");

            if (!scanner.hasNextLine()) break;
            input = scanner.nextLine();

            if (input.equals("exit")) {
                break;
            }

            // --- Command Parsing ---
//            String[] inputArray = input.trim().split("\\s+");
            List<String> parsedTokens = parseArguments(input);
            if (parsedTokens.size() == 0 || parsedTokens.get(0).isEmpty()) continue;

            String command = parsedTokens.get(0);
            String args = parsedTokens.size() > 1 ? parsedTokens.get(1) : "";
//            String[] commandArgs = Arrays.copyOfRange(parsedTokens, 1, parsedTokens.size());
            List<String> commandArgs = parsedTokens.size() > 1 ? parsedTokens.subList(1, parsedTokens.size()) : new ArrayList<>();
            String output = commandExecution(parsedTokens, command, commandArgs);
            // --- Output Redirection Handling ---
            Boolean redirectionExists = redirectionExists(commandArgs);

            if (redirectionExists) {
                if (parsedTokens.size() >= 4) {
//                    String fileName = parsedTokens.get(3);

                    String delimiter = "> ";
                    int index = parsedTokens.indexOf(delimiter);
                    String fileName = "";
                    if (index != -1) {
                        // index + 2 to skip the '>' and the ' '
//                        fileName = input.substring(index + delimiter.length()).trim();
                        fileName=parsedTokens.getLast();
                        System.out.println("name = "+fileName);
                    }
                    try {
                        if (fileName.isEmpty()) {
                            System.out.println("Syntax error: No file specified for output redirection.");
                            continue;
                        }
                        java.nio.file.Files.writeString(java.nio.file.Paths.get(shellState.getCurrentPath(), fileName), output);
                    } catch (IOException e) {
                        System.out.println("Error writing to file: " + e.getMessage());
                    }
                }
                else {
                    System.out.println("Syntax error: No file specified for output redirection.");
                }
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

    public String invokeExecutable(String executablePath, String workingDirectory,
//                                   String... arguments
                                   List<String> arguments) {
        try {
            // ... (ProcessBuilder command list creation remains the same)
            java.util.List<String> command = new ArrayList<>();
            command.add(executablePath);
            command.addAll(arguments);

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

    public static java.util.List<String> parseArguments(String input) {
        java.util.List<String> args = new java.util.ArrayList<>();
        StringBuilder currentArg = new StringBuilder();
        boolean inSingleQuotes = false;
        boolean insideArg = false;
        boolean insideDoubleQuote = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == '\\' && !inSingleQuotes && !insideDoubleQuote) {
                if (i + 1 < input.length()) {
                    // Peek at the next character and append it directly
                    currentArg.append(input.charAt(i + 1));
                    i++; // Skip the next character in the next loop iteration
                    insideArg = true;
                    continue; // Move to the next character in the string
                }
            } else if (c == '\\' && insideDoubleQuote) {
                if (i + 1 < input.length()) {
                    char nextChar = input.charAt(i + 1);
                    // Only certain characters are escaped inside double quotes
                    if (nextChar == '\"' || nextChar == '\\')
//                            || nextChar == '$' || nextChar == '`')
                    {
                        currentArg.append(nextChar);
                        i++; // Skip the next character in the next loop iteration
                    } else {
                        currentArg.append(c); // Keep the backslash if not escaping a special char
                    }
                    insideArg = true;
                    continue; // Move to the next character in the string
                }
            }
            if (c == '\"' && !inSingleQuotes) {
                // TOGGLE mode: don't append the quote itself
                insideDoubleQuote = !insideDoubleQuote;
                insideArg = true;
            } else if (c == '\'' && !insideDoubleQuote) {
                // TOGGLE mode: don't append the quote itself
                inSingleQuotes = !inSingleQuotes;
                insideArg = true;
            } else if ((Character.isWhitespace(c) && !inSingleQuotes && !insideDoubleQuote) || (Character.isWhitespace(c) && !insideDoubleQuote && !inSingleQuotes)) {
                // Space outside of quotes finishes the current word
                if (insideArg) {
                    args.add(currentArg.toString());
                    currentArg.setLength(0);
                    insideArg = false;
                }
            } else {
                // Regular character or space INSIDE quotes
                currentArg.append(c);
                insideArg = true;
            }
        }

        // Add the final piece
        if (insideArg) {
            args.add(currentArg.toString());
        }

        return args;
    }

    public String commandExecution(List<String> parsedTokens, String command, List<String> commandArgs) {
        String output = "";
        // --- Command Execution ---
        switch (command) {
            case "echo":
                // Your original echo logic
                for (int i = 1; i < parsedTokens.size(); i++) {
//                        System.out.print(parsedTokens.get(i));
                    output += parsedTokens.get(i);
                    if (i != parsedTokens.size() - 1) {
//                            System.out.print(" ");
                        output += " ";
                    }
                }
                System.out.println();
                break;
            case "pwd":
                // NEW PWD: Uses the persistent state
//                    System.out.println(shellState.getCurrentPath());
                output = shellState.getCurrentPath();
                break;
            case "cd":
                // NEW CD: Uses the persistent state manager
                if (parsedTokens.size() <= 2) {
                    String targetPath = parsedTokens.size() == 1 ? "" : parsedTokens.get(1); // Empty for `cd` with no args
//                        System.out.println("Changing directory to: " + targetPath);
                    if (!shellState.changeDirectory(targetPath)) {
//                            System.out.println("cd: " + targetPath + ": No such file or directory");
                        output = "cd: " + targetPath + ": No such file or directory";
                    }
                } else {
//                        System.out.println("cd: too many arguments");
                    output = "cd: too many arguments";
                }
                break;
            case "type":
                // Your original type logic (modified slightly for clean break)
                if (parsedTokens.size() == 2) {
                    String target = parsedTokens.get(1);
                    if (target.equals("echo") || target.equals("exit") || target.equals("type")
                            || target.equals("pwd") || target.equals("cd")) {
//                            System.out.println(target + " is a shell builtin");
                        output = target + " is a shell builtin";
                    } else {
                        String executablePath = findExecutableOnPath(target);
                        if (executablePath != null) {
//                                System.out.println(target + " is " + executablePath);
                            output = target + " is " + executablePath;
                        } else {
//                                System.out.println(target + ": not found");
                            output = target + ": not found";
                        }
                    }
                } else {
//                        System.out.println("type: too many arguments");
                    output = "type: too many arguments";
                }
                break;
            default:
                // External Command Execution
                String executablePath = findExecutableOnPath(command);
                if (executablePath != null) {
                    // Pass the current working directory to the invoke method
                    String response = invokeExecutable(command, shellState.getCurrentPath(), commandArgs);
                    if (response != null && !response.isEmpty()) {
//                            System.out.println(response);
                        output = response;
                    }
                } else {
//                        System.out.println(command + ": command not found");
                    output = command + ": command not found";
                }
                break;
        }
        return output;
    }

    public Boolean redirectionExists(List<String> commandArgs) {
//        Pattern pattern = Pattern.compile("\\s*(>|1>)\\s*\\S+\\s*$");
//        Matcher matcher = pattern.matcher(input);
        // Check for exact matches
         boolean hasRedirection = commandArgs.stream()
                .anyMatch(s -> s.equals(">") || s.equals("1>"));
        return hasRedirection;
//        return matcher.find();
    }
}
