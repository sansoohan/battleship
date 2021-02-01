package main

import (
	"bufio"
	"os"
	"io"
	"fmt"
	"net"
	"time"
	"strings"
	"strconv"
	"math/rand"
	"reflect"
)

const SECONDS = 1e9
const PATHSOCKET = "./var/go.sock"

func sleep(dt float64) {
	time.Sleep(time.Duration(dt * SECONDS))
}

func checkPrint(err error, message string) {
	if err != nil {
		panic(err)
	}
	fmt.Printf("%s\n", message)
}

type GameServer struct {
	host string
	port int
	GameRooms []*GameRoomObserver
	GameClients []*GameClient
	serverSocket net.Listener
}
func (gameServer *GameServer) Run() {
	fmt.Printf("GameServer started on %v:%v\n", gameServer.host, gameServer.port)
	for {
		clientSocket, err := gameServer.serverSocket.Accept()
		checkPrint(err, "Accepted connection.")
		gameClient := GameClient{
			roomObserver: &GameRoomObserver{},
			clientID: "",
			clientName: "",
			IsRoomMaster: false,
			IsReady: false,
			RoomNumber: 0,
			clientSocket: clientSocket,
			gameServer: gameServer,
		}
		gameServer.GameClients = append(gameServer.GameClients, &gameClient)
		go gameClient.Run()
	}
}
func (gameServer *GameServer) MessageToAllClients(message string) {
	for _, gameClient := range gameServer.GameClients {
		gameClient.MessageToClient(message)
	}
}
func (gameServer *GameServer) RemoveGameClient(gameClient *GameClient) {
	for k, v := range gameServer.GameClients {
		if gameClient == v {
			fmt.Println("Removed Client from Server")
			gameServer.GameClients = append(
				gameServer.GameClients[:k], 
				gameServer.GameClients[k+1:]...,
			)
			gameClient = nil
		}
	}
}

