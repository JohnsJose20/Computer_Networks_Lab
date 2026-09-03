import java.net.*;
import java.util.*;

public class client {

    public static void main(String[] args) throws Exception {

        DatagramSocket client = new DatagramSocket();

        Scanner sc = new Scanner(System.in);

        System.out.print("Enter new-generation English sentence: ");
        String sentence = sc.nextLine();

        byte[] sendData = sentence.getBytes();

        InetAddress address = InetAddress.getByName("localhost");

        DatagramPacket sendPacket =
                new DatagramPacket(
                        sendData,
                        sendData.length,
                        address,
                        9002
                );

        client.send(sendPacket);

        byte[] receiveData = new byte[1024];

        DatagramPacket receivePacket =
                new DatagramPacket(
                        receiveData,
                        receiveData.length
                );

        client.receive(receivePacket);

        String result = new String(
                receivePacket.getData(),
                0,
                receivePacket.getLength()
        );

        System.out.println("\nTranslated string sent back to the client:");
        System.out.println(result);

        client.close();
    }
}
