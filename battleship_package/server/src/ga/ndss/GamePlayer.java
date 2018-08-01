package ga.ndss;
import java.util.ArrayList;

public class GamePlayer{
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
    // 맵 객체의 특정 좌표에 메시지를 쓴다.
    public void setSpot(int i, int j,String posMessage){
        map.setSpot(i,j,posMessage);
    }
    // 맵 객체의 특정 좌표에 쓰여진 메시지를 가져온다.
    public String getSpot(int i,int j){
        return map.getSpot(i,j);
    }
    // 플레이어에 배를 추가.
    public void addShip(Ship ship){
        ships.add(ship);
    }
    public int getShipNum(){
        return shipNum; 
    }
    public int getLife(){
        return life;
    }
    // 라이프 감소
    public void loselife(){
        life--;
    }
    // 배의 좌표를 가지고 맵을 쓴다.
    public void initMap(){
        for(int i=0;i<shipNum;i++){
            for(int j=0;j<shipLength;j++){
                setSpot(ships.get(i).getPos()[j][0],ships.get(i).getPos()[j][1],"[ME]");
            }
        }
    }
}
//GamePlayer End

