import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.sound.midi.*;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;

public class GameWindows{
    public static void main(String[] args) {
        GameWindows windows = new GameWindows();
    }
    // 서버선택창
    ServerSelectForm serverSelect;
    // 로그인창
    LoginWindow loginWindow;
    // 회원가입양식
    NewAccountForm newAccountForm;
    // 패스워드분실양식
    PasswordLostForm passwordLostForm;
    // 대기실
    WaitRoom waitRoom;
    // 게임방
    PlayRoom playRoom;
    // 서버와 메시지 교환.
    SimpleChatClient client;
    public GameWindows(){
        serverSelect = new ServerSelectForm(this);
        loginWindow = new LoginWindow(this);
        newAccountForm = new NewAccountForm(this);
        passwordLostForm = new PasswordLostForm(this);
        waitRoom = new WaitRoom(this);
        playRoom = new PlayRoom(this);
        client = new SimpleChatClient(waitRoom.publicChatArea,this);
    }
}
// 게임방
class PlayRoom extends JFrame{
    // 객체간 메시지 전송
    GameWindows windows;
    private int roomNum;

    ArrayList<String> clientIDs = new ArrayList<String>();
    // GUI 맵
    Picture pic = new Picture();
        String imageSource = "ship.png";
        // 여기에 서버로 부터 받은 맵이 저장된다.
        String[] grid = new String[49];
    
    Box rightBox = new Box(BoxLayout.Y_AXIS);
        JLabel userID = new JLabel();
        JLabel userName = new JLabel();
        JPanel playersPanel = new JPanel();
            ArrayList<JLabel> playerIDs = new ArrayList<JLabel>();
        JLabel help = new JLabel("You can select player");
        Box littleBox1 = new Box(BoxLayout.X_AXIS);
            JComboBox waitingPlayers = new JComboBox();
            JButton selectButton = new JButton("request");
        JScrollPane scroller;
            JTextArea roomChatArea = new JTextArea(40,20);
        Box chatBoxRoom = new Box(BoxLayout.X_AXIS);
            JTextField roomChatField = new JTextField(15);
            JButton sendMessageRoom = new JButton("send");
        int leftTime=0;
        JLabel timeLabel = new JLabel(String.valueOf(leftTime));
        Box controlBoxRoom = new Box(BoxLayout.X_AXIS);
            JButton readyRoom = new JButton("Ready");
            JButton startRoom = new JButton("Start");
            JButton exitRoom = new JButton("Exit Room");
        
    JMenuBar menu = new JMenuBar();
        JMenu saveAsObject = new JMenu("Object");
            JMenuItem saveObject = new JMenuItem("saveObject");
            JMenuItem loadObject = new JMenuItem("loadObject");
        JMenu saveAsText = new JMenu("Text");
            JMenuItem saveText = new JMenuItem("saveText");
            JMenuItem loadText = new JMenuItem("loadText");
        
        JFileChooser chooser = new JFileChooser();

    // GUI객체연결/이벤트리스너등록
    public PlayRoom(GameWindows windows){
        this.windows = windows;
        saveObject.addActionListener(new saveO());
        loadObject.addActionListener(new loadO());
        saveText.addActionListener(new saveT());
        loadText.addActionListener(new loadT());
        
        saveAsObject.add(saveObject);
        saveAsObject.add(loadObject);
        saveAsText.add(saveText);
        saveAsText.add(loadText);
        
        menu.add(saveAsObject);
        menu.add(saveAsText);
        
        sendMessageRoom.addActionListener(new RoomSendButton());
        
        readyRoom.addActionListener(new ReadyRoomButton());
        startRoom.addActionListener(new StartRoomButton());
        exitRoom.addActionListener(new ExitRoomButton());

        roomChatField.addKeyListener(new RoomChatEnterKey());
        
        chatBoxRoom.add(roomChatField);
        chatBoxRoom.add(sendMessageRoom);
        
        controlBoxRoom.add(readyRoom);
        controlBoxRoom.add(startRoom);
        controlBoxRoom.add(exitRoom);

        littleBox1.add(waitingPlayers);
        littleBox1.add(selectButton);
        
        scroller = new JScrollPane(roomChatArea);
        scroller.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroller.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        
        rightBox.add(userID);
        playersPanel.setLayout(new GridLayout(6,1,0,0));
        for(int i=0;i<6;i++){
            JLabel label = new JLabel();
            playerIDs.add(label);
            // playersPanel.add(label);
        }
        rightBox.add(playersPanel);
        rightBox.add(help);
        rightBox.add(littleBox1);
        rightBox.add(scroller);
        rightBox.add(chatBoxRoom);
        rightBox.add(timeLabel);
        rightBox.add(controlBoxRoom);        

        setJMenuBar(menu);
        getContentPane().add(BorderLayout.CENTER, pic);
        getContentPane().add(BorderLayout.EAST, rightBox);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0,0,1200,900);
        setVisible(false);

