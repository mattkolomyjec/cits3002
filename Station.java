import java.util.ArrayList;
import java.net.Socket;
import java.net.SocketException;
import java.io.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;
import java.util.Scanner;
import java.util.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.time.format.DateTimeFormatter;
import java.time.LocalTime;

public class Station {

    // Message Variables
    String requiredDestination;
    boolean isOutgoing;
    String originDepartureTime;
    int numberStationsStoppedAt;
    ArrayList<String> path = new ArrayList<String>();
    ArrayList<String> departureTimes = new ArrayList<String>();
    ArrayList<String> arrivalTimes = new ArrayList<String>();
    int lastNodePort;
    boolean hasReachedFinalStation;
    String homeStation;

    // Station Variables
    String currentStation;
    int receivingDatagram;
    int webPort;
    int[] otherStationDatagrams;
    String latitude;
    String longitude;

    // Timetable Variables
    ArrayList<String> timetableDepartureTime = new ArrayList<String>();
    ArrayList<String> timetableLine = new ArrayList<String>();
    ArrayList<String> timetablePlatform = new ArrayList<String>();
    ArrayList<String> timetableArrivalTime = new ArrayList<String>();
    ArrayList<String> timetableDestinations = new ArrayList<String>();
    int[] timetablePorts;

    // Other Variables
    private Selector selector;
    public static final String httpHeader = "HTTP/1.1 200 OK\r\n";
    public static final String contentType = "Content-Type: text/html\r\n";
    String datagramClientMessage;
    boolean hasReceivedOtherStationNames = false;
    int receievedOtherStationNamesCount = 0;
    int[] receievedOtherStationPortsUnique;

    /**
     * A method to create an instance of the Station object
     * 
     * @param currentStation        the name of the current station
     * @param webPort               the port to open HTML hosting with
     * @param receivingDatagram     the port to recieve UDP communications from
     *                              other stations
     * @param otherStationDatagrams the receiving ports of adjacent stations
     */
    public Station(String currentStation, int webPort, int receivingDatagram, int[] otherStationDatagrams) {
        this.webPort = webPort;
        this.currentStation = currentStation;
        this.receivingDatagram = receivingDatagram;
        this.otherStationDatagrams = otherStationDatagrams;
        receievedOtherStationPortsUnique = new int[otherStationDatagrams.length];
    }

    /***
     * A method to read the Transperth timetable data in
     * 
     * @throws FileNotFoundException if the file cannot be located
     */
    public void readTimetableIn() throws FileNotFoundException {
        String chosenStation = "tt-" + currentStation;
        File file = new File("google_transit/" + chosenStation);
        Scanner sc = new Scanner(file);

        String temp[] = ((sc.nextLine()).split(","));
        latitude = temp[1];
        longitude = temp[2];

        while (sc.hasNextLine()) {
            String tempRoutes[] = ((sc.nextLine()).split(","));
            for (int i = 0; i < tempRoutes.length - 4; i += 4) {
                timetableDepartureTime.add(tempRoutes[i]);
                timetableLine.add(tempRoutes[i + 1]);
                timetablePlatform.add(tempRoutes[i + 2]);
                timetableArrivalTime.add(tempRoutes[i + 3]);
                timetableDestinations.add(tempRoutes[i + 4]);
            }
        }
        timetablePorts = new int[timetableDestinations.size()];
    }

    /***
     * A method to add the receieving port numbers to their corresponding station in
     * the transperth timetable data
     * 
     * @param ports a Hashmap detailing the name of station and its receving port
     *              number
     */
    public void addPortsToTimetable(HashMap<String, Integer> ports) {
        for (int i = 0; i < timetableDestinations.size(); i++) {
            String currentKey = timetableDestinations.get(i);
            if (ports.containsKey(currentKey)) {
                timetablePorts[i] = ports.get(currentKey);
            }
        }
        System.out.println("REACHED B");
    }

    /***
     * A method to receive the names and port numbers of adjacent stations.
     * 
     * @param message the UDP message read in
     */
    public void receiveOtherStationNames(String message) {
        String temp[] = message.split("\n");
        HashMap<String, Integer> map = new HashMap<String, Integer>();
        map.put(temp[1], Integer.parseInt(temp[2]));
        System.out.println("REACHED A");
        addPortsToTimetable(map);
    }

