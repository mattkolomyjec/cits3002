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

public class Station {

    // Message Variables
    String requiredDestination; // The intended destination of the user. WILL CAPS LOCK AFFECT THIS?
    boolean isOutgoing;
    String originDepartureTime;
    int numberStationsStoppedAt;
    ArrayList<String> path = new ArrayList<String>(); // The names of the nodes taken thus far to get to where we are
    ArrayList<String> departureTimes = new ArrayList<String>(); // The depature times to each node on the journey
    ArrayList<String> arrivalTimes = new ArrayList<String>(); // The depature times to each node on the journey

    // Station Variables
    String currentStation;
    String latitude;
    String longitude;
    ArrayList<String> destinations = new ArrayList<String>();
    int receivingDatagram;
    int[] otherStationDatagrams;

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
     * y A method to read the timetable data in froma text file and store it in an
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
        if (currentStation == requiredDestination) {
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
     * A method to send and receive datagram to other station servers
     * 
     * @param message
     * @param port
     * @throws SocketException
     */
    public void datagrams(String message, int port) throws SocketException {
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
            System.out.println(result);
            skt.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                // read request
                String line;
                line = in.readLine();
                StringBuilder raw = new StringBuilder();
                raw.append("" + line);
                boolean isPost = line.startsWith("POST");
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
                separateUserInputs(body.toString());

                // Flood the network with datagram requests
                /*
                 * for (int i = 0; i < datagramPorts.length; i++) {
                 * datagrams(requiredDestination, datagramPorts[i]); }
                 */

                // publishProgress(raw.toString());
                // send response
                out.write("HTTP/1.1 200 OK\r\n");
                out.write("Content-Type: text/html\r\n");
                out.write("\r\n");
                out.write(new Date().toString());
                if (isPost) {
                    out.write("<br><u>" + body.toString() + "</u>");
                } else {
                    out.write("<form method='POST'>");
                    out.write("<input name='origin' type='text'/>");
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