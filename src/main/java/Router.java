import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class provides a shortestPath method for finding routes between two points
 * on the map. Start by using Dijkstra's, and if your code isn't fast enough for your
 * satisfaction (or the autograder), upgrade your implementation by switching it to A*.
 * Your code will probably not be fast enough to pass the autograder unless you use A*.
 * The difference between A* and Dijkstra's is only a couple of lines of code, and boils
 * down to the priority you use to order your vertices.
 */
public class Router {

    static private Map<Long, Double> bestKnownDistance;
    static private Map<Long, Long> parent;
    static private PriorityQueue<Pair> fringe;

    static  {
        bestKnownDistance = new HashMap<>();
        parent = new HashMap<>();
        fringe = new PriorityQueue<>(1, new PairComparator());
    }


    /**
     * Return a List of longs representing the shortest path from the node
     * closest to a start location and the node closest to the destination
     * location.
     * @param g The graph to use.
     * @param stlon The longitude of the start location.
     * @param stlat The latitude of the start location.
     * @param destlon The longitude of the destination location.
     * @param destlat The latitude of the destination location.
     * @return A list of node id's in the order visited on the shortest path.
     */
    public static List<Long> shortestPath(GraphDB g, double stlon, double stlat,
                                          double destlon, double destlat) {

        ArrayList<Long> shortestPath = new ArrayList<>();

        Long startNodeId = g.closest(stlon, stlat);
        Long endNodeId = g.closest(destlon, destlat);

        //  Clear all the previous data
        fringe.clear();
        bestKnownDistance.clear();
        parent.clear();

        //  Initialize the fringe with the source vertex
        fringe.add(new Pair(startNodeId, g.distance(startNodeId, endNodeId)));
        bestKnownDistance.put(startNodeId, 0.0);
        parent.put(startNodeId, null);

        boolean found = false;

        if (startNodeId != endNodeId) {
            while (!fringe.isEmpty()) {
                Pair p = fringe.poll();
                Long currentId = p.id;

                ArrayList<Long> adjacent = (ArrayList<Long>) g.adjacent(currentId);

                for (Long vertex : adjacent) {
                    if (vertex == endNodeId) {
                        bestKnownDistance.put(vertex, bestKnownDistance.get(currentId) + g.distance(currentId, vertex));
                        parent.put(vertex, currentId);
                        found = true;
                        break;
                    }
                    //  Relax the edge(/vertex)

                    if (bestKnownDistance.get(vertex) == null || bestKnownDistance.get(vertex) > bestKnownDistance.get(currentId) + g.distance(currentId, vertex)) {
                        bestKnownDistance.put(vertex, bestKnownDistance.get(currentId) + g.distance(currentId, vertex));
                        parent.put(vertex, currentId);
                        fringe.add(new Pair(vertex, bestKnownDistance.get(currentId) + g.distance(currentId, vertex) + g.distance(vertex, endNodeId)));
                    }
                }

                if (found)
                    break;
            }
        }

        shortestPath.add(endNodeId);
        Long temp = parent.get(endNodeId);

        while (temp != null) {
            shortestPath.add(temp);
            temp = parent.get(temp);
        }

        Collections.reverse(shortestPath);

        return shortestPath;
    }

    /**
     * Create the list of directions corresponding to a route on the graph.
     * @param g The graph to use.
     * @param route The route to translate into directions. Each element
     *              corresponds to a node from the graph in the route.
     * @return A list of NavigationDirection objects corresponding to the input
     * route.
     */
    public static List<NavigationDirection> routeDirections(GraphDB g, List<Long> route) {

        ArrayList<NavigationDirection> routeDirections = new ArrayList<>();

        NavigationDirection dir = new NavigationDirection();
        dir.direction = 0;
        dir.distance = 0;
        dir.way = g.wayName(route.get(0));


        int counter = 0;

        for (int i = 1; i < route.size(); i++) {
            double relativeBearing = g.bearing(route.get(i-1), route.get(i));
            String currentWay = g.wayName(route.get(i));

            if ( ! currentWay.equals(dir.way) ) {

                if (dir.distance != 0)
                    routeDirections.add(dir);

                dir = new NavigationDirection();
                dir.way = g.wayName(route.get(i));
                dir.distance = 0;

                if (relativeBearing >= -15 && relativeBearing <= 15)
                    dir.direction = 1;
                else if (relativeBearing < -15 && relativeBearing >= -30)
                    dir.direction = 2;
                else if (relativeBearing > 15 && relativeBearing <= 30)
                    dir.direction = 3;
                else if (relativeBearing < -30 && relativeBearing >= -100)
                    dir.direction = 5;
                else if (relativeBearing > 30 && relativeBearing <= 100)
                    dir.direction = 4;
                else if (relativeBearing < -100)
                    dir.direction = 6;
                else if (relativeBearing > 100)
                    dir.direction = 7;
            }
            else {
                dir.distance += g.distance(route.get(i), route.get(i-1));
            }
        }

        routeDirections.add(dir);
        return routeDirections;
    }


