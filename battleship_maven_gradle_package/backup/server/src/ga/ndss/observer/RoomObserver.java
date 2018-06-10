package ga.ndss.observer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.io.*;
import java.net.*;

import ga.ndss.*;
import ga.ndss.subject.*;

public class RoomObserver implements Observer{
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
    // 게임시작.
    public void initUser(){
        // 플레이어가 몇명인지에 따라, 배를 몇척을 만들지, 배의 길이를 얼마로 할지 정한다.
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
        // 플레이어들에게 게임시작 메시지를 보낸다.
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
    // 배를 만든다.
    public void generateShips(){
        int playerCount=0;
        // 플레이어 수만큼 만들되,
        while(playerCount<gamePlayerNum){
            GamePlayer player = new GamePlayer(shipNum,shipLength);
            int shipCount=0;
            // 각 플레이어는 정해진 배의 수만큼 배를 만든다.
            while(shipCount<shipNum){
                Ship newShip = new Ship(shipLength);
                newShip.setPos();
                // 새로만든 배가 다른 배의 좌표가 충돌하지 않으면 
                if(!newShip.checkCollision(ships)){
                    // 배를 만든다.
                    ships.add(newShip);
                    player.addShip(newShip);
                    shipCount++;
                }
            }
            // 만들어진 배의 좌표를 이용해서 플레이어마다 초기 맵을 만든다.
            player.initMap();
            players.add(player);
            playerCount++;
        }
    }
    // 만들어진 초기 맵을 각 플레이어에게 전송한다.
    public void initNotify(){
        for(int i=0;i<players.size();i++){
            try{
                for(int j=0;j<7;j++){
                    for(int k=0;k<7;k++){
                        try{
                            // 클라이언트 GUI 상에서 자신의 배의 위치를 확인하게 된다.
                            clientsInRoom.get(i).getWriter().println("/init "+players.get(i).getSpot(j,k));
                            clientsInRoom.get(i).getWriter().flush();
                        }catch(Exception ex){ex.printStackTrace();}
                    }
                }
            }catch(Exception ex){ex.printStackTrace();}
        }
    }
    // 게임방에 클라이언트 추가
    public void addClient(ClientHandler client){
        clientsInRoom.add(client);
        gamePlayerNum++;
    }
    // 게임방에서 클라이언트 제외
    public void removeClient(ClientHandler client){
        clientsInRoom.remove(client);
        gamePlayerNum--;
    }
    // 방에있는 클라이언트들에게 메시지를 보낸다.
    public void sendmessageToRoomPlayer(String message){
        for(int i=0;i<players.size();i++){
            try{
                clientsInRoom.get(i).getWriter().println(message);
                clientsInRoom.get(i).getWriter().flush();
            }catch(Exception ex){ex.printStackTrace();}
        }
    }
    // 다음 플레리어 차례로 넘어간다.
    public void nextPlayer(){
        for(int count=0;count<gamePlayerNum;count++){
            // 다음 플레이어 차례.
            if(playerTurn == gamePlayerNum-1){
                playerTurn = 0;
            }
            else{
                playerTurn++;
            }
            // 다음 플레이어의 라이프가 0아닐 때,
            if(players.get(playerTurn).getLife()!=0){
                // 라이프가 0인 플레이어가 총 인원수-1 이라면, 혼자만 살아남았다는 말이므로,
                if(count==gamePlayerNum-1){
                    // 승자가 결정된다.
                    sendmessageToRoomPlayer(playerTurn+1+"P is a winner!");
                    // 게임을 다시 시작하기 위해서 기존의 게임 플레이어를 초기화한다.
                    resetGame();
                }
                // 라이프가 0인 플레이어가 있으면, 그 플레이어에게 차례가 넘어간다.
                else{
                    sendmessageToRoomPlayer(playerTurn+1+"P turn!");
                    break;
                }
            }
        }
        return;
    }
    public void resetGame(){
        // 클라이언트 GUI 상에서 게임판을 지운다.
        sendmessageToRoomPlayer("/resetgame ok");
        // 서버 상에서 게임 객체들을 지운다.
        this.ships = new ArrayList<Ship>();
        this.players = new ArrayList<GamePlayer>();
        // 방 안에 있는 클라이언트들을 초기화한다.
        for(ClientHandler client : clientsInRoom){
            client.resetClient();
        }
    }
    // 모든 플에이어의 GUI 상세어 공격좌표를 표시한다.
    public void attackNotify(int i, int j, String message){
        for(int k=0;k<players.size();k++){
            players.get(k).setSpot(i,j,message);
            try{
                // 클라이언트 GUI 상에서 공격좌표가 찍힌다.
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
