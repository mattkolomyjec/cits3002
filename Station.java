import java.util.ArrayList;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.*;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.Date;

public class Station {

    String origin; // The node where the user started
    String requiredDestination; // The intended destination of the user
    String currentStation; // The name of the current stop
    ArrayList<String> path; // The names of the nodes taken thus far to get to where we are
    ArrayList<String> times; // The times of departure from each node to get where we are thus far, indexed
                             // the same as Path
    String[] destinations; // The names of the destinations available directly from the currentStation

    /*
     * String origin, String requiredDestination, String currentStation,
     * ArrayList<String> path, ArrayList<String> times, String[] destinations)
     * this.origin = origin; this.requiredDestination = requiredDestination;
     * this.currentStation = currentStation; this.path = path; this.times = times;
     * this.destinations = destinations;
     */
    public Station(String origin) {
        this.origin = origin;

    }

    // ###############################################################################
    /**
     * A method to read the timetable data in for a given station and store it in
     * the Station Object variables.
     */
    public void readTimetableIn() {

    }

    // ###############################################################################
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

    // ###############################################################################
    public String sendDatagrams(String origin, String destination, String times) {
        // Each line must have a separate "stop" and the time it reached such a stop
        /*
         * DatagramSocket ds = new DatagramSocket(); String str = "Welcome java";
         * InetAddress ip = InetAddress.getByName("127.0.0.1");
         * 
         * DatagramPacket dp = new DatagramPacket(str.getBytes(), str.length(), ip,
         * 3000); ds.send(dp); ds.close();
         */
        return "";
    }
    // ###############################################################################

    private static final String OUTPUT = "<html><head><title>Example</title></head><body><p>Worked!!!</p></body></html>";
    private static final String OUTPUT_HEADERS = "HTTP/1.1 200 OK\r\n" + "Content-Type: text/html\r\n"
            + "Content-Length: ";
    private static final String OUTPUT_END_OF_HEADERS = "\r\n\r\n";

    public void run(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
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
                // System.out.println(in.readLine());
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
            Station station = new Station(origin);
            station.run(webPort);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