        // 대기실과 게임방 시작 위치를 화면의 한 가운데로 조정
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension fr = getSize();
        int xpos = (int)(screen.getWidth()/2 - fr.getWidth()/2);
        int ypos = (int)(screen.getHeight()/2 - fr.getHeight()/2);
        setLocation(xpos,ypos);
    }
    // 게임판 초기화
    public void resetGrid(){
        for(int i=0;i<49;i++){
            grid[i] = "[??]";
        }   
    }
    // 애니메이션을 위한 미디 맵. 미디로 쓰기보다는, 시간 흐름에 따른 에니메이션으로 많이 쓴다.
    Sequencer sequencer;
        Sequence seq;
            Track track;
                final int FRAME = 10;
                int mySpotNum;
                MidiEvent[][] midiEvents;

    // 애니메이션 그리기 과정1 : 애니메이션을 등록하고 실행한다.
    // 서버쪽에서 전송받은 자신의 배 좌표를 보여줄 애니메이션을 셋팅하고 실행한다.
    public void eventMidi(){
        // 애니메이션을 등록할 때 애니메이션을 보여줄 좌표를 int[]형으로 바꿔서 등록해줘야 한다.
        // int[]의 크기 확보.
        int count=0;
        for(int i=0;i<grid.length;i++){
            if(grid[i].equals("[ME]")){
                count++;
            }
        }
        mySpotNum = count;
        // int[]에 자신의 좌표 인덱스 보관.
        int[] myspots = new int[count];
        midiEvents = new MidiEvent[FRAME][mySpotNum];
        for(int i=0,k=0;i<grid.length;i++){
            if(grid[i].equals("[ME]")){
                myspots[k] = i;
                k++;
            }   
        }   
        try{
            sequencer = MidiSystem.getSequencer();
            sequencer.open();
            sequencer.addControllerEventListener(pic,myspots);
            seq = new Sequence(Sequence.PPQ,4);
            track = seq.createTrack();
            sequencer.setSequence(seq);
            // i : 애니메이션 프레임은 10개
            for(int i=0;i<FRAME;i++){
                // j : 애니메이션이 그려질 좌표들
                for(int j=0;j<mySpotNum;j++){
                    // 프레임이 그려지는 시간과 좌표들을 등록한다.
                    try{
                        MidiEvent midiEvent = new MidiEvent(new ShortMessage(176,1,myspots[j],i),i);
                        midiEvents[i][j] = midiEvent;
                        track.add(midiEvent);
                    }catch(Exception e){}
                }
            }
            // 애니메이션 반복 재생으로 설정
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            // 애니메이션 시작
            sequencer.start();
            // 애니메이션 빠르기 설정
            sequencer.setTempoInBPM(60*10);
        }catch(Exception e){}
    }
    // 게임을 재시작 하기 위해서 등록해 놓은 미디 이벤트들을 전부 지운다.
    public void resetMidi(){
        sequencer.stop();
        for(int i=0;i<FRAME;i++){
            for(int j=0;j<mySpotNum;j++){
                try{
                    track.remove(midiEvents[i][j]);
                }catch(Exception e){}
            }
        }
        pic.inst = new HashSet<Integer>();
        pic.repaint();
    }
    class Picture extends JPanel implements ControllerEventListener{
        // inst는 HashSet으로 add하여 중복좌표 저장을 방지한다.
        HashSet<Integer> inst = new HashSet<Integer>();
        // pit는 프레임별 애니메이션을 표시한다.
        int pit=0;
        public Picture() {
            // 마우스로부터 좌표 입력을 받는다.
            addMouseListener(new MyMouseListener());
            setSize(new Dimension(1024,900));
        }
        class MyMouseListener extends MouseAdapter {
            // 마우스 클릭 리스너
            public void mouseClicked(MouseEvent e) {
                // 마우스에 해당하는 격자구간을 구해서,
                int location = gridize(e.getPoint());
                if(location>=0){
                    // 서버에 공격 명령어로 전송한다.
                    try {
                        windows.client.getWriter().println("/attack "+new DecimalFormat("00").format(location));
                        windows.client.getWriter().flush();
                    }catch (Exception ex) {ex.printStackTrace();}
                }
            }
        }
        // 마우스로 입력받은 좌표를 맵좌표으로 바꾼다.
        public int gridize(Point p){
            int r=96;
            int x0=172;
            int y0=98;

            int col;
            for(col=0;col<7;col++){
                if(x0<=p.getX()+col*r && p.getX()<x0+(col+1)*r){
                    break;
                }
                if(col==7-1){
                    return -1;
                }
            }
            int raw;
            for(raw=0;raw<7;raw++){
                if(y0<=p.getY()+raw*r && p.getY()<y0+(raw+1)*r){
                    break;
                }
                if(raw==7-1){
                    return -1;        
                }
            }
            return raw*7+col;
        }
        // 애니메이션 그리기 과정2 : 애니메이션 변화를 업데이트 한다.
        // 애니메이션이 변화될 때마다 실행. 매우 짧은 간격으로 실행된다. pit과 inst에 저장한 후 repaint();
        public void controlChange(ShortMessage event){
            this.inst.add(event.getData1());
            this.pit = event.getData2();
            // refresh.
            repaint();
        }
        // 애니메이션 그리기 과정3 : Graphics가 그린다.
        // 그리기 툴로 그린다.
        public void paintComponent(Graphics g) {
            // 보드를 그린다.
            super.paintComponent(g);
            Image img1 = new ImageIcon("board.jpg").getImage();
            g.drawImage(img1,0,0,this);
            // 명중한 배를 그린다.
            for (int i = 0; i < grid.length; i++) {
                if(grid[i]==null){
                    continue;
                }
                if(grid[i].equals("[oo]")){
                    Image img2 = new ImageIcon("ship.png").getImage();
                    g.drawImage(img2,172+i%7*96+3,98+i/7*96+36,this);
                }
                if(grid[i].equals("[xx]")){
                    Image img3 = new ImageIcon("miss.png").getImage();
                    g.drawImage(img3,172+i%7*96+8,98+i/7*96+31,this);
                }    
            }
            // 애니메이션을 그린다.
            for(Iterator<Integer> it = inst.iterator();it.hasNext();){
                int num=it.next();
                g.setColor(Color.yellow);
                g.drawRoundRect(172+pit+num%7*96,98+pit+num/7*96,96-2*pit,96-2*pit,20,20);
            }
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class RoomChatEnterKey extends KeyAdapter {
        // 엔터키를 입력하면 메시지 전송버튼이 클릭된다. => roomChatField 에서 작동한다.
        public void keyPressed(KeyEvent e) {
            int keycode = e.getKeyCode();
            System.out.println(e.getKeyText(keycode) + " keyCode : "+keycode);
            e.getKeyCode();
            e.getKeyChar();    
            e.getKeyText(keycode);
            e.getModifiers();
            if(keycode == 10)
                sendMessageRoom.doClick();
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class RoomSendButton implements ActionListener{
        // 버튼을 누르면 roomChatField를 읽어서 메시지 전송한다.
        public void actionPerformed(ActionEvent ev){
            try {
                windows.client.getWriter().println(roomChatField.getText());
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
            // 내용을 지우고 커서를 옮긴다.
            roomChatField.setText("");
            roomChatField.requestFocus();
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class ExitRoomButton implements ActionListener{
        // 버튼은 누르면 /leave 메시지를 전송
        public void actionPerformed(ActionEvent ev){            
            try {
                windows.client.getWriter().println("/leave");
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class ReadyRoomButton implements ActionListener{
        // 버튼은 누르면 /ready 메시지를 전송
        public void actionPerformed(ActionEvent ev){            
            try {
                windows.client.getWriter().println("/ready");
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class StartRoomButton implements ActionListener{
        // 버튼은 누르면 /ready 메시지를 전송
        public void actionPerformed(ActionEvent ev){            
            try {
                windows.client.getWriter().println("/start");
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class selectButton implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class saveO implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class loadO implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class saveT implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // 이벤트 리스너 등록을 위한 클래스
    class loadT implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    public int getRoomNumber(){
        return roomNum;
    }
    public void setRoomNumber(int roomNum){
        this.roomNum = roomNum;
    }
    public ArrayList<JLabel> getPlayerIDs(){
        return playerIDs;
    }
}
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
// 대기실에 들어가는 게임방상태창
class Room extends JPanel implements ActionListener{
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

// 배 그림이 있는 기본 테마.
abstract class LoginThema extends JFrame{
    // 객체간 메시지 전송
    GameWindows windows;
    Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
    Dimension fr = super.getSize();
    int xpos = (int)(screen.getWidth()/2 - fr.getWidth()/2);
    int ypos = (int)(screen.getHeight()/2 - fr.getHeight()/2);
    public LoginThema(GameWindows windows){
        this.windows = windows;
        this.setSize(800,500);
        this.setLocation(xpos-200,ypos-200);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        getContentPane().add(BorderLayout.CENTER, pic);
    }
    Picture pic = new Picture();
    // JPanel을 확장해서 그리기 툴을 가져온다.
    class Picture extends JPanel{
        public Picture() {
            setLocationRelativeTo(this);
            setSize(new Dimension(400,200));
            repaint();
        }
        // 배그림을 그린다.
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Image img1 = new ImageIcon("LoginShip.jpg").getImage();
            g.drawImage(img1,0,0,this);
        }
    }
}
// 서버선택 창.
class ServerSelectForm extends LoginThema implements ActionListener{
    // GUI맵
    Box serverSelectBox = new Box(BoxLayout.Y_AXIS);
        Box urlBox = new Box(BoxLayout.X_AXIS);
            JLabel idLabel = new JLabel("Server URL : ");
            JTextField serverURL = new JTextField(50);
        Box buttonBox = new Box(BoxLayout.X_AXIS);
            JButton okButton = new JButton("Ok");
            JButton exitButton = new JButton("Exit");

    public ServerSelectForm(GameWindows windows){
        super(windows);

        okButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        okButton.addActionListener(this);

        exitButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        exitButton.addActionListener(new ExitButton());

        serverURL.setText("127.0.0.1");

        urlBox.add(idLabel);
        urlBox.add(serverURL);

        buttonBox.add(okButton);
        buttonBox.add(exitButton);

        serverSelectBox.add(urlBox);
        serverSelectBox.add(buttonBox);

        getContentPane().add(BorderLayout.SOUTH, serverSelectBox);

        this.setVisible(true);
    }
    // 
    public void actionPerformed(ActionEvent ev){
        windows.loginWindow.setVisible(true);
        this.setVisible(false);
        windows.client.setUpNetworking(serverURL.getText());
    }
    // cancel버튼에 등록할 이벤트 리스너
    class ExitButton implements ActionListener{
        // 패스워드 분실 창을 닫고 로그인창을 연다.
        public void actionPerformed(ActionEvent ev){
            System.exit(0);
        }
    }
}
// 로그인 창.
class LoginWindow extends LoginThema implements ActionListener{
    // GUI맵
    Box loginBox = new Box(BoxLayout.Y_AXIS);
        Box idBox = new Box(BoxLayout.X_AXIS);
            JLabel idLabel = new JLabel("ID : ");
            JTextField idTextField = new JTextField(50);
        Box passwdBox = new Box(BoxLayout.X_AXIS);
            JLabel passwdLabel = new JLabel("PASS : ");
            JPasswordField passwdTextField = new JPasswordField(50);
        Box buttonBox = new Box(BoxLayout.X_AXIS);
            JButton loginButton = new JButton("Login");
            JButton newAccountButton = new JButton("New Account");
            JButton passwdLostButton = new JButton("Password lost");

    public LoginWindow(GameWindows windows){
        super(windows);
        
        loginButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        loginButton.addActionListener(this);

        idTextField.addKeyListener(new LoginButtonEnterKey());
        passwdTextField.addKeyListener(new LoginButtonEnterKey());

        newAccountButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        newAccountButton.addActionListener(new NewAccountButton());

        passwdLostButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        passwdLostButton.addActionListener(new PassWordLostButton());

        loginBox.add(idBox);
        loginBox.add(passwdBox);
        loginBox.add(buttonBox);

        idBox.add(idLabel);
        idBox.add(idTextField);

        passwdBox.add(passwdLabel);
        passwdBox.add(passwdTextField);

        buttonBox.add(loginButton);
        buttonBox.add(newAccountButton);
        buttonBox.add(passwdLostButton);

        getContentPane().add(BorderLayout.SOUTH, loginBox);
    }
    // 입력확인 버튼을 누르면 서버로 id/pass가 서버로 전송된다.
    public void actionPerformed(ActionEvent ev){
        try {   
            windows.client.getWriter().println("/login "+idTextField.getText()+" "+passwdTextField.getText());
            windows.client.getWriter().flush();
        }catch (Exception ex) {ex.printStackTrace();}
        // 커서를 옮긴다.
        passwdTextField.requestFocus();
    }
    class LoginButtonEnterKey extends KeyAdapter {
        // 엔터키를 입력하면 로그인 버튼이 클릭된다. => roomChatField 에서 작동한다.
        public void keyPressed(KeyEvent e) {
            int keycode = e.getKeyCode();
            System.out.println(e.getKeyText(keycode) + " keyCode : "+keycode);
            e.getKeyCode();
            e.getKeyChar();    
            e.getKeyText(keycode);
            e.getModifiers();
            if(keycode == 10)
                loginButton.doClick();
        }
    }
    class NewAccountButton implements ActionListener{
        public void actionPerformed(ActionEvent ev){
            windows.loginWindow.setVisible(false);
            windows.newAccountForm.setVisible(true);
        }
    }
    class PassWordLostButton implements ActionListener{
        public void actionPerformed(ActionEvent ev){
            windows.loginWindow.setVisible(false);
            windows.passwordLostForm.setVisible(true);
        }
    }
}
// 회원가입 창.
class NewAccountForm extends LoginThema implements ActionListener{
    // GUI맵
    Box newAccountBox = new Box(BoxLayout.Y_AXIS);
        Box idBox = new Box(BoxLayout.X_AXIS);
            JLabel idLabel = new JLabel("ID : ");
            JTextField idTextField = new JTextField(50);
        Box passwdBox = new Box(BoxLayout.X_AXIS);
            JLabel passwdLabel = new JLabel("PASS : ");
            JTextField passwdTextField = new JTextField(50);
        Box nameBox = new Box(BoxLayout.X_AXIS);
            JLabel nameLabel = new JLabel("NAME : ");
            JTextField nameTextField = new JTextField(50);
        Box emailBox = new Box(BoxLayout.X_AXIS);
            JLabel emailLabel = new JLabel("EMAIL : ");
            JTextField emailTextField = new JTextField(50);
        Box buttonBox = new Box(BoxLayout.X_AXIS);
            JButton okButton = new JButton("Ok");
            JButton cancelButton = new JButton("Cancel");

    public NewAccountForm(GameWindows windows){
        super(windows);
        
        okButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        okButton.addActionListener(this);

        cancelButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        cancelButton.addActionListener(new CancelButton());

        idBox.add(idLabel);
        idBox.add(idTextField);

        passwdBox.add(passwdLabel);
        passwdBox.add(passwdTextField);

        nameBox.add(nameLabel);
        nameBox.add(nameTextField);

        emailBox.add(emailLabel);
        emailBox.add(emailTextField);

        buttonBox.add(okButton);
        buttonBox.add(cancelButton);

        newAccountBox.add(idBox);
        newAccountBox.add(passwdBox);
        newAccountBox.add(nameBox);
        newAccountBox.add(emailBox);
        newAccountBox.add(buttonBox);

        getContentPane().add(BorderLayout.SOUTH, newAccountBox);
    }
    // 입력확인 버튼을 누르면 서버로 id/pass/name/email이 서버로 전송된다.
    public void actionPerformed(ActionEvent ev){
        String newAccount = "/newaccount ";
        newAccount += idTextField.getText()+" ";
        newAccount += passwdTextField.getText()+" ";
        newAccount += nameTextField.getText()+" ";
        newAccount += emailTextField.getText();
        try {   
            windows.client.getWriter().println(newAccount);
            windows.client.getWriter().flush();
        }catch (Exception ex) {ex.printStackTrace();}
    }
    // cancel버튼에 등록할 이벤트 리스너
    class CancelButton implements ActionListener{
        // 회원가입창을 닫고 로그인창을 연다.
        public void actionPerformed(ActionEvent ev){
            windows.loginWindow.setVisible(true);
            windows.newAccountForm.setVisible(false);
        }
    }
}
// 패스워드 분실 창.
class PasswordLostForm extends LoginThema implements ActionListener{
    // GUI맵
    Box newAccountBox = new Box(BoxLayout.Y_AXIS);
        Box idBox = new Box(BoxLayout.X_AXIS);
            JLabel idLabel = new JLabel("ID : ");
            JTextField idTextField = new JTextField(50);
        Box emailBox = new Box(BoxLayout.X_AXIS);
            JLabel emailLabel = new JLabel("EMAIL : ");
            JTextField emailTextField = new JTextField(50);
        Box buttonBox = new Box(BoxLayout.X_AXIS);
            JButton okButton = new JButton("Ok");
            JButton cancelButton = new JButton("Cancel");

    public PasswordLostForm(GameWindows windows){
        super(windows);
        
        okButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        okButton.addActionListener(this);

        cancelButton.setMaximumSize(new Dimension(this.getWidth(), 40));
        cancelButton.addActionListener(new CancelButton());

        idBox.add(idLabel);
        idBox.add(idTextField);

        emailBox.add(emailLabel);
        emailBox.add(emailTextField);

        buttonBox.add(okButton);
        buttonBox.add(cancelButton);

        newAccountBox.add(idBox);
        newAccountBox.add(emailBox);
        newAccountBox.add(buttonBox);

        getContentPane().add(BorderLayout.SOUTH, newAccountBox);
    }
    // 입력확인 버튼을 누르면 서버로 id/pass/name/email이 서버로 전송된다.
    public void actionPerformed(ActionEvent ev){
        String newAccount = "/passwdlost ";
        newAccount += idTextField.getText()+" ";
        newAccount += emailTextField.getText();
        try {
            windows.client.getWriter().println(newAccount);
            windows.client.getWriter().flush();
        }catch (Exception ex) {ex.printStackTrace();}
    }
    // cancel버튼에 등록할 이벤트 리스너
    class CancelButton implements ActionListener{
        // 패스워드 분실 창을 닫고 로그인창을 연다.
        public void actionPerformed(ActionEvent ev){
            windows.loginWindow.setVisible(true);
            windows.passwordLostForm.setVisible(false);
        }
    }
}
class SimpleChatClient{
    private JTextArea incoming;
    // 객체간 메시지 전송
    private Thread readerThread;
    private GameWindows windows;
    private BufferedReader reader;
    private PrintWriter writer;
    private Socket sock;
    private int initCount=0;
    public SimpleChatClient(JTextArea incoming, GameWindows windows){
        this.incoming = incoming;
        this.windows = windows;
        readerThread = new Thread(new IncomingReader());
    }
    // 서버와 연결하고 입출력 스트림을 저장한다.
    public void setUpNetworking(String url) {
        try {
            sock = new Socket(url, 5000);
            reader = new BufferedReader(new InputStreamReader(sock.getInputStream()));
            writer = new PrintWriter(sock.getOutputStream());
            System.out.println("networking established");
        }
        catch(IOException ex){ex.printStackTrace();}
        readerThread.start();
    }
    // 서버로부터 메시지를 받는 스레드.
    class IncomingReader implements Runnable {
        public void run() {
            String message;
            try {
                // 메시지가 없을 때까지 읽어들이는데,
                while ((message = reader.readLine()) != null) {
                    System.out.println("client read " + message);
                    // '/' 로 시작하는 메시지는 명령어로 간주하고 채팅창에는 표시하지 않는다.
                    if(message.charAt(0)=='/'){
                        if(message.indexOf("init")==1){initGrid(message);}
                        else if(message.indexOf("make")==1){makeRoom(message);}
                        else if(message.indexOf("join")==1){joinRoom(message);}
                        else if(message.indexOf("leave")==1){leaveRoom(message);}
                        else if(message.indexOf("attack")==1){fire(message);}
                        else if(message.indexOf("resetgame")==1){resetGame(message);}
                        else if(message.indexOf("login")==1){login(message);}
                        else if(message.indexOf("newaccount")==1){newAccount(message);}
                        else if(message.indexOf("clientstate")==1){clientState(message);}
                    }
                    // 그렇지 않은 메시지는 그냥 채팅창에 표시한다.
                    else{
                        incoming.append(message + "\n");
                    }
                }
            } catch (IOException ex){ex.printStackTrace();}
        }
        // 게임시작 시에 서버로부터 49개 좌표값을 연속으로 받아온다.
        public void initGrid(String message){
            windows.playRoom.grid[initCount] = message.substring(6,10);
            initCount++;
            // 49개를 다 받았으면 애니메이션을 시작한다.
            if(initCount==49){
                windows.playRoom.eventMidi();
                windows.playRoom.pic.repaint();
            }
        }
        // 클라이언트는 /make 000 을 보내고, 서버로부터 /make 000 ok 를 기다린다.
        public void makeRoom(String message){
            String[] makeMessages = message.split(" ");
            if(makeMessages[2].equals("ok")){
                // 대기실을 지우고 게임룸을 보여준다.
                windows.waitRoom.setVisible(false);
                windows.playRoom.setVisible(true);
                // 클라이언트가 서버로부터 받은 메시지를 게임룸의 채팅창으로 가게끔 변경.
                incoming = windows.playRoom.roomChatArea;
            }
        }
        // 클라이언트는 /join 000 을 보내고, 서버로부터 /join 000 ok 를 기다린다.
        public void joinRoom(String message){
            // 메시지를 " "로 쪼개서,
            String[] joinMessages = message.split(" ");
            if(joinMessages[2].equals("ok")){
                // 대기실을 지우고 게임룸을 보여준다.
                windows.waitRoom.setVisible(false);
                windows.playRoom.setVisible(true);
                // 클라이언트가 서버로부터 받은 메시지를 게임룸의 채팅창으로 가게끔 변경.
                incoming = windows.playRoom.roomChatArea;
            }
        }
        // 클라이언트는 /leave 를 보내고, 서버로부터 /leave ok 를 기다린다.
        public void leaveRoom(String message){
            if(message.substring(7,9).equals("ok")){
                // 게임룸을 지우고 대기실을 보여준다.
                windows.waitRoom.setVisible(true);
                windows.playRoom.setVisible(false);
                // 클라이언트가 서버로부터 받은 메시지를 대기실의 채팅창으로 가게끔 변경.
                incoming = windows.waitRoom.publicChatArea;
            }
        }
        // 클라이언트는 /attack 00 를 보내고, 서버로부터 /attack 00 [??] 를 기다린다.
        public void fire(String message){
            // 1.클라이언트가 공격 좌표를 전송.
            // 2.서버가 공격좌표에 따른 적중여부 파악.
            // 3.적중결과를 모든 클라이언트에게 전송.
            int location = Integer.parseInt(message.substring(8,10));
            windows.playRoom.grid[location] = message.substring(11,15);
            System.out.println(windows.playRoom.grid[location]);
            windows.playRoom.pic.repaint();
        }
        // 클라이언트는, 서버로부터 /resetgame ok 를 기다린다.
        public void resetGame(String message){
            if(message.substring(11,13).equals("ok")){
                // 격자를 전부 [??]로 바꾸고 => 공격당한 지점이 전부 지워진다.
                windows.playRoom.resetGrid();
                // 저장한 애니메이션 프레임들을 모두 지운다.
                windows.playRoom.resetMidi();
                // /init 메시지를 받기 위해서 0으로 초기화한다.
                initCount=0;
            }
        }
        // 클라이언트는 /login id passwd를 보내고, 서버로부터 /login ok id name 을 기다린다.
        public void login(String message){
            System.out.println(message);
            String[] okMessages = message.split(" ");
            // 접속이 재대로 이루어지면 로그인창을 지우고 대기실을 보여준다.
            if(okMessages[1].equals("ok")){
                windows.playRoom.userID.setText(okMessages[2]);
                windows.playRoom.userName.setText(okMessages[3]);
                windows.waitRoom.setVisible(true);
                windows.loginWindow.setVisible(false);
            }
            // 서버로부터 id나 password 둘 중에 하나를 입력하지 않았다고 메시지를 받을 수 있다.
            else if(okMessages[1].equals("notentered")){
                JOptionPane.showConfirmDialog(null,"Please enter ID and Password");
            }
            // 서버로부터 id나 password 가 잘못 입력되었다고 메시지를 받을 수 있다.
            else if(okMessages[1].equals("mismatch")){
                JOptionPane.showConfirmDialog(null,"Please check your ID and Password");
            }
        }
        public void newAccount(String message){
            String[] okMessages = message.split(" ");
            // 새로운 계정을 정상적으로 만들었다면, 서버로부터 계정생성 메시지를 받을 수 있다.
            if(okMessages[1].equals("ok")){
                JOptionPane.showConfirmDialog(null,"Success!");
            }
            // 이미 존재하는 아이디라고, 서버로부터 에러 메시지를 받을 수 있다.
            else if(okMessages[1].equals("idcollision")){
                JOptionPane.showConfirmDialog(null,"The ID you entered is already exist");
            }
            // 아이디 형식이 맞지 않는다고, 서버로부터 에러 메시지를 받을 수 있다.
            else if(okMessages[1].equals("idformaterror")){
                JOptionPane.showConfirmDialog(null,"Special Character can't be used for ID");
            }
            // 이메일 형식이 맞지 않는다고, 서버로부터 에러 메시지를 받을 수 있다.
            else if(okMessages[1].equals("emailformaterror")){
                JOptionPane.showConfirmDialog(null,"Email format is not good");
            }
        }
        // 클라이언트가 방에 들어가거나 나올 때, 대기실을 업데이트 한다.
        public void clientState(String message){
            String stateMessages[] = message.split(" ");
            String clientID = stateMessages[1];
            int from = Integer.parseInt(stateMessages[3]);
            int to = Integer.parseInt(stateMessages[5]);
            // 클라이언트가 게임방에서 다른 곳으로 이동할 때,
            if(from!=0){
                ((Room)windows.waitRoom.rooms.getComponent(from-1)).removePlayer(clientID);
            }
            // 클라이언트가 대기실에서 다른 곳으로 이동할 때,
            else{
                for(int i=0;i<windows.waitRoom.clientIDs.size();i++){
                    if(windows.waitRoom.clientIDs.get(i).equals(clientID)){
                        windows.waitRoom.clientIDs.remove(i);
                        break;
                    }
                }
            }
            // 클라이언트가 게임방으로 이동할 때,
            if(to!=0){
                ((Room)windows.waitRoom.rooms.getComponent(to-1)).addPlayer(clientID);
            }
            // 클라이언트가 대기실로 이동할 때,
            else{
                windows.waitRoom.clientIDs.add(clientID);
            }
            windows.waitRoom.clientList.updateUI();
        }
    }
    public void setIncoming(JTextArea incoming){
        this.incoming = incoming;
    }
    public PrintWriter getWriter(){
        return writer;
    }
}