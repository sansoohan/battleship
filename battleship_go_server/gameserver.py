#!/usr/bin/env python3
from socket import AF_INET, socket, SOCK_STREAM
from threading import Thread
import os
import sys
import random
import time

BUFSIZ = 1024


class GameServer:
  def __init__(self, host, port):
    self.__host = host
    self.__port = port
    self.gameRooms = []
    self.gameClients = []
    self.__serverSocket = socket(AF_INET, SOCK_STREAM)
    self.__serverSocket.bind((host, port))

  def run(self):
    self.__serverSocket.listen()
    print(f"GameServer started on {self.__host}:{self.__port}\n")
    while (True):
      clientSocket, clientAddress = self.__serverSocket.accept()
      print("%s:%s has connected." % clientAddress)
      newClient = GameClient(clientSocket, self)
      self.gameClients.append(newClient)
      Thread(target=newClient.run).start()
  
  def messageToAllClients(self, message):
    for gameClient in self.gameClients:
      gameClient.messageToClient(message)

  def removeGameClient(self, gameClient):
    gameClient.getClientSocket().close()
    self.gameClients.remove(gameClient)    

  def close(self):
    self.__serverSocket.close()

# GameServer End

class GameMap:
  def __init__(self):
    self.__MAP_COL = 7
    self.__MAP_ROW = 7
    self.__posision = [['[??]' for i in range(7)] for i in range(7)]

  def getPosition(self, row, col):
    return self.__posision[row][col]

  def setPosition(self, row, col, flag):
    self.__posision[row][col] = flag
# GameMap End


