package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.sound.midi.*;
// 대기실
class WaitRoom extends JFrame{
    // 객체간 메시지 전송
    GameWindows windows;    
    // GUI맵
    Box publicChat = new Box(BoxLayout.Y_AXIS);
        JTextArea publicChatArea = new JTextArea(10,50);
        Box chatBoxPublic = new Box(BoxLayout.X_AXIS);
            JTextField publicChatField = new JTextField(15);
            JButton sendMessagePublic = new JButton("send");
    JPanel rooms = new JPanel();
        JScrollPane scroller2 = new JScrollPane(rooms);
    Box publicRightBox = new Box(BoxLayout.Y_AXIS);
        JLabel clientIDHeader = new    JLabel("Waiting Room");
        Vector<String> clientIDs = new Vector<String>();
            JList clientList = new JList(clientIDs);
                JScrollPane clientListScroller = new JScrollPane(clientList);

    public WaitRoom(GameWindows windows){
        this.windows = windows;

        publicChatField.addKeyListener(new PublicChatEnterKey());

        sendMessagePublic.addActionListener(new PublicSendButton());

        chatBoxPublic.add(publicChatField);
        chatBoxPublic.add(sendMessagePublic);

        publicChat.add(publicChatArea);
        publicChat.add(chatBoxPublic);

        clientListScroller.setPreferredSize(new Dimension(200,this.getHeight()));
        clientListScroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        clientListScroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        publicRightBox.add(clientIDHeader);
        publicRightBox.add(clientListScroller);

        scroller2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        rooms.setOpaque(true);
        rooms.setBackground(Color.BLUE);
        // 격자공간을 10,2 로 상하10, 좌우10 간격으로 배치.
        rooms.setLayout(new GridLayout(10,2,10,10));
        // 격자공간에 게임방 20개를 채워넣는다.
        for(int i=1;i<=20;i++)
            rooms.add(new Room(i,windows));

        getContentPane().add(BorderLayout.CENTER, scroller2);
        getContentPane().add(BorderLayout.EAST, publicRightBox);
        getContentPane().add(BorderLayout.SOUTH, publicChat);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0,0,1200,900);

        // 대기실과 게임방 시작 위치를 화면의 한 가운데로 조정
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension fr = getSize();
        int xpos = (int)(screen.getWidth()/2 - fr.getWidth()/2);
        int ypos = (int)(screen.getHeight()/2 - fr.getHeight()/2);
        setLocation(xpos,ypos);
    }
    // 이벤트 리스너 등록을 위한 클래스
    class PublicChatEnterKey extends KeyAdapter {
        // 엔터키를 입력하면 메시지 전송버튼이 클릭된다. => publicChatField 에서 작동한다.
        public void keyPressed(KeyEvent e) {
            int keycode = e.getKeyCode();
            System.out.println(e.getKeyText(keycode) + " keyCode : "+keycode);
            e.getKeyCode();
            e.getKeyChar();
            e.getKeyText(keycode);
            e.getModifiers();
            if(keycode == 10)
                sendMessagePublic.doClick();
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class PublicSendButton implements ActionListener{
        // 버튼을 누르면 publicChatField 읽어서 메시지 전송한다.
        public void actionPerformed(ActionEvent ev){
            try {
                windows.client.getWriter().println(publicChatField.getText());
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
            // 내용을 지우고 커서를 옮긴다.
            publicChatField.setText("");
            publicChatField.requestFocus();
        }
    }
}