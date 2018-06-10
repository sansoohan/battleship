package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.sound.midi.*;
import java.text.DecimalFormat;
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
            Image img1 = new ImageIcon(getClass().getResource("/images/board.jpg")).getImage();
            g.drawImage(img1,0,0,this);
            // 명중한 배를 그린다.
            for (int i = 0; i < grid.length; i++) {
                if(grid[i]==null){
                    continue;
                }
                if(grid[i].equals("[oo]")){
                    Image img2 = new ImageIcon(getClass().getResource("/images/ship.jpg")).getImage();
                    g.drawImage(img2,172+i%7*96+3,98+i/7*96+36,this);
                }
                if(grid[i].equals("[xx]")){
                    Image img3 = new ImageIcon(getClass().getResource("/images/miss.jpg")).getImage();
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