type GameClient struct {
	clientSocket net.Conn
	gameServer *GameServer
	roomObserver *GameRoomObserver
	clientID string
	clientName string
	IsReady bool
	IsRoomMaster bool
	RoomNumber int
}
func (gameClient *GameClient) MessageToClient(message string) {
	// sleep(0.5 + rand.Float64())
	gameClient.clientSocket.Write([]byte(message + "\n"))
}
func (gameClient *GameClient) Run() {
	buf := make([]byte, 1024)
	for {
		nread, err := gameClient.clientSocket.Read(buf)
		message := string(buf[0:nread])
		if err != nil {
			gameClient.gameServer.RemoveGameClient(gameClient)
			fmt.Printf("Client disconnected.\n")
			break
		}
		gameClient.MessageController(message)
		// reply := string(buf[0:nread]) // simple echo
		// sleep(0.5 + rand.Float64())
		// conn.Write([]byte(reply))
	}
}
func (gameClient *GameClient) GetClientSocket() net.Conn {
	return gameClient.clientSocket
}
func (gameClient *GameClient) GetClientID() string {
	return gameClient.clientID
}
func (gameClient *GameClient) GetClientName() string {
	return gameClient.clientName
}
func (gameClient *GameClient) MessageController(message string) {
	fmt.Printf(message)
	message = strings.TrimSpace(message)
	if len(message) >= 1 && message[0] == "/"[0] {
		command := strings.Split(message, " ")[0]
		if gameClient.RoomNumber == 0 {
			if command == "/make" {
				gameClient.MakeRoom(message)
			} else if command == "/join" {
				gameClient.JoinRoom(message)
			} else if command == "/test" {
				gameClient.Test(message)
			} else if command == "/login" {
				gameClient.Login(message)
			} else if command == "/newaccount" {
				gameClient.NewAccount(message)
			} else if command == "/passwdlost" {
				gameClient.PasswdLost(message)
			}
		} else {
			if command == "/ready" {
				gameClient.ReadyRoom(message)
			} else if command == "/leave" {
				gameClient.LeaveRoom(message)
			} else if command == "/start" {
				gameClient.StartTheGame(message)
			} else if command == "/attack" {
				gameClient.FireOnMap(message)
			}
		}
	} else {
		for _, client := range gameClient.gameServer.GameClients {
			if gameClient.RoomNumber == client.RoomNumber {
				client.MessageToClient(
					fmt.Sprintf("%v : %v", gameClient.clientName, message),
				)
			}
		}
	}
}
func (gameClient *GameClient) MakeRoom(message string) {
	selectedRoomNumber, err := strconv.Atoi(strings.Split(message, " ")[1])
	if err != nil {
		panic(err)
	}
	for _, gameRoom := range gameClient.gameServer.GameRooms {
		if gameRoom.RoomNumber == selectedRoomNumber {
			gameClient.MessageToClient(
				fmt.Sprintf("Room#%v is already exist!", selectedRoomNumber),
			)
			return
		}
	}
	gameClient.gameServer.MessageToAllClients(
		fmt.Sprintf(
			"/clientstate %v from %v to %v",
			gameClient.clientID,
			gameClient.RoomNumber,
			selectedRoomNumber,
		),
	)
	gameClient.MessageToClient(
		fmt.Sprintf("/make %v ok", selectedRoomNumber),
	)
	gameClient.RoomNumber = selectedRoomNumber
	gameClient.IsRoomMaster = true
	newRoom := GameRoomObserver{
		shipNum: 0,
		shipLength: 0,
		playerTurn: 0,
		GameShips: []*GameShip{},
		GamePlayers: []*GamePlayer{},
		GameClients: []*GameClient{gameClient},
		RoomNumber: selectedRoomNumber,
		gameServer: gameClient.gameServer,
	}
	gameClient.roomObserver = &newRoom
	gameClient.gameServer.GameRooms = append(gameClient.gameServer.GameRooms, &newRoom)
	return
}
func (gameClient *GameClient) JoinRoom(message string) {
	selectedRoomNumber, err := strconv.Atoi(strings.Split(message, " ")[1])
	if err != nil {
		panic(err)
	}
	for _, gameRoom := range gameClient.gameServer.GameRooms {
		if gameRoom.RoomNumber == selectedRoomNumber {
			if len(gameRoom.GamePlayers) != 0 {
				gameClient.MessageToClient(
					fmt.Sprintf("Room%v is now playing.", selectedRoomNumber),
				)
				return
			}
			if len(gameRoom.GameClients) == 6 {
				gameClient.MessageToClient(
					fmt.Sprintf("Room%v is full.", selectedRoomNumber),
				)
				return
			}
			gameRoom.GameClients = append(gameRoom.GameClients, gameClient)	
			gameClient.roomObserver = gameRoom
			gameClient.MessageToClient(
				fmt.Sprintf("/join %v ok", selectedRoomNumber),
			)
			gameClient.RoomNumber = selectedRoomNumber
			return
		}
	}
	gameClient.MessageToClient(
		fmt.Sprintf("Room#%v is not exist. Please Make Room.", selectedRoomNumber),
	)
}
func (gameClient *GameClient) Test(message string) string {
	return message
}
func (gameClient *GameClient) Login(message string) {
	userLoginInput := strings.Split(message, " ")[1:]
	if len(userLoginInput) != 2 {
		gameClient.MessageToClient("/login notentered")
		return
	}
	if _, err := os.Stat("userInfo"); os.IsNotExist(err) {
		os.Mkdir("userInfo", 0755)
	}
	file, err := os.Open("userInfo/userPrivacy.txt")
	if err != nil {
		panic(err)
	}
	defer file.Close()
	reader := bufio.NewReaderSize(file, 4096)
	for {
		lineData, _, err := reader.ReadLine()
		userData := strings.Split(string(lineData), ",")
		if userData[0] == userLoginInput[0] && userData[1] == userLoginInput[1] {
			gameClient.clientID = userData[0]
			gameClient.clientName = userData[2]
			for _, otherClient := range gameClient.gameServer.GameClients {
				otherClientSocket := otherClient.GetClientSocket()
				if &otherClientSocket != &gameClient.clientSocket {
					gameClient.MessageToClient(
						fmt.Sprintf(
							"/clientstate %v from %v to %v",
							otherClient.GetClientID(),
							otherClient.RoomNumber,
							otherClient.RoomNumber,
						),
					)
				}
			}
			gameClient.MessageToClient(
				fmt.Sprintf(
					"/login ok %v %v",
					gameClient.clientID,
					gameClient.clientName,
				),
			)
			file.Close()
			return
		}
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
	}
	gameClient.MessageToClient("/login mismatch")
}
func (gameClient *GameClient) NewAccount(message string) {
	userSignUpInput := strings.Split(message, " ")[1:]
	if _, err := os.Stat("userInfo"); os.IsNotExist(err) {
		os.Mkdir("userInfo", 0755)
	}
	file, err := os.Open("userInfo/userPrivacy.txt")
	if err != nil {
		panic(err)
	}
	defer file.Close()
	reader := bufio.NewReaderSize(file, 4096)
	for {
		lineData, _, err := reader.ReadLine()
		userData := strings.Split(string(lineData), ",")
		if userData[0] == userSignUpInput[0] {
			gameClient.MessageToClient("/newaccount idcollision")
			file.Close()
			return
		}
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
	}

	file, err = os.OpenFile("userInfo/userPrivacy.txt", os.O_APPEND|os.O_WRONLY, 0644)
	if err != nil {
    panic(err)
	}
	defer file.Close()
	fmt.Fprintf(file, "%v", strings.Join(userSignUpInput,",") + "\n")
	file.Close()
	gameClient.MessageToClient("/newaccount ok")
}
func (gameClient *GameClient) PasswdLost(message string) {
	userPasswdLostInput := strings.Split(message, " ")[1:]
	if _, err := os.Stat("userInfo"); os.IsNotExist(err) {
		os.Mkdir("userInfo", 0755)
	}
	if (len(userPasswdLostInput) != 3) {
		gameClient.MessageToClient("Please enter your ID and Email")
		return
	}
	file, err := os.Open("userInfo/userPrivacy.txt")
	if err != nil {
		panic(err)
	}
	defer file.Close()
	reader := bufio.NewReaderSize(file, 4096)
	for {
		lineData, _, err := reader.ReadLine()
		userData := strings.Split(string(lineData), ",")
		if userData[0] == userPasswdLostInput[0] {
			gameClient.MessageToClient("Password will be sent to your Email")
			file.Close()
			return
		}
		if err == io.EOF {
			break
		} else if err != nil {
			panic(err)
		}
	}
	file.Close()
	gameClient.MessageToClient("There is no matching user")
}
func (gameClient *GameClient) ReadyRoom(message string) {
	gameClient.IsReady = !gameClient.IsReady
	if (gameClient.IsReady) {
		gameClient.MessageToClient("You are ready")
	} else {
		gameClient.MessageToClient("You are not ready")
	}
}
func (gameClient *GameClient) LeaveRoom(message string) {
	if len(gameClient.roomObserver.GamePlayers) != 0 {
		gameClient.MessageToClient("You can't leave while playing")
	} else {
		gameClient.roomObserver.RemoveGameClient(gameClient)
		if gameClient.IsRoomMaster {
			gameClient.IsRoomMaster = false
			if len(gameClient.roomObserver.GameClients) == 0 {
				for k, v := range gameClient.gameServer.GameRooms {
					if gameClient.roomObserver == v {
						fmt.Println("Removed Client")
						gameClient.gameServer.GameRooms = append(
							gameClient.gameServer.GameRooms[:k],
							gameClient.gameServer.GameRooms[k+1:]...,
						)
					}
				}
			} else {
				newRoomMaster := gameClient.roomObserver.GameClients[0]
				newRoomMaster.MessageToClient("You are now room master")
				newRoomMaster.IsRoomMaster = true
			}
		}
		gameClient.roomObserver = &GameRoomObserver{}
		gameClient.gameServer.MessageToAllClients(
			fmt.Sprintf(
				"/clientstate %v from %v to 0",
				gameClient.clientID,
				gameClient.RoomNumber,
			),
		)
		gameClient.RoomNumber = 0
		gameClient.MessageToClient("/leave ok")
	}
}
func (gameClient *GameClient) StartTheGame(message string) {
	ready := true
	if gameClient.IsRoomMaster {
		for _, otherClient := range gameClient.roomObserver.GameClients {
			if !otherClient.IsReady {
				otherClient.MessageToClient("Ready Please")
				otherClient.roomObserver.MessageToAllClients(
					fmt.Sprintf(
						"%v is not ready",
						gameClient.clientName,
					),
				)
				ready = false
			}
		}
		if len(gameClient.roomObserver.GameClients) == 1 {
			gameClient.MessageToClient("You need at least two Players.")
			ready = false
		}
		if !ready {
			gameClient.MessageToClient("You can't start the game.")
		} else {
			gameClient.roomObserver.ConfigGameModel()
			gameClient.roomObserver.GenerateGameModel()
			gameClient.roomObserver.InitGameView()
		}
	} else {
		gameClient.MessageToClient("You are not a Room Master.")
	}
}
func (gameClient *GameClient) FireOnMap(message string) {
	firedTarget, err := strconv.Atoi(strings.Split(message, " ")[1])
	if err != nil {
		panic(err)
	}
	if len(gameClient.roomObserver.GamePlayers) == 0 {
		gameClient.MessageToClient("The game is not started")
		return
	}
	thisTurnPlayer := gameClient.roomObserver.GamePlayers[gameClient.roomObserver.GetPlayerTurn()]
	if thisTurnPlayer.GameClient == gameClient {
		row := firedTarget / 7
		col := firedTarget % 7
		fireError := gameClient.HasFireError(row, col, thisTurnPlayer)
		if fireError == "No Error" {
			rowString := "ABCDEFG"
			colString := "0123456"
			fireMessage := fmt.Sprintf(
				"%v fired on %v%v",
				gameClient.clientName,
				rowString[row],
				colString[col],
			)
			gameClient.roomObserver.MessageToAllClients(fireMessage)
			gameClient.roomObserver.FireMessageToAllPlayers(row, col)
			gameClient.roomObserver.NextTurn()
		} else if fireError == "Suicide Error" {
			gameClient.roomObserver.SuicidePlayer(thisTurnPlayer)
		} else if fireError == "Spot Error" {
			thisTurnPlayer.GameClient.MessageToClient("There is no ship. Please select other spot.")
		}
	} else {
		gameClient.MessageToClient("It is not your turn")
	}
}
func (gameClient *GameClient) HasFireError(row int, col int, thisTurnPlayer *GamePlayer) string {
	flag := thisTurnPlayer.GameMap.GetPosition(row, col)
	if flag == "[ME]" {
		return "Suicide Error"
	} else if flag == "[xx]" || flag == "[oo]" {
		return "Spot Error"
	} else if flag == "[??]" {
		return "No Error"
	}
	return ""
}


