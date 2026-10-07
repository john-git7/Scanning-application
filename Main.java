import java.util.*;

public class Main {
    public static void main(String[] args) {

        ScanController scanController = new ScanController();
        Scanner sc = new Scanner(System.in);
        while (true) {
            String command = sc.nextLine();
            scanController.handleCommand(command);
            if (command.trim().equalsIgnoreCase("exit")) {
                break;
            }
        }
        sc.close();
    }
}