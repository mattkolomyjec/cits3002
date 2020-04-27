import java.util.ArrayList;
// import java.net.DatagramSocket;

public class Station {

    String origin; // The node where the user started
    String requiredDestination; // The intended destination of the user
    String currentStation; // The name of the current stop
    ArrayList<String> path; // The names of the nodes taken thus far to get to where we are
    ArrayList<String> times; // The times of departure from each node to get where we are thus far, indexed
                             // the same as Path
    String[] destinations; // The names of the destinations available directly from the currentStation

    public Station(String origin, String requiredDestination, String currentStation, ArrayList<String> path,
            ArrayList<String> times, String[] destinations) {
        this.origin = origin;
        this.requiredDestination = requiredDestination;
        this.currentStation = currentStation;
        this.path = path;
        this.times = times;
        this.destinations = destinations;
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
    /**
     * A used by the station to receive a list of destinations from a requested node
     * 
     * @param origin
     * @return desintations[]
     */
    public String[] receiveNextNodeDestinations(String origin) {
        return null;
    }

    /***
     * A method used by the station to send its list of destinations to another node
     * requesting it
     * 
     * @param destinations
     * @return
     */
    public void sendNextNodeDestinations(String[] destinations) {
        /*
         * DatagramSocket datagramSocket = new DatagramSocket();
         * 
         * byte[] buffer = "0123456789".getBytes(); InetAddress receiverAddress =
         * InetAddress.getLocalHost();
         * 
         * DatagramPacket packet = new DatagramPacket(buffer, buffer.length,
         * receiverAddress, 80); datagramSocket.send(packet);
         */
    }

    // ###############################################################################

    public void sendToNextNode(String origin, String desintation) {

    }

    public Object receieveFromNode() {
        return null;
        // Create Station Object with data sent in
    }

    public void sendToPage() {
        if (isFinalStation()) {
            // Update PathList and TimesList
            // HTML POST TO USER PAGE
        }
    }

    // ###############################################################################
    // throw an exception if incorrect parameters are found
    public static void main(String args[]) {
        String origin = args[0];
        int latitude = Integer.parseInt(args[1]);
        int longitude = Integer.parseInt(args[2]);
        int webPort = Integer.parseInt(args[3]);
        int stationDatagrams = Integer.parseInt(args[4]);

        int[] otherStationDatagrams = new int[args.length - 5];
        int otherIndex = 5;
        for (int i = 0; i < otherStationDatagrams.length; i++) {
            otherStationDatagrams[i] = Integer.parseInt(args[otherIndex]);
            otherIndex++;
        }
        /*
         * System.out.printf("%s\n", origin); System.out.printf("%d\n", latitude);
         * System.out.printf("%d\n", longitude); System.out.printf("%d\n", webPort);
         * System.out.printf("%d\n", stationDatagrams);
         * 
         * for (int i = 0; i < otherStationDatagrams.length; i++) {
         * System.out.printf("%d\n", otherStationDatagrams[i]);
         * 
         * }
         */
    }
}