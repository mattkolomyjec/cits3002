#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

// Message Variables
String requiredDestination;

int main(int argCount, const char *args[])
{
    char origin = *args[0]; // Something wrong on this line with the pointer
    int webPort = atoi(args[1]);
    int stationDatagrams = atoi(args[2]);

    int otherStationDatagrams[argCount - 3];
    int otherIndex = 3;
    for (int i = 0; i < argCount; i++)
    {
        otherStationDatagrams[i] = atoi(args[otherIndex]);
        otherIndex++;
    }

    return 0;
}