import java.util.ArrayList;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Scanner;
import java.time.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.time.format.DateTimeFormatter;

public class Station {

    // Message Variables
    String requiredDestination; // The intended destination of the user. WILL CAPS LOCK AFFECT THIS?
    boolean isOutgoing;
    String originDepartureTime;
    int numberStationsStoppedAt;
    ArrayList<String> path = new ArrayList<String>();
    ArrayList<String> departureTimes = new ArrayList<String>();
    ArrayList<String> arrivalTimes = new ArrayList<String>();

    // Station Variables
    String currentStation;
    String latitude;
    String longitude;
    ArrayList<String> destinations = new ArrayList<String>();
    int receivingDatagram;
    int[] otherStationDatagrams;

    // Other Variables
    String datagramClientMessage;

    // ###############################################################################
    /**
     * Create an instance of Station object.
     * 
     * @param currentStation
     * @param receivingDatagram
     * @param otherStationDatagrams
     */
    public Station(String currentStation, int receivingDatagram, int[] otherStationDatagrams) {
        this.currentStation = currentStation;
        this.receivingDatagram = receivingDatagram;
        this.otherStationDatagrams = otherStationDatagrams;
    }

    // ###############################################################################
    /**
     * A method to read the timetable data in froma text file and store it in an
     * ArrayList
     */
    public void readTimetableIn() throws FileNotFoundException {
        File file = new File("routes.txt");
        Scanner sc = new Scanner(file);

        String temp[] = ((sc.nextLine()).split(","));
        currentStation = temp[0];
        latitude = temp[1];
        longitude = temp[2];

        while (sc.hasNextLine()) {
            String tempRoutes[] = ((sc.nextLine()).split(","));
            for (int i = 0; i < tempRoutes.length; i++) {
                destinations.add(tempRoutes[i]);
            }
        }
        sc.close();
    }

    /***
     * A method to check if the required destination is directly connected to the
     * current station
     * 
     * @return boolean indicataing if there is a direct connection to the required
     *         destination
     */
    public boolean hasDirectConnection() {
        boolean result = false;
        for (int i = 0; i < destinations.size(); i++) {
            if ((destinations.get(i)).contains(requiredDestination)) {
                result = true;
                break;
            }
        }
        return result;
    }

