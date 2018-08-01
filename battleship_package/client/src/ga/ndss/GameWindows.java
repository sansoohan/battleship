/*
src> javac -encoding utf8 -d ../classes ga/ndss/*.java
src> cd ../classes
classes> jar -cvmf manifest.txt gameclient.jar . ../images/
classes> java -jar gameclient.jar
*/

package ga.ndss;
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