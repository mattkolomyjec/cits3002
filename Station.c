#include "cnet.h"
#include <stdlib.h>
#include <string.h>

//  Written by Chris.McDonald@uwa.edu.au

/*  This is an implementation of a stop-and-wait data link protocol.
    It is based on Tanenbaum's `protocol 4', 2nd edition, p227
    but now employs piggybacking of acknowledgments:

    - When the receiver receives a DATA frame, it does not immediately send
      the corresponding ACK frame.

    - The receiver waits until it has its own outgoing DATA frame, and
      piggybacks the pending ACK in the header of the outgoing DATA frame.

    - If no DATA frame becomes available within PIGGYBACK_TIMEOUT usecs
      (our Application Layer does not have a message for delivery),
      the receiver must send an ACK frame, by itself.
 */

#define PIGGYBACK_TIMEOUT 1000000

#define UNUSED_SEQ (-1)
#define EV_DATA_TIMER EV_TIMER1
#define EV_PIGGYBACK_TIMER EV_TIMER2

typedef struct
{
    char data[MAX_MESSAGE_SIZE];
} MSG;

/*  Because every frame may potentially carry DATA and an ACK, we no
    longer need a FRAMEKIND in each frame.  Instead, use the DATA or ACK
    sequence numbers to indicate what is being carried.  If either piece
    is "absent", we set its sequence number to the invalid value UNUSED_SEQ.
*/

typedef struct
{
    size_t len;   /* the length of the msg field only */
    int checksum; /* checksum of the whole frame */
    int data_seq; /* only ever 0 or 1 or UNUSED_SEQ */
    int ack_seq;  /* only ever 0 or 1 or UNUSED_SEQ */
    MSG msg;
} FRAME;

#define FRAME_HEADER_SIZE (sizeof(FRAME) - sizeof(MSG))
#define FRAME_SIZE(f) (FRAME_HEADER_SIZE + f.len)

MSG *lastmsg;
size_t lastlength = 0;
CnetTimerID data_timer = NULLTIMER;
CnetTimerID piggyback_timer = NULLTIMER;

int ackexpected = 0;
int nextframetosend = 0;
int frameexpected = 0;

int ack_pending = UNUSED_SEQ;

void transmit_frame(MSG *msg, size_t length, int data_seq)
{
    FRAME f;
    int link = 1;

    f.data_seq = data_seq;
    f.ack_seq = ack_pending;
    f.checksum = 0;
    f.len = length;

    printf("FRAME transmitted, data_seq=%d, ack_seq=%d\n", f.data_seq, f.ack_seq);

    if (f.data_seq != UNUSED_SEQ)
    {
        CnetTime timeout;

        memcpy(&f.msg, msg, length);

        timeout = FRAME_SIZE(f) * ((CnetTime)8000000 / linkinfo[link].bandwidth) +
                  linkinfo[link].propagationdelay;

        data_timer = CNET_start_timer(EV_DATA_TIMER, 3 * timeout, 0);
    }
    length = FRAME_SIZE(f);
    f.checksum = CNET_ccitt((unsigned char *)&f, (int)length);
    CHECK(CNET_write_physical(link, &f, &length));

    if (ack_pending != UNUSED_SEQ)
    {
        CNET_stop_timer(piggyback_timer);
        ack_pending = UNUSED_SEQ;
    }
}

EVENT_HANDLER(application_ready)
{
    CnetAddr destaddr;

    lastlength = sizeof(MSG);
    CHECK(CNET_read_application(&destaddr, lastmsg, &lastlength));
    CNET_disable_application(ALLNODES);

    printf("down from application, seq=%d\n", nextframetosend);
    transmit_frame(lastmsg, lastlength, nextframetosend);
    nextframetosend = 1 - nextframetosend;
}

EVENT_HANDLER(physical_ready)
{
    FRAME f;
    size_t len;
    int link, checksum;

    len = sizeof(FRAME);
    CHECK(CNET_read_physical(&link, &f, &len));

    checksum = f.checksum;
    f.checksum = 0;
    if (CNET_ccitt((unsigned char *)&f, (int)len) != checksum)
    {
        printf("\t\t\t\tBAD checksum - frame ignored\n");
        return; // bad checksum, ignore frame
    }

    if (f.ack_seq == ackexpected)
    {
        printf("\t\t\t\tACK received, ack_seq=%d\n", f.ack_seq);
        CNET_stop_timer(data_timer);
        ackexpected = 1 - ackexpected;
        CNET_enable_application(ALLNODES);
    }

    if (f.data_seq == frameexpected)
    {
        printf("\t\t\t\tDATA received, data_seq=%d, to application\n", f.data_seq);
        len = f.len;
        CHECK(CNET_write_application(&f.msg, &len));
        frameexpected = 1 - frameexpected;
    }
    if (f.data_seq != UNUSED_SEQ)
    {
        ack_pending = f.data_seq;
        piggyback_timer =
            CNET_start_timer(EV_PIGGYBACK_TIMER, PIGGYBACK_TIMEOUT, 0);
    }
}

EVENT_HANDLER(data_timeouts)
{
    printf("data timeout, data_seq=%d\n", ackexpected);
    transmit_frame(lastmsg, lastlength, ackexpected);
}

EVENT_HANDLER(piggyback_timeouts)
{
    printf("piggyback timeout, ack_seq=%d\n", ack_pending);
    transmit_frame(NULL, 0, UNUSED_SEQ);
}

EVENT_HANDLER(showstate)
{
    printf(
        "\n\tackexpected\t= %d\n\tnextframetosend\t= %d\n\tframeexpected\t= %d\n",
        ackexpected, nextframetosend, frameexpected);
}

EVENT_HANDLER(reboot_node)
{
    if (nodeinfo.nodenumber > 1)
    {
        fprintf(stderr, "This is not a 2-node network!\n");
        exit(1);
    }

    lastmsg = calloc(1, sizeof(MSG));

    CHECK(CNET_set_handler(EV_APPLICATIONREADY, application_ready, 0));
    CHECK(CNET_set_handler(EV_PHYSICALREADY, physical_ready, 0));
    CHECK(CNET_set_handler(EV_DATA_TIMER, data_timeouts, 0));
    CHECK(CNET_set_handler(EV_PIGGYBACK_TIMER, piggyback_timeouts, 0));
    CHECK(CNET_set_handler(EV_DEBUG0, showstate, 0));

    CHECK(CNET_set_debug_string(EV_DEBUG0, "State"));

    CHECK(CNET_enable_application(ALLNODES));
}