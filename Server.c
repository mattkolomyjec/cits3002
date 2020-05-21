#include <arpa/inet.h>
#include <errno.h>
#include <netinet/in.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <strings.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#include <stdbool.h>
#include <assert.h>
#define PORT 4002
#define MAXLINE 1024
#define MAXPATH 1024 * 6
#define MAXTIMETABLE 1024 * 100

// Message Variables
char requiredDestination;
bool isOutgoing;
char originDepartureTime;
int numberStationsStoppedAt;
char path[MAXPATH];
char departureTimes[MAXPATH];
char arrivalTimes[MAXPATH];
int lastNodePort;
bool hasReachedFinalStation;
char homeStation;

// Station  Variables
const char currentStation[1024];
int receivingDatagram;
int webPort;
int otherStationDatagrams; //***
char latitude;
char longitude;

// Timetable Variables
char timetableDepatureTime[1024][MAXTIMETABLE];
char timetableLine[1024][MAXTIMETABLE];
char timetablePlatform[1024][MAXTIMETABLE];
char timetableArrivalTime[1024][MAXTIMETABLE];
char timetableDestinations[1024][MAXTIMETABLE];
char timetablePorts[1024][MAXTIMETABLE];

// Other Variables
char datagramClientMessage;
bool hasReceivedOtherStationNames = false;
int otherStationPorts[MAXPATH]; //**
bool alreadyWritten = false;

int max(int x, int y)
{
    if (x > y)
        return x;
    else
        return y;
}
// https://stackoverflow.com/questions/9210528/split-string-with-delimiters-in-c
char **str_split(char *a_str, const char a_delim)
{
    char **result = 0;
    size_t count = 0;
    char *tmp = a_str;
    char *last_comma = 0;
    char delim[2];
    delim[0] = a_delim;
    delim[1] = 0;

    /* Count how many elements will be extracted. */
    while (*tmp)
    {
        if (a_delim == *tmp)
        {
            count++;
            last_comma = tmp;
        }
        tmp++;
    }

    /* Add space for trailing token. */
    count += last_comma < (a_str + strlen(a_str) - 1);

    /* Add space for terminating null string so caller
       knows where the list of returned strings ends. */
    count++;

    result = malloc(sizeof(char *) * count);

    if (result)
    {
        size_t idx = 0;
        char *token = strtok(a_str, delim);

        while (token)
        {
            assert(idx < count);
            *(result + idx++) = strdup(token);
            token = strtok(0, delim);
        }
        assert(idx == count - 1);
        *(result + idx) = 0;
    }

    return result;
}

void readTimetableIn()
{
    // char chosenStation[1024];
    // char chosenStation = "google_transit/tt-Cottesloe_Stn";
    //*chosenStation = *strcat("google_transit/tt-", currentStation);
    //printf("%s", chosenStation);
    FILE *fp;
    fp = fopen("google_transit/tt-Cottesloe_Stn", "r");
    if (fp == NULL)
    {
        perror("Error opening file");
    }
    char line[256];
    bool firstLineDone = true;
    while (fgets(line, sizeof(line), fp))
    {
        if (firstLineDone == false)
        {
            // FIRST LINE OPERSATIONS
            char **tokens;
            tokens = str_split(line, ',');
            firstLineDone = true;
        }
        char **tokens;
        tokens = str_split(line, ',');
        if (tokens)
        {
            int i;
            for (i = 0; *(tokens + i); i++)
            {
                // timetableDepatureTime[i][sizeof(timetableDepatureTime) / sizeof(timetableDepatureTime[0])] = *(tokens + i);
                printf("month=[%s]\n", *(tokens + i));
                free(*(tokens + i));
            }
            printf("\n");
            free(tokens);
        }
    }

    fclose(fp);
}

void writeUDP(char body[], int port)
{
    int sockfd;
    char buffer[MAXLINE];
    // char *hello = "Hello from client";
    struct sockaddr_in servaddr;

    // Creating socket file descriptor
    if ((sockfd = socket(AF_INET, SOCK_DGRAM, 0)) < 0)
    {
        perror("socket creation failed");
        exit(EXIT_FAILURE);
    }

    memset(&servaddr, 0, sizeof(servaddr));

    // Filling server information
    servaddr.sin_family = AF_INET;
    servaddr.sin_port = htons(port);
    servaddr.sin_addr.s_addr = INADDR_ANY;

    int n, len;

    sendto(sockfd, (const char *)body, strlen(body),
           0, (const struct sockaddr *)&servaddr,
           sizeof(servaddr));
    printf("Hello message sent.\n");
    close(sockfd);
}
/*
void sendOtherStationNames()
{
    char message[1024] = "#\n";
    strcat(message, currentStation);
    strcat(message, "\n");
    char buf[256];
    sprintf(buf, "%d", receivingDatagram);
    strcat(message, buf);

    for (int i = 0; i < (sizeof(otherStationDatagrams) / sizeof(otherStationDatagrams[0]); i++)
    {
        writeUDP(message, otherStationDatagrams[i]);
    }
}
*/
/*
bool isFinalStation()
{
    bool result = false;
    const char buf[256];
    sprintf(buf, "%s", requiredDestination);
    if (strcmp(currentStation, buf))
    {
        result = true;
    }
    return result;
}
*/
void receieveOtherStationNames(char message)
{
}