type GameRoomObserver struct {
	shipNum int
	shipLength int
	playerTurn int
	GameShips []*GameShip
	GamePlayers []*GamePlayer
	GameClients []*GameClient
	RoomNumber int
	gameServer *GameServer
}
func (gameRoom *GameRoomObserver) GetPlayerTurn() int {
	return gameRoom.playerTurn
}
func (gameRoom *GameRoomObserver) ConfigGameModel() {
	fmt.Println("ConfigGameModel")
	switch(len(gameRoom.GameClients)) {
		case 2: gameRoom.shipNum = 1; gameRoom.shipLength = 1
		case 3: gameRoom.shipNum = 3; gameRoom.shipLength = 3
		case 4: gameRoom.shipNum = 2; gameRoom.shipLength = 3
		case 5: gameRoom.shipNum = 2; gameRoom.shipLength = 3
		case 6: gameRoom.shipNum = 2; gameRoom.shipLength = 2
	}
}
func (gameRoom *GameRoomObserver) GenerateGameModel() {
	fmt.Println("GenerateGameModel")
	gameRoom.playerTurn = 0
	gameRoom.GamePlayers = []*GamePlayer{}
	gameRoom.GameShips = []*GameShip{}
	for _, gameClient := range gameRoom.GameClients {
		shipCount := 0
		gameMap := GameMap{
			mapCol: 7,
			mapRow: 7,
			posision: [][]string{
				{"[??]", "[??]", "[??]", "[??]", "[??]", "[??]", "[??]"},
				{"[??]", "[??]", "[??]", "[??]", "[??]", "[??]", "[??]"},
				{"[??]", "[??]", "[??]", "[??]", "[??]", "[??]", "[??]"},
				{"[??]", "[??]", "[??]", "[??]", "[??]", "[??]", "[??]"},
				{"[??]", "[??]", "[??]", "[??]", "[??]", "[??]", "[??]"},
				{"[??]", "[??]", "[??]", "[??]", "[??]", "[??]", "[??]"},
				{"[??]", "[??]", "[??]", "[??]", "[??]", "[??]", "[??]"},
			},
		}
		newPlayer := GamePlayer{
			SuicideCount: 0,
			GameClient: gameClient,
			GameShips: []*GameShip{},
			GameMap: &gameMap,
			shipNum: gameRoom.shipNum,
			shipLength: gameRoom.shipLength,
			life: gameRoom.shipNum * gameRoom.shipLength,
		}
		for shipCount < gameRoom.shipNum {
			newShip := GameShip{
				mapSize: 7,
				shipLength: gameRoom.shipLength,
				PosX: make([]int, gameRoom.shipLength),
				PosY: make([]int, gameRoom.shipLength),
			}
			newShip.SetPosition()
			fmt.Println(shipCount)
			if !newShip.HasCollision(gameRoom.GameShips) {
				gameRoom.GameShips = append(gameRoom.GameShips, &newShip)
				newPlayer.GameShips = append(newPlayer.GameShips, &newShip)
				shipCount += 1
			}
		}
		newPlayer.SetMapModel("[ME]")
		gameRoom.GamePlayers = append(gameRoom.GamePlayers, &newPlayer)
	}
	playerName := gameRoom.GamePlayers[gameRoom.playerTurn].GameClient.GetClientName()
	gameRoom.MessageToAllClients("Game Start!")
	gameRoom.MessageToAllClients(
		fmt.Sprintf(
			"%v turn",
			playerName,
		),
	)
}
func (gameRoom *GameRoomObserver) InitGameView() {
	fmt.Println("InitGameView")
	for _, gamePlayer := range gameRoom.GamePlayers {
		for row := 0; row < 7; row++ {
			for col := 0; col < 7; col++ {
				pos := gamePlayer.GameMap.GetPosition(row, col)
				gamePlayer.GameClient.MessageToClient(
					fmt.Sprintf("/init %v", pos),
				)
			}
		}
	}
}
func (gameRoom *GameRoomObserver) NextTurn() {
	beforeTurn := gameRoom.playerTurn
	fmt.Println(gameRoom.GamePlayers[gameRoom.playerTurn].GetLife())
	gameRoom.NextPlayer()
	for true {
		if gameRoom.GamePlayers[gameRoom.playerTurn].GetLife() == 0 {
			gameRoom.NextPlayer()
		} else if reflect.DeepEqual(gameRoom.GamePlayers[gameRoom.playerTurn].GameClient, GameClient{}) {
			gameRoom.NextPlayer()
		} else {
			break
		}
	}
	playerName := gameRoom.GamePlayers[gameRoom.playerTurn].GameClient.GetClientName()
	afterTurn := gameRoom.playerTurn
	if beforeTurn == afterTurn {
		gameRoom.MessageToAllClients(
			fmt.Sprintf(
				"%v is a winner!",
				playerName,
			),
		)
		gameRoom.ResetGame()
	} else {
		gameRoom.MessageToAllClients(
			fmt.Sprintf(
				"%v turn!",
				playerName,
			),
		)
	}
}
func (gameRoom *GameRoomObserver) NextPlayer() {
	if gameRoom.playerTurn == len(gameRoom.GamePlayers)-1 {
		gameRoom.playerTurn = 0
	} else {
		gameRoom.playerTurn += 1
	}
	fmt.Println(gameRoom.playerTurn)
}
func (gameRoom *GameRoomObserver) ResetGame() {
	gameRoom.shipNum = 0
	gameRoom.shipLength = 0
	gameRoom.playerTurn = 0
	gameRoom.GameShips = []*GameShip{}
	gameRoom.GamePlayers = []*GamePlayer{}
	gameRoom.MessageToAllClients("/resetgame ok")
	for _, gameClient := range gameRoom.GameClients {
		gameClient.IsReady = false
	}
}
func (gameRoom *GameRoomObserver) MessageToAllClients(message string) {
	if len(gameRoom.GamePlayers) != 0 {
		for _, gamePlayer := range gameRoom.GamePlayers {
			gamePlayer.GameClient.MessageToClient(message)
		}
	} else {
		for _, gameClient := range gameRoom.GameClients {
			gameClient.MessageToClient(message)
		}
	}
}
func (gameRoom *GameRoomObserver) FireMessageToAllPlayers(row int, col int) {
	flag := "[xx]"
	for _, gamePlayer := range gameRoom.GamePlayers {
		if !reflect.DeepEqual(gamePlayer.GameClient, GameClient{}) {
			if gamePlayer.GameMap.GetPosition(row, col) == "[ME]" {
				gamePlayer.LoosLife()
				flag = "[oo]"
			}
			gamePlayer.GameMap.SetPosition(row, col, flag)
		}
	}
	attackPosition := fmt.Sprintf("%02d", row*7+col)
	gameRoom.MessageToAllClients(
		fmt.Sprintf(
			"/attack %v %v",
			attackPosition,
			flag,
		),
	)
}
func (gameRoom *GameRoomObserver) RemoveGameClient(gameClient *GameClient) {
	for k, v := range gameRoom.GameClients {
		if gameClient.clientSocket == v.clientSocket {
			fmt.Println("Removed Client from room")
			gameRoom.GameClients = append(
				gameRoom.GameClients[:k],
				gameRoom.GameClients[k+1:]...,
			)
			// gameRoom.SuicidePlayer(&v)
			// v.GameClient = GameClient{}
		}
	}
}
func (gameRoom *GameRoomObserver) SuicidePlayer(gamePlayer *GamePlayer) {
	if (gamePlayer.SuicideCount == 0) {
		gamePlayer.GameClient.MessageToClient("It is your ship. Select other place.")
	} else if (gamePlayer.SuicideCount == 1) {
		gamePlayer.GameClient.MessageToClient("Do you want to give up?")
	} else if (gamePlayer.SuicideCount == 2) {
		gamePlayer.GameClient.MessageToClient("You gave up this game. Your ships will be sink.")
		gameRoom.DestroyPlayer(gamePlayer)
		gameRoom.NextTurn()
	}
	gamePlayer.SuicideCount += 1
}
func (gameRoom *GameRoomObserver) DestroyPlayer(gamePlayer *GamePlayer) {
	for row := 0; row < 7; row++ {
		for col := 0; col < 7; col++ {
			if gamePlayer.GameMap.GetPosition(row, col) == "[ME]" {
				gameRoom.FireMessageToAllPlayers(row, col)
			}
		}
	}
}

