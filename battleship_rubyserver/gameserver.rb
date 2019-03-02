require "socket"
require "thread"
require 'securerandom'

class GameServer
    def initialize(port)
        @gameRoomList = Array::new
        @clients = Array::new
        @serverSocket = TCPServer.new("", port)
        @serverSocket.setsockopt(Socket::SOL_SOCKET, Socket::SO_REUSEADDR, 1)
        printf("GameServer started on port %d\n", port)
    end
    def run
        while 1
            Thread.start(@serverSocket.accept) do |clientSocket|
                newClient = GameClient.new(clientSocket, self)
                @clients.push(newClient)
                begin
                    newClient.run
                rescue Errno::ECONNRESET => e
                    printf("Client Left %s:%s\n",newClient.clientSocket.peeraddr[2], newClient.clientSocket.peeraddr[1])
                    newClient.clientSocket.close
                    @clients.delete(newClient)
                    if newClient.roomNumber
                        for gameRoom in @gameRoomList do
                            gameRoom.removeClient(newClient)
                        end
                    end
                    Thread.current.kill
                end
            end
        end
    end

    def messageToEverybody(message)
        for client in @clients do
            begin
                client.messageToClient(message) 
            rescue => exception
                puts exception
            end
        end        
    end

    def gameRoomList
        @gameRoomList
    end
    def clients
        @clients
    end
    def serverSocket
        @serverSocket
    end
end

class GameMap
    def initialize
        @MAPCOL = 7
        @MAPROW = 7
        @position = Array.new(@MAPROW) {Array.new(@MAPCOL, '[??]')}
    end
    def getPosition(row, col)
        @position[row][col]
    end
    def setPosition(row, col, flag)
        @position[row][col] = flag
    end
end # GameMap End


