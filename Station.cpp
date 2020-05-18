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
#include <fstream>
#include <map>
#include <time.h>

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
// int otherStationDatagrams[];
string latitude;
string longitude;

// Timetable Variables
vector<string> timetableDepatureTime;
vector<string> timetableLine;
vector<string> timetablePlatform;
vector<string> timetableArrivalTime;
vector<string> timetableDestinations;
//int timetablePorts[];

// Other Variables
// Selector selector;
string httpHeader = "HTTP/1.1 200 OK\r\n";
string contentType = "Content-Type: text/html\r\n";
string datagramClientMessage;
bool hasReceivedOtherStationNames = false;
vector<int> otherStationPorts;
bool alreadyWritten = false;

// ###############################################################################

vector<string> explode(const string &str, const char &ch)
{
    string next;
    vector<string> result;

    // For each character in the string
    for (string::const_iterator it = str.begin(); it != str.end(); it++)
    {
        // If we've hit the terminal character
        if (*it == ch)
        {
            // If we have some characters accumulated
            if (!next.empty())
            {
                // Add them to the result vector
                result.push_back(next);
                next.clear();
            }
        }
        else
        {
            // Accumulate the next character into the sequence
            next += *it;
        }
    }
    if (!next.empty())
        result.push_back(next);
    return result;
}

void readTimetableIn()
{
    string chosenStation = "tt-" + currentStation;
    string line;
    ifstream timetable;
    timetable.open("google_transit/" + chosenStation);

    if (!timetable)
    {
        cerr << "Unable to open timetable file";
        exit(1);
    }

    bool firstLineRead = false;

    while (getline(timetable, line))
    {
        vector<string> result = explode(line, ',');
        if (!firstLineRead)
        {
            latitude = result[1];
            longitude = result[2];
            firstLineRead = true;
        }
        else
        {
            result = explode(line, ',');
            timetableDepatureTime.push_back(result.at(0));
            timetableLine.push_back(result.at(1));
            timetablePlatform.push_back(result.at(2));
            timetableArrivalTime.push_back(result.at(3));
            timetableDestinations.push_back(result.at(4));
        }
    }
    // JAVA timetablePorts initalised here
}
/*
void addPortsToTimetable(map<string, int> ports)
{
    for (int i = 0; i < timetableDestinations.size(); i++)
    {
        string currentKey = timetableDestinations.at(i);
        if (ports.find(currentKey))
        {
            timetablePorts[i] = ports.at(currentKey);
        }
    }
}
*/

void receiveOtherStationNames(string message)
{
    // TODO
}

void removePortIfCovered(string message)
{
    vector<string> result = explode(message, '\n');
    int portInReference = stoi(result.at(2));
    for (int i = 0; i < otherStationPorts.size(); i++)
    {
        if (otherStationPorts.at(i) == portInReference)
        {

            // otherStationPorts.erase(i);
            cout << otherStationPorts.at(i);
            break;
        }
    }
}

void sendOtherStationNames()
{
    // TODO
}

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

void separateUserInputs(string body)
{
    vector<string> result = explode(body, '(?!^)');
    int startIndex = 0;
    int endIndex = 0;
    for (int i = 1; i < result.size(); i++)
    {
        if (result.at(i).find("=") && result.at(i - 1).find("o"))
        {
            startIndex = i + 1;
        }
        if (result.at(i).find("H") && result.at(i).find(" "))
        {
            endIndex = i - 1;
        }
    }
    requiredDestination = body.substr(startIndex, endIndex);
    trim(requiredDestination);
}

/*
void addCurrentStationToDatagram(vector<string> path, vector<string> departureTimes, vector<string> arrivalTimes, int portNumber)
{
    time_t now;

    path.push_back(currentStation);
    int index = 0;
    for (int i = 0; i < timetableDepatureTime.size(); i++)
    {
        string timetableString = timetableDepatureTime.at(i);
    }
}
*/

string determineOriginDepartureTime(int port)
{
    // TODO
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
                         vector<string> arrivalTimes, int lastNodePort, bool hasReachedFinalDestination, string homeStation)
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
    result += requiredDestination + " " + originDepartureTime + " " + to_string(numberStationsStoppedAt) + " " + to_string(lastNodePort) + " " + to_string(hasReachedFinalDestination) + " " + homeStation + " \n";
    for (int i = 0; i < path.size(); i++)
    {
        result += path.at(i) + " " + departureTimes.at(i) + " " + arrivalTimes.at(i) + " \n";
    }
    return result;
}

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

// ###############################################################################

int main(int argCount, const char *args[])
{
    currentStation = args[1]; // Something wrong on this line with the pointer
    webPort = atoi(args[2]);
    receivingDatagram = atoi(args[3]);

    int otherStationDatagrams[argCount - 5];

    int otherIndex = 4;

    for (int i = 0; i <= argCount - 5; i++)
    {
        otherStationDatagrams[i] = atoi(args[otherIndex]);
        otherIndex++;
    }

    string message = "#\ncottesloe-stn\n4005";
    removePortIfCovered(message);
    readTimetableIn();

    //cout << (constructDatagram(true, "Subiaco-Stn", "9:00", 10, path, departureTimes, arrivalTimes));
    return 0;
}

// Protocol

// Outgoing/Incoming (whether it has reached final destination yet or not)
// Destination, origin depature time, number of stations stopped at
// Station stopped at, Departure Time, Arrival Time
// Station stopped at, Departure Time, Arrival Time
// Station stopped at, Departure Time, Arrival Time