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
}