class GameClient
    def initialize(clientSocket, server)
        @roomObserver
        @clientID = ""
        @clientName = ""
        @isRoomMaster = false
        @isReady = false
        @roomNumber = 0
        @clientSocket = clientSocket
        @server = server
        @semaphore = Mutex.new
        @messageFromClient
        @thread
    end

    def run
        while 1
            @messageFromClient = getMessage
            begin
                messageController(@messageFromClient)    
            rescue => exception
                puts exception
            end
        end
    end

    def messageController(message)
        # Response Message
        puts message
        if message[0] == '/'
            if @roomNumber == 0
                case message.rstrip.split(/ /)[0]
                when '/make'
                    makeRoom(message)
                when '/join'
                    joinRoom(message)
                when '/test'
                    test(message)
                when '/login'
                    login(message)
                when '/newaccount'
                    newAccount(message)
                when '/passwdlost'
                    passwdLost(message)
                end
            else
                case message.rstrip.split(/ /)[0]
                when '/ready'
                    readyRoom(message)
                when '/leave'
                    leaveRoom(message)
                when '/start'
                    startTheGame(message)
                when '/attack'
                    fireOnMap(message)
                end
            end
        # Room Chat
        # Waiting Room : 0
        # Game Room : 1 ~ 20
        else
            for client in @server.clients do
                if @roomNumber == client.roomNumber
                    sleep(0.01)
                    begin
                        client.messageToClient(message)
                    rescue => exception
                        puts exception 
                    end
                end
            end
        end
    end

    def makeRoom(message)
        temp = message.rstrip.split(/ /)[1]
        temp = temp.to_i
        for gameRoom in @server.gameRoomList do
            if gameRoom.roomNumber == temp
                begin
                    messageToClient("Room#{temp} is already exist!")
                rescue => exception
                    puts exception 
                end
                return
            end
        end
        @server.messageToEverybody("/clientstate #{@clientID} from #{@roomNumber} to #{temp}")
        begin
            messageToClient("/make #{temp} ok")
        rescue => exception
            puts exception 
        end
        @roomNumber = temp
        @isRoomMaster = true
        newRoom = GameRoomObserver.new(temp, @server)
        newRoom.roomNumber = temp
        newRoom.clientsInRoom.push(self)
        @roomObserver = newRoom
        @server.gameRoomList.push(newRoom)
    end

    def joinRoom(message)
        temp = message.rstrip.split(/ /)[1]
        for gameRoom in @server.gameRoomList do
            if gameRoom.roomNumber == temp.to_i
                if gameRoom.playersInRoom
                    begin
                        messageToClient("Room#{temp} is now playing.")
                    rescue => exception
                        puts exception 
                    end
                    return
                end
                if gameRoom.clientsInRoom.length == 6
                    begin
                        messageToClient("Room#{temp} is full.")
                    rescue => exception
                        puts exception 
                    end
                    return
                end
                gameRoom.clientsInRoom.push(self)
                @roomObserver = gameRoom
                begin
                    messageToClient("/join #{temp} ok")
                rescue => exception
                    puts exception 
                end
                @roomNumber = temp.to_i
                return
            end
        end
        begin
            messageToClient("Room#{temp} is not exist. Please Make Room.")
        rescue => exception
            puts exception 
        end
    end
    def test(message)
        
    end
    def login(message)
        userInfo = message.rstrip.split(/ /)
        userInfo.shift
        if userInfo.length != 2
            begin
                messageToClient("/login notentered")
            rescue => exception
                puts exception 
            end
            return
        end
        Dir.mkdir('userInfo') unless File.exists?('userInfo')
        File.new("userInfo/userPrivacy.txt","w") unless File.exist?("userInfo/userPrivacy.txt")
        for lineData in File.open("userInfo/userPrivacy.txt","r") do
            userData = lineData.rstrip.split(/,/)
            if (userData[0] == userInfo[0]) && (userData[1] == userInfo[1])
                @clientID = userData[0]
                @clientName = userData[2]
                @server.messageToEverybody("/clientstate #{@clientID} from #{@roomNumber} to #{@roomNumber}")
                for otherClient in @server.clients do
                    if otherClient.clientSocket != @clientSocket
                        begin
                            messageToClient("/clientstate #{otherClient.clientID} from #{otherClient.roomNumber} to #{otherClient.roomNumber}") 
                        rescue => exception
                            puts exception 
                        end
                    end
                end
                begin
                    messageToClient("/login ok #{@clientID} #{@clientName}")
                rescue => exception
                    puts exception 
                end
                return
            end
        end
        begin
            messageToClient("/login mismatch")
        rescue => exception
            puts exception 
        end
    end
    def newAccount(message)
        userInfo = message.rstrip.split(/ /)
        userInfo.shift
        Dir.mkdir('userInfo') unless File.exists?('userInfo')
        File.new("userInfo/userPrivacy.txt","w") unless File.exist?("userInfo/userPrivacy.txt")
        userPravacyFile = File.new("userInfo/userPrivacy.txt","a")
        for lineData in File.open("userInfo/userPrivacy.txt", "r") do
            userData = lineData.rstrip.split(/,/)
            if userData[0] == userInfo[0]
                begin
                    messageToClient("/newaccount idcollision")
                rescue => exception
                    puts exception 
                end
                return
            end
        end.close
        userInfoTofile = userInfo.join(',')
        userPravacyFile.puts(userInfoTofile)
        userPravacyFile.close
        begin
            messageToClient("/newaccount ok")
        rescue => exception
            puts exception 
        end
    end
    def passwdLost(message)
        userInfo = message.rstrip.split(/ /)
        userInfo.shift
        if userInfo.length != 3
            begin
                messageToClient("Please enter your id and email")
            rescue => exception
                puts exception 
            end
            return
        end
        Dir.mkdir('userInfo') unless File.exists?('userInfo')
        File.new("userInfo/userPrivacy.txt","w") unless File.exist?("userInfo/userPrivacy.txt")
        for lineData in File.open("userInfo/userPrivacy.txt", "r") do
            userData = lineData.split(/,/)
            if userData[0] == userInfo[0]
                begin
                    messageToClient("Password will be sent to your email")
                rescue => exception
                    puts exception 
                end
                return
            end
        end
        begin
            messageToClient("There is no matching user")
        rescue => exception
            puts exception 
        end
    end

    def getMessage
        return @clientSocket.gets()
    end
    
    def messageToClient(message)
        @semaphore.synchronize do
            @clientSocket.puts(message)
            @clientSocket.flush
        end
    end

    def readyRoom(message)
        if @isReady == true
            @isReady = false
        else
            @isReady = true
        end
        begin
            messageToClient("You are #{@isReady ? "ready" : "not ready"}")
        rescue => exception
            puts exception 
        end
    end
    def leaveRoom(message)
        if @roomObserver.playersInRoom
            begin
                messageToClient("You can't leave while playing")
            rescue => exception
                puts exception 
            end
        else
            @roomObserver.clientsInRoom.removeClient(self)
            if @isRoomMaster == true
                @isRoomMaster = false
                if @roomObserver.clientsInRoom.length == 0
                    @server.gameRoomLiast.delete(@roomObserver)
                else
                    newRoomMaster = @roomObserver.clientsInRoom[0]
                    begin
                        newRoomMaster.messageToClient("You are now room master")
                    rescue => exception
                        puts exception 
                    end
                    newRoomMaster.isRoomMaster = true
                end
            end
            @roomObserver = nil
            @server.messageToEverybody("/clientstate #{@clientID} from #{@roomNumber} to 0")
            @roomNumber = 0
            begin
                messageToClient("/leave ok")
            rescue => exception
                puts exception 
            end
        end
    end
    def startTheGame(message)
        ready = true
        if @isRoomMaster == true
            for roomClient in @roomObserver.clientsInRoom do
                if roomClient.isReady == false
                    begin
                        roomClient.messageToClient("Ready Please")
                    rescue => exception
                        puts exception 
                    end
                    @roomObserver.messageToRoom("#{@clientName} is not ready")
                    ready = false
                end
            end
            playersSize = @roomObserver.clientsInRoom.length
            if playersSize == 1
                begin
                    messageToClient("You need at least two Players.")
                rescue => exception
                    puts exception 
                end
                ready = false
            end

            if ready == false
                begin
                    messageToClient("You can't start the game")
                rescue => exception
                    puts exception 
                end
            else
                @roomObserver.configGameModel(self)
                @roomObserver.generateGameModel
                @roomObserver.initGameView
            end
        else
            begin
                messageToClient("You are not a Room Master.")
                ret = false
            rescue => exception
                puts exception
            end
        end
    end
    def fireOnMap(message)
        firedTarget = message.rstrip.split(/ /)[1].to_i
        if @roomObserver.playersInRoom == nil
            begin
                messageToClient("The game is not started")
            rescue => exception
                puts exception 
            end
            return
        end
        thisTurnPlayer = @roomObserver.playersInRoom[@roomObserver.playerTurn]
        if thisTurnPlayer.client == self
            row = firedTarget / 7
            col = firedTarget % 7
            fireError = hasFireError(row,col,thisTurnPlayer)
            case fireError
            when "No Error"
                rowString = "ABCDEFG"
                colString = "0123456"
                fireMessage = "#{@clientName} fired on #{rowString[row]}#{colString[col]}"
                puts fireMessage
                @roomObserver.messageToRoom(fireMessage)
                @roomObserver.fireMessageToPlayers(row,col)
                @roomObserver.nextTurn
            when "Suicide Error"
                @roomObserver.suicidePlayer(thisTurnPlayer)
            when "Spot Error"
                thisTurnPlayer.client.messageToClient("There is no ship. Please select other spot.")
            end
        else
            begin
                messageToClient("It is not your turn.")
            rescue => exception
                puts exception 
            end
        end
    end

    def hasFireError(row,col,thisTurnPlayer)
        flag = thisTurnPlayer.gameMap.getPosition(row, col)
        case flag
        when '[ME]'
            return "Suicide Error"
        when '[xx]','[oo]'
            return "Spot Error"
        when '[??]'
            return "No Error"
        end
    end

    def thread
        @thread
    end
    def thread=(th)
        @thread = th
    end
    def clientSocket
        @clientSocket
    end

    def roomNumber
        @roomNumber
    end
    def roomNumber=(number)
        @roomNumber = number
    end
    def clientID
        @clientID
    end
    def clientID=(id)
        @clientID = id
    end
    def clientName
        @clientName
    end
    def roomObserver=(ob)
        @roomObserver = ob
    end
    def isRoomMaster
        @isRoomMaster
    end
    def isRoomMaster=(isMaster)
        @isRoomMaster = isMaster
    end
    def isReady
        @isReady
    end
    def isReady=(ready)
        @isReady = ready
    end
