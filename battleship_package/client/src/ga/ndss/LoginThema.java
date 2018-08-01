package ga.ndss;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
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
            Image img1 = new ImageIcon(getClass().getResource("/images/LoginShip.jpg")).getImage();
            g.drawImage(img1,0,0,this);
        }
    }
}