package ga.ndss;
public class Map{
    private String posMessages[][];
    final int MAPCOL=7;
    final int MAPROW=7;
    public Map(){
        posMessages = new String[MAPCOL][MAPROW];
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