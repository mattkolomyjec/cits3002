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

public class Station {

    // Message Variables
    String requiredDestination; // The intended destination of the user. WILL CAPS LOCK AFFECT THIS?
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
    ArrayList<String> destinations = new ArrayList<String>();

    // Other Variables
    private Selector selector;
    public static final String httpHeader = "HTTP/1.1 200 OK\r\n";
    public static final String contentType = "Content-Type: text/html\r\n";
    String datagramClientMessage;

    public Station(String currentStation, int webPort, int receivingDatagram, int[] otherStationDatagrams) {
        this.webPort = webPort;
        this.currentStation = currentStation;
        this.receivingDatagram = receivingDatagram;
        this.otherStationDatagrams = otherStationDatagrams;
    }

    public void readTimetableIn() throws FileNotFoundException {
        File file = new File("routes.txt");
        Scanner sc = new Scanner(file);

        String temp[] = ((sc.nextLine()).split(","));
        // currentStation = temp[0];
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

    public boolean isFinalStation() {
        boolean result = false;
        String c = currentStation.trim();
        String r = requiredDestination.trim();
        if (c.contains(r)) {
            result = true;
        }

        return result;
    }

    // GET /?to=Warwick-Stn HTTP/1.1
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

    public void addCurrentStationToDatagram(ArrayList<String> path, ArrayList<String> departureTimes,
            ArrayList<String> arrivalTimes) {
        path.add(currentStation);
        departureTimes.add("TEST");
        arrivalTimes.add("TEST");
    }

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

    public void datagramChecks() throws IOException {
        String message;
        if (isFinalStation() && isOutgoing && !hasReachedFinalStation) { // State 1 - Reached required destination, now
                                                                         // need to travel back
            // to start node
            System.out.println("REACHED 3");
            isOutgoing = false;
            hasReachedFinalStation = true;
            numberStationsStoppedAt++;

            lastNodePort = receivingDatagram;
            addCurrentStationToDatagram(path, departureTimes, arrivalTimes);

            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt,
                    path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation);
            for (int i = 0; i < otherStationDatagrams.length; i++) {
                System.out.println("Node sent to =" + otherStationDatagrams[i]);
                writeUDP(message, otherStationDatagrams[i]);
            }
        } else if (!isFinalStation() && isOutgoing && !hasReachedFinalStation) { // State 2 - Has not reached required
                                                                                 // destination and need
            // to continue travelling to it
            numberStationsStoppedAt++;
            System.out.println("REACHED 4");

            int oldPort = lastNodePort;
            lastNodePort = receivingDatagram;

            addCurrentStationToDatagram(path, departureTimes, arrivalTimes);

            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt,
                    path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation, homeStation);
            for (int i = 0; i < otherStationDatagrams.length; i++) {
                if (otherStationDatagrams[i] == oldPort) {
                    continue;
                } else {
                    writeUDP(message, otherStationDatagrams[i]);
                }
            }
        } else if (!isFinalStation() && !isOutgoing && hasReachedFinalStation) { // State 3 - Is on the way back to the
                                                                                 // original node but
            // has not returned
            System.out.println("REACHED 5");

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

    public void run(int webPort, int receivingDatagram, int[] otherStationDatagrams) throws IOException {
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

        System.out.println("Server started on port >> " + webPort);

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

    // accept client connection
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
        System.out.println(message);
        buffer.clear();

    }

    // read from the socket channel
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
        String message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime,
                numberStationsStoppedAt, path, departureTimes, arrivalTimes, lastNodePort, hasReachedFinalStation,
                homeStation);
        for (int i = 0; i < otherStationDatagrams.length; i++) {
            writeUDP(message, otherStationDatagrams[i]);
        }
    }

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
        readDatagramIn(msg);
        System.out.println(msg);

        if (hasReachedFinalStation && !isOutgoing && currentStation.contains(homeStation)) {
            System.out.println("NiHao");
            channel.register(this.selector, SelectionKey.OP_WRITE);
        } else {
            datagramChecks();
            // channel.register(this.selector, SelectionKey.OP_READ);
        }

    }

    // POST request?
    public void writeTCP(SelectionKey key) throws IOException {
        System.out.println("FOUND 2");
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String date = new Date().toString() + "<br>";
        String message = /* httpHeader + contentType + */ "\r\n" + "<br>" + date
                + "<br> <strong> Destination:</strong> " + requiredDestination
                + "<div> <br> <strong> Route </strong> (Departing Stop | Departure Time | Arrival Time) </div>\n";

        for (int i = 0; i < path.size(); i++) {
            message += "<br>" + path.get(i) + " " + departureTimes.get(i) + " " + arrivalTimes.get(i);
        }
        message += "<br>";
        message += "________________________________________";
        message += "<br>";
        buffer.put(message.getBytes());
        buffer.flip();
        channel.write(buffer);
        System.out.println(message);
        buffer.clear();

    }

    public void writeUDP(String message, int port) throws SocketException {
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
            station.readTimetableIn();
            station.run(webPort, stationDatagrams, otherStationDatagrams);

            // station.currentStation = "North_Terminus";
            // station.requiredDestination = "North_Terminus";
            // System.out.println(station.isFinalStation());
            // station.isOutgoing = true;
            // station.requiredDestination = "North_Terminus";
            // station.originDepartureTime = "9:00";
            // station.numberStationsStoppedAt = 2;
            // station.lastNodePort = 4002;
            // station.hasReachedFinalStation = true;

            // ArrayList<String> destinations = new ArrayList<String>();
            // ArrayList<String> departureTimes = new ArrayList<String>();
            // ArrayList<String> arrivalTimes = new ArrayList<String>();
            // destinations.add("Subiaco");
            // destinations.add("Thornlie");
            // departureTimes.add("09:00");
            // departureTimes.add("09:45");
            // arrivalTimes.add("09:10");
            // arrivalTimes.add("10:00");

            // String testDatagram = station.constructDatagram(station.isOutgoing,
            // station.requiredDestination,
            // station.originDepartureTime, station.numberStationsStoppedAt, destinations,
            // departureTimes,
            // arrivalTimes, station.lastNodePort, station.hasReachedFinalStation);
            // System.out.println("TEST" + testDatagram);

            // station.reset();

            // station.readDatagramIn(testDatagram);
            // System.out.println("isOutgoing =" + station.isOutgoing);
            // System.out.println("requiredDestination =" + station.requiredDestination);
            // System.out.println("originDepartureTime =" + station.originDepartureTime);
            // System.out.println("numberStationsStoppedAt =" +
            // station.numberStationsStoppedAt);
            // System.out.println("lastNodePort =" + station.lastNodePort);
            // System.out.println("hasReachedFinalStation =" +
            // station.hasReachedFinalStation);

            // station.currentStation = "North_Terminus";
            // System.out.println("isFinalStation" + station.isFinalStation());
            // for (int i = 0; i < station.destinations.size(); i++) {
            // System.out.println("destinations =" + station.destinations.get(i));
            // System.out.println("departureTimes =" + station.departureTimes.get(i));
            // System.out.println("arrivalTimes =" + station.arrivalTimes.get(i));
            // }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}