type GameShip struct {
	mapSize int
	shipLength int
	PosX []int
	PosY []int
}
func (gameShip *GameShip) SetPosition() {
	rand.Seed(time.Now().UnixNano())
	direction := rand.Intn(2)
	rand.Seed(time.Now().UnixNano())
	shortPos := rand.Intn(gameShip.mapSize - gameShip.shipLength)
	rand.Seed(time.Now().UnixNano())
	longPos := rand.Intn(gameShip.mapSize - 1)
	for i := 0; i < gameShip.shipLength; i++ {
		if direction == 0 {
			gameShip.PosX[i] = shortPos + i
			gameShip.PosY[i] = longPos
		} else {
			gameShip.PosX[i] = longPos
			gameShip.PosY[i] = shortPos + i
		}
	}
}
func (gameShip *GameShip) GetShipLength() int {
	return gameShip.shipLength
}
func (gameShip *GameShip) HasCollision(savedShips []*GameShip) bool {
	for _, savedShip := range savedShips {
		for savedShipPos := 0; savedShipPos < savedShip.GetShipLength(); savedShipPos++ {
			for thisShipPos := 0; thisShipPos < gameShip.shipLength; thisShipPos++ {
				if 
					savedShip.PosX[savedShipPos] == gameShip.PosX[thisShipPos] &&
					savedShip.PosY[savedShipPos] == gameShip.PosY[thisShipPos] {
					return true
				}
			}
		}
	}
	return false
}

