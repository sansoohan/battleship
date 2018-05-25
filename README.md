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


battleship_package
------------------
소스 문자포멧 UTF-8
서버와 클라이언트파일을 클래스들을 쪼개서 하위폴더로 정리했습니다. 유지보수용입니다.
TDD및 빌드툴을 고려하지 않고 만들었습니다.

주의사항
-------------------
문자 포멧 때문에 컴파일시 주석부분에서 에러가 일어날 수 있습니다.
리눅스를 쓰신다면 UTF-8포멧인 battleship_package,
윈도우를 쓰신다면 EUC-KR포멧인 battleship_nonpackage로 컴파일하시면 됩니다.
한글로 쓰여진 주석을 다 지워버리시는 것도 방법입니다.

제작자 컨택
-------------------
name : 한산수
email : sansoo2002@naver.com
phone : 010-8835-9229