void separateUserInputs(char body)
{
}

void addCurrentStationToDatagram(int path[], int departureTimes[], int arrivalTimes[], int portNumber)
{
}

char determineOriginDepartureTime(int port)
{
    return 'a';
}
/*
char constructDatagram(bool isOutgoing, char requiredDestination, char originDepartureTime, int numberStationsStoppedAt, char path[], char departureTimes[], char arrivalTimes[], int lastNodePort, bool hasReachedFinalDestination)
{
    char result;
    if (isOutgoing)
    {
        result = 'Outgoing \n';
    }
    else
    {
        result = 'Incoming \n';
    }
    strcat(result, requiredDestination);
    strcat(result, ' ');
    strcat(result, originDepartureTime);
    strcat(result, ' ');

    char ODT[10];
    sprintf(ODT, "%d", numberStationsStoppedAt);
    strcat(result, ODT);
    strcat(result, ' ');

    char LNP[10];
    sprintf(LNP, "%d", lastNodePort);
    strcat(result, LNP);
    strcat(result, ' ');

    if (hasReachedFinalDestination)
    {
        strcat(result, 'true');
    }
    else
    {
        strcat(result, 'false');
    }
    strcat(result, ' ');

    strcat(result, homeStation);

    strcat(result, ' \n');

    // ADD PATH HERE!
}
*/

// https://stackoverflow.com/questions/5308734/how-can-i-search-for-substring-in-a-buffer-that-contains-null
char *search_buffer(char *haystack, size_t haystacklen, char *needle, size_t needlelen)
{ /* warning: O(n^2) */
    int searchlen = haystacklen - needlelen + 1;
    for (; searchlen-- > 0; haystack++)
        if (!memcmp(haystack, needle, needlelen))
            return haystack;
    return NULL;
}

void reset()
{
    isOutgoing = false;
    requiredDestination = ' ';
    originDepartureTime = ' ';
    numberStationsStoppedAt = ' ';
    memset(path, 0, sizeof path);
    memset(departureTimes, 0, sizeof departureTimes);
    memset(arrivalTimes, 0, sizeof(arrivalTimes));
    lastNodePort = 0;
    hasReachedFinalStation = false;
    homeStation = ' ';
}

static bool
str_to_uint16(const char *str, uint16_t *res)
{
    char *end;
    errno = 0;
    intmax_t val = strtoimax(str, &end, 10);
    if (errno == ERANGE || val < 0 || val > UINT16_MAX || end == str || *end != '\0')
        return false;
    *res = (uint16_t)val;
    return true;
}

int main(int argc, char *argv[])
{
    // readTimetableIn();
    //writeUDP("hello", 4004);

    int listenfd, connfd, udpfd, nready, maxfdp1;
    char buffer[MAXLINE];
    pid_t childpid;
    fd_set rset;
    ssize_t n;
    socklen_t len;
    const int on = 1;
    struct sockaddr_in cliaddr, servaddr;
    char *message = "HTTP/1.1 200 OK\r\n"
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
    const char port = *argv[2];
    int x = atoi(argv[2]);
    char snum[5];

    // convert 123 to string [buf]
    sprintf(snum, "%d", x);
    printf("%s\n", snum);
    str_to_uint16(snum, )
        servaddr.sin_port = htons(snum);

    //  printf("%i", servaddr.sin_port); // 41487
    // printf("%i", htons(port)); // 13312

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
                // GET /?to=Cottesloe_Stn HTTP/1.1
                char needle[] = "GET /?to=";
                char *res = search_buffer(buffer, sizeof(buffer) - 1, needle, sizeof(needle) - 1);
                // printf("%s", res);
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
            sendto(udpfd, (const char *)message, sizeof(buffer), 0,
                   (struct sockaddr *)&cliaddr, sizeof(cliaddr));
        }
    }
}
