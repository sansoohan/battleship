package ga.ndss;
import ga.ndss.*;
import ga.ndss.observer.*;
import ga.ndss.subject.*;


public interface Subject{
    public void registObserver(RoomObserver observer);
    public void unregistObserver(RoomObserver observer);
    public void initGame(RoomObserver observer);
}