    /**
     * Class to represent a pair associating an id with a priority for
     * the fringe.
     */
    private static class Pair {
        Long id;
        Double priority;

        Pair (Long id, Double priority) {
            this.id = id;
            this.priority = priority;
        }
    }

    private static class PairComparator implements Comparator<Pair> {
        public int compare(Pair p1, Pair p2) {
            if (p1.priority > p2.priority)
                return 1;
            else if (p1.priority < p2.priority)
                return -1;
            return 0;
        }
    }

    /**
     * Class to represent a navigation direction, which consists of 3 attributes:
     * a direction to go, a way, and the distance to travel for.
     */
    public static class NavigationDirection {

        /** Integer constants representing directions. */
        public static final int START = 0;
        public static final int STRAIGHT = 1;
        public static final int SLIGHT_LEFT = 2;
        public static final int SLIGHT_RIGHT = 3;
        public static final int RIGHT = 4;
        public static final int LEFT = 5;
        public static final int SHARP_LEFT = 6;
        public static final int SHARP_RIGHT = 7;

        /** Number of directions supported. */
        public static final int NUM_DIRECTIONS = 8;

        /** A mapping of integer values to directions.*/
        public static final String[] DIRECTIONS = new String[NUM_DIRECTIONS];

        /** Default name for an unknown way. */
        public static final String UNKNOWN_ROAD = "unknown road";
        
        /** Static initializer. */
        static {
            DIRECTIONS[START] = "Start";
            DIRECTIONS[STRAIGHT] = "Go straight";
            DIRECTIONS[SLIGHT_LEFT] = "Slight left";
            DIRECTIONS[SLIGHT_RIGHT] = "Slight right";
            DIRECTIONS[LEFT] = "Turn left";
            DIRECTIONS[RIGHT] = "Turn right";
            DIRECTIONS[SHARP_LEFT] = "Sharp left";
            DIRECTIONS[SHARP_RIGHT] = "Sharp right";
        }

        /** The direction a given NavigationDirection represents.*/
        int direction;
        /** The name of the way I represent. */
        String way;
        /** The distance along this way I represent. */
        double distance;

        /**
         * Create a default, anonymous NavigationDirection.
         */
        public NavigationDirection() {
            this.direction = STRAIGHT;
            this.way = UNKNOWN_ROAD;
            this.distance = 0.0;
        }

        public String toString() {
            return String.format("%s on %s and continue for %.3f miles.",
                    DIRECTIONS[direction], way, distance);
        }

        /**
         * Takes the string representation of a navigation direction and converts it into
         * a Navigation Direction object.
         * @param dirAsString The string representation of the NavigationDirection.
         * @return A NavigationDirection object representing the input string.
         */
        public static NavigationDirection fromString(String dirAsString) {
            String regex = "([a-zA-Z\\s]+) on ([\\w\\s]*) and continue for ([0-9\\.]+) miles\\.";
            Pattern p = Pattern.compile(regex);
            Matcher m = p.matcher(dirAsString);
            NavigationDirection nd = new NavigationDirection();
            if (m.matches()) {
                String direction = m.group(1);
                if (direction.equals("Start")) {
                    nd.direction = NavigationDirection.START;
                } else if (direction.equals("Go straight")) {
                    nd.direction = NavigationDirection.STRAIGHT;
                } else if (direction.equals("Slight left")) {
                    nd.direction = NavigationDirection.SLIGHT_LEFT;
                } else if (direction.equals("Slight right")) {
                    nd.direction = NavigationDirection.SLIGHT_RIGHT;
                } else if (direction.equals("Turn right")) {
                    nd.direction = NavigationDirection.RIGHT;
                } else if (direction.equals("Turn left")) {
                    nd.direction = NavigationDirection.LEFT;
                } else if (direction.equals("Sharp left")) {
                    nd.direction = NavigationDirection.SHARP_LEFT;
                } else if (direction.equals("Sharp right")) {
                    nd.direction = NavigationDirection.SHARP_RIGHT;
                } else {
                    return null;
                }

                nd.way = m.group(2);
                try {
                    nd.distance = Double.parseDouble(m.group(3));
                } catch (NumberFormatException e) {
                    return null;
                }
                return nd;
            } else {
                // not a valid nd
                return null;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof NavigationDirection) {
                return direction == ((NavigationDirection) o).direction
                    && way.equals(((NavigationDirection) o).way)
                    && distance == ((NavigationDirection) o).distance;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(direction, way, distance);
        }
    }
}
