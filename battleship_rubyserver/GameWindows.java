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
    // ��������â
    ServerSelectForm serverSelect;
    // �α���â
    LoginWindow loginWindow;
    // ȸ�����Ծ��
    NewAccountForm newAccountForm;
    // �н�����нǾ��
    PasswordLostForm passwordLostForm;
    // ����
    WaitRoom waitRoom;
    // ���ӹ�
    PlayRoom playRoom;
    // ������ �޽��� ��ȯ.
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
// ���ӹ�
class PlayRoom extends JFrame{
    // ��ü�� �޽��� ����
    GameWindows windows;
    private int roomNum;

    ArrayList<String> clientIDs = new ArrayList<String>();
    // GUI ��
    Picture pic = new Picture();
        String imageSource = "ship.png";
        // ���⿡ ������ ���� ���� ���� ����ȴ�.
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

    // GUI��ü����/�̺�Ʈ�����ʵ��
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

        // ���ǰ� ���ӹ� ���� ��ġ�� ȭ���� �� ����� ����
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension fr = getSize();
        int xpos = (int)(screen.getWidth()/2 - fr.getWidth()/2);
        int ypos = (int)(screen.getHeight()/2 - fr.getHeight()/2);
        setLocation(xpos,ypos);
    }
    // ������ �ʱ�ȭ
    public void resetGrid(){
        for(int i=0;i<49;i++){
            grid[i] = "[??]";
        }   
    }
    // �ִϸ��̼��� ���� �̵� ��. �̵�� ���⺸�ٴ�, �ð� �帧�� ���� ���ϸ��̼����� ���� ����.
    Sequencer sequencer;
        Sequence seq;
            Track track;
                final int FRAME = 10;
                int mySpotNum;
                MidiEvent[][] midiEvents;

    // �ִϸ��̼� �׸��� ����1 : �ִϸ��̼��� ����ϰ� �����Ѵ�.
    // �����ʿ��� ���۹��� �ڽ��� �� ��ǥ�� ������ �ִϸ��̼��� �����ϰ� �����Ѵ�.
    public void eventMidi(){
        // �ִϸ��̼��� ����� �� �ִϸ��̼��� ������ ��ǥ�� int[]������ �ٲ㼭 �������� �Ѵ�.
        // int[]�� ũ�� Ȯ��.
        int count=0;
        for(int i=0;i<grid.length;i++){
            if(grid[i].equals("[ME]")){
                count++;
            }
        }
        mySpotNum = count;
        // int[]�� �ڽ��� ��ǥ �ε��� ����.
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
            // i : �ִϸ��̼� �������� 10��
            for(int i=0;i<FRAME;i++){
                // j : �ִϸ��̼��� �׷��� ��ǥ��
                for(int j=0;j<mySpotNum;j++){
                    // �������� �׷����� �ð��� ��ǥ���� ����Ѵ�.
                    try{
                        MidiEvent midiEvent = new MidiEvent(new ShortMessage(176,1,myspots[j],i),i);
                        midiEvents[i][j] = midiEvent;
                        track.add(midiEvent);
                    }catch(Exception e){}
                }
            }
            // �ִϸ��̼� �ݺ� ������� ����
            sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
            // �ִϸ��̼� ����
            sequencer.start();
            // �ִϸ��̼� ������ ����
            sequencer.setTempoInBPM(60*10);
        }catch(Exception e){}
    }
    // ������ ����� �ϱ� ���ؼ� ����� ���� �̵� �̺�Ʈ���� ���� �����.
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
        // inst�� HashSet���� add�Ͽ� �ߺ���ǥ ������ �����Ѵ�.
        HashSet<Integer> inst = new HashSet<Integer>();
        // pit�� �����Ӻ� �ִϸ��̼��� ǥ���Ѵ�.
        int pit=0;
        public Picture() {
            // ���콺�κ��� ��ǥ �Է��� �޴´�.
            addMouseListener(new MyMouseListener());
            setSize(new Dimension(1024,900));
        }
        class MyMouseListener extends MouseAdapter {
            // ���콺 Ŭ�� ������
            public void mouseClicked(MouseEvent e) {
                // ���콺�� �ش��ϴ� ���ڱ����� ���ؼ�,
                int location = gridize(e.getPoint());
                if(location>=0){
                    // ������ ���� ���ɾ�� �����Ѵ�.
                    try {
                        windows.client.getWriter().println("/attack "+new DecimalFormat("00").format(location));
                        windows.client.getWriter().flush();
                    }catch (Exception ex) {ex.printStackTrace();}
                }
            }
        }
        // ���콺�� �Է¹��� ��ǥ�� ����ǥ���� �ٲ۴�.
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
        // �ִϸ��̼� �׸��� ����2 : �ִϸ��̼� ��ȭ�� ������Ʈ �Ѵ�.
        // �ִϸ��̼��� ��ȭ�� ������ ����. �ſ� ª�� �������� ����ȴ�. pit�� inst�� ������ �� repaint();
        public void controlChange(ShortMessage event){
            this.inst.add(event.getData1());
            this.pit = event.getData2();
            // refresh.
            repaint();
        }
        // �ִϸ��̼� �׸��� ����3 : Graphics�� �׸���.
        // �׸��� ���� �׸���.
        public void paintComponent(Graphics g) {
            // ���带 �׸���.
            super.paintComponent(g);
            Image img1 = new ImageIcon("board.jpg").getImage();
            g.drawImage(img1,0,0,this);
            // ������ �踦 �׸���.
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
            // �ִϸ��̼��� �׸���.
            for(Iterator<Integer> it = inst.iterator();it.hasNext();){
                int num=it.next();
                g.setColor(Color.yellow);
                g.drawRoundRect(172+pit+num%7*96,98+pit+num/7*96,96-2*pit,96-2*pit,20,20);
            }
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class RoomChatEnterKey extends KeyAdapter {
        // ����Ű�� �Է��ϸ� �޽��� ���۹�ư�� Ŭ���ȴ�. => roomChatField ���� �۵��Ѵ�.
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
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class RoomSendButton implements ActionListener{
        // ��ư�� ������ roomChatField�� �о �޽��� �����Ѵ�.
        public void actionPerformed(ActionEvent ev){
            try {
                windows.client.getWriter().println(roomChatField.getText());
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
            // ������ ����� Ŀ���� �ű��.
            roomChatField.setText("");
            roomChatField.requestFocus();
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class ExitRoomButton implements ActionListener{
        // ��ư�� ������ /leave �޽����� ����
        public void actionPerformed(ActionEvent ev){            
            try {
                windows.client.getWriter().println("/leave");
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class ReadyRoomButton implements ActionListener{
        // ��ư�� ������ /ready �޽����� ����
        public void actionPerformed(ActionEvent ev){            
            try {
                windows.client.getWriter().println("/ready");
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class StartRoomButton implements ActionListener{
        // ��ư�� ������ /ready �޽����� ����
        public void actionPerformed(ActionEvent ev){            
            try {
                windows.client.getWriter().println("/start");
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class selectButton implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class saveO implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class loadO implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class saveT implements ActionListener{
        public void actionPerformed(ActionEvent ev){
        }
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
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
// ����
class WaitRoom extends JFrame{
    // ��ü�� �޽��� ����
    GameWindows windows;    
    // GUI��
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
        // ���ڰ����� 10,2 �� ����10, �¿�10 �������� ��ġ.
        rooms.setLayout(new GridLayout(10,2,10,10));
        // ���ڰ����� ���ӹ� 20���� ä���ִ´�.
        for(int i=1;i<=20;i++)
            rooms.add(new Room(i,windows));

        getContentPane().add(BorderLayout.CENTER, scroller2);
        getContentPane().add(BorderLayout.EAST, publicRightBox);
        getContentPane().add(BorderLayout.SOUTH, publicChat);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setBounds(0,0,1200,900);

        // ���ǰ� ���ӹ� ���� ��ġ�� ȭ���� �� ����� ����
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension fr = getSize();
        int xpos = (int)(screen.getWidth()/2 - fr.getWidth()/2);
        int ypos = (int)(screen.getHeight()/2 - fr.getHeight()/2);
        setLocation(xpos,ypos);
    }
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class PublicChatEnterKey extends KeyAdapter {
        // ����Ű�� �Է��ϸ� �޽��� ���۹�ư�� Ŭ���ȴ�. => publicChatField ���� �۵��Ѵ�.
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
    // �̺�Ʈ ������ ����� ���� Ŭ����
    class PublicSendButton implements ActionListener{
        // ��ư�� ������ publicChatField �о �޽��� �����Ѵ�.
        public void actionPerformed(ActionEvent ev){
            try {
                windows.client.getWriter().println(publicChatField.getText());
                windows.client.getWriter().flush();
            }catch (Exception ex) {ex.printStackTrace();}
            // ������ ����� Ŀ���� �ű��.
            publicChatField.setText("");
            publicChatField.requestFocus();
        }
    }
}
// ���ǿ� ���� ���ӹ����â
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
        // 3,3 ���ڰ����� ����5, �¿�5�� �������� ��ġ
        this.setLayout(new GridLayout(3,3,5,5));
        JLabel roomLabel = new JLabel("Room "+new DecimalFormat("000").format(roomNum));
        roomLabel.setForeground(Color.YELLOW);
        //�����ʴ� �ΰ��� ������� ��� ����.
        join.addActionListener(this);
        make.addActionListener(new MakeRoomButton());

        // 9���� ���ڰ����� 3���� ��ư�� ���ȣ,
        this.add(roomLabel);
        this.add(make);
        this.add(join);
        // ������ 6���� �濡 ������ ����� �̸��� �����ش�.
        for(int i=1;i<=6;i++){
            JLabel newLabel = new JLabel("");
            // 0,2,4���� �����
            if(i%2==0){
                newLabel.setOpaque(true);
                newLabel.setBackground(Color.YELLOW);
            }
            // 1,3,5���� ��Ȳ��
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
    // �����ʸ� �ִ� ù��° ��� : implements ActionListener
    public void actionPerformed(ActionEvent ev){
        try {
            windows.client.getWriter().println("/join "+ new DecimalFormat("000").format(roomNum));
            windows.client.getWriter().flush();
        }catch (Exception ex) {ex.printStackTrace();}
    }
    // �����ʸ� �ִ� �ι�° ��� : inner class
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

// �� �׸��� �ִ� �⺻ �׸�.
abstract class LoginThema extends JFrame{
    // ��ü�� �޽��� ����
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
    // JPanel�� Ȯ���ؼ� �׸��� ���� �����´�.
    class Picture extends JPanel{
        public Picture() {
            setLocationRelativeTo(this);
            setSize(new Dimension(400,200));
            repaint();
        }
        // ��׸��� �׸���.
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Image img1 = new ImageIcon("LoginShip.jpg").getImage();
            g.drawImage(img1,0,0,this);
        }
    }
}
// �������� â.
class ServerSelectForm extends LoginThema implements ActionListener{
    // GUI��
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
    // cancel��ư�� ����� �̺�Ʈ ������
    class ExitButton implements ActionListener{
        // �н����� �н� â�� �ݰ� �α���â�� ����.
        public void actionPerformed(ActionEvent ev){
            System.exit(0);
        }
    }
}
// �α��� â.
class LoginWindow extends LoginThema implements ActionListener{
    // GUI��
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
    // �Է�Ȯ�� ��ư�� ������ ������ id/pass�� ������ ���۵ȴ�.
    public void actionPerformed(ActionEvent ev){
        try {   
            windows.client.getWriter().println("/login "+idTextField.getText()+" "+passwdTextField.getText());
            // windows.client.getWriter().flush();
        }catch (Exception ex) {ex.printStackTrace();}
        // Ŀ���� �ű��.
        passwdTextField.requestFocus();
    }
    class LoginButtonEnterKey extends KeyAdapter {
        // ����Ű�� �Է��ϸ� �α��� ��ư�� Ŭ���ȴ�. => roomChatField ���� �۵��Ѵ�.
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
// ȸ������ â.
class NewAccountForm extends LoginThema implements ActionListener{
    // GUI��
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
    // �Է�Ȯ�� ��ư�� ������ ������ id/pass/name/email�� ������ ���۵ȴ�.
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
    // cancel��ư�� ����� �̺�Ʈ ������
    class CancelButton implements ActionListener{
        // ȸ������â�� �ݰ� �α���â�� ����.
        public void actionPerformed(ActionEvent ev){
            windows.loginWindow.setVisible(true);
            windows.newAccountForm.setVisible(false);
        }
    }
}
// �н����� �н� â.
class PasswordLostForm extends LoginThema implements ActionListener{
    // GUI��
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
    // �Է�Ȯ�� ��ư�� ������ ������ id/pass/name/email�� ������ ���۵ȴ�.
    public void actionPerformed(ActionEvent ev){
        String newAccount = "/passwdlost ";
        newAccount += idTextField.getText()+" ";
        newAccount += emailTextField.getText();
        try {
            windows.client.getWriter().println(newAccount);
            windows.client.getWriter().flush();
        }catch (Exception ex) {ex.printStackTrace();}
    }
    // cancel��ư�� ����� �̺�Ʈ ������
    class CancelButton implements ActionListener{
        // �н����� �н� â�� �ݰ� �α���â�� ����.
        public void actionPerformed(ActionEvent ev){
            windows.loginWindow.setVisible(true);
            windows.passwordLostForm.setVisible(false);
        }
    }
}
class SimpleChatClient{
    private JTextArea incoming;
    // ��ü�� �޽��� ����
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
    // ������ �����ϰ� ����� ��Ʈ���� �����Ѵ�.
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
    // �����κ��� �޽����� �޴� ������.
    class IncomingReader implements Runnable {
        public void run() {
            String message;
            try {
                // �޽����� ���� ������ �о���̴µ�,
                while ((message = reader.readLine()) != null) {
                    System.out.println("client read " + message);
                    // '/' �� �����ϴ� �޽����� ���ɾ�� �����ϰ� ä��â���� ǥ������ �ʴ´�.
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
                    // �׷��� ���� �޽����� �׳� ä��â�� ǥ���Ѵ�.
                    else{
                        incoming.append(message + "\n");
                    }
                }
            } catch (IOException ex){ex.printStackTrace();}
        }
        // ���ӽ��� �ÿ� �����κ��� 49�� ��ǥ���� �������� �޾ƿ´�.
        public void initGrid(String message){
            windows.playRoom.grid[initCount] = message.substring(6,10);
            initCount++;
            // 49���� �� �޾����� �ִϸ��̼��� �����Ѵ�.
            if(initCount==49){
                windows.playRoom.eventMidi();
                windows.playRoom.pic.repaint();
            }
        }
        // Ŭ���̾�Ʈ�� /make 000 �� ������, �����κ��� /make 000 ok �� ��ٸ���.
        public void makeRoom(String message){
            String[] makeMessages = message.split(" ");
            if(makeMessages[2].equals("ok")){
                // ������ ����� ���ӷ��� �����ش�.
                windows.waitRoom.setVisible(false);
                windows.playRoom.setVisible(true);
                // Ŭ���̾�Ʈ�� �����κ��� ���� �޽����� ���ӷ��� ä��â���� ���Բ� ����.
                incoming = windows.playRoom.roomChatArea;
            }
        }
        // Ŭ���̾�Ʈ�� /join 000 �� ������, �����κ��� /join 000 ok �� ��ٸ���.
        public void joinRoom(String message){
            // �޽����� " "�� �ɰ���,
            String[] joinMessages = message.split(" ");
            if(joinMessages[2].equals("ok")){
                // ������ ����� ���ӷ��� �����ش�.
                windows.waitRoom.setVisible(false);
                windows.playRoom.setVisible(true);
                // Ŭ���̾�Ʈ�� �����κ��� ���� �޽����� ���ӷ��� ä��â���� ���Բ� ����.
                incoming = windows.playRoom.roomChatArea;
            }
        }
        // Ŭ���̾�Ʈ�� /leave �� ������, �����κ��� /leave ok �� ��ٸ���.
        public void leaveRoom(String message){
            if(message.substring(7,9).equals("ok")){
                // ���ӷ��� ����� ������ �����ش�.
                windows.waitRoom.setVisible(true);
                windows.playRoom.setVisible(false);
                // Ŭ���̾�Ʈ�� �����κ��� ���� �޽����� ������ ä��â���� ���Բ� ����.
                incoming = windows.waitRoom.publicChatArea;
            }
        }
        // Ŭ���̾�Ʈ�� /attack 00 �� ������, �����κ��� /attack 00 [??] �� ��ٸ���.
        public void fire(String message){
            // 1.Ŭ���̾�Ʈ�� ���� ��ǥ�� ����.
            // 2.������ ������ǥ�� ���� ���߿��� �ľ�.
            // 3.���߰���� ��� Ŭ���̾�Ʈ���� ����.
            int location = Integer.parseInt(message.substring(8,10));
            windows.playRoom.grid[location] = message.substring(11,15);
            System.out.println(windows.playRoom.grid[location]);
            windows.playRoom.pic.repaint();
        }
        // Ŭ���̾�Ʈ��, �����κ��� /resetgame ok �� ��ٸ���.
        public void resetGame(String message){
            if(message.substring(11,13).equals("ok")){
                // ���ڸ� ���� [??]�� �ٲٰ� => ���ݴ��� ������ ���� ��������.
                windows.playRoom.resetGrid();
                // ������ �ִϸ��̼� �����ӵ��� ��� �����.
                windows.playRoom.resetMidi();
                // /init �޽����� �ޱ� ���ؼ� 0���� �ʱ�ȭ�Ѵ�.
                initCount=0;
            }
        }
        // Ŭ���̾�Ʈ�� /login id passwd�� ������, �����κ��� /login ok id name �� ��ٸ���.
        public void login(String message){
            String[] okMessages = message.split(" ");
            // ������ ���� �̷������ �α���â�� ����� ������ �����ش�.
            if(okMessages[1].equals("ok")){
                windows.playRoom.userID.setText(okMessages[2]);
                windows.playRoom.userName.setText(okMessages[3]);
                windows.waitRoom.setVisible(true);
                windows.loginWindow.setVisible(false);
            }
            // �����κ��� id�� password �� �߿� �ϳ��� �Է����� �ʾҴٰ� �޽����� ���� �� �ִ�.
            else if(okMessages[1].equals("notentered")){
                JOptionPane.showConfirmDialog(null,"Please enter ID and Password");
            }
            // �����κ��� id�� password �� �߸� �ԷµǾ��ٰ� �޽����� ���� �� �ִ�.
            else if(okMessages[1].equals("mismatch")){
                JOptionPane.showConfirmDialog(null,"Please check your ID and Password");
            }
        }
        public void newAccount(String message){
            String[] okMessages = message.split(" ");
            // ���ο� ������ ���������� ������ٸ�, �����κ��� �������� �޽����� ���� �� �ִ�.
            if(okMessages[1].equals("ok")){
                JOptionPane.showConfirmDialog(null,"Success!");
            }
            // �̹� �����ϴ� ���̵���, �����κ��� ���� �޽����� ���� �� �ִ�.
            else if(okMessages[1].equals("idcollision")){
                JOptionPane.showConfirmDialog(null,"The ID you entered is already exist");
            }
            // ���̵� ������ ���� �ʴ´ٰ�, �����κ��� ���� �޽����� ���� �� �ִ�.
            else if(okMessages[1].equals("idformaterror")){
                JOptionPane.showConfirmDialog(null,"Special Character can't be used for ID");
            }
            // �̸��� ������ ���� �ʴ´ٰ�, �����κ��� ���� �޽����� ���� �� �ִ�.
            else if(okMessages[1].equals("emailformaterror")){
                JOptionPane.showConfirmDialog(null,"Email format is not good");
            }
        }
        // Ŭ���̾�Ʈ�� �濡 ���ų� ���� ��, ������ ������Ʈ �Ѵ�.
        public void clientState(String message){
            String stateMessages[] = message.split(" ");
            String clientID = stateMessages[1];
            int from = Integer.parseInt(stateMessages[3]);
            int to = Integer.parseInt(stateMessages[5]);
            // Ŭ���̾�Ʈ�� ���ӹ濡�� �ٸ� ������ �̵��� ��,
            if(from!=0){
                ((Room)windows.waitRoom.rooms.getComponent(from-1)).removePlayer(clientID);
            }
            // Ŭ���̾�Ʈ�� ���ǿ��� �ٸ� ������ �̵��� ��,
            else{
                for(int i=0;i<windows.waitRoom.clientIDs.size();i++){
                    if(windows.waitRoom.clientIDs.get(i).equals(clientID)){
                        windows.waitRoom.clientIDs.remove(i);
                        break;
                    }
                }
            }
            // Ŭ���̾�Ʈ�� ���ӹ����� �̵��� ��,
            if(to!=0){
                ((Room)windows.waitRoom.rooms.getComponent(to-1)).addPlayer(clientID);
            }
            // Ŭ���̾�Ʈ�� ���Ƿ� �̵��� ��,
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