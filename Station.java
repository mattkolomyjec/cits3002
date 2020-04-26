import java.util.ArrayList;

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
    public static void main(String args[]) {
        String origin = args[1];
        int[] ports = new int[args.length];
        for (int i = 1; i < ports.length; i++) {
            ports[i - 1] = Integer.parseInt(args[i]);
        }

    }
}