    /***
     * A method to determine if adjacent station port number has already been read
     * in
     * 
     * @param message
     * @return result indicating where the read in is unique (false) or not unique
     *         (true)
     */
    public boolean checkIfPortIsNotUnique(String message) {
        boolean result = false;
        String temp[] = message.split("\n");
        int portInReference = Integer.parseInt(temp[2]);
        for (int i = 0; i < receievedOtherStationPortsUnique.length; i++) {
            if (receievedOtherStationPortsUnique[i] == portInReference) {
                result = true;
                break;
            }
        }
        return result;
    }

    /***
     * A method to send adjacent stations the name and recieving port number of this
     * station
     * 
     * @throws IOException if the IO does not work
     */
    public void sendOtherStationNames() throws IOException {
        String message = "#" + "\n" + currentStation + "\n" + receivingDatagram;
        for (int i = 0; i < otherStationDatagrams.length; i++) {
            writeUDP(message, otherStationDatagrams[i]);
        }
    }

    /***
     * A method to determine if a given datagram is at its required destination or
     * not
     * 
     * @return true if the datagram is at its required destination and false if not
     */
    public boolean isFinalStation() {
        boolean result = false;
        String current = currentStation.trim();
        String required = requiredDestination.trim();
        if (current.contains(required)) {
            result = true;
        }

        return result;
    }

    /***
     * A method to seperate the required destination from the GET request sent by
     * the HTML form
     * 
     * @param body the HTML form GET request
     */
    public void separateUserInputs(String body) {
        String[] temp = body.split("(?!^)");
        int startIndex = 0;
        int endIndex = 0;
        for (int i = 1; i < temp.length; i++) {
            if (temp[i].contains("=") && temp[i - 1].contains("o")) {
                startIndex = i + 1;
            }
            if (temp[i].contains("H") && temp[i - 1].contains(" ")) {
                endIndex = i - 1;
            }
        }

        requiredDestination = body.substring(startIndex, endIndex);
        requiredDestination.trim();
    }

    /**
     * A method to add the current stations data (including timetabling) to the
     * outbound datagram
     * 
     * @param path           the path travelled so far by the datagram
     * @param departureTimes the departure times of the datagram so far along its
     *                       path
     * @param arrivalTimes   the arrival times of the datagram so far along its path
     * @param portNumber     the outbound port number
     */
    public void addCurrentStationToDatagram(ArrayList<String> path, ArrayList<String> departureTimes,
            ArrayList<String> arrivalTimes, int portNumber) {
        System.out.println("REACHED C");
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime now = LocalTime.now();
        System.out.println(dtf.format(now));

        // Problem, not necessarily the fastest route. Could be better to leave later
        // for next stop to be shorter?
        path.add(currentStation);
        int index = 0;
        for (int i = 0; i < timetableDepartureTime.size(); i++) {
            String timetableString = timetableDepartureTime.get(i);
            LocalTime time = LocalTime.parse(timetableString, dtf);

            LocalTime currentTime = LocalTime.now();
            String t = currentTime.format(DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime compare = LocalTime.parse(t);

            if (arrivalTimes.size() == 0) {
                if (time.isAfter(compare) && timetablePorts[i] == portNumber) {
                    index = i;
                    System.out.println("Index " + i);
                    break;
                }
            } else if (arrivalTimes.size() > 0) {
                String lastArrivalTime = arrivalTimes.get(arrivalTimes.size() - 1);
                LocalTime nextTime = LocalTime.parse(lastArrivalTime);
                if (time.isAfter(nextTime) && timetablePorts[i] == portNumber) {
                    index = i;
                    break;
                }
            }

        }
        departureTimes.add(timetableDepartureTime.get(index));
        arrivalTimes.add(timetableArrivalTime.get(index));

    }

    /***
     * A method to determine what the depature time of the origin was
     * 
     * @param port the outbound port number
     * @return the origin depature time
     */
    public String determineOriginDepartureTime(int port) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
        LocalTime now = LocalTime.now();
        int index = 0;
        for (int i = 0; i < timetableDepartureTime.size(); i++) {
            String timetableString = timetableDepartureTime.get(i);
            LocalTime time = LocalTime.parse(timetableString, dtf);
            if (time.isAfter(now) && timetablePorts[i] == port) {
                index = i;
                break;
            }
        }
        originDepartureTime = timetableDepartureTime.get(index);
        return originDepartureTime;
    }

    public void fastestTime() {

    }