    /**
     * A method to check whether the current station is the final destination
     * 
     * @return boolean indicating whether the current station is the final station
     */
    public boolean isFinalStation() {
        boolean result = false;
        String c = currentStation.trim();
        String r = currentStation.trim();
        if (c.equals(r)) {
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
    public void separateUserInputs(String body) {
        String[] temp = body.split("(?!^)");
        int endIndex = 0;
        for (int i = 0; i < temp.length; i++) {
            if (temp[i].contains("=")) {
                endIndex = i + 1;
            }
        }
        requiredDestination = body.substring(endIndex);
    }

    // ###############################################################################
    /**
     * A method to add the current station to the datagram before sending it onto
     * the next node
     * 
     * @param path
     * @param departureTimes
     * @param arrivalTimes
     */
    public void addCurrentStationToDatagram(ArrayList<String> path, ArrayList<String> departureTimes,
            ArrayList<String> arrivalTimes) {
        // departureTimes.add("")
        path.add(currentStation);
        departureTimes.add("TEST");
        arrivalTimes.add("TEST");
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
    public String constructDatagram(boolean isOutgoing, String requiredDestination, String originDepartureTime,
            int numberStationsStoppedAt, ArrayList<String> path, ArrayList<String> departureTimes,
            ArrayList<String> arrivalTimes) {
        String result;
        if (isOutgoing) {
            result = "Outgoing \n";
        } else {
            result = "Incoming \n";
        }
        result += requiredDestination + " " + originDepartureTime + " " + numberStationsStoppedAt + " \n";
        for (int i = 0; i < path.size(); i++) {
            result += path.get(i) + " " + departureTimes.get(i) + " " + arrivalTimes.get(i) + " \n";
        }
        return result;
    }

    /***
     * A method to reset the variables that hold the current message in hand
     */
    public void reset() {
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
    public void readDatagramIn(String message) {
        reset();
        String temp[] = message.split(" ");
        if (temp[0].contains("Outgoing")) {
            isOutgoing = true;
        } else {
            isOutgoing = false;
        }
        requiredDestination = temp[1];
        originDepartureTime = temp[2];
        numberStationsStoppedAt = Integer.parseInt(temp[3]);

        int pathIndex = 4;
        int departureIndex = 5;
        int arrivalIndex = 6;
        for (int i = 0; i < numberStationsStoppedAt; i++) {
            path.add(temp[pathIndex]);
            pathIndex += 3;
            departureTimes.add(temp[departureIndex]);
            departureIndex += 3;
            arrivalTimes.add(temp[arrivalIndex]);
            arrivalIndex += 3;
        }
    }

    /***
     * A method to establish the datagram client.
     * 
     * @param message the GET request sent by the client to the server requesting
     *                journey details.
     * @param port    to communicate with the server on.
     * @throws SocketException
     */
    public void datagramClient(String message, int port) throws SocketException {
        DatagramSocket skt;
        try {
            // Sending the Datagram
            skt = new DatagramSocket();
            byte[] b = message.getBytes();
            InetAddress host = InetAddress.getByName("localhost");
            DatagramPacket request = new DatagramPacket(b, b.length, host, port);
            skt.send(request);

            // Receiveing the Datagram
            byte[] buffer = new byte[1000];
            DatagramPacket reply = new DatagramPacket(buffer, buffer.length);
            skt.receive(reply);
            String result = new String(reply.getData());
            // System.out.println(new String(reply.getData()));
            // System.out.println(result);
            datagramClientMessage = result;
            skt.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * A method to calculate the total journey time (in minutes) given departure and
     * arrival times
     * 
     * @param departureTime
     * @param arrivalTime
     * @return
     */
    public int calculateJourneyTime(ArrayList<String> departureTime, ArrayList<String> arrivalTime) {
        /*
         * int[] durations = new int[departureTime.size()]; for (int i = 0; i <
         * departureTime.size(); i++) {
         * 
         * Duration diff = Duration.between( (LocalDateTime.parse(arrivalTime.get(i),
         * DateTimeFormatter.ofPattern("hh:mm", Locale.US))
         * .atZone(ZoneId.of("Australia/Perth")).toInstant()),
         * (LocalDateTime.parse(departureTime.get(i),
         * DateTimeFormatter.ofPattern("hh:mm", Locale.US))
         * .atZone(ZoneId.of("Australia/Perth")).toInstant()));
         * 
         * durations[i] = diff.toMinutesPart(); } int sum = 0; for (int i = 0; i <
         * durations.length; i++) { sum += durations[i]; }
         */
        return 0;
    }

    // ###############################################################################
    /**
     * A method to run the core socket server.
     * 
     * @param port
     * @throws IOException
     */
    public void run(int webPort, int[] datagramPorts) throws IOException {
        ServerSocket serverSocket = new ServerSocket(webPort);
        while (true) {
            try {
                // #############################################################################
                // ESTABLISH A CONNECTION WITH THE WEB PAGE

                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));

                String line;
                line = in.readLine();
                StringBuilder raw = new StringBuilder();
                raw.append("" + line);
                boolean isPost = line.startsWith("POST");
                boolean isGet = line.startsWith("GET");
                int contentLength = 0;
                while (!(line = in.readLine()).equals("")) {
                    raw.append('\n' + line);
                    if (isPost) {
                        final String contentHeader = "Content-Length: ";
                        if (line.startsWith(contentHeader)) {
                            contentLength = Integer.parseInt(line.substring(contentHeader.length()));
                        }
                    }
                }
                StringBuilder body = new StringBuilder();

                if (isPost) {
                    int c = 0;
                    for (int i = 0; i < contentLength; i++) {
                        c = in.read();
                        body.append((char) c);
                        // Log.d("JCD", "POST: " + ((char) c) + " " + c);
                    }
                }
                raw.append(body.toString());

                // publishProgress(raw.toString());
                // send response
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Content-Type: text/html\r\n");
                out.write("\r\n");
                out.write(new Date().toString());

                // Sending out a datagram to determine the journey for first time
                if (isPost) {
                    reset();
                    separateUserInputs(body.toString());
                    isOutgoing = true;

                    // ######################
                    // need a method to find the routes to travel to a given destination
                    originDepartureTime = destinations.get(0);
                    // ######################

                    String datagramToSendToNodes = constructDatagram(isOutgoing, requiredDestination,
                            originDepartureTime, numberStationsStoppedAt, path, departureTimes, arrivalTimes);

                    for (int i = 0; i < datagramPorts.length; i++) {
                        datagramClient(datagramToSendToNodes, datagramPorts[0]);
                    }
                    // System.out.println(datagramClientMessage);
                    // out.write("<br><u>" + body.toString() + "</u>");
                    out.write("<br><u>" + datagramClientMessage + "</u>");
                } else if (isGet) { // is this correct ?? is it actually a get request
                    // #############################################################################
                    // RUN THE RECEIVING SERVER
                    DatagramSocket skt = null;
                    skt = new DatagramSocket(receivingDatagram);
                    byte[] buffer = new byte[1000];

                    DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                    skt.receive(request);

                    readDatagramIn(new String(request.getData()));
                    String message = "";

                    if (isFinalStation() && isOutgoing) { // State 1 - Reached required destination, now need to travel
                                                          // back
                        // to start node
                        isOutgoing = false;
                        numberStationsStoppedAt++;
                        addCurrentStationToDatagram(path, departureTimes, arrivalTimes);
                        message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                                numberStationsStoppedAt, path, departureTimes, arrivalTimes);
                    } else if (!isFinalStation() && isOutgoing) { // State 2 - Has not reached required destination and
                                                                  // need
                        // to continue travelling to it
                        numberStationsStoppedAt++;
                        addCurrentStationToDatagram(path, departureTimes, arrivalTimes);
                        message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                                numberStationsStoppedAt, path, departureTimes, arrivalTimes);
                    } else if (!isFinalStation() && !isOutgoing) { // State 3 - Is on the way back to the original node
                                                                   // but
                        // has not returned
                        message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                                numberStationsStoppedAt, path, departureTimes, arrivalTimes);
                    } else if (isFinalStation() && !isOutgoing) { // State 4 - Has arrived back to the original node and
                        // needs to transmit the route to the HTML page.
                        // SEND HTML back
                        out.write("<br><u>" + datagramClientMessage + "</u>");
                    }

                    byte[] sendMsg = (message).getBytes();
                    DatagramPacket reply = new DatagramPacket(sendMsg, sendMsg.length, request.getAddress(),
                            request.getPort());
                    skt.send(reply);
                } else {
                    out.write("<form method='POST'>");
                    out.write("<input name='destination' type='text'/>");
                    out.write("<input type='submit'/>");
                    out.write("</form>");
                }
                // do not in.close();
                out.flush();
                out.close();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    // ###############################################################################
    public static void main(String args[]) {
        if (args.length < 4) {
            throw new IllegalArgumentException(
                    "ERROR: Invalid input! Correct input follows: {Station Name} {Web Port Number} {Datagram Receipt Port Number} {Next Station Port Number(s)}");
        }
        String origin = args[0];
        int webPort = Integer.parseInt(args[1]);
        int stationDatagrams = Integer.parseInt(args[2]);

        int[] otherStationDatagrams = new int[args.length - 3];
        int otherIndex = 3;
        for (int i = 0; i < otherStationDatagrams.length; i++) {
            otherStationDatagrams[i] = Integer.parseInt(args[otherIndex]);
            otherIndex++;
        }
        try {
            Station station = new Station(origin, stationDatagrams, otherStationDatagrams);
            station.readTimetableIn();

            // ArrayList<String> destinations = new ArrayList<String>();
            // ArrayList<String> departureTimes = new ArrayList<String>();
            // ArrayList<String> arrivalTimes = new ArrayList<String>();
            // destinations.add("Subiaco");
            // destinations.add("Thornlie");
            // departureTimes.add("09:00");
            // departureTimes.add("09:45");
            // arrivalTimes.add("09:10");
            // arrivalTimes.add("10:00");

            // String test = station.constructDatagram(true, "Fremantle", "08:45", 2,
            // destinations, departureTimes,
            // arrivalTimes);
            // System.out.println(webPort);

            station.run(webPort, otherStationDatagrams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

// Protocol

// Outgoing/Incoming (whether it has reached final destination yet or not)
// Destination, origin depature time, number of stations stopped at
// Station stopped at, Departure Time, Arrival Time
// Station stopped at, Departure Time, Arrival Time
// Station stopped at, Departure Time, Arrival Time

// Testing Variables

// ArrayList<String> destinations = new ArrayList<String>();
// ArrayList<String> departureTimes = new ArrayList<String>();
// ArrayList<String> arrivalTimes = new ArrayList<String>();
// destinations.add("Subiaco");
// destinations.add("Thornlie");
// departureTimes.add("09:00");
// departureTimes.add("09:45");
// arrivalTimes.add("09:10");
// arrivalTimes.add("10:00");

// String test = station.constructDatagram(true, "Fremantle", "08:45", 2,
// destinations, departureTimes,
// arrivalTimes);
// station.readDatagramIn(test);

// System.out.println(test);