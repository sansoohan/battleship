package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.sound.midi.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;

import ga.ndss.*;

public class SimpleChatClient{
    private JTextArea incoming;
    // 객체간 메시지 전송
    private Thread readerThread;
    private GameWindows windows;
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket sock;
    private int initCount=0;
    public SimpleChatClient(JTextArea incoming, GameWindows windows){
        this.incoming = incoming;
        this.windows = windows;
        readerThread = new Thread(new IncomingReader());
    }
    // 서버와 연결하고 입출력 스트림을 저장한다.
    public void setUpNetworking(String url) {
        try {
            sock = new Socket(url, 5000);
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            writer = new PrintWriter(sock.getOutputStream());
            System.out.println("networking established");
        }
        catch(IOException ex){ex.printStackTrace();}
        readerThread.start();
    }
    // 서버로부터 메시지를 받는 스레드.
    class IncomingReader implements Runnable {
        public void run() {
            String message;
            try {
                // 메시지가 없을 때까지 읽어들이는데,
                while ((message = reader.readLine()) != null) {
                    System.out.println("client read " + message);
                    // '/' 로 시작하는 메시지는 명령어로 간주하고 채팅창에는 표시하지 않는다.
                    if(message.charAt(0)=='/'){
                        if(message.indexOf("init")==1){initGrid(message);}
                        else if(message.indexOf("make")==1){makeRoom(message);}
                        else if(message.indexOf("join")==1){joinRoom(message);}
                        else if(message.indexOf("leave")==1){leaveRoom(message);}
                        else if(message.indexOf("attack")==1){fire(message);}
                        else if(message.indexOf("resetgame")==1){resetGame(message);}
                        else if(message.indexOf("login")==1){login(message);}
                        else if(message.indexOf("newaccount")==1){newAccount(message);}
                        else if(message.indexOf("clientstate")==1){clientState(message);}
                    }
                    // 그렇지 않은 메시지는 그냥 채팅창에 표시한다.
                    else{
                        incoming.append(message + "\n");
                    }
                }
            } catch (IOException ex){ex.printStackTrace();}
        }
        // 게임시작 시에 서버로부터 49개 좌표값을 연속으로 받아온다.
        public void initGrid(String message){
            windows.playRoom.grid[initCount] = message.substring(6,10);
            initCount++;
            // 49개를 다 받았으면 애니메이션을 시작한다.
            if(initCount==49){
                windows.playRoom.eventMidi();
                windows.playRoom.pic.repaint();
            }
        }
        // 클라이언트는 /make 000 을 보내고, 서버로부터 /make 000 ok 를 기다린다.
        public void makeRoom(String message){
            String[] makeMessages = message.split(" ");
            if(makeMessages[2].equals("ok")){
                // 대기실을 지우고 게임룸을 보여준다.
                windows.waitRoom.setVisible(false);
                windows.playRoom.setVisible(true);
                // 클라이언트가 서버로부터 받은 메시지를 게임룸의 채팅창으로 가게끔 변경.
                incoming = windows.playRoom.roomChatArea;
            }
        }
        // 클라이언트는 /join 000 을 보내고, 서버로부터 /join 000 ok 를 기다린다.
        public void joinRoom(String message){
            // 메시지를 " "로 쪼개서,
            String[] joinMessages = message.split(" ");
            if(joinMessages[2].equals("ok")){
                // 대기실을 지우고 게임룸을 보여준다.
                windows.waitRoom.setVisible(false);
                windows.playRoom.setVisible(true);
                // 클라이언트가 서버로부터 받은 메시지를 게임룸의 채팅창으로 가게끔 변경.
                incoming = windows.playRoom.roomChatArea;
            }
        }
        // 클라이언트는 /leave 를 보내고, 서버로부터 /leave ok 를 기다린다.
        public void leaveRoom(String message){
            if(message.substring(7,9).equals("ok")){
                // 게임룸을 지우고 대기실을 보여준다.
                windows.waitRoom.setVisible(true);
                windows.playRoom.setVisible(false);
                // 클라이언트가 서버로부터 받은 메시지를 대기실의 채팅창으로 가게끔 변경.
                incoming = windows.waitRoom.publicChatArea;
            }
        }
        // 클라이언트는 /attack 00 를 보내고, 서버로부터 /attack 00 [??] 를 기다린다.
        public void fire(String message){
            // 1.클라이언트가 공격 좌표를 전송.
            // 2.서버가 공격좌표에 따른 적중여부 파악.
            // 3.적중결과를 모든 클라이언트에게 전송.
            int location = Integer.parseInt(message.substring(8,10));
            windows.playRoom.grid[location] = message.substring(11,15);
            System.out.println(windows.playRoom.grid[location]);
            windows.playRoom.pic.repaint();
        }
        // 클라이언트는, 서버로부터 /resetgame ok 를 기다린다.
        public void resetGame(String message){
            if(message.substring(11,13).equals("ok")){
                // 격자를 전부 [??]로 바꾸고 => 공격당한 지점이 전부 지워진다.
                windows.playRoom.resetGrid();
                // 저장한 애니메이션 프레임들을 모두 지운다.
                windows.playRoom.resetMidi();
                // /init 메시지를 받기 위해서 0으로 초기화한다.
                initCount=0;
            }
        }
        // 클라이언트는 /login id passwd를 보내고, 서버로부터 /login ok id name 을 기다린다.
        public void login(String message){
            String[] okMessages = message.split(" ");
            // 접속이 재대로 이루어지면 로그인창을 지우고 대기실을 보여준다.
            if(okMessages[1].equals("ok")){
                windows.playRoom.userID.setText(okMessages[2]);
                windows.playRoom.userName.setText(okMessages[3]);
                windows.waitRoom.setVisible(true);
                windows.loginWindow.setVisible(false);
            }
            // 서버로부터 id나 password 둘 중에 하나를 입력하지 않았다고 메시지를 받을 수 있다.
            else if(okMessages[1].equals("notentered")){
                JOptionPane.showConfirmDialog(null,"Please enter ID and Password");
            }
            // 서버로부터 id나 password 가 잘못 입력되었다고 메시지를 받을 수 있다.
            else if(okMessages[1].equals("mismatch")){
                JOptionPane.showConfirmDialog(null,"Please check your ID and Password");
            }
        }
        public void newAccount(String message){
            String[] okMessages = message.split(" ");
            // 새로운 계정을 정상적으로 만들었다면, 서버로부터 계정생성 메시지를 받을 수 있다.
            if(okMessages[1].equals("ok")){
                JOptionPane.showConfirmDialog(null,"Success!");
            }
            // 이미 존재하는 아이디라고, 서버로부터 에러 메시지를 받을 수 있다.
            else if(okMessages[1].equals("idcollision")){
                JOptionPane.showConfirmDialog(null,"The ID you entered is already exist");
            }
            // 아이디 형식이 맞지 않는다고, 서버로부터 에러 메시지를 받을 수 있다.
            else if(okMessages[1].equals("idformaterror")){
                JOptionPane.showConfirmDialog(null,"Special Character can't be used for ID");
            }
            // 이메일 형식이 맞지 않는다고, 서버로부터 에러 메시지를 받을 수 있다.
            else if(okMessages[1].equals("emailformaterror")){
                JOptionPane.showConfirmDialog(null,"Email format is not good");
            }
        }
        // 클라이언트가 방에 들어가거나 나올 때, 대기실을 업데이트 한다.
        public void clientState(String message){
            String stateMessages[] = message.split(" ");
            String clientID = stateMessages[1];
            int from = Integer.parseInt(stateMessages[3]);
            int to = Integer.parseInt(stateMessages[5]);
            // 클라이언트가 게임방에서 다른 곳으로 이동할 때,
            if(from!=0){
                ((Room)windows.waitRoom.rooms.getComponent(from-1)).removePlayer(clientID);
            }
            // 클라이언트가 대기실에서 다른 곳으로 이동할 때,
            else{
                for(int i=0;i<windows.waitRoom.clientIDs.size();i++){
                    if(windows.waitRoom.clientIDs.get(i).equals(clientID)){
                        windows.waitRoom.clientIDs.remove(i);
                        break;
                    }
                }
            }
            // 클라이언트가 게임방으로 이동할 때,
            if(to!=0){
                ((Room)windows.waitRoom.rooms.getComponent(to-1)).addPlayer(clientID);
            }
            // 클라이언트가 대기실로 이동할 때,
            else{
                windows.waitRoom.clientIDs.add(clientID);
            }
            windows.waitRoom.clientList.updateUI();
        }
    }
    public void setIncoming(JTextArea incoming){
        this.incoming = incoming;
    }
    public PrintWriter getWriter(){
        return writer;
    }
}