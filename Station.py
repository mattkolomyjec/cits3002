from socket import *
from select import select
import sys 
import re
import time
from datetime import datetime
import argparse

# Message Variables
requiredDestination = None
isOutgoing = None
originDepartureTime = None
numberStationsStoppedAt = None
path = []
departureTimes = []
arrivalTimes = []
lastNodePort = None
hasReachedFinalStation = None
homeStation = None

# Station Variables
currentStation = None
receivingDatagram = None
webPort = None
otherStationDatagrams = []
latitude = None
longitude = None

# Other Variables
datagramClientMessage = None
hasReceievedOtherStationNames = False
alreadyWritten = False
timetablePorts = []
hasReturnedHome = False


"""A method to read the Transperth timetable data in
:param currentStation: the current station
"""
def readTimetableIn(currentStation):
    chosenStation = "google_transit/tt-" + currentStation
    f = open(chosenStation, "r")
    input = f.readline().split(',')
    global latitude
    latitude = input[1]
    global longitude 
    longitude = input[2]

    global timetableDepartureTime
    timetableDepartureTime = []
    global timetableLine
    timetableLine = []
    global timetablePlatform
    timetablePlatform = []
    global timetableArrivalTime
    timetableArrivalTime = []
    global timetableDestinations
    timetableDestinations = []

    input = f.readline().split(',')
    while(not "" in input):
        timetableDepartureTime.append(input[0])
        timetableLine.append(input[1])
        timetablePlatform.append(input[2])
        timetableArrivalTime.append(input[3])
        timetableDestinations.append(input[4])
        input = f.readline().split(',')

"""A method to add the receieving port numbers to their corresponding station in
    the transperth timetable data
:param ports: a map with the adjacent station as a key and their recieving port number
"""
def addPortsToTimetable(ports):
    index = 0
    print(len(timetableDestinations))
    for i in timetableDestinations:
        currentKey = timetableDestinations[index]
        currentKey = currentKey.rstrip("\n")
        if(currentKey in ports.keys()):
            timetablePorts.insert(index, ports[currentKey])
        index += 1
         
    print(timetablePorts) 
    print(len(timetablePorts))
    print(timetablePorts[1])

"""A method to receive the names and ports of the adjacent stations
:param message: the message sent from the adjacent station
"""
def receiveOtherStationNames(message):
    temp = message.split('\n')
    map = {temp[1]: temp[2]}
    addPortsToTimetable(map)

"""A method to remove a port from the required ports if it has not been covered yet
:param message: the message sent from the adjacent station
"""
def removePortIfCovered(message):
    temp = message.split('\n')
    portInReference = int(temp[2])
    global otherStationPorts
    otherStationPorts = []
    for i in otherStationPorts:
        if(otherStationPorts[i] == portInReference):
            otherStationPorts.remove(i)
            break

"""A method to send adjacent stations the station name and port number of this station
"""
def sendOtherStationNames():
    message = "#" + "\n" + currentStation + "\n" + str(receivingDatagram)
    for i in otherStationDatagrams:
        writeUDP(message, i)

"""A method to determine if the required station of a given datagram is equal to the current station
:return result: whether it is the final station or not
"""
def isFinalStation():
    result = False
    current = currentStation.strip()
    required = requiredDestination.strip()
    if(current in required):
        result = True
    return result

"""A method to split a string into individual characters. Courtesy of:
https://www.geeksforgeeks.org/python-split-string-into-list-of-characters/
:return result: the split up word
"""
def split(word): 
    return [char for char in word]  

"""A method to split the GET request and extract the required destination
:return requiredDestination: the required destination
"""
def separateUserInputs(body):
    temp = split(body)
    startIndex = 0
    endIndex = 0
    index = 0
    for i in temp:
        if("=" in i):
            startIndex = index + 1
        if(" " in i):
            endIndex = index - 1
        index += 1

    requiredDestination = body[startIndex:endIndex]
    requiredDestination.strip() # not a global variable?
    return requiredDestination
    
