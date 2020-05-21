from socket import *
from select import select
import sys 
import re
import time, datetime
import argparse

# Verified
def readTimetableIn():
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

## Not Verified
def addPortsToTimetable(ports):
    for i in timetableDestinations:
        currentKey = timetableDestinations[i]
        if(currentKey in ports):
            global timetablePorts
            timetablePorts[i] = ports[currentKey]

## Not Verified
def receiveOtherStationNames(message):
    temp = message.split('\n')
    map = {temp[1]: temp[2]}
    addPortsToTimetable(map)

def removePortIfCovered(message):
    temp = message.split('\n')
    portInReference = int(temp[2])
    global otherStationPorts
    otherStationPorts = []
    for i in otherStationPorts:
        if(otherStationPorts[i] == portInReference):
            otherStationPorts.remove(i)
            break

def sendOtherStationNames():
    message = "#" + "\n" + currentStation + "\n" + receivingDatagram
    for i in otherStationDatagrams:
        writeUDP(message, otherStationDatagrams[i])

# Verified
def isFinalStation():
    result = False
    current = currentStation.strip()
    required = requiredDestination.strip()
    if(current in required):
        result = True
    return result

# Python3 program to Split string into characters https://www.geeksforgeeks.org/python-split-string-into-list-of-characters/
def split(word): 
    return [char for char in word]  

# Verified
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
    requiredDestination.strip()

def addCurrentStationToDatagram(path, departureTimes, arrivalTimes, portNumber):
    now = datetime.datetime.now()
    print now.hour, ":", now.minute
    path.append(currentStation)
    i = 0
    index = 0
    for i in timetableDepartureTime:
        timetableString = i
        datetime_object = datetime.strptime(timetableString,'%I:%M')
        print(datetime_object)
        if(arrivalTimes.size() > 0):
            if(datetime_object > now and timetablePorts[i] == portNumber):
                index = i
                break
        elif(arrivalTimes.size() > 0):
            lastArrivalTime = arrivalTimes[arrivalTimes.size()-1]
            nextTime = datetime.strptime(lastArrivalTime,'%I:%M')
            if(datetime_object > nextTime and timetablePorts[i] == portNumber):
                index = i
                break
        i += 1
    departureTimes.append(timetableDepartureTime[index])
    arrivalTimes.append(timetableArrivalTime[index])

# Verified
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

#Verified
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

def datagramHasNull(message):
    result = False
    temp = message.split(' ')
    for i in temp:
        if("null" in i):
            result = True
            break
    return result

#Verified
def readDatagramIn(message):
    reset()
    temp = message.split(" ")
    if("Outgoing" in temp[0]):
        isOutgoing = True
    else:
        isOutgoing = False
    requiredDestination = temp[1]
    originDepartureTime = temp[2]

    trimString = temp[3]
    trimString.strip()
    numberStationsStoppedAt = int(trimString)

    trimString = ""

    trimString = temp[4]
    trimString.strip()
    lastNodePort = int(trimString)

    trimString = ""

    trimString = temp[5]
    trimString.strip()
    if("true" in trimString):
        hasReachedFinalStation = True
    else:
        hasReachedFinalStation = False
    
    trimString = ""

    trimString = temp[6]
    trimString.strip()
    homeStation = trimString

    pathIndex = 7
    departureIndex = 8
    arrivalIndex = 9
    i = 0
    while(i <= numberStationsStoppedAt-1):
        path.append(temp[pathIndex])
        pathIndex += 3
        departureTimes.append(temp[departureIndex])
        departureIndex += 3
        arrivalTimes.append(temp[arrivalIndex])
        arrivalIndex += 3
        i += 1

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

def read_tcp(s):
        client,addr = s.accept()
        data = client.recv(4002)
        client.close()
        print "Recv TCP:'%s'" % data

def read_udp(s):
        data,addr = s.recvfrom(4003)
        print "Recv UDP:'%s'" % data

def write_tcp(s):
    #s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    client,addr = s.accept()
    #s.connect(('localhost', 50000))
    message = "HTTP/1.1 200 OK\r\n"
    message += "Content-length: %ld\r\n"
    message += "Content-Type: text/html\r\n"
    message += "\r\n"
    message += "%s"
    message += "<form method='GET'>"
    message += "<input name='to' type='text'/>"
    message += "<input type='submit'/>"
    message += "</form>"
    s.sendall(message)

def run(webPort, receivingDatagram, otherDatagrams):
    host = ''
    port = 4002
    size = 8000
    backlog = 5

    # create tcp socket
    tcp = socket(AF_INET, SOCK_STREAM)
    tcp.bind(('',webPort))
    tcp.listen(backlog)
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

    #tcp.connect(('127.0.0.1', webPort))
    # Send a request to the host
    #print(message)
    #tcp.send(message)

    # create udp socket
    udp = socket(AF_INET, SOCK_DGRAM)
    udp.bind(('',receivingDatagram))

    input = [tcp,udp]

    while True:
        inputready,outputready,exceptready = select(input,[],[])

        for s in inputready:
            if s == tcp:
                read_tcp(s)
                #write_tcp(s)
                # Create a socket
                

               
                
            elif s == udp:
                read_udp(s)
            else:
                print "unknown socket:", s

def main():
    # Message Variables
    global requiredDestination
    global isOutgoing
    global originDepartureTime
    global numberStationsStoppedAt
    global path
    path = []
    global departureTimes
    departureTimes = []
    global arrivalTimes
    arrivalTimes = []
    global lastNodePort
    global hasReachedFinalStation
    global homeStation

    # Station Variables
    global currentStation 
    global receivingDatagram
    global webPort
    # global otherStationDatagrams
    global latitude
    global longitude

    # Other Variables
    global datagramClientMessage
    global hasReceievedOtherStationNames 
    hasReceievedOtherStationNames = False
    global alreadyWritten
    alreadyWritten = False

    
    currentStation = sys.argv[1]
    webPort = int(sys.argv[2])
    receivingDatagram = int(sys.argv[3])
    
    global otherStationDatagrams
    otherStationDatagrams = [] 
    index = 4
    while(len(sys.argv)-1 >= index):
        otherStationDatagrams.append(int(sys.argv[index]))
        index += 1

    otherStationPorts = otherStationDatagrams


    path.append("BLAH")
    departureTimes.append("8:45")
    arrivalTimes.append("7:00")
    message = constructDatagram(True, "Warwick-Stn", "9:00", 1, path, departureTimes, arrivalTimes, 3004, False, "Thornlie-Stn")
    readDatagramIn(message)
    
    

    otherDatagrams = int(sys.argv[4])
    readTimetableIn()
    requiredDestination = "Cottesloe_Stn "
    separateUserInputs("GET /?to=Warwick-Stn HTTP/1.1")
    now = datetime.datetime.now()
    run(webPort,  receivingDatagram, otherDatagrams)

if __name__ == '__main__':
    main()