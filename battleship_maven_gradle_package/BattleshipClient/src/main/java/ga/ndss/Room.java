package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.sound.midi.*;
import java.text.DecimalFormat;
// 대기실에 들어가는 게임방상태창
public class Room extends JPanel implements ActionListener{
    ArrayList<JLabel> labelList = new ArrayList<JLabel>();
    GameWindows windows;
    JButton join = new JButton("join");
    JButton make = new JButton("make");
    int roomNum;
    int playerNum = 0;
    public Room(int roomNum, GameWindows windows){
        this.roomNum = roomNum;
        this.windows = windows;
        this.setOpaque(true);
        this.setBackground(Color.BLACK);
        // 3,3 격자공간을 상하5, 좌우5의 간격으로 배치
        this.setLayout(new GridLayout(3,3,5,5));
        JLabel roomLabel = new JLabel("Room "+new DecimalFormat("000").format(roomNum));
        roomLabel.setForeground(Color.YELLOW);
        //리스너는 두가지 방법으로 등록 가능.
        join.addActionListener(this);
        make.addActionListener(new MakeRoomButton());

        // 9개의 격자공간중 3개는 버튼과 방번호,
        this.add(roomLabel);
        this.add(make);
        this.add(join);
        // 나머지 6개는 방에 접속한 사람의 이름을 보여준다.
        for(int i=1;i<=6;i++){
            JLabel newLabel = new JLabel("");
            // 0,2,4번은 노란색
            if(i%2==0){
                newLabel.setOpaque(true);
                newLabel.setBackground(Color.YELLOW);
            }
            // 1,3,5번은 주황색
            else{
                newLabel.setOpaque(true);
                newLabel.setBackground(Color.ORANGE);
            }
            labelList.add(newLabel);
            this.add(newLabel);
        }
    }
    public void addPlayer(String playerID){
        for(JLabel label : labelList){
            if(label.getText().equals("")){
                label.setText(playerID);
                label.updateUI();
                playerNum++;
                break;
            }
        }
    }
    public void removePlayer(String playerID){
        for(JLabel label : labelList){
            if(label.getText().equals(playerID)){
                label.setText("");
                label.updateUI();
                playerNum--;
                break;        
            }
        }
    }
    // 리스너를 넣는 첫번째 방법 : implements ActionListener
    public void actionPerformed(ActionEvent ev){
        try {
            windows.client.getWriter().println("/join "+ new DecimalFormat("000").format(roomNum));
            windows.client.getWriter().flush();
        }catch (Exception ex) {ex.printStackTrace();}
    }
    // 리스너를 넣는 두번째 방법 : inner class
    class MakeRoomButton implements ActionListener{
        public void actionPerformed(ActionEvent ev){
            try {
                windows.client.getWriter().println("/make "+ new DecimalFormat("000").format(roomNum));
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
        }
    }
    public ArrayList<JLabel> getLabelList(){
        return labelList;
    }
    public void addLabel(JLabel label){
        labelList.add(label);
    }
}