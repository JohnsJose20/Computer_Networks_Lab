import java.net.*;

public class TimeServer {

    public static void main(String[] args) throws Exception {

        DatagramSocket socket = new DatagramSocket(5000);

        System.out.println("Time Server is running...");

        while (true) {

            byte[] buffer = new byte[100];

            DatagramPacket request =
                    new DatagramPacket(buffer, buffer.length);

            socket.receive(request);

            // Create a new thread for each client
            new Thread(() -> {

                try {

                    String time = new java.util.Date().toString();

                    byte[] data = time.getBytes();

                    DatagramPacket response =
                            new DatagramPacket(
                                    data,
                                    data.length,
                                    request.getAddress(),
                                    request.getPort()
                            );

                    socket.send(response);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();
        }
    }
}
