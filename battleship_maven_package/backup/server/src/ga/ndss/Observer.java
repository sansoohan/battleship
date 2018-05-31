package ga.ndss;
import java.util.ArrayList;
import ga.ndss.*;
import ga.ndss.observer.*;
import ga.ndss.subject.*;
public interface Observer{
//옵저퍼 패턴의 Observer : 디자인 패턴 과목과 연계
    public ArrayList<GamePlayer> getPlayers();
    public void attackNotify(int i, int j, String message);
    public void initNotify();
    public void initUser();
    public void sendmessageToRoomPlayer(String message);
}
