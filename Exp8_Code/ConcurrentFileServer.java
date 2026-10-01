import java.io.*;
import java.net.*;

public class ConcurrentFileServer {
    private static final int PORT = 5000;

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            long pid = ProcessHandle.current().pid();

            System.out.println("Concurrent File Server Started...");
            System.out.println("Server PID: " + pid);
            System.out.println("Listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();

                System.out.println("New client connected: "
                        + clientSocket.getInetAddress());

                // Create new thread for each client - This makes it concurrent
                new Thread(new ClientHandler(clientSocket, pid)).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {

    private Socket clientSocket;
    private long serverPid;

    public ClientHandler(Socket socket, long pid) {
        this.clientSocket = socket;
        this.serverPid = pid;
    }

    @Override
    public void run() {

        try (
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));

            PrintWriter out = new PrintWriter(
                    clientSocket.getOutputStream(), true)
        ) {

            // 1. Read filename from client
            String fileName = in.readLine();

            System.out.println("[" + Thread.currentThread().getId()
                    + "] Requested file: " + fileName);

            File file = new File(fileName);

            // 2. Send PID first
            out.println("Server PID: " + serverPid);
            out.println("Servicing Thread ID: "
                    + Thread.currentThread().getId());

            if (file.exists() && file.isFile()) {

                out.println("--- FILE FOUND ---");

                BufferedReader fileReader =
                        new BufferedReader(new FileReader(file));

                String line;

                while ((line = fileReader.readLine()) != null) {
                    out.println(line);
                }

                fileReader.close();

            } else {

                out.println("--- ERROR: File '" + fileName
                        + "' does not exist on server. ---");
            }

            out.println("EOF"); // End of file marker

        } catch (IOException e) {
            e.printStackTrace();

        } finally {

            try {
                clientSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
