battleship
=========

gameclient.jar 실행
------------------
jar파일을 더블클릭을 하셔도 되고 명령창에 java -jar gameclient.jar라고 입력하셔도 됩니다.

gameserver.jar 실행
-------------------
jar파일을 더블클릭을 하셔도 되고 명령창에 java -jar gameserver.jar라고 입력하셔도 됩니다.
gameserver같은 경우에는 더블클릭으로 실행하면 백그라운드에서 돌기 때문에 종료할 때 번거롭습니다.
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

클라이언트 컴파일/빌드/실행
battleship_nonpackage> javac GameWindows.java
battleship_nonpackage> java GameWindows

서버 컴파일/빌드/실행
battleship_nonpackage> javac VerySimpleChatServer.java
battleship_nonpackage> java VerySimpleChatServer

battleship_package
------------------
소스 문자포멧 UTF-8
서버와 클라이언트파일을 클래스들을 쪼개서 하위폴더로 정리했습니다. 유지보수용입니다. TDD및 빌드툴을 고려하지 않고 만들었습니다.

클라이언트 컴파일/빌드/실행
src> javac -encoding utf8 -d ../classes ga/ndss/*.java
src> cd ../classes
classes> jar -cvmf manifest.txt gameclient.jar . ../images/
classes> java -jar gameclient.jar

서버 컴파일/빌드/실행
src> javac -encoding utf8 -d ../classes ga/ndss/*.java
src> cd ../classes
classes> jar -cvmf manifest.txt gameclient.jar .
classes> java -jar gameserver.jar


battleship_maven_package
------------------
소스 문자포멧 UTF-8
TDD를 고려해서 만들 생각입니다.

클라이언트 컴파일/빌드/실행
BattleshipClient> mvn compile
BattleshipClient> mvn package
BattleshipClient> java -jar target/BattleshipClient-1.0-SNAPSHOT.jar

서버 컴파일/빌드/실행
BattleshipServer> mvn compile
BattleshipServer> mvn package
BattleshipServer> java -jar target/BattleshipServer-1.0-SNAPSHOT.jar

주의사항
-------------------
UTF-8 인코딩이면 자바컴파일 시 아래처럼 인코딩옵션을 줘야 합니다.
javac -encoding utf8

제작자 컨택
-------------------
name : 한산수
email : sansoo2002@naver.com
phone : 010-8835-9229
