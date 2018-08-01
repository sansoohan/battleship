package ga.ndss;
import java.util.ArrayList;

public class Ship{
    int posX[];
    int posY[];
    int shipLength;
    final int MAPSIZE = 7;
    public Ship(int shipLength){
        // 배의 길이에 따라 좌표 개수가 달라진다.
        this.shipLength = shipLength;
        posX = new int[shipLength];
        posY = new int[shipLength];
    }
    // 배의 좌표를 랜덤하게 생성.
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
    // 배의 좌표들을 2차원 배열로 바꿔서 내보낸다.
    public int[][] getPos(){
        int ret[][] = new int[shipLength][2];
        for(int i=0;i<shipLength;i++){
            ret[i][0] = posX[i];
            ret[i][1] = posY[i];
        }
        return ret;
    }
    // 배의 좌표가 다른 배들의 좌표와 충돌하는지 검사.
    public Boolean checkCollision(ArrayList<Ship> ships){
        for(int i=0;i<ships.size();i++){
            for(int j=0;j<ships.get(i).shipLength;j++){
                for(int k=0;k<shipLength;k++){
                    // 다른 배와 이 배의, x좌표와 y좌표가 모두 같으면,
                    if(ships.get(i).getPos()[j][0]==getPos()[k][0]
                    &&ships.get(i).getPos()[j][1]==getPos()[k][1])
                    {
                        // 충돌로 간주한다. 하나라도 충돌하면 배를 다시 만들게 된다.
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
//Ship End