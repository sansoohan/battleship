battleship
=========

gameclient.jar Execute
------------------
Just double click gameclient.jar
Or, You can use command line
java -jar gameclient.jar

gameserver.jar Execute
-------------------
Just double click gameclient.jar
Or, You can use command line
java -jar gameserver.jar
But i recommand you run the program in command line because it will be running with double click.

GameClient.png
---------------
ClassDiagram for gameclient.

GameServer.png
---------------
ClassDiagram for gameserver.

battleship_nonpackage
------------------
source code format : EUC-KR
This is not packaged one.

client compile/build/execute<br>
battleship_nonpackage> javac GameWindows.java<br>
battleship_nonpackage> java GameWindows<br>

server compile/build/execute<br>
battleship_nonpackage> javac VerySimpleChatServer.java<br>
battleship_nonpackage> java VerySimpleChatServer<br>

battleship_package
------------------
source code format : UTF-8<br>
I have packaged this. I packaged it with only java command.<br>

client compile/build/execute<br>
src> javac -encoding utf8 -d ../classes ga/ndss/*.java<br><br>
src> cd ../classes<br>
classes> jar -cvmf manifest.txt gameclient.jar . ../images/<br>
classes> java -jar gameclient.jar<br>

server compile/build/execute<br>
src> javac -encoding utf8 -d ../classes ga/ndss/*.java<br>
src> cd ../classes<br>
classes> jar -cvmf manifest.txt gameclient.jar .<br>
classes> java -jar gameserver.jar<br>


battleship_maven_gradle_package
------------------
source code format : UTF-8<br>
I have packaged this. I packaged it with maven and gradle. I had to package with maven to do TDD. I think it is getting bigger<br>

client compile/build/execute with maven<br>
BattleshipClient> mvn compile<br>
BattleshipClient> mvn package<br>
BattleshipClient> java -jar target/BattleshipClient-1.0-SNAPSHOT.jar<br>

server compile/build/execute with maven<br>
BattleshipServer> mvn compile<br>
BattleshipServer> mvn package<br>
BattleshipServer> java -jar target/BattleshipServer-1.0-SNAPSHOT.jar<br>

client compile/build/execute with gradle<br>
BattleshipClient> gradle runJar<br>

server compile/build/execute with gradle<br>
BattleshipServer> gradle runJar<br>

Contact
-------------------
name : SanSoo Han<br>
email : sansoo2002@naver.com<br>
phone : +82 10-8835-9229<br>
