package ga.ndss.subject;
import java.util.ArrayList;
import ga.ndss.*;
import ga.ndss.observer.*;
public class SingletonServer implements Subject{
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
    public void initGame(RoomObserver observer){
        observer.initUser();
        observer.initNotify();
    }
    public void attackPos(RoomObserver observer,int i, int j){
        String message = "[xx]";
        for(int k=0;k<observer.getPlayers().size();k++){
            if(observer.getPlayers().get(k).getSpot(i,j).equals("[ME]")){
                observer.getPlayers().get(k).loselife();
                message = "[oo]";
            }
        }
        observer.attackNotify(i,j,message);
    }
    public void sendmessageToRoomPlayer(RoomObserver observer,String message){
        observer.sendmessageToRoomPlayer(message);
    }
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
