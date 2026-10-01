import java.io.*;
import java.net.*;
import java.util.Scanner;

public class FileClient {

    public static void main(String[] args) {

        String serverAddress = "localhost";
        int port = 5000;

        try (
            Socket socket = new Socket(serverAddress, port);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);

            Scanner sc = new Scanner(System.in)
        ) {

            System.out.print(
                    "Enter filename to request from server: ");

            String fileName = sc.nextLine();

            // Send filename to server
            out.println(fileName);

            // Receive and display response
            String responseLine;

            System.out.println("\n--- Response from Server ---");

            while ((responseLine = in.readLine()) != null) {

                if (responseLine.equals("EOF"))
                    break;

                System.out.println(responseLine);
            }

        } catch (IOException e) {

            System.out.println("Could not connect to server.");
            e.printStackTrace();
        }
    }
}
