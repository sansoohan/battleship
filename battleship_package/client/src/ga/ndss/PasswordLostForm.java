package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;
import javax.sound.midi.*;
// 패스워드 분실 창.
public class PasswordLostForm extends LoginThema implements ActionListener{
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