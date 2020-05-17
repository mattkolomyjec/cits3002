#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <string>
#include <string.h>
#include <strings.h>
#include <iostream>
#include <vector>
#include <array>

using namespace std;

// Message Variables
string requiredDestination;
bool isOutgoing;
string originDepartureTime;
int numberStationsStoppedAt;
vector<string> path;
vector<string> departureTimes;
vector<string> arrivalTimes;
int lastNodePort;
bool hasReachedFinalStation;
string homeStation;

// Station  Variables
string currentStation;
int receivingDatagram;
int webPort;
vector<int> otherStationDatagrams;
string latitude;
string longitude;

// Timetable Variables
vector<string> timetableDepatureTime;
vector<string> timetableLine;
vector<string> timetablePlatform;
vector<string> timetableArrivalTime;
vector<string> timetableDestinations;
int timetablePorts[];

// Other Variables
// Selector selector;
string httpHeader = "HTTP/1.1 200 OK\r\n";
string contentType = "Content-Type: text/html\r\n";
string datagramClientMessage;
bool hasReceivedOtherStationNames = false;
int receievedOtherStationNamesCount = 0;
int receievedOtherStationPortsUnique[];

// ###############################################################################

/***
     * A method to read the Transperth timetable data in
     * 
     * @throws FileNotFoundException if the file cannot be located
     */
void readTimetableIn() throws FileNotFoundException
{
    string chosenStation = "tt-" + currentStation;
    File file = new File("google_transit/" + chosenStation);
    Scanner sc = new Scanner(file);

    string temp[] = ((sc.nextLine()).split(","));
    latitude = temp[1];
    longitude = temp[2];

    while (sc.hasNextLine())
    {
        string tempRoutes[] = ((sc.nextLine()).split(","));
        for (int i = 0; i < tempRoutes.length - 4; i += 4)
        {
            timetableDepartureTime.add(tempRoutes[i]);
            timetableLine.add(tempRoutes[i + 1]);
            timetablePlatform.add(tempRoutes[i + 2]);
            timetableArrivalTime.add(tempRoutes[i + 3]);
            timetableDestinations.add(tempRoutes[i + 4]);
        }
    }
    timetablePorts = new int[timetableDestinations.size()];
}

/**
 * Helper method to trim white space from a string
 */
string trim(const string &str)
{
    size_t first = str.find_first_not_of(' ');
    if (string::npos == first)
    {
        return str;
    }
    size_t last = str.find_last_not_of(' ');
    return str.substr(first, (last - first + 1));
}

/**
     * A method to check whether the current station is the final destination
     * 
     * @return boolean indicating whether the current station is the final station
     */
bool isFinalStation()
{
    bool result = false;
    string c = trim(currentStation);
    string r = trim(requiredDestination);
    if (c.find(r))
    {
        result = true;
    }
    return result;
}

/***
     * A method to extract the required destination from the body parsed in the
     * following format: origin={GIVEN}&destination={EXTRACT}
     * 
     * @param body
     */
/*
void separateUserInputs(string body)
{
    //string temp[] = body.split("(?!^)");
    cout << body[1];
    int endIndex = 0;
    for (int i = 0; i < body.length; i++)
    {
        if (body[i].find("="))
        {
            endIndex = i + 1;
        }
    }
    requiredDestination = body.substr(endIndex);
}
*/

// ###############################################################################
/**
     * A method to add the current station to the datagram before sending it onto
     * the next node
     * 
     * @param path
     * @param departureTimes
     * @param arrivalTimes
     */
void addCurrentStationToDatagram(vector<string> path, vector<string> departureTimes,
                                 vector<string> arrivalTimes)
{
    path.insert(path.end(), currentStation);
    departureTimes.insert(path.end(), "TEST");
    arrivalTimes.insert(path.end(), "TEST");
}

/***
     * Constructs a datagram to send based on the protcol created. The stops and
     * arrivalTime ArrayLists are indexed the same.
     * 
     * @param isOutgoing
     * @param requiredDestination
     * @param originDepartureTime
     * @param stops
     * @param departureTimes
     * @return result a string ready to be sent
     */
string constructDatagram(bool isOutgoing, string requiredDestination, string originDepartureTime,
                         int numberStationsStoppedAt, vector<string> path, vector<string> departureTimes,
                         vector<string> arrivalTimes)
{
    string result;
    if (isOutgoing)
    {
        result = "Outgoing \n";
    }
    else
    {
        result = "Incoming \n";
    }
    result += requiredDestination + " " + originDepartureTime + " " + to_string(numberStationsStoppedAt) + " \n";
    for (int i = 0; i < path.size(); i++)
    {
        result += path.at(i) + " " + departureTimes.at(i) + " " + arrivalTimes.at(i) + " \n";
    }
    return result;
}

/***
     * A method to reset the variables that hold the current message in hand
     */
void reset()
{
    isOutgoing = false;
    requiredDestination = "";
    originDepartureTime = "";
    numberStationsStoppedAt = 0;
    path.clear();
    departureTimes.clear();
    arrivalTimes.clear();
}

/***
     * A method to read a datagram sent from another node in.
     * 
     * @param message the datagram sent in
     */
/*
void readDatagramIn(string message)
{
    reset();
    string temp[] = message.split(" ");
    if (temp[0].contains("Outgoing"))
    {
        isOutgoing = true;
    }
    else
    {
        isOutgoing = false;
    }
    requiredDestination = temp[1];
    originDepartureTime = temp[2];
    numberStationsStoppedAt = atoi(temp[3]);

    int pathIndex = 4;
    int departureIndex = 5;
    int arrivalIndex = 6;
    for (int i = 0; i < numberStationsStoppedAt; i++)
    {
        path.insert(path.end(), temp[pathIndex]);
        pathIndex += 3;
        departureTimes.insert(departureTimes.end(), temp[departureIndex]);
        departureIndex += 3;
        arrivalTimes.insert(arrivalTimes.end(), temp[arrivalIndex]);
        arrivalIndex += 3;
    }
}
*/
/**
     * A method to calculate the total journey time (in minutes) given departure and
     * arrival times
     * 
     * @param departureTime
     * @param arrivalTime
     * @return
     */

int calculateJourneyTime(vector<string> departureTime, vector<string> arrivalTime)
{
    return 0;
}

// ###############################################################################

int main(int argCount, const char *args[])
{
    string origin = args[0]; // Something wrong on this line with the pointer
    int webPort = atoi(args[1]);
    int stationDatagrams = atoi(args[2]);

    int otherStationDatagrams[argCount - 3];
    int otherIndex = 3;
    for (int i = 0; i < argCount; i++)
    {
        otherStationDatagrams[i] = atoi(args[otherIndex]);
        otherIndex++;
    }

    cout << (constructDatagram(true, "Subiaco-Stn", "9:00", 10, path, departureTimes, arrivalTimes));
    return 0;
}

// Protocol

// Outgoing/Incoming (whether it has reached final destination yet or not)
// Destination, origin depature time, number of stations stopped at
// Station stopped at, Departure Time, Arrival Time
// Station stopped at, Departure Time, Arrival Time
// Station stopped at, Departure Time, Arrival Time