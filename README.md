battleship
=========

gameclient.jar 실행
------------------
jar파일을 더블클릭을 하셔도 되고 명령창에 java -jar gameclient.jar라고 입력하셔도 됩니다.

gameserver.jar 실행
-------------------
jar파일을 더블클릭을 하셔도 되고 명령창에 java -jar gameserver.jar라고 입력하셔도 됩니다.<br>
gameserver같은 경우에는 더블클릭으로 실행하면 백그라운드에서 돌기 때문에 종료할 때 번거롭습니다.<br>
명령창에서 실행하실 것을 권합니다.

GameClient.png
---------------
클라이언트 프로그램의 클래스 다이어그램입니다.

GameServer.png
---------------
서버 프로그램의 클래스 다이어그램입니다.

battleship_nonpackage
------------------
소스 문자포멧 EUC-KR
서버와 클라이언트 파일이 따로따로 하나씩 있고 정리해놓지 않은 초기 개발용입니다.

클라이언트 컴파일/빌드/실행<br>
battleship_nonpackage> javac GameWindows.java<br>
battleship_nonpackage> java GameWindows<br>

서버 컴파일/빌드/실행<br>
battleship_nonpackage> javac VerySimpleChatServer.java<br>
battleship_nonpackage> java VerySimpleChatServer<br>

battleship_package
------------------
소스 문자포멧 UTF-8<br>
서버와 클라이언트파일을 클래스들을 쪼개서 하위폴더로 정리했습니다. 유지보수용입니다. TDD및 빌드툴을 고려하지 않고 만들었습니다.<br>

클라이언트 컴파일/빌드/실행<br>
src> javac -encoding utf8 -d ../classes ga/ndss/*.java<br><br>
src> cd ../classes<br>
classes> jar -cvmf manifest.txt gameclient.jar . ../images/<br>
classes> java -jar gameclient.jar<br>

서버 컴파일/빌드/실행<br>
src> javac -encoding utf8 -d ../classes ga/ndss/*.java<br>
src> cd ../classes<br>
classes> jar -cvmf manifest.txt gameclient.jar .<br>
classes> java -jar gameserver.jar<br>


battleship_maven_package
------------------
소스 문자포멧 UTF-8<br>
TDD를 고려해서 만들 생각입니다.<br>

클라이언트 컴파일/빌드/실행<br>
BattleshipClient> mvn compile<br>
BattleshipClient> mvn package<br>
BattleshipClient> java -jar target/BattleshipClient-1.0-SNAPSHOT.jar<br>

서버 컴파일/빌드/실행<br>
BattleshipServer> mvn compile<br>
BattleshipServer> mvn package<br>
BattleshipServer> java -jar target/BattleshipServer-1.0-SNAPSHOT.jar<br>

주의사항
-------------------
UTF-8 인코딩이면 자바컴파일 시 아래처럼 인코딩옵션을 줘야 합니다.<br>

제작자 컨택
-------------------
name : 한산수<br>
email : sansoo2002@naver.com<br>
phone : 010-8835-9229<br>