end # GameClient End



class GameShip
    def initialize(shipLength)
        @MAPSIZE = 7
        @shipLength = shipLength
        @posX = Array.new(shipLength, nil)
        @posY = Array.new(shipLength, nil)
        setPosition
    end
    def setPosition
        direction = SecureRandom.random_number(2)
        shortPos = SecureRandom.random_number(@MAPSIZE - @shipLength +1)
        longPos = SecureRandom.random_number(@MAPSIZE)
        for i in (0..@shipLength-1) do
            if direction == 0
                @posX[i] = shortPos + i
                @posY[i] = longPos
            else
                @posX[i] = longPos
                @posY[i] = shortPos + i
            end
        end
    end
    def hasCollision(shipList)
        if shipList
            for savedShip in shipList do
                for savedShipPos in (0..savedShip.shipLength-1) do
                    for thisShipPos in (0..@shipLength-1) do
                        if (savedShip.posX[savedShipPos] == posX[thisShipPos]) && (savedShip.posY[savedShipPos] == posY[thisShipPos])
                            return true
                        end
                    end
                end
            end
        end
        false
    end
    def shipLength
        @shipLength
    end
    def posX
        @posX
    end
    def posY
        @posY
    end
end # GameShip End

class GameRoomObserver
    def initialize(roomNumber, server)
        @shipNum
        @shipLength
        @playerTurn
        @shipList
        @playersInRoom
        @clientsInRoom = Array::new
        @roomNumber = roomNumber
        @server = server
    end
    def configGameModel(client)
        ret = true
        case @clientsInRoom.length
        when 2; @shipNum = 1; @shipLength = 1
        when 3; @shipNum = 3; @shipLength = 3
        when 4; @shipNum = 2; @shipLength = 3
        when 5; @shipNum = 2; @shipLength = 3
        when 6; @shipNum = 2; @shipLength = 2
        end
        ret
    end

    def generateGameModel
        @playerTurn = 0
        @playersInRoom = Array::new
        @shipList = Array::new
        for client in @clientsInRoom do
            shipCount = 0
            newPlayer = GamePlayer.new(client, @shipNum, @shipLength)
            while shipCount < @shipNum
                newShip = GameShip.new(@shipLength)
                if newShip.hasCollision(@shipList) == false
                    @shipList.push(newShip)
                    newPlayer.shipList.push(newShip)
                    shipCount += 1
                end
            end
            newPlayer.setMapModel('[ME]')
            @playersInRoom.push(newPlayer)
        end
        playerName = @playersInRoom[@playerTurn].client.clientName
        messageToRoom("Game Start!")
        messageToRoom("#{playerName} turn")
    end

    def initGameView
        for player in @playersInRoom do        
            for row in (0..6) do
                for col in (0..6) do
                    pos = player.gameMap.getPosition(row,col)
                    # puts pos
                    begin
                        player.client.messageToClient("/init #{pos}")
                    rescue => exception
                        puts exception 
                    end                    
                end
            end
        end
    end
    def nextTurn
        beforeTurn = @playerTurn
        puts @playersInRoom[@playerTurn].life
        nextPlayer
        while 1
            if @playersInRoom[@playerTurn].life == 0
                nextPlayer
            elsif @playersInRoom[@playerTurn].client == nil
                nextPlayer
            else
                break
            end
        end
        playerName = @playersInRoom[@playerTurn].client.clientName
        afterTurn = @playerTurn
        if beforeTurn == afterTurn
            messageToRoom("#{playerName} is a winner!")
            resetGame
        else
            messageToRoom("#{playerName} turn!")
        end
    end

    def nextPlayer
        if @playerTurn == @playersInRoom.length-1
            @playerTurn = 0
        else 
            @playerTurn += 1
        end
    end

    def resetGame
        @roomNumber = nil
        @shipNum = nil
        @shipLength = nil
        @playerTurn = nil
        @shipList = nil
        @playersInRoom = nil
        messageToRoom("/resetgame ok")
        for client in @clientsInRoom do
            if client
                client.isReady = false
            end
        end
    end
    def messageToRoom(message)
        if @playersInRoom
            for player in @playersInRoom do
                if player.client
                    begin
                        player.client.messageToClient(message)  
                    rescue => exception
                        puts exception
                    end
                end
            end
        else
            for client in @clientsInRoom do
                begin
                    client.messageToClient(message)
                rescue => exception
                    puts exception
                end
            end
        end
    end

    def fireMessageToPlayers(row, col)
        flag = '[xx]'
        for player in @playersInRoom do
            if player.client
                if player.gameMap.getPosition(row, col) == '[ME]'
                    player.loseLife
                    flag = '[oo]'
                end
                player.gameMap.setPosition(row, col, flag)
            end
        end
        attackPosision = (row*7+col).to_s.rjust(2,'0')
        begin
            messageToRoom("/attack #{attackPosision} #{flag}")
        rescue => exception
            puts exception
        end
    end

    def removeClient(client)
        @clientsInRoom.delete(client)
        if @playersInRoom
            for player in @playerInRoom do
                if player.client == client
                    suicidePlayer(player)
                    player.cleint = nil
                end
            end
        end
    end

    def suicidePlayer(thisTurnPlayer)
        case thisTurnPlayer.suicideCount
        when 0; thisTurnPlayer.client.messageToClient("It is your ship. Select other place.")
        when 1; thisTurnPlayer.client.messageToClient("Do you want to give up?")
        when 2
            thisTurnPlayer.client.messageToClient("You gave up this game. Your ships will be sink.")
            destroyPlayer(thisTurnPlayer)
            nextTurn
        end
        thisTurnPlayer.suicideCount += 1
    end

    def destroyPlayer(thisTurnPlayer)
        for row in (0..6) do
            for col in (0..6) do
                if thisTurnPlayer.gameMap.getPosition(row, col) == '[ME]'
                    fireMessageToPlayers(row, col)
                end
            end                    
        end
    end

    def playerTurn
        @playerTurn
    end
    def playersInRoom
        @playersInRoom
    end
    def clientsInRoom
        @clientsInRoom
    end
    def roomNumber
        @roomNumber
    end
    def roomNumber=(room)
        @roomNumber = room
    end
end

class GamePlayer
    def initialize(client, shipNum, shipLength)
        @suicideCount = 0
        @client = client
        @shipList = Array::new
        @gameMap = GameMap.new
        @shipNum = shipNum
        @shipLength = shipLength
        @life = @shipLength * @shipNum
    end

    def setMapModel(flag)
        for ship in @shipList do
            for pos in (0..@shipLength-1) do
                @gameMap.setPosition(ship.posX[pos], ship.posY[pos], flag)
            end
        end
    end

    def gameMap
        @gameMap
    end
    def shipList
        @shipList
    end
    def shipNum
        @shipNum
    end
    def life
        @life
    end
    def loseLife
        @life -= 1
    end
    def client
        @client
    end
    def suicideCount
        @suicideCount
    end
    def suicideCount=(count)
        @suicideCount = count
    end
end

myGameServer = GameServer.new(5000)
myGameServer.run