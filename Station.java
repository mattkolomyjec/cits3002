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
import java.util.*;
import java.nio.*;
import java.time.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.time.format.DateTimeFormatter;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.IntStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.Buffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.channels.FileChannel;

public class Station {

    // Other Variables
    private Selector selector;
    public static final String httpHeader = "HTTP/1.1 200 OK\r\n";
    public static final String contentType = "Content-Type: text/html\r\n";
    String datagramClientMessage;

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
    int receivingDatagram;
    int webPort;
    int[] otherStationDatagrams;
    String latitude;
    String longitude;
    ArrayList<String> destinations = new ArrayList<String>();

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

    public boolean isFinalStation() {
        boolean result = false;
        String c = currentStation.trim();
        String r = requiredDestination.trim();
        if (c.equals(r)) {
            result = true;
        }
        return result;
    }

    public void addCurrentStationToDatagram(ArrayList<String> path, ArrayList<String> departureTimes,
            ArrayList<String> arrivalTimes) {
        path.add(currentStation);
        departureTimes.add("TEST");
        arrivalTimes.add("TEST");
    }

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

    public void reset() {
        isOutgoing = false;
        requiredDestination = "";
        originDepartureTime = "";
        numberStationsStoppedAt = 0;
        path.clear();
        departureTimes.clear();
        arrivalTimes.clear();
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

    public void datagramChecks() throws IOException {
        String message;
        if (isFinalStation() && isOutgoing) { // State 1 - Reached required destination, now need to travel back
            // to start node
            isOutgoing = false;
            numberStationsStoppedAt++;
            addCurrentStationToDatagram(path, departureTimes, arrivalTimes);
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt,
                    path, departureTimes, arrivalTimes);
            for (int i = 0; i < otherStationDatagrams.length; i++) {
                writeUDP(message, otherStationDatagrams[i]);
            }
        } else if (!isFinalStation() && isOutgoing) { // State 2 - Has not reached required destination and need
            // to continue travelling to it
            numberStationsStoppedAt++;
            addCurrentStationToDatagram(path, departureTimes, arrivalTimes);
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt,
                    path, departureTimes, arrivalTimes);
            for (int i = 0; i < otherStationDatagrams.length; i++) {
                writeUDP(message, otherStationDatagrams[i]);
            }
        } else if (!isFinalStation() && !isOutgoing) { // State 3 - Is on the way back to the original node but
            // has not returned
            message = constructDatagram(isOutgoing, requiredDestination, originDepartureTime, numberStationsStoppedAt,
                    path, departureTimes, arrivalTimes);
            for (int i = 0; i < otherStationDatagrams.length; i++) {
                writeUDP(message, otherStationDatagrams[i]);
            }
        } else if (isFinalStation() && !isOutgoing) { // State 4 - Has arrived back to the original node and
            // needs to transmit the route to the HTML page.
            readDatagramIn(datagramClientMessage);
            /// !! !!!!!!!!
        }
    }

    public void run(int webPort, int receivingDatagram, int[] otherStationDatagrams) throws IOException {
        InetSocketAddress listenAddress = new InetSocketAddress(webPort);

        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // bind server socket channel to port
        serverChannel.socket().bind(listenAddress);
        serverChannel.register(selector, SelectionKey.OP_ACCEPT); // ONLY REGISTERED FOR OP_ACCEPT

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
                    this.accept(key);

                } else if (key.isReadable()) { // Read from client

                    if (key.channel().equals(channel)) { // If the current channel is the datagram channel, read it in
                        this.readUDP(key);
                    } else if (key.channel().equals(serverChannel)) { // if the current channel is the TCP channel, read
                                                                      // it in
                        // PROBLEM IS THAT THIS IS NOT BEING REACHED
                        System.out.println(key.channel().equals(serverChannel));
                        this.readTCP(key);
                    }
                } else if (key.isWritable()) {
                    System.out.println(key.isWritable());
                    // write data to client...
                    if (key.channel().equals(channel)) { // If the current channel is the datagram channel, read it in
                        // this.writeUDP(message, port);
                    } else if (key.channel().equals(serverChannel)) { // if the current channel is the TCP channel, read
                                                                      // it in
                        this.writeTCP(key);
                    }

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

        /*
         * Register channel with selector for further IO (record it for read/write
         * operations, here we have used read operation)
         */

        // POST HTML FORM TO WEBPORT
        if (socket.getLocalPort() == webPort) {

            channel.register(this.selector, SelectionKey.OP_READ);
            System.out.println("Port = " + socket.getLocalPort());
            System.out.println("Client... started");
            ByteBuffer buffer = ByteBuffer.allocate(1024);
            String date = new Date().toString() + "<br>";
            String message = httpHeader + contentType + "\r\n" + date + "<form method='POST'>"
                    + "<input name='destination' type='text'/>" + "<input type='submit'/>" + "</form>";
            buffer.put(message.getBytes());
            buffer.flip();
            channel.write(buffer);
            System.out.println(message);
            buffer.clear();

        }
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
        System.out.println(data);
        // if is localhost do this
        System.out.println("Got: " + new String(data));

        // if (isFinalStation() && !isOutgoing)

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

    }

    public void writeTCP(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        String date = new Date().toString() + "<br>";
        String message = httpHeader + contentType + "\r\n" + date + "<br> <strong> Destination:</strong> "
                + requiredDestination
                + "<div> <br> <strong> Route </strong> (Departing Stop | Departure Time | Arrival Time) </div>\r\n";

        // for (int i = 0; i < path.size(); i++) {
        // message += "<br>" + path.get(i) + " " + departureTimes.get(i) + " " +
        // arrivalTimes.get(i);
        // }
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
            skt.send(request);

            // Receiveing the Datagram
            /*
             * byte[] buffer = new byte[1000]; DatagramPacket reply = new
             * DatagramPacket(buffer, buffer.length); skt.receive(reply); String result =
             * new String(reply.getData()); datagramClientMessage = result; skt.close();
             */
            skt.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeMessage(String[] messages) throws IOException {

        for (int i = 0; i < messages.length; i++) {
            InetSocketAddress hostAddress = new InetSocketAddress("localhost", 2041);
            SocketChannel client = SocketChannel.open(hostAddress);
            System.out.println("Client... started");
            ByteBuffer buffer = ByteBuffer.allocate(74);
            buffer.put(messages[i].getBytes());
            buffer.flip();
            client.write(buffer);
            System.out.println(messages[i]);
            buffer.clear();
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

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}