"""A method to add the current station to the datagram
"""
def addCurrentStationToDatagram(path, departureTimes, arrivalTimes, portNumber):
    now = datetime.now()
    #print now.hour, ":", now.minute
    path.append(currentStation)
    i = 0
    index = 0
    for i in timetableDepartureTime:
        timetableString = i
        datetime_object = datetime.strptime(timetableString,'%I:%M')
        if(arrivalTimes.size() > 0):
            if(datetime_object > now and timetablePorts[i] == portNumber):
                index = i
                break
        elif(arrivalTimes.size() > 0):
            lastArrivalTime = arrivalTimes[arrivalTimes.size()-1]
            nextTime = datetime.strptime(lastArrivalTime,'%H:%M')
            if(datetime_object > nextTime and timetablePorts[i] == portNumber):
                index = i
                break
        i += 1
    departureTimes.append(timetableDepartureTime[index])
    arrivalTimes.append(timetableArrivalTime[index])

"""A method to determine the origin departure time
:param port: the port number in reference
:return originDepartureTime: the origin departure time
"""
def determineOriginDepartureTime(port):
    now = datetime.now()
    index = 0
    k = 0
    for i in timetableDepartureTime:
        timetableString = i
        nextTime = datetime.strptime(timetableString,'%H:%M')
        if(nextTime > now and timetablePorts[k] == port):
            index = k
            break
        k += 1
    originDepartureTime = timetableDepartureTime[index]
    return originDepartureTime

"""A method to determine the origin departure time
:param isOutgoing: whether the datagram is outgoing or incoming
:param requiredDestination: the required destination of the datagram
:param originDepartureTime: the origin departure time of the datagram
:param numberStationsStoppedAt: the number of stations the datagram has stopped at
:param path: the path the datagram travlled along
:param departureTimes: the times of departure of the datagram along the path
:param arrivalTimes: the times of arrival of the datagram along the path
:param lastNodePort: the receiving port of the last station passed through
:param hasReachedFinalStation: whether the datagram has reached the its final station
:param: homeStation: the home station of the datagram
:return result: the constructed datagram
"""
def constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation):
    if(isOutgoing):
        result = "Outgoing \n"
    else:
        result = "Incoming \n"
    
    result += (requiredDestination + " " + str(originDepartureTime) + " " + str(numberStationsStoppedAt) + " " + str(lastNodePort)
    + " " + str(hasReachedFinalStation) + " " + str(homeStation) + " \n")
    
    index = 0
    for i in path:
        result += i + " " + departureTimes[index] + " " + arrivalTimes[index] + " \n"
    return result

"""A method to reset the message variables
"""
def reset():
    isOutgoing = True
    requiredDestination = ""
    originDepartureTime = ""
    numberStationsStoppedAt = 0
    del path[:]
    del departureTimes[:]
    del arrivalTimes[:]
    lastNodePort = 0
    hasReachedFinalStation = False
    homeStation = ""

"""A method to check the incoming datagram does not have a null variable
:param message: the incoming datagram to be tested
:return result: whether the datagram has null in it or not
"""
def datagramHasNull(message):
    result = False
    temp = message.split(' ')
    for i in temp:
        if("null" in i):
            result = True
            break
    return result