    /***
     * A method to construct a datgram message in the established protocol format
     * 
     * @param isOutgoing                 whether the datagram is outgoing or
     *                                   incoming
     * @param requiredDestination        the required destination of the datagram
     *                                   message
     * @param originDepartureTime        the depature time from the origin
     * @param numberStationsStoppedAt    the number of stations the datgram has
     *                                   stopped at along its path
     * @param path                       the stations the datagram has stopped at
     *                                   along its path
     * @param departureTimes             the times of depature along the datagram's
     *                                   path
     * @param arrivalTimes               the times of arrival along the datagram's
     *                                   path
     * @param lastNodePort               the port number of the station last stopped
     *                                   at
     * @param hasReachedFinalDestination whether the datagram has reached its final
     *                                   destination or not
     * @param homeStation                the origin of the datagram
     * @return a String in the required datagram format
     */
    public String constructDatagram(boolean isOutgoing, String requiredDestination, String originDepartureTime,
            int numberStationsStoppedAt, ArrayList<String> path, ArrayList<String> departureTimes,
            ArrayList<String> arrivalTimes, int lastNodePort, boolean hasReachedFinalDestination, String homeStation) {
        String result;
        if (isOutgoing) {
            result = "Outgoing \n";
        } else {
            result = "Incoming \n";
        }
        result += requiredDestination + " " + originDepartureTime + " " + numberStationsStoppedAt + " " + lastNodePort
                + " " + hasReachedFinalStation + " " + homeStation + " \n";
        for (int i = 0; i < path.size(); i++) {
            result += path.get(i) + " " + departureTimes.get(i) + " " + arrivalTimes.get(i) + " \n";
        }
        return result;
    }

