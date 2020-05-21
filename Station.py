from socket import *
from select import select
import sys 
import re
import time, datetime
import argparse

"""
def readTimetableIn():
    chosenStation = "google_transit/tt-" + currentStation
    f = open(chosenStation, "r")
    input = f.readline().split(',')
    global latitude
    latitude = input[1]
    global longitude 
    longitude = input[2]

    timetableDepartureTime = []
    timetableLine = []
    timetablePlatform = []
    timetableArrivalTime = []
    timetableDestinations = []

    input = f.readline().split(',')
    print(input)

    for i in input:
       global timetableDepartureTime.append(i)
        
        #print(timetableDepartureTime[0])
        continue
        timetableLine.append(i)
        print(timetableLine[0])

        timetablePlatform(c)
        timetableArrivalTime(d)
        timetableDestinations(e)
        #print(f.read())
"""
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
    otherStationPorts = []
    global otherStationPorts
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



def constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation):
    if(isOutgoing):
        result = "Outgoing \n"
    else:
        result = "Incoming \n"
    result += requiredDestination + " " + originDepartureTime + " " + numberStationsStoppedAt + " " + lastNodePort
    + " " + hasReachedFinalStation + " " + homeStation + " \n"
    
    index = 0
    for i in path:
        result += i + " " + departureTimes[index] + " " + arrivalTimes[index] + " \n"
    return result

def reset():
    isOutgoing = True
    requiredDestination = ""
    originDepartureTime = ""
    numberStationsStoppedAt = 0
    path.clear()
    departureTimes.clear()
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

    #### 398 in Java

def datagramChecks():
    if(isFinalStation() and isOutgoing and hasReachedFinalStation == False):
        isOutgoing = False
        hasReachedFinalStation = True
        numberStationsStoppedAt += 1
        lastNodePort = receivingDatagram

        for i in otherStationDatagrams:
            ## add current station to adatagram
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation)
            writeUDP(message, i)
    elif(isFinalStation() == False and isOutgoing and hasReachedFinalStation == False):
        numberStationsStoppedAt += 1
        oldPort = lastNodePort
        for i in otherStationDatagrams:
            if(i == oldPort):
                continue
            else:
                ## add current station to datagram
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

def run(webPort, receivingDatagram, otherDatagrams):
    host = ''
    port = 4002
    size = 8000
    backlog = 5

    # create tcp socket
    tcp = socket(AF_INET, SOCK_STREAM)
    tcp.bind(('',webPort))
    tcp.listen(backlog)

    # create udp socket
    udp = socket(AF_INET, SOCK_DGRAM)
    udp.bind(('',receivingDatagram))

    input = [tcp,udp]

    while True:
        inputready,outputready,exceptready = select(input,[],[])

        for s in inputready:
            if s == tcp:
                read_tcp(s)
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
    #global path = []
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

    # Timetable Variables
    ## CHECK

    # Other Variables
    global datagramClientMessage
    global hasReceievedOtherStationNames 
    hasReceievedOtherStationNames = False
    global alreadyWritten
    alreadyWritten = False

    
    currentStation = sys.argv[1]
    webPort = int(sys.argv[2])
    receivingDatagram = int(sys.argv[3])
   
    otherStationDatagrams = [] # HERE
    index = 4
    for i in otherStationDatagrams:
        otherStationDatagrams[i] = int(sys.argv[index])
    

    otherDatagrams = int(sys.argv[4])
    # readTimetableIn()
    requiredDestination = "Cottesloe_Stn "
    separateUserInputs("GET /?to=Warwick-Stn HTTP/1.1")
    run(webPort,  receivingDatagram, otherDatagrams)

if __name__ == '__main__':
    main()