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
#include <sys/select.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <unistd.h>
#include <sys/types.h>
#include <chrono>
#include <ctime>
#include <sys/socket.h>
#include <netdb.h>

#include <sys/socket.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <netdb.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <errno.h>
#include <string.h>
#include <errno.h>
#include <arpa/inet.h>

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <netdb.h>

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
vector<int> timetablePorts;

// Other Variables
// Selector selector;
string datagramClientMessage;
bool hasReceivedOtherStationNames = false;
vector<int> otherStationPorts;
bool alreadyWritten = false;

/**
 * A method to split a string into an array by a delimiter
 * */
vector<string> explode(const string &str, const char &ch)
{
    string next;
    vector<string> result;

    for (string::const_iterator it = str.begin(); it != str.end(); it++)
    {
        if (*it == ch)
        {
            if (!next.empty())
            {
                result.push_back(next);
                next.clear();
            }
        }
        else
        {
            next += *it;
        }
    }
    if (!next.empty())
        result.push_back(next);
    return result;
}
/***
     * A method to read the Transperth timetable data in
     * 
     * */
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

void addPortsToTimetable(map<string, int> ports)
{
    for (int i = 0; i < timetableDestinations.size(); i++)
    {
        string currentKey = timetableDestinations.at(i);
        if (ports.at(currentKey))
        {
            timetablePorts[i] = ports.at(currentKey);
        }
    }
}

void receiveOtherStationNames(string message)
{
    vector<string> result = explode(message, '\n');
    map<string, int> map;
    int temp = stoi(result.at(2));
    map.insert(pair<string, int>(result.at(1), temp));
    addPortsToTimetable(map);
}

void removePortIfCovered(string message)
{
    vector<string> result = explode(message, '\n');
    int portInReference = stoi(result.at(2));
    for (int i = 0; i < otherStationPorts.size(); i++)
    {
        if (otherStationPorts.at(i) == portInReference)
        {

            otherStationPorts.erase(otherStationPorts.begin() + i);
            break;
        }
    }
}

bool writeUDP(string message, int port)
{
    sockaddr_in servaddr;
    int fd = socket(AF_INET, SOCK_DGRAM, 0);
    if (fd < 0)
    {
        perror("cannot open socket");
        return false;
    }

    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
    servaddr.sin_port = htons(port);

    char cstr[message.size() + 1];
    strcpy(cstr, message.c_str());

    if (sendto(fd, cstr, strlen(cstr) + 1, 0, // +1 to include terminator
               (sockaddr *)&servaddr, sizeof(servaddr)) < 0)
    {
        perror("cannot send message");
        close(fd);
        return false;
    }
    close(fd);
    return true;
}
#define h_addr h_addr_list[0]
void accept()
{

    string ip = "127.0.0.1";
    const char *domain = ip.c_str();
    //char *path = strchr(domain, '/');
    //*path++ = '\0';
    //printf("host: %s; path: %s\n", domain, path);

    int sock, bytes_recieved;
    char send_data[1024], recv_data[9999];
    struct sockaddr_in server_addr;
    struct hostent *he;

    he = gethostbyname(domain);
    if (he == NULL)
    {
        herror("gethostbyname");
        exit(1);
    }

    if ((sock = socket(AF_INET, SOCK_STREAM, 0)) == -1)
    {
        perror("Socket");
        exit(1);
    }
    server_addr.sin_family = AF_INET;
    server_addr.sin_port = htons(webPort);
    server_addr.sin_addr = *((struct in_addr *)he->h_addr);
    bzero(&(server_addr.sin_zero), 8);
    if (connect(sock, (struct sockaddr *)&server_addr, sizeof(struct sockaddr)) == -1)
    {
        perror("Connect");
        exit(1);
    }
    // string message = httpHeader + contentType + "\r\n" + "<form method='GET'>" + "<input name='to' type='text'/>" + "<input type='submit'/>" + "</form>";
    //const char *cmessage = message.c_str();
    //snprintf(send_data, sizeof(send_data), "%s", cmessage);
    //printf("%s\n", send_data);
    send(sock, send_data, strlen(send_data), 0);
    printf("Data sended.\n");
    bytes_recieved = recv(sock, recv_data, 9999, 0);
    recv_data[bytes_recieved] = '\0';
    close(sock);
    printf("Data reveieved.\n");
    printf("%s\n", recv_data);
}

