package ga.ndss;
import java.io.*;
import java.net.*;
import java.util.*;

import ga.ndss.*;
import ga.ndss.observer.*;
import ga.ndss.subject.*;

public class ClientHandler implements Runnable {
    private SingletonServer serverOnlyOne;
    private RoomObserver observer;
    private String clientID = "";
    private String clientName = "";
    private boolean roomMaster = false;
    private boolean ready = false;
    private boolean start = false;
    private int roomNumber = 0;
    PrintWriter writer;
    BufferedReader reader;
    public ClientHandler(Socket clientSocket) {
        try {
            // 클라이언트소켓으로 입출력스트림을 만들어서 클라이언트 객체에 주고,
            writer = new PrintWriter(clientSocket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // 서버객체는 싱글턴
            serverOnlyOne = SingletonServer.getInstance();
            // 서버객체에 클아이언트를 추가한다.
            serverOnlyOne.addClient(this);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    public void run() {
        String message;
        try {
            // 메시지가 다 사라질때까지 한줄씩 읽어들이는데,
            while ((message = reader.readLine()) != null) {
                System.out.println("read " + message);
                // 메시지가 /로 시작하는 글자열은 명령어로 분류,
                if(message.charAt(0)=='/'){
                    // 대기실에 있으면 아래와 같은 명령을 쓰며,
                    if(roomNumber==0){
                        if(message.indexOf("make")==1){makeRoom(message);}
                        else if(message.indexOf("join")==1){joinRoom(message);}
                        else if(message.indexOf("test")==1){test(message);}
                        else if(message.indexOf("login")==1){login(message);}
                        else if(message.indexOf("newaccount")==1){newAccount(message);}
                        else if(message.indexOf("passwdlost")==1){passwdLost(message);}
                    }
                    // 게임방에 있으면 아래와 같은 명령을 쓴다.
                    else{
                        if(message.indexOf("ready")==1){readyRoom();}
                        else if(message.indexOf("leave")==1){leaveRoom();}
                        else if(message.indexOf("start")==1){startTheGame();}
                        else if(message.indexOf("attack")==1){fireOnMap(message);}
                    }
                }
                // 그외에는 일반 채팅으로 취급한다.
                else{
                    tellEveryone(message);
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    // 명령어 테스트
    public void test(String message){
        System.out.println(message);
    }
    // 서버는 클라이언트가 방 만들기 명령어를 보내기를 기다린다.
    public void makeRoom(String message){
        // /make 000 의 형식으로 읽어들인다.
        int temp = Integer.parseInt(message.substring(6,9));
        // 클라이언트들 중에서 어떤 클라이언트가
        for(int i=0;i<serverOnlyOne.getClients().size();i++){
            // 해당하는 방번호를 가지고 있으면,
            if(serverOnlyOne.getClients().get(i).getRoomNumber()==temp){
                // 그 방은 이미 만들어져 있으므로 들어갈 수 없다.
                try {
                    writer.println("Room"+temp+" is already exist");
                    writer.flush();
                } catch (Exception ex) { ex.printStackTrace(); }
                return;
            }
            // 방을 다 찼고도 해당하는 방번호를 가지고 있는 클라이언트가 없으면,
            if(i == serverOnlyOne.getClients().size()-1){
                // 그 방은 아직 없으므로 만들 수 있다.
                serverOnlyOne.sendmessageToAllClient("/clientstate "+clientID+" from "+roomNumber+" to "+temp);
                try {
                    // 클라이언트 GUI 상에서 방을 만들게끔 클라이언트에게 명령어를 전송한다.
                    writer.println("/make "+temp+" ok");
                    writer.flush();
                } catch (Exception ex) { ex.printStackTrace(); }
                // 방을 만들고 클라이언트를 추가한다.
                observer = new RoomObserver(temp);
                observer.addClient(this);
                // 게임에 새로 만들어진 방을 등록한다.
                serverOnlyOne.registObserver(observer);
                // 클라이언트는 방번호를 부여받고,
                roomNumber = temp;
                // 방을 만든 클라이언트는 방장이 된다.
                roomMaster = true;
            }
        }
    }
    // 서버는 클라이언트가 방 들어가기 명령어를 보내기를 기다린다.
    public void joinRoom(String message){
        // /join 000 의 형식으로 읽어들인다.
        int temp = Integer.parseInt(message.substring(6,9));
        // 이미 만들어져 있는 방들중에서,
        for(RoomObserver observer : serverOnlyOne.getRoomObservers()){
            // 방이 해당하는 번호를 가지고 있으면서,
            if(observer.getRoomNumber()==temp){
                // 게임중이면,
                if(observer.getClientsInRoom().get(0).getStart()==true){
                    // 그 게임중이라 들어갈 수 없다고 알려준다.
                    try {
                        writer.println("Room"+temp+" is now playing");
                        writer.flush();
                    } catch (Exception ex) { ex.printStackTrace(); }
                    break;
                }
                // 정원 6명에 도달했으면,
                if(observer.getClientsInRoom().size()==6){
                    // 그 방이 정원에 도달해서 들어갈 수 없다고 알려준다.
                    try {
                        writer.println("Room"+temp+" is full");
                        writer.flush();
                    } catch (Exception ex) { ex.printStackTrace(); }
                    break;
                }
                // 특별한 예외가 없으면 방에 들어간다.
                this.observer = observer;
                observer.addClient(this);
                // 클라이언트 GUI 상에서 방을 들어가게끔 클라이언트에게 명령어를 전송한다.
                serverOnlyOne.sendmessageToAllClient("/clientstate "+clientID+" from "+roomNumber+" to "+temp);
                try {
                    writer.println("/join "+temp+" ok");
                    writer.flush();
                } catch (Exception ex) { ex.printStackTrace(); }
                roomNumber = temp;
                break;
            }
        }
        // 해당하는 번호를 가진 방이 없으면 방이 없다고 알려준다.
        try {
            writer.println("Room"+temp+" is not exits");
            writer.flush();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    public void login(String message){
        String[] userInfo = message.split(" ");
        try {
            if(userInfo.length!=3){
                writer.println("/login notentered");
                writer.flush();
                return;
            }
            File desti = new File("userInfo");
            if(!desti.exists()){
                desti.mkdirs();
            }
            File destiFile = new File("userInfo/userPrivacy.txt");
            destiFile.createNewFile();
            BufferedReader fileReader = new BufferedReader(new FileReader("userInfo/userPrivacy.txt"));
            ArrayList<String> userDatabase = new ArrayList<String>();
            String buffer;
            String[] splitedBuffer;
            while((buffer = fileReader.readLine()) != null){
                splitedBuffer = buffer.split(",");
                if(splitedBuffer[0].equals(userInfo[1]) && splitedBuffer[1].equals(userInfo[2])){
                    clientID = splitedBuffer[0];
                    clientName = splitedBuffer[2];
                    serverOnlyOne.sendmessageToAllClient("/clientstate "+clientID+" from "+roomNumber+" to "+roomNumber);
                    for(ClientHandler otherClientID : serverOnlyOne.getClients()){
                        writer.println("/clientstate "+otherClientID.getClientID()+" from "+otherClientID.getRoomNumber()+" to "+otherClientID.getRoomNumber());
                        writer.flush();
                    }
                    fileReader.close();
                    writer.println("/login ok "+splitedBuffer[0]+" "+splitedBuffer[2]);
                    writer.flush();
                    return;
                }
            }
            writer.println("/login mismatch");
            writer.flush();
            fileReader.close();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    public void newAccount(String message){
        String[] userInfo = message.split(" ");
        String buffer;
        String[] splitedBuffer;

        try{
            File desti = new File("userInfo");
            if(!desti.exists()){
                desti.mkdirs();
            }
            File destiFile = new File("userInfo/userPrivacy.txt");
            destiFile.createNewFile();
            BufferedReader fileReader = new BufferedReader(new FileReader("userInfo/userPrivacy.txt"));
            while((buffer = fileReader.readLine()) != null){
                splitedBuffer = buffer.split(",");
                if(splitedBuffer[0].equals(userInfo[1])){
                    writer.println("/newaccount idcollision");
                    writer.flush();
                    fileReader.close();
                    return;
                }
            }
            PrintWriter pw = new PrintWriter(new FileWriter("userInfo/userPrivacy.txt",true));
            String toFile = "";
            for(int i=1;i<userInfo.length;i++){
                toFile += userInfo[i];
                if(i!=userInfo.length-1){
                    toFile += ",";
                }
            }
            pw.println(toFile);
            pw.close();
            writer.println("/newaccount ok");
            writer.flush();
        }catch(Exception ex){ex.printStackTrace();}
    }
    public void passwdLost(String message){
        String[] userInfo = message.split(" ");
        if(userInfo.length!=3){
            try {
                writer.println("Please enter your id and email");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
            return;
        }
        try {
            BufferedReader fileReader = new BufferedReader(new FileReader("userInfo/userPrivacy.txt"));
            ArrayList<String> userDatabase = new ArrayList<String>();
            String buffer;
            String[] splitedBuffer;
            while((buffer = fileReader.readLine()) != null){
                splitedBuffer = buffer.split(",");
                if(splitedBuffer[0].equals(userInfo[1]) && splitedBuffer[3].equals(userInfo[2])){
                    writer.println("Password will be sent to your email");
                    writer.flush();
                    fileReader.close();
                    return;
                }
            }
            writer.println("There is no matching user");
            writer.flush();
            fileReader.close();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    // 서버는 클라이언트가 게임준비 명령어를 보내기를 기다린다.
    public void readyRoom(){
        // 게임준비는 스위칭 형식으로 켜고 끌 수 있으며,
        if(ready==false){
            ready = true;
        }
        else{
            ready = false;
        }
        // 준비 상태가 유저에게 전달된다.
        try {
            writer.println("You are "+(ready?"ready":"not ready"));
            writer.flush();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    // 서버는 클라이언트가 방나가기 명령어를 보내기를 기다린다.
    public void leaveRoom(){
        // 게임중이라면, 
        if(this.start == true){
            // 나갈 수 없다고 알려준다.
            try {
                writer.println("You can't leave while playing");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        // 게임중이 아니면,
        else{
            // 게임준비를 해제하고,
            ready = false;
            // 클라이언트를 방에게 제외한다.
            observer.removeClient(this);
            // 만약 클라이언트가 방장이면,
            if(roomMaster == true){
                // 방장권한을 해제하고,
                roomMaster = false;
                // 방 안에 있는 다른 클라이언트가 없으면,
                if(observer.getClientsInRoom().size()==0){
                    // 방을 닫는다.
                    serverOnlyOne.unregistObserver(observer);
                }
                // 방 안에 있는 다른 클라이언트가 있으면,
                else{
                    ClientHandler client = observer.getClientsInRoom().get(0);
                    // 방장을 넘겨준다.
                    try {
                        client.getWriter().println("You are now room master");
                        client.getWriter().flush();
                        client.setRoomMaster(true);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
            // 클라이언트 GUI 상에서 방을 나오게끔 클라이언트에게 명령어를 전송한다.
            try {
                writer.println("/leave ok");
                writer.flush();
                serverOnlyOne.sendmessageToAllClient("/clientstate "+clientID+" from "+roomNumber+" to "+0);
            } catch (Exception ex) { ex.printStackTrace(); }
            // 클라이언트의 방번호를 0(대기실)로 바꾼다.
            roomNumber = 0;
        }
    }
    // 서버는 클라이언트가 게임시작 명령어를 보내기를 기다린다.
    public void startTheGame(){
        // 방장이면 게임을 시작할 권한이 있다.
        if(this.roomMaster==true){
            this.start=true;
            // 클라이언트들 중에서,
            Iterator it = serverOnlyOne.getClients().iterator();
            int playersCount = 0;
            while (it.hasNext()) {
                ClientHandler client = (ClientHandler)it.next();
                // 같은 방번호를 가진 클라이언트에 대해,
                if(this.roomNumber==client.getRoomNumber()){
                    playersCount++;
                    // 게임준비 상태가 아닌 클라이언트들이 있으면,
                    if(client.getReady()==false){
                        try {
                            // 그 클라이언트에게 게임준비를 해달라고 메시지를 보내고,
                            client.getWriter().println("Ready Please");
                            client.getWriter().flush();
                            // 방장에게는 어느 클라이언트가 준비가 안 되어있는지 알려준다.
                            this.getWriter().println(client.getClientName()+" is not ready");
                            this.getWriter().flush();
                            // 게임을 시작할 수 없는 상태로 바꾼다.
                            this.start = false;
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
            }
            // 방에 있는 사람이 1명이거나 6명을 초과하면,
            if(playersCount==1 || playersCount>6){
                // 게임을 시작할 수 없다.
                this.start = false;
            }
            // 게임을 시작할 수 없는 상태이면,
            if(this.start==false){
                // 게임을 시작할 수 없다고 메시지를 보낸다.
                try {
                    this.getWriter().println("Can't start the game");
                    this.getWriter().flush();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
            // 게임을 시작할 수 있는 상태이면,
            else{
                // 게임을 만든다.
                serverOnlyOne.initGame(observer);
            }
        }
        // 방장이 아니면 게임을 시작할 권한이 없다.
        else{
            try {
                writer.println("You are not a room master");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
    // 서버는 클라이언트가 게임내 공격 명령어를 보내기를 기다린다.
    public void fireOnMap(String message){
        // 게임이 시작되지 않았으면,
        if(this.start == false){
            // 게임이 시작되지 않았다고 알려준다.
            try {
                writer.println("The game is not started");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
            return;
        }
        // 해당 플레이어 차례이면,
        if(serverOnlyOne.getClients().indexOf(this)==observer.getPlayerTurn()){
            // /attack 00 라는 명령어를 서버가 받아서 좌표로 변환하고
            int j = Integer.parseInt(message.substring(8,10))/7;
            int k = Integer.parseInt(message.substring(8,10))%7;
            // 모든 플에이어들에게 어느 좌표가 공격되었는지 알려주고
            serverOnlyOne.sendmessageToRoomPlayer(observer, observer.getPlayerTurn()+1+"P fired on ("+k+","+j+")");
            // 게임방에서 좌표를 공격한다.
            serverOnlyOne.attackPos(observer,j,k);
            // 공격한 후에 다음플레이어의 차례로 넘긴다.
            observer.nextPlayer();
        }
        // 해당 플레이어 차례가 아니면,
        else{
            // 해당 플레이어의 차례가 아니라고 메시지를 보낸다.
            try {
                writer.println("It is not your turn");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
    // 서버는 클라이언트가 일반 메시지를 보내기를 기다린다.
    public void tellEveryone(String message) {
        // 클라이언트들 중에서
        Iterator it = serverOnlyOne.getClients().iterator();
        while (it.hasNext()) {
            ClientHandler client = (ClientHandler)it.next();
            // 같은 방에 있는 클라이언트에게만
            if(this.roomNumber==client.getRoomNumber()){
                // 메시지를 보낸다.
                try {
                    client.getWriter().println(message);
                    client.getWriter().flush();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
    }
    public void resetClient(){
        start = false;
        ready = false;
    }
    public int getRoomNumber(){
        return roomNumber;
    }
    public boolean getReady(){
        return ready;
    }
    public boolean getStart(){
        return start;
    }
    public PrintWriter getWriter(){
        return writer;
    }
    public String getClientName(){
        return clientName;
    }
    public String getClientID(){
        return clientID;
    }
    public Observer getObserver(){
        return observer;
    }
    public void setStart(boolean start){
        this.start = start;
    }
    public void setRoomMaster(boolean roomMaster){
        this.roomMaster = roomMaster;
    }
    public void setObserver(Observer observer){
        this.observer = (RoomObserver)observer;
    }
}
//ClientHandler End