class GameClient:
  def __init__(self, clientSocket, gameServer):
    self.__roomObserver = None
    self.__clientID = ''
    self.__clientName = ''
    self.isRoomMaster = False
    self.isReady = False
    self.roomNumber = 0
    self.__clientSocket = clientSocket
    self.__gameServer = gameServer

  def messageToClient(self, message):
    message += '\n'
    message = message.encode()
    self.__clientSocket.send(message)
  
  def run(self):
    while True:
      try:
        message = self.__clientSocket.recv(BUFSIZ)
        self.messageController(message)
      except ConnectionResetError:
        self.__gameServer.removeGameClient(self)
        break
  
  def getClientSocket(self):
    return self.__clientSocket

  def getClientID(self):
    return self.__clientID

  def getClientName(self):
    return self.__clientName

  def messageController(self, message):
    message = message.decode()
    print(message)
    if (message[0] == '/'):
      command = message.rstrip().split(' ')[0]
      if (self.roomNumber == 0):
        if (command == '/make'):
          self.makeRoom(message)
        elif (command == '/join'):
          self.joinRoom(message)
        elif (command == '/test'):
          self.test(message)
        elif (command == '/login'):
          self.login(message)
        elif (command == '/newaccount'):
          self.newAccount(message)
        elif (command == '/passwdlost'):
          self.passwdLost(message)
      else:
        if (command == '/ready'):
          self.readyRoom(message)
        elif (command == '/leave'):
          self.leaveRoom(message)
        elif (command == '/start'):
          self.startTheGame(message)
        elif (command == '/attack'):
          self.fireOnMap(message)
    else:
      for client in self.__gameServer.gameClients:
        if (self.roomNumber == client.roomNumber):
          client.messageToClient(message)

  def makeRoom(self, message):
    selectedRoomNumber = int(message.rstrip().split(' ')[1])
    for gameRoom in self.__gameServer.gameRooms:
      if (gameRoom.roomNumber == selectedRoomNumber):
        self.messageToClient(f"Room#{selectedRoomNumber} is already exist!")
        return
    self.__gameServer.messageToAllClients(f"/clientstate {self.__clientID} from {self.roomNumber} to {selectedRoomNumber}")
    self.messageToClient(f"/make {selectedRoomNumber} ok")
    self.roomNumber = selectedRoomNumber
    self.isRoomMaster = True
    newRoom = GameRoomObserver(selectedRoomNumber, self.__gameServer)
    newRoom.roomNumber = selectedRoomNumber
    newRoom.gameClients.append(self)
    self.__roomObserver = newRoom
    self.__gameServer.gameRooms.append(newRoom)

  def joinRoom(self, message):
    selectedRoomNumber = int(message.rstrip().split(' ')[1])
    for gameRoom in self.__gameServer.gameRooms:
      if (gameRoom.roomNumber == selectedRoomNumber):
        if (gameRoom.gamePlayers):
          self.messageToClient(f"Room{selectedRoomNumber} is now playing.")
          return
        if (len(gameRoom.gameClients) == 6):
          self.messageToClient(f"Room{selectedRoomNumber} is full.")
          return
        gameRoom.gameClients.append(self)
        self.__roomObserver = gameRoom
        self.messageToClient(f"/join {selectedRoomNumber} ok")
        self.roomNumber = selectedRoomNumber
        return
    self.messageToClient(f"Room#{selectedRoomNumber} is not exist. Please Make Room.")

  def test(self, message):
    return message
  
  def login(self, message):
    userLoginInput = message.rstrip().split(' ')
    userLoginInput.pop(0)
    if (len(userLoginInput) != 2):
      self.messageToClient("/login notentered")
      return
    if (not os.path.exists('userInfo')):
      os.makedirs('userInfo')
    file = open('userInfo/userPrivacy.txt', 'r')
    for lineData in file.readlines():
      userData = lineData.rstrip().split(',')
      if (userData[0] == userLoginInput[0]) and (userData[1] == userLoginInput[1]):
        self.__clientID = userData[0]
        self.__clientName = userData[2]
        for otherClient in self.__gameServer.gameClients:
          if (otherClient.getClientSocket() != self.__clientSocket):
            self.messageToClient(f"/clientstate {otherClient.getClientID()} from {otherClient.roomNumber} to {otherClient.roomNumber}")
        self.messageToClient(f"/login ok {self.__clientID} {self.__clientName}")
        file.close()
        return
    file.close()
    self.messageToClient("/login mismatch")

  def newAccount(self, message):
    userSignUpInput = message.rstrip().split(' ')
    userSignUpInput.pop(0)
    if (not os.path.exists('userInfo')):
      os.makedirs('userInfo')
    file = open('userInfo/userPrivacy.txt', 'r')
    for lineData in file.readlines():
      userData = lineData.rstrip().split(',')
      if (userData[0] == userSignUpInput[0]):
        self.messageToClient("/newaccount idcollision")
        file.close()
        return
    file.close()

    file = open('userInfo/userPrivacy.txt', 'a+')
    file.write(','.join(userSignUpInput)+'\n')
    file.close()
    self.messageToClient("/newaccount ok")
  
  def passwdLost(self, message):
    userPasswdLostInput = message.rstrip().split(' ')
    userPasswdLostInput.pop(0)
    if (len(userPasswdLostInput) != 3):
      self.messageToClient("Please enter your ID and Email")
      return
    if (not os.path.exists('userInfo')):
      os.makedirs('userInfo')
    file = open('userInfo/userPrivacy.txt', 'r')
    for lineData in file.readlines():
      userData = lineData.rstrip().split(',')
      if (userData[0] == userPasswdLostInput[0]):
        self.messageToClient("Password will be sent to your Email")
        file.close()
        return
    file.close()
    self.messageToClient("There is no matching user")

  def readyRoom(self, message):
    self.isReady = not self.isReady
    if (self.isReady):
      self.messageToClient("You are ready")
    else:
      self.messageToClient("You are not ready")

  def leaveRoom(self, message):
    if (self.__roomObserver.gamePlayers):
      self.messageToClient("You can't leave while playing")
    else:
      self.__roomObserver.removeGameClient(self)
      if (self.isRoomMaster):
        self.isRoomMaster = False
        if (len(self.__roomObserver.gameClients) == 0):
          self.__gameServer.gameRooms.remove(self.__roomObserver)
        else:
          newRoomMaster = self.__roomObserver.gameClients[0]
          newRoomMaster.messageToClient("You are now room master")
          newRoomMaster.isRoomMaster = True
      self.__roomObserver = None
      self.__gameServer.messageToAllClients(f"/clientstate {self.__clientID} from {self.roomNumber} to 0")
      self.roomNumber = 0
      self.messageToClient("/leave ok")

  def startTheGame(self, message):
    ready = True
    if (self.isRoomMaster):
      for gameClient in self.__roomObserver.gameClients:
        if (not gameClient.isReady):
          gameClient.messageToClient("Ready Please")
          self.__roomObserver.messageToAllClients(f"{self.__clientName} is not ready")
          ready = False
      if (len(self.__roomObserver.gameClients) == 1):
        self.messageToClient("You need at least two Players.")
        ready = False
      
      if (not ready):
        self.messageToClient("You can't start the game.")
      else:
        self.__roomObserver.configGameModel()
        self.__roomObserver.generateGameModel()
        self.__roomObserver.initGameView()
    else:
      self.messageToClient("You are not a Room Master.")

  def fireOnMap(self, message):
    firedTarget = int(message.rstrip().split(' ')[1])
    if (not self.__roomObserver.gamePlayers):
      self.messageToClient("The game is not started")
    thisTurnPlayer = self.__roomObserver.gamePlayers[self.__roomObserver.getPlayerTurn()]
    if (thisTurnPlayer.gameClient == self):
      row = firedTarget // 7
      col = firedTarget % 7
      fireError = self.hasFireError(row, col, thisTurnPlayer)
      if (fireError == "No Error"):
        rowString = "ABCDEFG"
        colString = "0123456"
        fireMessage = f"{self.__clientName} fired on {rowString[row]}{colString[col]}"
        print(fireMessage)
        self.__roomObserver.messageToAllClients(fireMessage)
        self.__roomObserver.fireMessageToAllPlayers(row, col)
        self.__roomObserver.nextTurn()
      elif (fireError == "Suicide Error"):
        self.__roomObserver.suicidePlayer(thisTurnPlayer)
      elif (fireError == "Spot Error"):
        thisTurnPlayer.gameClient.messageToClient("There is no ship. Please select other spot.")
    else:
      self.messageToClient("It is not your turn")

  def hasFireError(self, row, col, thisTurnPlayer):
    flag = thisTurnPlayer.gameMap.getPosition(row, col)
    if (flag == '[ME]'):
      return "Suicide Error"
    elif (flag == '[xx]') or (flag == '[oo]'):
      return "Spot Error"
    elif (flag == '[??]'):
      return "No Error"