    /***
     * A method to reset all the message variables as a new message is read in
     */
    public void reset() {
        isOutgoing = true;
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

    /***
     * A method the check that a read in datagram does not have any null values
     * (which indicate an error)
     * 
     * @param message the datagram read in
     * @return true if the datgram has null variables in it
     */
    public boolean datagramHasNull(String message) {
        boolean result = false;
        String temp[] = message.split(" ");
        for (int i = 0; i < temp.length; i++) {
            if (temp[i].contains("null")) {
                result = true;
                break;
            }
        }
        return result;
    }

    /***
     * A method to read a datagram in
     * 
     * @param message the datagram message sent to the station
     */
    public void readDatagramIn(String message) {
        reset();
        // try {
        // readTimetableIn();
        // COME BACK!!!!!!
        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        String temp[] = message.split(" ");
        if (temp[0].contains("Outgoing")) {
            isOutgoing = true;
        } else {
            isOutgoing = false;
        }
        requiredDestination = temp[1];
        originDepartureTime = temp[2];

        String trimString = temp[3];
        trimString.trim();
        numberStationsStoppedAt = Integer.parseInt(trimString);

        trimString = "";

        trimString = temp[4];
        trimString.trim();
        lastNodePort = Integer.parseInt(trimString);

        trimString = "";

        trimString = temp[5];
        trimString.trim();
        if (trimString.contains("true")) {
            hasReachedFinalStation = true;
        } else {
            hasReachedFinalStation = false;
        }

        trimString = "";
        trimString = temp[6];
        trimString.trim();
        homeStation = trimString;

        int pathIndex = 7;
        int departureIndex = 8;
        int arrivalIndex = 9;
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
     * A method to determine the next location of the datagram once it has been read
     * in
     * 
     * @throws IOException should the IO not work
     */
    public void datagramChecks() throws IOException {
        String message;
        System.out.println("Required Station = " + requiredDestination);
        System.out.println("Current Station = " + currentStation);
        for (int i = 0; i < timetablePorts.length; i++) {
            System.out.println(timetablePorts[i]);
        }
        if (isFinalStation() && isOutgoing && !hasReachedFinalStation) { // State 1 - Reached required destination, now
                                                                         // need to travel back
            // to start nod
            isOutgoing = false;
            hasReachedFinalStation = true;
            numberStationsStoppedAt++;

            lastNodePort = receivingDatagram;
            // addCurrentStationToDatagram(path, departureTimes, arrivalTimes);

            for (int i = 0; i < otherStationDatagrams.length; i++) {
                addCurrentStationToDatagram(path, departureTimes, arrivalTimes, otherStationDatagrams[i]);
                message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                        numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort,
                        hasReachedFinalStation, homeStation);
                writeUDP(message, otherStationDatagrams[i]);
            }
        } else if (!isFinalStation() && isOutgoing && !hasReachedFinalStation) { // State 2 - Has not reached required
                                                                                 // destination and need
            // to continue travelling to it
            numberStationsStoppedAt++;

            int oldPort = lastNodePort;
            lastNodePort = receivingDatagram;

            for (int i = 0; i < otherStationDatagrams.length; i++) {
                if (otherStationDatagrams[i] == oldPort) {
                    continue;
                } else {
                    addCurrentStationToDatagram(path, departureTimes, arrivalTimes, otherStationDatagrams[i]);
                    message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                            numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort,
                            hasReachedFinalStation, homeStation);
                    writeUDP(message, otherStationDatagrams[i]);
                }
            }
        } else if (!isFinalStation() && !isOutgoing && hasReachedFinalStation) { // State 3 - Is on the way back to the
                                                                                 // original node but
            // has not returned

            int oldPort = lastNodePort;
            lastNodePort = receivingDatagram;

            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt,
                    path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation);
            for (int i = 0; i < otherStationDatagrams.length; i++) {
                if (otherStationDatagrams[i] == oldPort) {
                    continue;
                } else {
                    writeUDP(message, otherStationDatagrams[i]);
                }
            }
        }
    }

    /***
     * A method to run the core server via Select()
     * 
     * @param webPort               the port to host HTML on
     * @param receivingDatagram     the port receiving datagrams from other stations
     * @param otherStationDatagrams the receiving ports of adjacent stations
     * @throws IOException should IO not work
     */
    public void run(int webPort, int receivingDatagram, int[] otherStationDatagrams) throws IOException {
        readTimetableIn();

        InetSocketAddress listenAddress = new InetSocketAddress(webPort);

        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // bind server socket channel to port
        serverChannel.socket().bind(listenAddress);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);

        // Register Datagram Receipt Port
        DatagramChannel channel = DatagramChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(receivingDatagram));
        channel.register(selector, SelectionKey.OP_READ);

        System.out.println("Reading data from adjacent ports!");
        // Send station names until receieved
        while (!hasReceivedOtherStationNames) {
            sendOtherStationNames();

            int readyCount = selector.select();
            if (readyCount == 0) {
                sendOtherStationNames();
                continue;
            }

            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();

                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isReadable()) {
                    sendOtherStationNames();
                    if (channel.keyFor(selector) == key) {
                        this.readUDP(key);
                    }
                }
            }
        }

        System.out.println("Server started on port >> " + webPort);
        // boolean alreadyWritten = false;
        while (true) {
            // wait for events
            int readyCount = selector.select();
            if (readyCount == 0) {
                continue;
            }

            // process selected keys...
            Set<SelectionKey> readyKeys = selector.selectedKeys();
            Iterator iterator = readyKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = (SelectionKey) iterator.next();

                // Remove key from set so we don't process it twice
                iterator.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) { // Accept client connections
                    if (serverChannel.keyFor(selector) == key) {

                        this.accept(key);
                    }
                } else if (key.isReadable()) { // Read from client
                    if (channel.keyFor(selector) == key) {
                        this.readUDP(key);
                    } else {
                        this.readTCP(key);
                    }
                } else if (key.isWritable()) {
                    if (channel.keyFor(selector) == key) {
                        channel.register(this.selector, SelectionKey.OP_READ);
                        break;
                    }

                    if (hasReachedFinalStation && !isOutgoing && currentStation.contains(homeStation)) {
                        // alreadyWritten = true;
                        this.writeTCP(key);
                        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
                        // break;
                        // break;

                    }
                    // channel.register(this.selector, SelectionKey.OP_READ);
                    // serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);
                    // break;

                    // break;
                }
            }
        }
    }

    /***
     * A method to accept the channel distributed via Select()
     * 
     * @param key the key of the channel with activity
     * @throws IOException
     */
    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);

        channel.register(this.selector, SelectionKey.OP_READ);

        System.out.println("Client... started");
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String date = new Date().toString() + "<br>";
        String message = httpHeader + contentType + "\r\n" + date + "<form method='GET'>"
                + "<input name='to' type='text'/>" + "<input type='submit'/>" + "</form>";
        buffer.put(message.getBytes());
        buffer.flip();
        channel.write(buffer);
        // System.out.println(message);
        buffer.clear();

    }

    /***
     * A method to read TCP activity from the web page
     * 
     * @param key the key of the channel with activity
     * @throws IOException
     */
    private void readTCP(SelectionKey key) throws IOException {

        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int numRead = -1;
        numRead = channel.read(buffer);

        if (numRead == -1) {
            Socket socket = channel.socket();
            SocketAddress remoteAddr = socket.getRemoteSocketAddress();
            System.out.println("Connection closed by client: " + remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data = new byte[numRead];
        System.arraycopy(buffer.array(), 0, data, 0, numRead);
        System.out.println("Got: " + new String(data));
        String result = new String(data);
        separateUserInputs(result);
        channel.register(this.selector, SelectionKey.OP_WRITE);

        // Flood datagrams to all ports
        lastNodePort = receivingDatagram;
        isOutgoing = true;
        homeStation = currentStation;

        for (int i = 0; i < otherStationDatagrams.length; i++) {
            originDepartureTime = determineOriginDepartureTime(otherStationDatagrams[i]);
            String message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                    numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation,
                    homeStation);
            writeUDP(message, otherStationDatagrams[i]);
        }
    }

    /***
     * A method to read a UDP datagram in
     * 
     * @param key the key of the channel with activity
     * @throws IOException if IO does not work
     */
    public void readUDP(SelectionKey key) throws IOException {
        DatagramChannel channel = (DatagramChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        SocketAddress remoteAdd = channel.receive(buffer);

        buffer.flip();
        int limits = buffer.limit();
        byte bytes[] = new byte[limits];
        buffer.get(bytes, 0, limits);
        String msg = new String(bytes);
        System.out.println("Client at " + remoteAdd + "  sent: " + msg);
        // channel.send(buffer, remoteAdd);

        if (msg.startsWith("#")) {
            if (!checkIfPortIsNotUnique(msg)) {
                receievedOtherStationNamesCount++;
                if (receievedOtherStationNamesCount >= otherStationDatagrams.length) {
                    System.out.println("DONE 1");
                    receiveOtherStationNames(msg);

                    hasReceivedOtherStationNames = true;
                } else {
                    receiveOtherStationNames(msg);
                    System.out.println("DONE 2");
                }
            }
        } else {
            if (msg != null && !datagramHasNull(msg) && hasReceivedOtherStationNames)
                readDatagramIn(msg);
            if (hasReachedFinalStation && !isOutgoing && currentStation.contains(homeStation)) {
                channel.register(this.selector, SelectionKey.OP_WRITE);
            } else {
                datagramChecks();
                // channel.register(this.selector, SelectionKey.OP_READ);
            }
        }
    }

    /***
     * A method to write HTML to the TCP port
     * 
     * @param key the key of the channel with activity
     * @throws IOException if IO does not work
     */
    public void writeTCP(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String date = new Date().toString() + "<br>";
        String message = /* httpHeader + contentType + */ "\r\n" + "<br>" + date
                + "<br> <strong> Destination:</strong> " + requiredDestination + "<br> <strong> Origin:</strong> "
                + currentStation
                + "<div> <br> <strong> Route </strong> (Departing Time | Stop | Arrival Time) </div>\n";

        for (int i = 0; i < path.size(); i++) {
            message += "<br>" + "Departing at: " + "<strong>" + departureTimes.get(i) + "</strong>" + " " + "To: "
                    + "<strong>" + path.get(i) + "</strong>" + " " + "Arriving at: " + "<strong>" + arrivalTimes.get(i)
                    + "</strong>";
        }
        message += "<br>";
        message += "________________________________________";
        message += "<br>";
        buffer.put(message.getBytes());
        buffer.flip();
        channel.write(buffer);
        buffer.clear();

    }

    /***
     * A method to send a datagram to a specified port
     * 
     * @param message the message to be sent
     * @param port    the port to send the message to
     * @throws SocketException if IO does not work
     */
    public void writeUDP(String message, int port) throws SocketException {
        if (message != null) {
            DatagramSocket skt;
            try {
                // Sending the Datagram
                skt = new DatagramSocket();
                byte[] b = message.getBytes();
                InetAddress host = InetAddress.getByName("localhost");
                DatagramPacket request = new DatagramPacket(b, b.length, host, port);
                // System.out.println("REACHED 4");
                skt.send(request);
                skt.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

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
            Station station = new Station(origin, webPort, stationDatagrams, otherStationDatagrams);
            station.run(webPort, stationDatagrams, otherStationDatagrams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}