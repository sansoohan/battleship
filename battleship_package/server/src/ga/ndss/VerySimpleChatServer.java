/*
src> javac -d ../classes ga/ndss/*.java ga/ndss/observer/*.java ga/ndss/subject/*.java
src> cd ../classes
classes> jar -cvmf manifest.txt gameserver.jar .
classes> java -jar gameserver.jar
*/
package ga.ndss;

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DecimalFormat;
import ga.ndss.*;
import ga.ndss.observer.*;
import ga.ndss.subject.*;

public class VerySimpleChatServer{
    public static void main(String[] args) {
        new VerySimpleChatServer().go();
    }
    public void go() {
        try {
            ServerSocket serverSock = new ServerSocket(5000);
            while(true) {
                // 클라이언트가 접속되면,
                Socket clientSocket = serverSock.accept();
                // 클라이언트 소켓을 객체로 넘겨주고,
                ClientHandler newClient = new ClientHandler(clientSocket);
                // 스레드에서 각 클아이언트들이 작동한다.
                new Thread(newClient).start();
                System.out.println("got a connection");
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
//VerySimpleChatServer End
