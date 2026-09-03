import java.net.*;
import java.util.*;

public class server {

    public static void main(String[] args) throws Exception {

        DatagramSocket server = new DatagramSocket(9002);

        byte[] receiveData = new byte[1024];
        byte[] sendData;

        System.out.println("Server waiting for message...");

        DatagramPacket receivePacket =
                new DatagramPacket(receiveData, receiveData.length);

        server.receive(receivePacket);

        String sentence = new String(
                receivePacket.getData(),
                0,
                receivePacket.getLength()
        );

        System.out.println("\nReceived sentence:");
        System.out.println(sentence);

        String[] words = sentence.split("\\s+");

        Map<String, String> dict = new HashMap<>();

        dict.put("tbh", "to be honest");
        dict.put("ig", "I guess");
        dict.put("tbf", "to be fair");
        dict.put("atm", "at the moment");
        dict.put("irl", "in real life");
        dict.put("lol", "laugh out loud");
        dict.put("asap", "as soon as possible");
        dict.put("omg", "oh my god");
        dict.put("ttyl", "talk to you later");
        dict.put("idk", "I don't know");
        dict.put("nvm", "never mind");

        for (int i = 0; i < words.length; i++) {

            String word = words[i];

            String punctuation = "";

            if (word.matches(".*[,.!?]$")) {
                punctuation = word.substring(word.length() - 1);
                word = word.substring(0, word.length() - 1);
            }

            if (dict.containsKey(word.toLowerCase())) {
                words[i] = dict.get(word.toLowerCase()) + punctuation;
            }
        }

        String result = String.join(" ", words);

        System.out.println("\nTranslated sentence:");
        System.out.println(result);

        sendData = result.getBytes();

        DatagramPacket sendPacket =
                new DatagramPacket(
                        sendData,
                        sendData.length,
                        receivePacket.getAddress(),
                        receivePacket.getPort()
                );

        server.send(sendPacket);

        server.close();
    }
}
