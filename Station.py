from socket import *
#import socket
from select import select
import sys 
import re
import time
from datetime import datetime
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

## Verified
def addPortsToTimetable(ports):
    index = 0
    for i in timetableDestinations:
        currentKey = timetableDestinations[index]
        currentKey = currentKey.rstrip("\n")
        if(currentKey in ports.keys()):
            timetablePorts.insert(index, ports[currentKey])
            index += 1 
        else:
            #timetablePorts.insert(index, 0)
            index += 1

## Verified
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
    message = "#" + "\n" + currentStation + "\n" + str(receivingDatagram)
    for i in otherStationDatagrams:
        writeUDP(message, i)

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
    print(requiredDestination)

def addCurrentStationToDatagram(path, departureTimes, arrivalTimes, portNumber):
    now = datetime.now()
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

def determineOriginDepartureTime(port):
    now = datetime.now()
  
    #datetime_object = datetime.strptime(now,'%I:%M')
    print(now)
    index = 0
    k = 0
    for i in timetableDepartureTime:
        timetableString = i
        nextTime = datetime.strptime(timetableString,'%I:%M')
        print(nextTime > now)
        if(nextTime > now and timetablePorts[k] == port):
            index = k
            break
        k += 1
    originDepartureTime = timetableDepartureTime[index]
    return originDepartureTime

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

def readTCP(s, webPort):
        client,addr = s.accept()
        data = client.recv(webPort)
        client.close()
        for line in data.splitlines():
            print(line)
            separateUserInputs(line)
            break
        # Flood to all ports
        lastNodePort = receivingDatagram
        isOutgoing = True
        homeStation = currentStation

        for i in otherStationDatagrams:
            originDepartureTime = determineOriginDepartureTime(i)
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation)
            writeUDP(message, i)

def readUDP(s, receivingDatagram):
        data,addr = s.recvfrom(receivingDatagram)
        #print "Recv UDP:'%s'" % data
        if(data.startswith('#')):
            removePortIfCovered(data)
            if(len(otherStationPorts) == 0):
                receiveOtherStationNames(data)
                hasReceievedOtherStationNames = True
            else:
                receiveOtherStationNames(data)
        else:
            if(datagramHasNull(data) == False and hasReceievedOtherStationNames == True):
                readDatagramIn(data)
            if(hasReachedFinalStation and isOutgoing == False and homeStation in currentStation):
                ## REGISTER CHANNEL WRITING?
                writeTCP(s)
            else:
                datagramChecks()


def writeUDP(message, port):
    serverAddressPort   = ("127.0.0.1", port)
    udp.sendto(message, serverAddressPort)
    #localIP     = "127.0.0.1"
    #UDPServerSocket = socket.socket(family=socket.AF_INET, type=socket.SOCK_DGRAM)
    

# Method to send the result
def writeTCP(s):
    client,addr = s.accept()
    client.send('HTTP/1.0 200 OK\n')
    client.send('Content-Type: text/html\n')
    client.send('\n')
    client.send('<br>')
    client.send('<br> <strong> Destination:</strong> ')
    client.send(requiredDestination)
    client.send('<br> <strong> Origin:</strong> ')
    client.send(currentStation)
    client.send('<div> <br> <strong> Route </strong> (Departing Time | Stop | Arrival Time) </div>\n')
    index = 0
    for i in path:
        client.send('<br>')
        cleint.send('Departing at: <strong>')
        client.send(departureTimes[index])
        client.send('</strong>; To: ')
        client.send('<strong>')
        client.send(i)
        client.send('</strong>; Arriving at:  <strong>')
        client.send(arrivalTimes[index])
        client.send('</strong>;')
        index += 1
    client.send('<br>')
    client.send('________________________________________')
    client.send('<br>')
    
def run(webPort, receivingDatagram, otherDatagrams):
    host = ''
    #port = 4002
    size = 8000
    backlog = 5

    # create tcp socket
    tcp = socket(AF_INET, SOCK_STREAM)
    tcp.bind(('',webPort))
    tcp.listen(backlog)

    # create udp socket
    global udp
    udp = socket(AF_INET, SOCK_DGRAM)
    udp.bind(('',receivingDatagram))

    input = [tcp,udp]

    print("Reading data from adjacent ports!")

    # Send station names until receieved
    while(hasReceievedOtherStationNames == False):
        sendOtherStationNames()

        inputready,outputready,exceptready = select(input,[],[])

        for s in inputready:
            if s == udp:
                readUDP(s, receivingDatagram)

    print("Server started on port >> " + webPort)

    # send html data
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
    #conn.close()

    while True:
        inputready,outputready,exceptready = select(input,[],[])

        for s in inputready:
            if s == tcp:
                readTCP(s, webPort)
                #write_tcp(s)
 
            elif s == udp:
                readUDP(s)
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
    global timetablePorts
    timetablePorts = []

    
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
    
    readTimetableIn()
    run(webPort,  receivingDatagram, otherStationDatagrams)

if __name__ == '__main__':
    main()