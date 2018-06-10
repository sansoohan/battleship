package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
// 서버선택 창.
public class ServerSelectForm extends LoginThema implements ActionListener{
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