"""A method to read an incoming datagram in
:param message: the incoming datagram to be tested
"""
def readDatagramIn(message):
    reset()
    print(message)
    global isOutgoing
    temp = message.split(" ")
    if("Outgoing" in temp[0]):
        isOutgoing = True
    else:
        isOutgoing = False
    global requiredDestination
    requiredDestination = temp[1]
    global originDepartureTime 
    originDepartureTime = temp[2]

    trimString = temp[3]
    trimString.strip()
    global numberStationsStoppedAt 
    numberStationsStoppedAt = int(trimString)

    trimString = ""

    trimString = temp[4]
    trimString.strip()
    global lastNodePort 
    lastNodePort = int(trimString)

    trimString = ""

    trimString = temp[5]
    trimString.strip()
    global hasReachedFinalStation
    if("true" in trimString or "True" in trimString):
        hasReachedFinalStation = True
    else:
        hasReachedFinalStation = False
    
    trimString = ""

    trimString = temp[6]
    trimString.strip()
    global homeStation 
    homeStation = trimString

    pathIndex = 7
    departureIndex = 8
    arrivalIndex = 9
    i = 0
    global path
    path = []
    global departureTimes
    departureTimes = []
    global arrivalTimes
    arrivalTimes = []
    while(i <= numberStationsStoppedAt-1):
        path.append(temp[pathIndex])
        pathIndex += 3
        departureTimes.append(temp[departureIndex])
        departureIndex += 3
        arrivalTimes.append(temp[arrivalIndex])
        arrivalIndex += 3
        i += 1

"""A method to determine the next destination of a given datagram
"""
def datagramChecks():
    if(isFinalStation() and isOutgoing and hasReachedFinalStation == False):
        isOutgoing = False
        hasReachedFinalStation = True
        numberStationsStoppedAt += 1
        lastNodePort = receivingDatagram

        for i in otherStationDatagrams:
            addCurrentStationToDatagram(path, departureTimes, arrivalTimes, i)
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation)
            writeUDP(message, i)
    elif(isFinalStation() == False and isOutgoing and hasReachedFinalStation == False):
        numberStationsStoppedAt += 1
        oldPort = lastNodePort
        for i in otherStationDatagrams:
            if(i == oldPort):
                continue
            else:
                addCurrentStationToDatagram(path, departureTimes, arrivalTimes, i)
                message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation)
                writeUDP(message, i)
    elif(isFinalStation() == False and isOutgoing == False and hasReachedFinalStation):
        oldPort = lastNodePort
        lastNodePort = receivingDatagram
        message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation)
        for i in otherStationDatagrams:
            if(i == oldPort):
                continue
            else:
                writeUDP(message, i)

"""A method to read from TCP port
:param s: the socket in reference via select()
:param webPort: the port to read from
"""
def readTCP(s, webPort):
        client,addr = s.accept()
        data = client.recv(webPort)
        for line in data.splitlines():
            requiredDestination = separateUserInputs(line)
            break
        print(requiredDestination)

        lastNodePort = receivingDatagram
        isOutgoing = True
        homeStation = currentStation
        numberStationsStoppedAt = 0
        hasReachedFinalStation = False

        for i in otherStationDatagrams:
            originDepartureTime = determineOriginDepartureTime(i)
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation)
            print(message)
            writeUDP(message, i)

"""A method to check the read an incoming UDP connection
:param s: the socket in reference via select()
:param receivingDatagram: the port to read from
"""
def readUDP(s, receivingDatagram):
        data,addr = s.recvfrom(receivingDatagram) 
        if(data.startswith('#')):
            removePortIfCovered(data)
            if(len(otherStationPorts) == 0):
                receiveOtherStationNames(data)
                hasReceievedOtherStationNames = True
            else:
                receiveOtherStationNames(data)
        else:
            if(datagramHasNull(data) == False): #and hasReceievedOtherStationNames == True): 
                readDatagramIn(data)
            if(hasReachedFinalStation and isOutgoing == False and homeStation in currentStation):
                global hasReturnedHome
                hasReturnedHome = True
            else:
                datagramChecks()

"""A method to write an outbound datagram
:param message: the message to send
:param port: the port to send to
"""
def writeUDP(message, port):
    serverAddressPort   = ("127.0.0.1", port)
    udp.sendto(message, serverAddressPort)

