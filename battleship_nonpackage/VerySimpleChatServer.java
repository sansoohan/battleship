import java.io.*;
import java.net.*;
import java.util.*;
import java.text.DecimalFormat;

public class VerySimpleChatServer{
    public static void main(String[] args) {
        new VerySimpleChatServer().go();
    }
    public void go() {
        try {
            ServerSocket serverSock = new ServerSocket(5000);
            while(true) {
                // Ŭ���̾�Ʈ�� ���ӵǸ�,
                Socket clientSocket = serverSock.accept();
                // Ŭ���̾�Ʈ ������ ��ü�� �Ѱ��ְ�,
                ClientHandler newClient = new ClientHandler(clientSocket);
                // �����忡�� �� Ŭ���̾�Ʈ���� �۵��Ѵ�.
                new Thread(newClient).start();
                System.out.println("got a connection");
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
}
//VerySimpleChatServer End
class ClientHandler implements Runnable {
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
            // Ŭ���̾�Ʈ�������� ����½�Ʈ���� ���� Ŭ���̾�Ʈ ��ü�� �ְ�,
            writer = new PrintWriter(clientSocket.getOutputStream());
            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            // ������ü�� �̱���
            serverOnlyOne = SingletonServer.getInstance();
            // ������ü�� Ŭ���̾�Ʈ�� �߰��Ѵ�.
            serverOnlyOne.addClient(this);
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    public void run() {
        String message;
        try {
            // �޽����� �� ����������� ���پ� �о���̴µ�,
            while ((message = reader.readLine()) != null) {
                System.out.println("read " + message);
                // �޽����� /�� �����ϴ� ���ڿ��� ��ɾ�� �з�,
                if(message.charAt(0)=='/'){
                    // ���ǿ� ������ �Ʒ��� ���� ����� ����,
                    if(roomNumber==0){
                        if(message.indexOf("make")==1){makeRoom(message);}
                        else if(message.indexOf("join")==1){joinRoom(message);}
                        else if(message.indexOf("test")==1){test(message);}
                        else if(message.indexOf("login")==1){login(message);}
                        else if(message.indexOf("newaccount")==1){newAccount(message);}
                        else if(message.indexOf("passwdlost")==1){passwdLost(message);}
                    }
                    // ���ӹ濡 ������ �Ʒ��� ���� ����� ����.
                    else{
                        if(message.indexOf("ready")==1){readyRoom();}
                        else if(message.indexOf("leave")==1){leaveRoom();}
                        else if(message.indexOf("start")==1){startTheGame();}
                        else if(message.indexOf("attack")==1){fireOnMap(message);}
                    }
                }
                // �׿ܿ��� �Ϲ� ä������ ����Ѵ�.
                else{
                    tellEveryone(message);
                }
            }
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    // ��ɾ� �׽�Ʈ
    public void test(String message){
        System.out.println(message);
    }
    // ������ Ŭ���̾�Ʈ�� �� ����� ��ɾ �����⸦ ��ٸ���.
    public void makeRoom(String message){
        // /make 000 �� �������� �о���δ�.
        int temp = Integer.parseInt(message.substring(6,9));
        // Ŭ���̾�Ʈ�� �߿��� � Ŭ���̾�Ʈ��
        for(int i=0;i<serverOnlyOne.getClients().size();i++){
            // �ش��ϴ� ���ȣ�� ������ ������,
            if(serverOnlyOne.getClients().get(i).getRoomNumber()==temp){
                // �� ���� �̹� ������� �����Ƿ� �� �� ����.
                try {
                    writer.println("Room"+temp+" is already exist");
                    writer.flush();
                } catch (Exception ex) { ex.printStackTrace(); }
                return;
            }
            // ���� �� á�� �ش��ϴ� ���ȣ�� ������ �ִ� Ŭ���̾�Ʈ�� ������,
            if(i == serverOnlyOne.getClients().size()-1){
                // �� ���� ���� �����Ƿ� ���� �� �ִ�.
                serverOnlyOne.sendmessageToAllClient("/clientstate "+clientID+" from "+roomNumber+" to "+temp);
                try {
                    // Ŭ���̾�Ʈ GUI �󿡼� ���� ����Բ� Ŭ���̾�Ʈ���� ��ɾ �����Ѵ�.
                    writer.println("/make "+temp+" ok");
                    writer.flush();
                } catch (Exception ex) { ex.printStackTrace(); }
                // ���� ����� Ŭ���̾�Ʈ�� �߰��Ѵ�.
                observer = new RoomObserver(temp);
                observer.addClient(this);
                // ���ӿ� ���� ������� ���� ����Ѵ�.
                serverOnlyOne.registObserver(observer);
                // Ŭ���̾�Ʈ�� ���ȣ�� �ο��ް�,
                roomNumber = temp;
                // ���� ���� Ŭ���̾�Ʈ�� ������ �ȴ�.
                roomMaster = true;
            }
        }
    }
    // ������ Ŭ���̾�Ʈ�� �� ���� ��ɾ �����⸦ ��ٸ���.
    public void joinRoom(String message){
        // /join 000 �� �������� �о���δ�.
        int temp = Integer.parseInt(message.substring(6,9));
        // �̹� ������� �ִ� ����߿���,
        for(RoomObserver observer : serverOnlyOne.getRoomObservers()){
            // ���� �ش��ϴ� ��ȣ�� ������ �����鼭,
            if(observer.getRoomNumber()==temp){
                // �������̸�,
                if(observer.getClientsInRoom().get(0).getStart()==true){
                    // �� �������̶� �� �� ���ٰ� �˷��ش�.
                    try {
                        writer.println("Room"+temp+" is now playing");
                        writer.flush();
                    } catch (Exception ex) { ex.printStackTrace(); }
                    break;
                }
                // ���� 6�� ����������,
                if(observer.getClientsInRoom().size()==6){
                    // �� ���� ������ �����ؼ� �� �� ���ٰ� �˷��ش�.
                    try {
                        writer.println("Room"+temp+" is full");
                        writer.flush();
                    } catch (Exception ex) { ex.printStackTrace(); }
                    break;
                }
                // Ư���� ���ܰ� ������ �濡 ����.
                this.observer = observer;
                observer.addClient(this);
                // Ŭ���̾�Ʈ GUI �󿡼� ���� ���Բ� Ŭ���̾�Ʈ���� ��ɾ �����Ѵ�.
                serverOnlyOne.sendmessageToAllClient("/clientstate "+clientID+" from "+roomNumber+" to "+temp);
                try {
                    writer.println("/join "+temp+" ok");
                    writer.flush();
                } catch (Exception ex) { ex.printStackTrace(); }
                roomNumber = temp;
                break;
            }
        }
        // �ش��ϴ� ��ȣ�� ���� ���� ������ ���� ���ٰ� �˷��ش�.
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
            if(fileReader==null){
                fileReader = new BufferedReader(new FileReader("userInfo/userPrivacy.txt"));
            }
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
    // ������ Ŭ���̾�Ʈ�� �����غ� ��ɾ �����⸦ ��ٸ���.
    public void readyRoom(){
        // �����غ�� ����Ī �������� �Ѱ� �� �� ������,
        if(ready==false){
            ready = true;
        }
        else{
            ready = false;
        }
        // �غ� ���°� �������� ���޵ȴ�.
        try {
            writer.println("You are "+(ready?"ready":"not ready"));
            writer.flush();
        } catch (Exception ex) { ex.printStackTrace(); }
    }
    // ������ Ŭ���̾�Ʈ�� �泪���� ��ɾ �����⸦ ��ٸ���.
    public void leaveRoom(){
        // �������̶��, 
        if(this.start == true){
            // ���� �� ���ٰ� �˷��ش�.
            try {
                writer.println("You can't leave while playing");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
        // �������� �ƴϸ�,
        else{
            // �����غ� �����ϰ�,
            ready = false;
            // Ŭ���̾�Ʈ�� �濡�� �����Ѵ�.
            observer.removeClient(this);
            // ���� Ŭ���̾�Ʈ�� �����̸�,
            if(roomMaster == true){
                // ��������� �����ϰ�,
                roomMaster = false;
                // �� �ȿ� �ִ� �ٸ� Ŭ���̾�Ʈ�� ������,
                if(observer.getClientsInRoom().size()==0){
                    // ���� �ݴ´�.
                    serverOnlyOne.unregistObserver(observer);
                }
                // �� �ȿ� �ִ� �ٸ� Ŭ���̾�Ʈ�� ������,
                else{
                    ClientHandler client = observer.getClientsInRoom().get(0);
                    // ������ �Ѱ��ش�.
                    try {
                        client.getWriter().println("You are now room master");
                        client.getWriter().flush();
                        client.setRoomMaster(true);
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
            // Ŭ���̾�Ʈ GUI �󿡼� ���� �����Բ� Ŭ���̾�Ʈ���� ��ɾ �����Ѵ�.
            try {
                writer.println("/leave ok");
                writer.flush();
                serverOnlyOne.sendmessageToAllClient("/clientstate "+clientID+" from "+roomNumber+" to "+0);
            } catch (Exception ex) { ex.printStackTrace(); }
            // Ŭ���̾�Ʈ�� ���ȣ�� 0(����)�� �ٲ۴�.
            roomNumber = 0;
        }
    }
    // ������ Ŭ���̾�Ʈ�� ���ӽ��� ��ɾ �����⸦ ��ٸ���.
    public void startTheGame(){
        // �����̸� ������ ������ ������ �ִ�.
        if(this.roomMaster==true){
            this.start=true;
            // Ŭ���̾�Ʈ�� �߿���,
            Iterator it = serverOnlyOne.getClients().iterator();
            int playersCount = 0;
            while (it.hasNext()) {
                ClientHandler client = (ClientHandler)it.next();
                // ���� ���ȣ�� ���� Ŭ���̾�Ʈ�� ����,
                if(this.roomNumber==client.getRoomNumber()){
                    playersCount++;
                    // �����غ� ���°� �ƴ� Ŭ���̾�Ʈ���� ������,
                    if(client.getReady()==false){
                        try {
                            // �� Ŭ���̾�Ʈ���� �����غ� �ش޶�� �޽����� ������,
                            client.getWriter().println("Ready Please");
                            client.getWriter().flush();
                            // ���忡�Դ� ��� Ŭ���̾�Ʈ�� �غ� �� �Ǿ��ִ��� �˷��ش�.
                            this.getWriter().println(client.getClientName()+" is not ready");
                            this.getWriter().flush();
                            // ������ ������ �� ���� ���·� �ٲ۴�.
                            this.start = false;
                        } catch (Exception ex) { ex.printStackTrace(); }
                    }
                }
            }
            // �濡 �ִ� ����� 1���̰ų� 6���� �ʰ��ϸ�,
            if(playersCount==1 || playersCount>6){
                // ������ ������ �� ����.
                this.start = false;
            }
            // ������ ������ �� ���� �����̸�,
            if(this.start==false){
                // ������ ������ �� ���ٰ� �޽����� ������.
                try {
                    this.getWriter().println("Can't start the game");
                    this.getWriter().flush();
                } catch (Exception ex) { ex.printStackTrace(); }
            }
            // ������ ������ �� �ִ� �����̸�,
            else{
                // ������ �����.
                serverOnlyOne.initGame(observer);
            }
        }
        // ������ �ƴϸ� ������ ������ ������ ����.
        else{
            try {
                writer.println("You are not a room master");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
    // ������ Ŭ���̾�Ʈ�� ���ӳ� ���� ��ɾ �����⸦ ��ٸ���.
    public void fireOnMap(String message){
        // ������ ���۵��� �ʾ�����,
        if(this.start == false){
            // ������ ���۵��� �ʾҴٰ� �˷��ش�.
            try {
                writer.println("The game is not started");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
            return;
        }
        // �ش� �÷��̾� �����̸�,
        if(serverOnlyOne.getClients().indexOf(this)==observer.getPlayerTurn()){
            // /attack 00 ��� ��ɾ ������ �޾Ƽ� ��ǥ�� ��ȯ�ϰ�
            int j = Integer.parseInt(message.substring(8,10))/7;
            int k = Integer.parseInt(message.substring(8,10))%7;
            // ��� �ÿ��̾�鿡�� ��� ��ǥ�� ���ݵǾ����� �˷��ְ�
            serverOnlyOne.sendmessageToRoomPlayer(observer, observer.getPlayerTurn()+1+"P fired on ("+k+","+j+")");
            // ���ӹ濡�� ��ǥ�� �����Ѵ�.
            serverOnlyOne.attackPos(observer,j,k);
            // ������ �Ŀ� �����÷��̾��� ���ʷ� �ѱ��.
            observer.nextPlayer();
        }
        // �ش� �÷��̾� ���ʰ� �ƴϸ�,
        else{
            // �ش� �÷��̾��� ���ʰ� �ƴ϶�� �޽����� ������.
            try {
                writer.println("It is not your turn");
                writer.flush();
            } catch (Exception ex) { ex.printStackTrace(); }
        }
    }
    // ������ Ŭ���̾�Ʈ�� �Ϲ� �޽����� �����⸦ ��ٸ���.
    public void tellEveryone(String message) {
        // Ŭ���̾�Ʈ�� �߿���
        Iterator it = serverOnlyOne.getClients().iterator();
        while (it.hasNext()) {
            ClientHandler client = (ClientHandler)it.next();
            // ���� �濡 �ִ� Ŭ���̾�Ʈ���Ը�
            if(this.roomNumber==client.getRoomNumber()){
                // �޽����� ������.
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

// ������ ������ Subject : ������ ���� ����� ����
interface Subject{
    public void registObserver(RoomObserver observer);
    public void unregistObserver(RoomObserver observer);
    public void initGame(RoomObserver observer);
}
    // ���Ӽ��� SingleTon���� : ������ ���� ����� ����
    class SingletonServer implements Subject{
        private static SingletonServer serverOnlyOne;
        private ArrayList<RoomObserver> roomObservers;
        private ArrayList<ClientHandler> clients;
        public SingletonServer(){
            roomObservers = new ArrayList<RoomObserver>();
            clients = new ArrayList<ClientHandler>();
        }
        public static SingletonServer getInstance(){
            if(serverOnlyOne == null){
                try{
                    System.out.println(Thread.currentThread().getName());
                    // Thread.sleep(1000);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
            synchronized(SingletonServer.class){
                if(serverOnlyOne == null){
                    serverOnlyOne = new SingletonServer();
                }
            }
            return serverOnlyOne;
        }
        public void addClient(ClientHandler client){
            clients.add(client);
        }
        public ArrayList<ClientHandler> getClients(){
            return clients;
        }
        public void registObserver(RoomObserver observer){
            System.out.println("Created : Observer as new game");
            roomObservers.add(observer);
        }
        public void unregistObserver(RoomObserver observer){
            for(int i=0;i<roomObservers.size();i++){
                if(roomObservers.get(i).equals(observer)){
                    System.out.println("Deleted : Observer");
                    roomObservers.remove(i);
                }
            }
        }
        // ������ �����.
        public void initGame(RoomObserver observer){
            observer.initUser();
            observer.initNotify();
        }
        // ���� ���ݸ�ɾ �����ȴ�.
        public void attackPos(RoomObserver observer,int i, int j){
            String message = "[xx]";
            for(int k=0;k<observer.getPlayers().size();k++){
                // ���ӹ� ������ �÷��̾� �÷��̾ ��, �� �÷��̾��� ��ǥ�� [ME]�̸�,
                if(observer.getPlayers().get(k).getSpot(i,j).equals("[ME]")){
                    // �ش��÷��̾�� �������� �ϳ� �Ұ�
                    observer.getPlayers().get(k).loselife();
                    // ��ο��� �ش���ġ�� �����ߴٰ� �˸���.
                    message = "[oo]";
                }
            }
            observer.attackNotify(i,j,message);
        }
        // �� �ȿ� �ִ� �÷��̾�鿡�� �޽����� ������.
        public void sendmessageToRoomPlayer(RoomObserver observer,String message){
            observer.sendmessageToRoomPlayer(message);
        }
        // ������ ��� Ŭ���̾�Ʈ�鿡�� �޽����� ������.(�����ڿ�)
        public void sendmessageToAllClient(String message){
            for(ClientHandler client : clients){
                try{
                    client.getWriter().println(message);
                    client.getWriter().flush();
                }catch(Exception ex){ex.printStackTrace();}
            }
        }
        public ArrayList<RoomObserver> getRoomObservers(){
            return roomObservers;
        }
    }
    //SingletonServer End
//Subject End
//������ ������ Observer : ������ ���� ����� ����
interface Observer{
    public ArrayList<GamePlayer> getPlayers();
    public void attackNotify(int i, int j, String message);
    public void initNotify();
    public void initUser();
    public void sendmessageToRoomPlayer(String message);
}
    class RoomObserver implements Observer{
        private ArrayList<Ship> ships;
        private int gamePlayerNum = 0;
        private int shipNum;
        private int shipLength;
        private ArrayList<GamePlayer> players;
        private ArrayList<ClientHandler> clientsInRoom;
        private int playerTurn;
        private int roomNumber;
        public RoomObserver(int roomNumber){
            this.roomNumber = roomNumber;
            ships = new ArrayList<Ship>();
            players = new ArrayList<GamePlayer>();
            clientsInRoom = new ArrayList<ClientHandler>();
        }
        // ���ӽ���.
        public void initUser(){
            // �÷��̾ ��������� ����, �踦 ��ô�� ������, ���� ���̸� �󸶷� ���� ���Ѵ�.
            switch(gamePlayerNum){
                case 1:
                    clientsInRoom.get(0).getWriter().println("You can't start game");
                    clientsInRoom.get(0).getWriter().flush();
                    return;
                case 2: shipNum = 1; shipLength = 1; break;
                case 3: shipNum = 3; shipLength = 3; break;
                case 4: shipNum = 2; shipLength = 3; break;
                case 5: shipNum = 2; shipLength = 3; break;
                case 6: shipNum = 2; shipLength = 2; break;
            }
            playerTurn = 0;
            // �÷��̾�鿡�� ���ӽ��� �޽����� ������.
            for(int i=0;i<clientsInRoom.size();i++){
                try {
                    PrintWriter writer = clientsInRoom.get(i).getWriter();
                    writer.println("Game start!");
                    writer.flush();
                    writer.println("You are "+(i+1)+"P");
                    writer.flush();
                    writer.println(playerTurn+1+"P turn!");
                    writer.flush();
                    clientsInRoom.get(i).setStart(true);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
            generateShips();
        }
        // �踦 �����.
        public void generateShips(){
            int playerCount=0;
            // �÷��̾� ����ŭ �����,
            while(playerCount<gamePlayerNum){
                GamePlayer player = new GamePlayer(shipNum,shipLength);
                int shipCount=0;
                // �� �÷��̾�� ������ ���� ����ŭ �踦 �����.
                while(shipCount<shipNum){
                    Ship newShip = new Ship(shipLength);
                    newShip.setPos();
                    // ���θ��� �谡 �ٸ� ���� ��ǥ�� �浹���� ������ 
                    if(!newShip.checkCollision(ships)){
                        // �踦 �����.
                        ships.add(newShip);
                        player.addShip(newShip);
                        shipCount++;
                    }
                }
                // ������� ���� ��ǥ�� �̿��ؼ� �÷��̾�� �ʱ� ���� �����.
                player.initMap();
                players.add(player);
                playerCount++;
            }
        }
        // ������� �ʱ� ���� �� �÷��̾�� �����Ѵ�.
        public void initNotify(){
            for(int i=0;i<players.size();i++){
                try{
                    for(int j=0;j<7;j++){
                        for(int k=0;k<7;k++){
                            try{
                                // Ŭ���̾�Ʈ GUI �󿡼� �ڽ��� ���� ��ġ�� Ȯ���ϰ� �ȴ�.
                                clientsInRoom.get(i).getWriter().println("/init "+players.get(i).getSpot(j,k));
                                clientsInRoom.get(i).getWriter().flush();
                            }catch(Exception ex){ex.printStackTrace();}
                        }
                    }
                }catch(Exception ex){ex.printStackTrace();}
            }
        }
        // ���ӹ濡 Ŭ���̾�Ʈ �߰�
        public void addClient(ClientHandler client){
            clientsInRoom.add(client);
            gamePlayerNum++;
        }
        // ���ӹ濡�� Ŭ���̾�Ʈ ����
        public void removeClient(ClientHandler client){
            clientsInRoom.remove(client);
            gamePlayerNum--;
        }
        // �濡�ִ� Ŭ���̾�Ʈ�鿡�� �޽����� ������.
        public void sendmessageToRoomPlayer(String message){
            for(int i=0;i<players.size();i++){
                try{
                    clientsInRoom.get(i).getWriter().println(message);
                    clientsInRoom.get(i).getWriter().flush();
                }catch(Exception ex){ex.printStackTrace();}
            }
        }
        // ���� �÷����� ���ʷ� �Ѿ��.
        public void nextPlayer(){
            for(int count=0;count<gamePlayerNum;count++){
                // ���� �÷��̾� ����.
                if(playerTurn == gamePlayerNum-1){
                    playerTurn = 0;
                }
                else{
                    playerTurn++;
                }
                // ���� �÷��̾��� �������� 0�ƴ� ��,
                if(players.get(playerTurn).getLife()!=0){
                    // �������� 0�� �÷��̾ �� �ο���-1 �̶��, ȥ�ڸ� ��Ƴ��Ҵٴ� ���̹Ƿ�,
                    if(count==gamePlayerNum-1){
                        // ���ڰ� �����ȴ�.
                        sendmessageToRoomPlayer(playerTurn+1+"P is a winner!");
                        // ������ �ٽ� �����ϱ� ���ؼ� ������ ���� �÷��̾ �ʱ�ȭ�Ѵ�.
                        resetGame();
                    }
                    // �������� 0�� �÷��̾ ������, �� �÷��̾�� ���ʰ� �Ѿ��.
                    else{
                        sendmessageToRoomPlayer(playerTurn+1+"P turn!");
                        break;
                    }
                }
            }
            return;
        }
        public void resetGame(){
            // Ŭ���̾�Ʈ GUI �󿡼� �������� �����.
            sendmessageToRoomPlayer("/resetgame ok");
            // ���� �󿡼� ���� ��ü���� �����.
            this.ships = new ArrayList<Ship>();
            this.players = new ArrayList<GamePlayer>();
            // �� �ȿ� �ִ� Ŭ���̾�Ʈ���� �ʱ�ȭ�Ѵ�.
            for(ClientHandler client : clientsInRoom){
                client.resetClient();
            }
        }
        // ��� �ÿ��̾��� GUI �󼼾� ������ǥ�� ǥ���Ѵ�.
        public void attackNotify(int i, int j, String message){
            for(int k=0;k<players.size();k++){
                players.get(k).setSpot(i,j,message);
                try{
                    // Ŭ���̾�Ʈ GUI �󿡼� ������ǥ�� ������.
                    clientsInRoom.get(k).getWriter().println("/attack "+new DecimalFormat("00").format(i*7+j)+" "+message);
                    clientsInRoom.get(k).getWriter().flush();
                }catch(Exception ex){ex.printStackTrace();}
            }
        }
        public int getPlayerTurn(){
            return playerTurn;
        }
        public ArrayList<GamePlayer> getPlayers(){
            return players;
        }
        public ArrayList<ClientHandler> getClientsInRoom(){
            return clientsInRoom;
        }
        public int getRoomNumber(){
            return roomNumber;
        }
    }
    //RoomObserver End
//Observer End
class GamePlayer{
    private ArrayList<Ship> ships;
    private int shipNum;
    private int shipLength;
    private Map map;
    private int life;
    public GamePlayer(int shipNum,int shipLength){
        this.shipNum = shipNum;
        this.shipLength = shipLength;
        ships = new ArrayList<Ship>();
        map = new Map();
        life = shipLength*shipNum;
    }
    // �� ��ü�� Ư�� ��ǥ�� �޽����� ����.
    public void setSpot(int i, int j,String posMessage){
        map.setSpot(i,j,posMessage);
    }
    // �� ��ü�� Ư�� ��ǥ�� ������ �޽����� �����´�.
    public String getSpot(int i,int j){
        return map.getSpot(i,j);
    }
    // �÷��̾ �踦 �߰�.
    public void addShip(Ship ship){
        ships.add(ship);
    }
    public int getShipNum(){
        return shipNum; 
    }
    public int getLife(){
        return life;
    }
    // ������ ����
    public void loselife(){
        life--;
    }
    // ���� ��ǥ�� ������ ���� ����.
    public void initMap(){
        for(int i=0;i<shipNum;i++){
            for(int j=0;j<shipLength;j++){
                setSpot(ships.get(i).getPos()[j][0],ships.get(i).getPos()[j][1],"[ME]");
            }
        }
    }
}
//GamePlayer End
class Map{
    private String posMessages[][];
    // �� ������
    final int MAPCOL=7;
    final int MAPROW=7;
    public Map(){
        posMessages = new String[MAPCOL][MAPROW];
        // �� �⺻ �ʱ�ȭ
        for(int i=0;i<MAPCOL;i++){
            for(int j=0;j<MAPROW;j++){
                posMessages[i][j] = "[??]";
            }
        }
    }
    public String getMessage(int i,int j){
        return posMessages[i][j];
    }
    public void setSpot(int i,int j,String message){
        posMessages[i][j] = message;
    }
    public String getSpot(int i,int j){
        return posMessages[i][j];
    }
}
//Map End
class Ship{
    int posX[];
    int posY[];
    int shipLength;
    final int MAPSIZE = 7;
    public Ship(int shipLength){
        // ���� ���̿� ���� ��ǥ ������ �޶�����.
        this.shipLength = shipLength;
        posX = new int[shipLength];
        posY = new int[shipLength];
    }
    // ���� ��ǥ�� �����ϰ� ����.
    public void setPos(){
        int direction = (int)(Math.random()*2);
        int firstloc = (int)(Math.random()*(MAPSIZE-shipLength));
        int secondloc = (int)(Math.random()*MAPSIZE);
        for(int k=0;k<shipLength;k++){
            if(direction==0){
                posX[k] = firstloc;
                posY[k] = secondloc;
            }
            else{
                posX[k] = secondloc;
                posY[k] = firstloc;
            }
            firstloc++;
        }
    }
    // ���� ��ǥ���� 2���� �迭�� �ٲ㼭 ��������.
    int[][] getPos(){
        int ret[][] = new int[shipLength][2];
        for(int i=0;i<shipLength;i++){
            ret[i][0] = posX[i];
            ret[i][1] = posY[i];
        }
        return ret;
    }
    // ���� ��ǥ�� �ٸ� ����� ��ǥ�� �浹�ϴ��� �˻�.
    Boolean checkCollision(ArrayList<Ship> ships){
        for(int i=0;i<ships.size();i++){
            for(int j=0;j<ships.get(i).shipLength;j++){
                for(int k=0;k<shipLength;k++){
                    // �ٸ� ��� �� ����, x��ǥ�� y��ǥ�� ��� ������,
                    if(ships.get(i).getPos()[j][0]==getPos()[k][0]
                    &&ships.get(i).getPos()[j][1]==getPos()[k][1])
                    {
                        // �浹�� �����Ѵ�. �ϳ��� �浹�ϸ� �踦 �ٽ� ����� �ȴ�.
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
//Ship End