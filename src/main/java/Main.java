import java.io.File;
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
            if(inputArray[0].equals("type") && inputArray.length == 2){
                if(inputArray[1].equals("echo")||inputArray[1].equals("exit")||inputArray[1].equals("type"))
                    System.out.println(inputArray[1]+" is a shell builtin");
                 else{
                    String executablePath = findExecutableOnPath(inputArray[1]);
                    if (executablePath != null) {
                        System.out.println(inputArray[1] + " is " + executablePath);
                        continue;
                    }
                    else {
                        System.out.println(inputArray[1]+": not found");
                        continue;
                    }
                }
                    System.out.println(inputArray[1]+": not found");

//                System.out.println("This is a simple echo program.");
                continue;
            }

            if(inputArray[0].equals("echo")){
                for(int i = 1; i < inputArray.length; i++){
                    System.out.print(inputArray[i]);
                    if(i != inputArray.length - 1){
                        System.out.print(" ");
                    }
                }
                System.out.println();
                continue;
            }
            System.out.println(input + ": command not found");
//            scanner.close();
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
}