"""A method to run the core Select() server
:param webPort: the port to post/read HTML from
:param receivingDatagram: the port the program will receive other datagrams from
:param otherDatagrams: the receiving UDP ports of adjacent stations
"""  
def run(webPort, receivingDatagram, otherDatagrams):
    host = ''
    size = 8000
    backlog = 5

    global tcp
    tcp = socket(AF_INET, SOCK_STREAM)
    tcp.bind(('',webPort))
    tcp.listen(backlog)

    global udp
    udp = socket(AF_INET, SOCK_DGRAM)
    udp.bind(('',receivingDatagram))

    input = [tcp,udp]
    
    print("Reading data from adjacent ports!")
   
    while(hasReceievedOtherStationNames == False):
        sendOtherStationNames()

        inputready,outputready,exceptready = select(input,[],[])

        for s in inputready:
            if s == udp:
                readUDP(s, receivingDatagram)
    
    
    
    print("Server started on port >> " + str(webPort))

    conn, addr = tcp.accept()
    conn.send('HTTP/1.0 200 OK\n')
    conn.send('Content-Type: text/html\n')
    conn.send('\n') 
    conn.send("""
            <form method='GET'>
            <input name='to' type='text'/>
            <input type='submit'/>
            </form>
            """)
    conn.recv(1000)
    conn.close()
    

    while True:
        while (hasReturnedHome == False):
            print("THIS IS")
            print(hasReturnedHome)
            inputready,outputready,exceptready = select(input,[],[])

            for s in inputready:
                if s == tcp:
                    readTCP(s, webPort)
                        #write_tcp(s)
                elif s == udp:
                    readUDP(s, receivingDatagram)
                else:
                    print "unknown socket:", s
        
        if(hasReturnedHome == True):
                tcp = socket(AF_INET, SOCK_STREAM)
                tcp.bind(('',webPort))
                tcp.listen(backlog)
                
                conn, addr = tcp.accept()
                print("ELFINITO")
                print(conn)
                conn.send('HTTP/1.0 200 OK\n')
                conn.send('Content-Type: text/html\n')
                conn.send('\n') 
                conn.send("""
                        <form method='GET'>
                        <input name='to' type='text'/>
                        <input type='submit'/>
                        </form>
                        """)
                conn.send("""
                        <br>
                        <br> <strong> Destination:</strong> """)
                conn.send('{requiredDestination}')
                conn.send("""
                        <br> <strong> Origin:</strong> """)
                conn.send('{currentStation}')
                conn.send("""
                        <div> <br> <strong> Route </strong> (Departing Time | Stop | Arrival Time) </div>\n """)
                index = 0
                for i in path:
                    conn.send('<br>')
                    conn.send('Departing at: <strong>')
                    conn.send('{departureTimes[index]}')
                    conn.send('</strong>; To: ')
                    conn.send('<strong>')
                    conn.send('{i}')
                    conn.send('</strong>; Arriving at:  <strong>')
                    conn.send('{arrivalTimes[index]}')
                    conn.send('</strong>;')
                index += 1
                conn.send('<br>')
                conn.send('________________________________________')
                conn.close()
                conn.recv(1000)
                conn.close()
                print("MADE IT THIS FAR")
                
"""The main method
"""
def main():
    # Message Variables
    requiredDestination
    isOutgoing
    originDepartureTime
    numberStationsStoppedAt
    path = []
    departureTimes = []
    arrivalTimes = []
    lastNodePort
    hasReachedFinalStation

    # Station Variables
    global currentStation 
    global receivingDatagram
    global webPort
    global otherStationDatagrams
    global latitude
    global longitude

    # Other Variables
    datagramClientMessage
    hasReceievedOtherStationNames = False
    alreadyWritten = False
    timetablePorts = []

    currentStation = sys.argv[1]
    webPort = int(sys.argv[2])
    receivingDatagram = int(sys.argv[3])
    
    otherStationDatagrams = [] 
    index = 4
    while(len(sys.argv)-1 >= index):
        otherStationDatagrams.append(int(sys.argv[index]))
        index += 1

    otherStationPorts = otherStationDatagrams
    
    readTimetableIn(currentStation)
    run(webPort,  receivingDatagram, otherStationDatagrams)

if __name__ == '__main__':
    main()