import java.io.*;
import java.net.*;

public class server {

    public static void main(String[] args) throws Exception {

        ServerSocket server = new ServerSocket(9002);
        System.out.println("Server waiting for connection...");

        Socket socket = server.accept();
        System.out.println("Client Connected.");

        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

        int n = in.readInt();

        int[][] matrix = (int[][]) in.readObject();

        System.out.println("\nReceived Matrix:");

        for(int i=0;i<n;i++)
        {
            for(int j=0;j<n;j++)
                System.out.print(matrix[i][j]+" ");
            System.out.println();
        }

        boolean upper=true;
        boolean lower=true;
        boolean diagonal=true;

        for(int i=0;i<n;i++)
        {
            for(int j=0;j<n;j++)
            {
                if(i>j && matrix[i][j]!=0)
                    upper=false;

                if(i<j && matrix[i][j]!=0)
                    lower=false;

                if(i!=j && matrix[i][j]!=0)
                    diagonal=false;
            }
        }

        String result;

        if(diagonal)
            result="Diagonal Matrix";
        else if(upper)
            result="Upper Triangular Matrix";
        else if(lower)
            result="Lower Triangular Matrix";
        else
            result="Normal Matrix";

        System.out.println("\nMatrix Type : "+result);

        DataOutputStream out =
                new DataOutputStream(socket.getOutputStream());

        out.writeUTF(result);

        socket.close();
        server.close();
    }
}