type GamePlayer struct {
	SuicideCount int
	GameClient *GameClient
	GameShips []*GameShip
	GameMap *GameMap
	shipNum int
	shipLength int
	life int
}
func (gamePlayer *GamePlayer) SetMapModel(flag string) {
	for _, gameShip := range gamePlayer.GameShips {
		for pos := 0; pos < len(gamePlayer.GameShips); pos++ {
			gamePlayer.GameMap.SetPosition(gameShip.PosX[pos], gameShip.PosY[pos], flag)
		}
	}
}
func (gamePlayer *GamePlayer) LoosLife() {
	gamePlayer.life -= 1
}
func (gamePlayer *GamePlayer) GetLife() int {
	return gamePlayer.life
}


type GameMap struct {
	mapCol int
	mapRow int
	posision [][]string
}
func (gameMap *GameMap) GetPosition(row int, col int) string {
	return gameMap.posision[row][col]
}
func (gameMap *GameMap) SetPosition(row int, col int, flag string) {
	gameMap.posision[row][col] = flag
}

func main() {
	fmt.Println("Hello, playground")
	serverSocket, err := net.Listen("tcp", "localhost:5000")
	checkPrint(err, "Server is ready.")
	gameServer := GameServer{
		host: "localhost",
		port: 5000,
		GameRooms: []*GameRoomObserver{},
		GameClients: []*GameClient{},
		serverSocket: serverSocket,
	}
	gameServer.Run()
}