# GameClient End

class GameShip:
  def __init__(self, shipLength):
    self.__MAP_SIZE = 7
    self.__shipLength = shipLength
    self.posX = [None for i in range(shipLength)]
    self.posY = [None for i in range(shipLength)]
    self.setPosition()
    print(self.posX)
    print(self.posY)

  def setPosition(self):
    direction = random.randint(0,1)
    shortPos = random.randint(0, self.__MAP_SIZE - self.__shipLength)
    longPos = random.randint(0, self.__MAP_SIZE - 1)
    for i in range(self.__shipLength):
      if (direction == 0):
        self.posX[i] = shortPos + i
        self.posY[i] = longPos
      else:
        self.posX[i] = longPos
        self.posY[i] = shortPos + i

  def getShipLength(self):
    return self.__shipLength

  def hasCollision(self, savedShips):
    if (savedShips):
      for savedShip in savedShips:
        for savedShipPos in range(savedShip.getShipLength()):
          for thisShipPos in range(self.__shipLength):
            if (savedShip.posX[savedShipPos] == self.posX[thisShipPos]) and (savedShip.posY[savedShipPos] == self.posY[thisShipPos]):
              return True
    return False  
# GameShip End

class GameRoomObserver:
  def __init__(self, roomNumber, gameServer):
    self.__shipNum = None
    self.__shipLength = None
    self.__playerTurn = None
    self.gameShips = None
    self.gamePlayers = None
    self.gameClients = []
    self.roomNumber = roomNumber
    self.__gameServer = gameServer 

  def getPlayerTurn(self):
    return self.__playerTurn

  def configGameModel(self):
    if(len(self.gameClients) == 2): self.__shipNum = 1; self.__shipLength = 1
    elif(len(self.gameClients) == 3): self.__shipNum = 3; self.__shipLength = 3
    elif(len(self.gameClients) == 4): self.__shipNum = 2; self.__shipLength = 3
    elif(len(self.gameClients) == 5): self.__shipNum = 2; self.__shipLength = 3
    elif(len(self.gameClients) == 6): self.__shipNum = 2; self.__shipLength = 2

  def generateGameModel(self):
    self.__playerTurn = 0
    self.gamePlayers = []
    self.gameShips = []
    for gameClient in self.gameClients:
      shipCount = 0
      newPlayer = GamePlayer(gameClient, self.__shipNum, self.__shipLength)
      while (shipCount < self.__shipNum):
        newShip = GameShip(self.__shipLength)
        if (not newShip.hasCollision(self.gameShips)):
          self.gameShips.append(newShip)
          newPlayer.gameShips.append(newShip)
          shipCount += 1
      newPlayer.setMapModel('[ME]')
      self.gamePlayers.append(newPlayer)

    playerName = self.gamePlayers[self.__playerTurn].gameClient.getClientName()
    self.messageToAllClients('Game Start!')
    self.messageToAllClients(f"{playerName} turn")

  def initGameView(self):
    for gamePlayer in self.gamePlayers:
      for row in range(7):
        for col in range(7):
          pos = gamePlayer.gameMap.getPosition(row, col)
          gamePlayer.gameClient.messageToClient(f"/init {pos}")

  def nextTurn(self):
    beforeTurn = self.__playerTurn
    print(self.gamePlayers[self.__playerTurn].getLife())
    self.nextPlayer()
    while (True):
      if (self.gamePlayers[self.__playerTurn].getLife() == 0):
        self.nextPlayer()
      elif (self.gamePlayers[self.__playerTurn].gameClient == None):
        self.nextPlayer()
      else:
        break
    playerName = self.gamePlayers[self.__playerTurn].gameClient.getClientName()
    afterTurn = self.__playerTurn
    if (beforeTurn == afterTurn):
      self.messageToAllClients(f"{playerName} is a winner!")
      self.resetGame()
    else:
      self.messageToAllClients(f"{playerName} turn!")

  def nextPlayer(self):
    if (self.__playerTurn == len(self.gamePlayers)-1):
      self.__playerTurn = 0
    else:
      self.__playerTurn += 1

  def resetGame(self):
    self.__shipNum = None
    self.__shipLength = None
    self.__playerTurn = None
    self.gameShips = None
    self.gamePlayers = None
    self.messageToAllClients("/resetgame ok")
    for gameClient in self.gameClients:
      if (gameClient):
        gameClient.isReady = False

  def messageToAllClients(self, message):
    if self.gamePlayers:
      for gamePlayer in self.gamePlayers:
        if (gamePlayer.gameClient):
          gamePlayer.gameClient.messageToClient(message)
    else:
      for gameClient in self.gameClients:
        gameClient.messageToClient(message)

  def fireMessageToAllPlayers(self, row, col):
    flag = '[xx]'
    for gamePlayer in self.gamePlayers:
      if (gamePlayer.gameClient):
        if (gamePlayer.gameMap.getPosition(row, col) == '[ME]'):
          gamePlayer.loosLife()
          flag = '[oo]'
        gamePlayer.gameMap.setPosition(row, col, flag)
    attackPosition = f"%02d" % (row*7+col)
    self.messageToAllClients(f"/attack {attackPosition} {flag}")

  def removeGameClient(self, gameClient):
    self.gameClients.remove(gameClient)
    if (self.gamePlayers):
      for gamePlayer in self.gamePlayers:
        if (gamePlayer.gameClient == gameClient):
          suicidePlayer(gamePlayer)

  def suicidePlayer(self, gamePlayer):
    if (gamePlayer.suicideCount == 0): gamePlayer.gameClient.messageToClient("It is your ship. Select other place.")
    elif (gamePlayer.suicideCount == 1): gamePlayer.gameClient.messageToClient("Do you want to give up?")
    elif (gamePlayer.suicideCount == 2):
      gamePlayer.client.messageToClient("You gave up this game. Your ships will be sink.")
      self.destroyPlayer(gamePlayer)
      self.nextTurn()
    gamePlayer.suicideCount += 1

  def destroyPlayer(self, gamePlayer):
    for row in range(7):
      for col in range(7):
        if (gamePlayer.gameMap.getPosition(row, col) == '[ME]'):
          self.fireMessageToAllPlayers(row, col)

class GamePlayer:
  def __init__(self, gameClient, shipNum, shipLength):
    self.suicideCount = 0
    self.gameClient = gameClient
    self.gameShips = []
    self.gameMap = GameMap()
    self.__shipNum = shipNum
    self.__shipLength = shipLength
    self.__life = shipNum * shipLength
  
  def setMapModel(self, flag):
    for gameShip in self.gameShips:
      for pos in range(len(self.gameShips)):
        self.gameMap.setPosition(gameShip.posX[pos], gameShip.posY[pos], flag)

  def loosLife(self):
    self.__life -= 1

  def getLife(self):
    return self.__life

if __name__ == "__main__":
  gameServer = GameServer('', 5000)
  ACCEPT_THREAD = Thread(target=gameServer.run)
  ACCEPT_THREAD.start()
  ACCEPT_THREAD.join()
  gameServer.close()