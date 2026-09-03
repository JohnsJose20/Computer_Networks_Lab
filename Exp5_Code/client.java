import java.io.*;
import java.net.*;
import java.util.*;

public class client {

    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("localhost",9002);

        Scanner sc = new Scanner(System.in);

        Random rand = new Random();

        System.out.print("Enter order of matrix : ");
        int n=sc.nextInt();

        int[][] matrix=new int[n][n];

        for(int i=0;i<n;i++)
        {
            for(int j=0;j<n;j++)
            {
                matrix[i][j]=rand.nextInt(50)+1;
            }
        }

        int type=rand.nextInt(4);

        switch(type)
        {
            case 0:         //Upper
                for(int i=0;i<n;i++)
                    for(int j=0;j<n;j++)
                        if(i>j)
                            matrix[i][j]=0;
                break;

            case 1:         //Lower
                for(int i=0;i<n;i++)
                    for(int j=0;j<n;j++)
                        if(i<j)
                            matrix[i][j]=0;
                break;

            case 2:         //Diagonal
                for(int i=0;i<n;i++)
                    for(int j=0;j<n;j++)
                        if(i!=j)
                            matrix[i][j]=0;
                break;

            default:        //Normal Matrix
                break;
        }

        System.out.println("\nGenerated Matrix");

        for(int i=0;i<n;i++)
        {
            for(int j=0;j<n;j++)
                System.out.print(matrix[i][j]+" ");
            System.out.println();
        }

        ObjectOutputStream out =
                new ObjectOutputStream(socket.getOutputStream());

        out.writeInt(n);
        out.writeObject(matrix);
        out.flush();

        DataInputStream in =
                new DataInputStream(socket.getInputStream());

        String result=in.readUTF();

        System.out.println("\nMatrix Type : "+result);

        socket.close();
    }
}
