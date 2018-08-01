package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
// 로그인 창.
public class LoginWindow extends LoginThema implements ActionListener{
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