void sendOtherStationNames()
{
    string message = "#\n";
    message += currentStation;
    message += "\n";
    message += receivingDatagram;
    for (int i = 0; i < otherStationDatagrams.size(); i++)
    {
        writeUDP(message, otherStationDatagrams.at(i));
    }
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

vector<char> split(const string &str)
{
    vector<char> result;

    for (char ch : str)
    {
        result.push_back(ch);
    }
    return result;
}

void separateUserInputs(string body)
{
    int startIndex = 0;
    int endIndex = 0;
    for (int i = 1; i < body.size(); i++)
    {
        vector<char> result = split(body);

        if (result.at(i) == '=' && result.at(i - 1) == 'o')
        {
            startIndex = i + 1;
        }
        if (result.at(i) == 'H' && result.at(i - 1) == ' ')
        {
            endIndex = i - 1;
        }
    }
    requiredDestination = "";
    requiredDestination = body.substr(startIndex, endIndex - 9);
    trim(requiredDestination);
}

const string currentDateTime()
{
    time_t now = time(0);
    struct tm tstruct;
    char buf[80];
    tstruct = *localtime(&now);

    strftime(buf, sizeof(buf), "%R", &tstruct);

    return buf;
}

void addCurrentStationToDatagram(vector<string> path, vector<string> departureTimes, vector<string> arrivalTimes, int portNumber)
{
    int index = 0;

    string time = currentDateTime();
    const char *nowTime = time.c_str();
    struct tm tm;
    //time_t convertedTime = strptime(nowTime, "%H:%M", &tm);

    for (int i = 0; i < timetableDepatureTime.size(); i++)
    {
        if (arrivalTimes.size() == 0)
        {
            cout << "reached";
        }
        else if (arrivalTimes.size() > 0)
        {
            string lastArrivalTime = arrivalTimes.at(arrivalTimes.size() - 1);
            const char *cstr = lastArrivalTime.c_str();
            struct tm tm;
            strptime(cstr, "%H:%M", &tm);
            time_t t = mktime(&tm); // t is now your desired time_t

            //double comparison = difftime(t, convertedTime);

            //if (comparison >= 0.00 && timetablePorts.at(i) == portNumber)
            // {
            //    index = i;
            ///    break;
            //}
        }
    }
}

/*
string determineOriginDepartureTime(int port)
{
    // TODO
}
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
    lastNodePort = 0;
    hasReachedFinalStation = false;
    homeStation = "";
}

bool datagramHasNull(string message)
{
    bool answer = false;
    vector<string> result = explode(message, ' ');
    for (int i = 0; i < result.size(); i++)
    {
        if (result.at(i).find("null"))
        {
            answer = true;
            break;
        }
    }
    return answer;
}

void readDatagramIn(string message)
{
    reset();
    vector<string> result = explode(message, ' ');
    if (result.at(0).find("Outgoing"))
    {
        isOutgoing = true;
    }
    else
    {
        isOutgoing = false;
    }
    requiredDestination = result.at(1);
    originDepartureTime = result.at(2);

    string trimString = result.at(3);
    trim(trimString);
    numberStationsStoppedAt = stoi(trimString);

    trimString = "";

    trimString = result.at(4);
    trim(trimString);
    lastNodePort = stoi(trimString);

    trimString = "";

    trimString = result.at(5);
    trim(trimString);
    if (trimString.find("true"))
    {
        hasReachedFinalStation = true;
    }
    else
    {
        hasReachedFinalStation = false;
    }

    trimString = "";
    trimString = result.at(6);
    trim(trimString);
    homeStation = trimString;

    int pathIndex = 7;
    int departureIndex = 8;
    int arrivalIndex = 9;
    for (int i = 0; i < numberStationsStoppedAt; i++)
    {
        path.push_back(result.at(pathIndex));
        pathIndex += 3;
        departureTimes.push_back(result.at(departureIndex));
        departureIndex += 3;
        arrivalTimes.push_back(result.at(arrivalIndex));
        arrivalIndex += 3;
    }
}

void datagramChecks()
{
    string message;
    if (isFinalStation() && isOutgoing && !hasReachedFinalStation)
    {
        isOutgoing = false;
        hasReachedFinalStation = true;
        numberStationsStoppedAt++;

        lastNodePort = receivingDatagram;

        for (int i = 0; i < otherStationDatagrams.size(); i++)
        {
            // TODO addCurrentStationToDatagram writeUDP
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                                        numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort,
                                        hasReachedFinalStation, homeStation);
            writeUDP(message, otherStationDatagrams.at(i));
        }
    }
    else if (!isFinalStation() && isOutgoing && !hasReachedFinalStation)
    {
        numberStationsStoppedAt++;
        int oldPort = lastNodePort;
        lastNodePort = receivingDatagram;

        for (int i = 0; i < otherStationDatagrams.size(); i++)
        {
            if (otherStationDatagrams.at(i) == oldPort)
            {
                continue;
            }
            else
            {
                // TODO
                message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                                            numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort,
                                            hasReachedFinalStation, homeStation);
                writeUDP(message, otherStationDatagrams.at(i));
            }
        }
    }
    else if (!isFinalStation() && !isOutgoing && hasReachedFinalStation)
    {
        int oldPort = lastNodePort;
        lastNodePort = receivingDatagram;
        message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                                    numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort,
                                    hasReachedFinalStation, homeStation);
        for (int i = 0; i < otherStationDatagrams.size(); i++)
        {
            if (otherStationDatagrams.at(i) == oldPort)
            {
                continue;
            }
            else
            {
                writeUDP(message, otherStationDatagrams.at(i));
            }
        }
    }
}

// get sockaddr, IPv4 or IPv6:
void *get_in_addr(struct sockaddr *sa)
{
    if (sa->sa_family == AF_INET)
    {
        return &(((struct sockaddr_in *)sa)->sin_addr);
    }

    return &(((struct sockaddr_in6 *)sa)->sin6_addr);
}

int send_msg_to_client(int socketfd, char *data)
{

    struct msghdr msg;
    struct iovec iov;
    int s;

    memset(&msg, 0, sizeof(msg));
    memset(&iov, 0, sizeof(iov));

    msg.msg_name = NULL;
    msg.msg_namelen = 0;
    iov.iov_base = data;
    // replace sizeof(data) by strlen(data)+1
    iov.iov_len = strlen(data) + 1;
    msg.msg_iov = &iov;
    msg.msg_iovlen = 1;
    msg.msg_control = NULL;
    msg.msg_controllen = 0;
    msg.msg_flags = 0;

    printf("msg: %s\n", (char *)iov.iov_base);

    s = sendmsg(socketfd, &msg, 0);

    if (s < 0)
    {
        perror("sendmsg");
        return 0;
    }

    return s;
}

void run(int webPort, int receivingDatagram, vector<int> otherStationDatagrams)
{

    fd_set master;   // master file descriptor list
    fd_set read_fds; // temp file descriptor list for select()
    int fdmax;       // maximum file descriptor number

    int listener;                       // listening socket descriptor
    int newfd;                          // newly accept()ed socket descriptor
    struct sockaddr_storage remoteaddr; // client address
    socklen_t addrlen;

    char buf[256]; // buffer for client data
    int nbytes;

    char remoteIP[INET6_ADDRSTRLEN];

    int yes = 1; // for setsockopt() SO_REUSEADDR, below
    int i, j, rv;

    struct addrinfo hints, *ai, *p;

    FD_ZERO(&master); // clear the master and temp sets
    FD_ZERO(&read_fds);

    // get us a socket and bind it
    memset(&hints, 0, sizeof hints);
    hints.ai_family = AF_UNSPEC;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_flags = AI_PASSIVE;

#define port "4005"
    if ((rv = getaddrinfo(NULL, port, &hints, &ai)) != 0)
    {
        fprintf(stderr, "selectserver: %s\n", gai_strerror(rv));
        exit(1);
    }

    for (p = ai; p != NULL; p = p->ai_next)
    {
        listener = socket(p->ai_family, p->ai_socktype, p->ai_protocol);
        if (listener < 0)
        {
            continue;
        }

        // lose the pesky "address already in use" error message
        setsockopt(listener, SOL_SOCKET, SO_REUSEADDR, &yes, sizeof(int));

        if (::bind(listener, p->ai_addr, p->ai_addrlen) < 0)
        {
            close(listener);
            continue;
        }

        break;
    }

    // if we got here, it means we didn't get bound
    if (p == NULL)
    {
        fprintf(stderr, "selectserver: failed to bind\n");
        exit(2);
    }

    freeaddrinfo(ai); // all done with this

    // listen
    if (listen(listener, 10) == -1)
    {
        perror("listen");
        exit(3);
    }

    // add the listener to the master set
    FD_SET(listener, &master);

    // keep track of the biggest file descriptor
    fdmax = listener; // so far, it's this one

    // main loop
    while (true)
    {
        read_fds = master; // copy it
        if (select(fdmax + 1, &read_fds, NULL, NULL, NULL) == -1)
        {
            perror("select");
            exit(4);
        }

        // run through the existing connections looking for data to read
        for (i = 0; i <= fdmax; i++)
        {
            if (FD_ISSET(i, &read_fds))
            { // we got one!!
                if (i == listener)
                {
                    // handle new connections
                    addrlen = sizeof remoteaddr;
                    newfd = accept(listener,
                                   (struct sockaddr *)&remoteaddr,
                                   &addrlen);

                    if (newfd == -1)
                    {
                        perror("accept");
                    }
                    else
                    {
                        FD_SET(newfd, &master); // add to master set
                        if (newfd > fdmax)
                        { // keep track of the max
                            fdmax = newfd;
                        }
                        printf("selectserver: new connection from %s on "
                               "socket %d\n",
                               inet_ntop(remoteaddr.ss_family,
                                         get_in_addr((struct sockaddr *)&remoteaddr),
                                         remoteIP, INET6_ADDRSTRLEN),
                               newfd);
                        // WRITE TCP here
                        const char *fmt = "HTTP/1.1 200 OK\r\n"
                                          "Content-length: %ld\r\n"
                                          "Content-Type: text/html\r\n"
                                          "\r\n"
                                          "%s";

                        const char *data = "<form method='GET'>"
                                           "<input name='to' type='text'/>"
                                           "<input type='submit'/>"
                                           "</form>";
                        int datasize = strlen(data);

                        char m[1024];
                        sprintf(m, fmt, datasize, data);

                        send(newfd, m, strlen(m), 0);
                    }
                }
                else
                {
                    // handle data from a client
                    if ((nbytes = recv(i, buf, sizeof buf, 0)) <= 0)
                    {
                        cout << buf;
                        int i = 0;
                        while (buf[i] != '\0')
                        {
                            if (buf[i] == '=')
                            {
                                cout << buf[i + 1];
                            }
                            i++;
                        }
                        // got error or connection closed by client
                        if (nbytes == 0)
                        {
                            // connection closed
                            printf("selectserver: socket %d hung up\n", i);
                        }
                        else
                        {
                            perror("recv");
                        }
                        close(i);           // bye!
                        FD_CLR(i, &master); // remove from master set
                    }
                    else
                    {
                        // we got some data from a client
                        for (j = 0; j <= fdmax; j++)
                        {
                            // send to everyone!
                            if (FD_ISSET(j, &master))
                            {
                                // except the listener and ourselves
                                if (j != listener && j != i)
                                {
                                    if (send(j, buf, nbytes, 0) == -1)
                                    {
                                        perror("send");
                                    }
                                }
                            }
                        }
                    }
                } // END handle data from client
            }     // END got new incoming connection
        }         // END looping through file descriptors
    }             // END for(;;)--and you thought it would never end!
}

int main(int argCount, const char *args[])
{
    currentStation = args[1]; // Something wrong on this line with the pointer
    webPort = atoi(args[2]);
    receivingDatagram = atoi(args[3]);

    int otherIndex = 4;
    for (int i = 0; i <= argCount - 5; i++)
    {
        otherStationDatagrams.push_back(atoi(args[otherIndex]));
        otherIndex++;
    }

    //separateUserInputs("GET /?to=Warwick-Stn HTTP/1.1");
    //writeUDP("hello", 4004);
    arrivalTimes.push_back("9:01");
    departureTimes.push_back("9:00");
    timetableDepatureTime.push_back("9:00");
    addCurrentStationToDatagram(path, departureTimes, arrivalTimes, 4004);
    run(webPort, receivingDatagram, otherStationDatagrams);

    //string message = "#\nCottesloe_Stn\n4005";
    // cout << split("What are you having for lunch today?");

    //removePortIfCovered(message);
    //readTimetableIn();
    //addCurrentStationToDatagram(path, departureTimes, arrivalTimes, receivingDatagram);
    //separateUserInputs("GET /?to=Warwick-Stn HTTP/1.1");
    // receiveOtherStationNames(message);
    //cout << (constructDatagram(true, "Subiaco-Stn", "9:00", 10, path, departureTimes, arrivalTimes));
    return 0;
}
