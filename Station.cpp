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

string convertToString(char *a, int size)
{
    int i;
    string s = "";
    for (i = 0; i < size; i++)
    {
        s = s + a[i];
    }
    return s;
}

void run2()
{
#define PORT 4005
#define MAXLINE 1024
    int listenfd, connfd, udpfd, nready, maxfdp1;
    char buffer[MAXLINE];
    pid_t childpid;
    fd_set rset;
    ssize_t n;
    socklen_t len;
    const int on = 1;
    struct sockaddr_in cliaddr, servaddr;
    const char *message = "HTTP/1.1 200 OK\r\n"
                          "Content-length: %ld\r\n"
                          "Content-Type: text/html\r\n"
                          "\r\n"
                          "%s"
                          "<form method='GET'>"
                          "<input name='to' type='text'/>"
                          "<input type='submit'/>"
                          "</form>";
    void sig_chld(int);

    /* create listening TCP socket */
    listenfd = socket(AF_INET, SOCK_STREAM, 0);
    bzero(&servaddr, sizeof(servaddr));
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = htonl(INADDR_ANY);
    servaddr.sin_port = htons(PORT);

    // binding server addr structure to listenfd
    bind(listenfd, (struct sockaddr *)&servaddr, sizeof(servaddr));
    listen(listenfd, 10);

    /* create UDP socket */
    udpfd = socket(AF_INET, SOCK_DGRAM, 0);
    // binding server addr structure to udp sockfd
    bind(udpfd, (struct sockaddr *)&servaddr, sizeof(servaddr));

    // clear the descriptor set
    FD_ZERO(&rset);

    // get maxfd
    maxfdp1 = max(listenfd, udpfd) + 1;
    for (;;)
    {

        // set listenfd and udpfd in readset
        FD_SET(listenfd, &rset);
        FD_SET(udpfd, &rset);

        // select the ready descriptor
        nready = select(maxfdp1, &rset, NULL, NULL, NULL);

        // if tcp socket is readable then handle
        // it by accepting the connection
        if (FD_ISSET(listenfd, &rset))
        {

            write(connfd, (const char *)message, sizeof(buffer));
            len = sizeof(cliaddr);
            connfd = accept(listenfd, (struct sockaddr *)&cliaddr, &len);
            if ((childpid = fork()) == 0)
            {
                close(listenfd);
                bzero(buffer, sizeof(buffer));
                printf("Message From TCP client: ");
                read(connfd, buffer, sizeof(buffer));
                puts(buffer);
                write(connfd, (const char *)message, sizeof(buffer));
                close(connfd);
                exit(0);
            }
            close(connfd);
        }
        // if udp socket is readable receive the message.
        if (FD_ISSET(udpfd, &rset))
        {
            len = sizeof(cliaddr);
            bzero(buffer, sizeof(buffer));
            printf("\nMessage from UDP client: ");
            n = recvfrom(udpfd, buffer, sizeof(buffer), 0,
                         (struct sockaddr *)&cliaddr, &len);
            puts(buffer);
            int i = 0;
            if (buffer[i] == '#')
            {
                // Read UDP IN
            }
            else
            {
                // READ datagram in
            }
            // sendto(udpfd, (const char *)message, sizeof(buffer), 0,
            //      (struct sockaddr *)&cliaddr, sizeof(cliaddr));
        }
    }
}

int main(int argCount, const char *args[])
{
    currentStation = args[1]; // Something wrong on this line with the pointer
    webPort = atoi(args[2]);
    const char *webPortCHAR = args[2];
    receivingDatagram = atoi(args[3]);

    int otherIndex = 4;
    for (int i = 0; i <= argCount - 5; i++)
    {
        otherStationDatagrams.push_back(atoi(args[otherIndex]));
        otherIndex++;
    }

    //run(webPort, receivingDatagram, otherStationDatagrams);
    run2();

    return 0;
}

/*
                                char *line = strtok(strdup(buf), "\n");
                                while (line)
                                {
                                    if (strcmp(line, "=") == true)
                                    {
                                        printf("%s", line);
                                    }
                                    //printf("%s", line);
                                    //line = strtok(NULL, "\n");
                                }
                                */

//cout << buf[i + 1];
//cout << buf[i + 2];
//cout << buf[i + 3];

//     while (buf[i] != ' ')
//   {
//       result += buf[i];
//   }
