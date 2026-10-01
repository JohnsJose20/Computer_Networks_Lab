import java.net.*;

public class TimeClient {

    public static void main(String[] args) throws Exception {

        DatagramSocket socket = new DatagramSocket();

        // Send request to server
        byte[] data = "TIME".getBytes();

        InetAddress server =
                InetAddress.getByName("localhost");

        DatagramPacket request =
                new DatagramPacket(
                        data,
                        data.length,
                        server,
                        5000
                );

        socket.send(request);

        // Receive time from server
        byte[] buffer = new byte[100];

        DatagramPacket response =
                new DatagramPacket(
                        buffer,
                        buffer.length
                );

        socket.receive(response);

        String time = new String(
                response.getData(),
                0,
                response.getLength()
        );

        System.out.println("Server Time: " + time);

        socket.close();
    }
}
