import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.util.*;

/**
 * Graph for storing all of the intersection (vertex) and road (edge) information.
 * Uses your GraphBuildingHandler to convert the XML files into a graph. Your
 * code must include the vertices, adjacent, distance, closest, lat, and lon
 * methods. You'll also need to include instance variables and methods for
 * modifying the graph (e.g. addNode and addEdge).
 *
 * @author Alan Yao, Josh Hug
 */
public class GraphDB {
    /** Your instance variables for storing the graph. You should consider
     * creating helper classes, e.g. Node, Edge, etc. */

    /**
     * Example constructor shows how to create and start an XML parser.
     * You do not need to modify this constructor, but you're welcome to do so.
     * @param dbPath Path to the XML file to be parsed.
     */
    private Map<Long, Node> adjacencyList;
    private Map<String, ArrayList<Map<String, Object>>> nameMap;
    private Trie t;

    public GraphDB(String dbPath) {
        try {
            File inputFile = new File(dbPath);
            FileInputStream inputStream = new FileInputStream(inputFile);
            // GZIPInputStream stream = new GZIPInputStream(inputStream);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();
            adjacencyList = new HashMap<>();
            nameMap = new HashMap<>();
            t = new Trie();
            GraphBuildingHandler gbh = new GraphBuildingHandler(this);
            saxParser.parse(inputStream, gbh);
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
        clean();
    }

    /*
        Code to add nodes and edges as GraphBuildingHandler parses XML
     */

    public void addNode(Map<String, String> nodeParams) {
        Node node = new Node(nodeParams.get("lon"), nodeParams.get("lat"), nodeParams.get("name"));
        adjacencyList.put(Long.parseLong(nodeParams.get("id")), node);
        insertIntoNameMap(nodeParams.get("lon"), nodeParams.get("lat"), nodeParams.get("name"), nodeParams.get("id"));

        if (cleanString(nodeParams.get("name")) != "" && cleanString(nodeParams.get("name")) != " ") {
            t.insert(cleanString(nodeParams.get("name")), nodeParams.get("name"));
        }
    }

    public void addEdges(Map<String, ArrayList> edgeParams) {
        ArrayList<String> temp = edgeParams.get("nodes");
        ArrayList<String> name = edgeParams.get("name");


        for (int i = 1; i < temp.size(); ++i) {
            addEdge(Long.parseLong(temp.get(i-1)), Long.parseLong(temp.get(i)), name.get(0));
            addEdge(Long.parseLong(temp.get(i)), Long.parseLong(temp.get(i-1)), name.get(0));
        }

        //Node node = adjacencyList.get(Long.parseLong(temp.get(temp.size() - 1)));
        //node.addWayNameToNode(name.get(0));

    }

    public boolean addEdge(Long id_1, Long id_2, String wayName) {
        Node node = adjacencyList.get(id_1);

        if (node != null) {
            node.addEdge(id_2);

            node.addWayNameToNode(wayName);
            return true;
        }
        return false;
    }

    public ArrayList<Map<String, Object>> locations(String s) {
        return nameMap.get(cleanString(s));
    }

    /**
     * Helper to process strings into their "cleaned" form, ignoring punctuation and capitalization.
     * @param s Input string.
     * @return Cleaned string.
     */
    static String cleanString(String s) {
        return s.replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }

    /**
     *  Remove nodes with no connections from the graph.
     *  While this does not guarantee that any two nodes in the remaining graph are connected,
     *  we can reasonably assume this since typically roads are connected.
     */
    private void clean() {
        // TODO: Your code here.
        Iterator it = adjacencyList.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Object node = (Node) pair.getValue();
            if (((Node) node).edges.size() == 0) {
                it.remove();
            }
        }
    }

    private void insertIntoNameMap(String lon, String lat, String name, String id) {
        Map<String, Object> temp = new HashMap<>();
        temp.put("lat", Double.parseDouble(lat));
        temp.put("lon", Double.parseDouble(lon));
        temp.put("name", name);
        temp.put("id", Long.parseLong(id));

        String cleanName = cleanString(name);
        if (cleanName != "") {
            if (nameMap.get(cleanName) == null) {
                ArrayList<Map<String, Object>> alist = new ArrayList<>();
                alist.add(temp);
                nameMap.put(cleanName, alist);
            } else {
                nameMap.get(cleanName).add(temp);
            }
        }
    }

    /**
     * Returns an iterable of all vertex IDs in the graph.
     * @return An iterable of id's of all vertices in the graph.
     */
    Iterable<Long> vertices() {
        //YOUR CODE HERE, this currently returns only an empty list.
        return adjacencyList.keySet();
    }

    /**
     * Returns ids of all vertices adjacent to v.
     * @param v The id of the vertex we are looking adjacent to.
     * @return An iterable of the ids of the neighbors of v.
     */
    Iterable<Long> adjacent(long v) {
        Node node = adjacencyList.get(v);
        if (node == null)
            return null;
        return node.edges;
    }

    /**
     * Returns the great-circle distance between vertices v and w in miles.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The great-circle distance between the two locations from the graph.
     */
    double distance(long v, long w) {
        return distance(lon(v), lat(v), lon(w), lat(w));
    }

    static double distance(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double dphi = Math.toRadians(latW - latV);
        double dlambda = Math.toRadians(lonW - lonV);

        double a = Math.sin(dphi / 2.0) * Math.sin(dphi / 2.0);
        a += Math.cos(phi1) * Math.cos(phi2) * Math.sin(dlambda / 2.0) * Math.sin(dlambda / 2.0);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return 3963 * c;
    }

    /**
     * Returns the initial bearing (angle) between vertices v and w in degrees.
     * The initial bearing is the angle that, if followed in a straight line
     * along a great-circle arc from the starting point, would take you to the
     * end point.
     * Assumes the lon/lat methods are implemented properly.
     * <a href="https://www.movable-type.co.uk/scripts/latlong.html">Source</a>.
     * @param v The id of the first vertex.
     * @param w The id of the second vertex.
     * @return The initial bearing between the vertices.
     */
    double bearing(long v, long w) {
        return bearing(lon(v), lat(v), lon(w), lat(w));
    }

    static double bearing(double lonV, double latV, double lonW, double latW) {
        double phi1 = Math.toRadians(latV);
        double phi2 = Math.toRadians(latW);
        double lambda1 = Math.toRadians(lonV);
        double lambda2 = Math.toRadians(lonW);

        double y = Math.sin(lambda2 - lambda1) * Math.cos(phi2);
        double x = Math.cos(phi1) * Math.sin(phi2);
        x -= Math.sin(phi1) * Math.cos(phi2) * Math.cos(lambda2 - lambda1);
        return Math.toDegrees(Math.atan2(y, x));
    }

    /**
     * Returns the vertex closest to the given longitude and latitude.
     * @param lon The target longitude.
     * @param lat The target latitude.
     * @return The id of the node in the graph closest to the target.
     */
    long closest(double lon, double lat) {
        double minDistance = Double.MAX_VALUE;
        double currentDistance = 0;
        long closestNodeId = 0;

        Iterator it = adjacencyList.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            Object node = (Node) pair.getValue();
            currentDistance = distance(lon, lat, Double.parseDouble(((Node) node).lon), Double.parseDouble(((Node) node).lat));
            if (currentDistance < minDistance) {
                minDistance = currentDistance;
                closestNodeId = (Long) pair.getKey();
            }
        }

        return closestNodeId;
    }

    /**
     * Gets the longitude of a vertex.
     * @param v The id of the vertex.
     * @return The longitude of the vertex.
     */
    double lon(long v) {
        Node node = adjacencyList.get(v);
        return Double.parseDouble(node.lon);

    }

    /**
     * Gets the latitude of a vertex.
     * @param v The id of the vertex.
     * @return The latitude of the vertex.
     */
    double lat(long v) {
        Node node = adjacencyList.get(v);
        return Double.parseDouble(node.lat);
    }

    /**
     * Gets the way name the vertex is on
     * @param v the id of the vertex
     * @return The way name of the vertex
     */
    String wayName(Long v) {
        Node node = adjacencyList.get(v);
        return node.way_name;
    }

    ArrayList<String> matchingLocations(String prefix) {
        ArrayList<String> alist = new ArrayList<>();
        HashSet<String> temp = (HashSet<String>) t.collect(cleanString(prefix));

        for (String loc : temp)
            alist.add(loc);

        return alist;
    }

    /**
     * Class to represent a single node in the graph which consists of
     * a longitude, latitude, name (optional), way name (optional), and
     * a list of adjacent vertices.
     */
    private class Node {

        private String lon;
        private String lat;
        private String name;
        private String way_name;
        private ArrayList edges = new ArrayList<>();

        public Node(String lon, String lat, String name) {
            this.lon = lon;
            this.lat = lat;
            this.name = name;
        }

        public void addWayNameToNode(String way_name) {
            if (way_name != "")
                this.way_name = way_name;
            else
                this.way_name = "unknown road";
        }

        public void addEdge(Long id) {
            edges.add(id);
        }
    }

    private static class Trie {

        static final int SIZE = 26;

        static TrieNode root;

        Trie() {
            root = new TrieNode();
        }

        static void insert(String key, String value) {
            TrieNode temp = root;
            int size = key.length();
            int index;

            for (int i = 0; i < size; i++) {
                if (key.charAt(i) == ' ')
                    continue;

                index = (int) key.charAt(i) - 'a';
                if (temp.children[index] == null) {
                    temp.children[index] = new TrieNode();
                }
                temp = temp.children[index];
            }
            temp.isEndOfWord = true;
            temp.fullName = value;
        }

        static Set<String> collect(String prefix) {
            System.out.println(prefix);
            Set<String> matches = new HashSet<>();

            TrieNode temp = root;
            int size = prefix.length();
            int index;

            for (int i = 0; i < size - 1; i++) {
                if (prefix.charAt(i) == ' ')
                    continue;

                index = prefix.charAt(i) - 'a';

                if(temp.children[index] == null)
                    return matches;
                temp = temp.children[index];
            }
            index = size - 1;
            if (temp.children[index] == null)
                return matches;
            else {
                if (temp.isEndOfWord)
                    matches.add(prefix);

                temp = temp.children[index];
            }

            collectHelper(temp, matches);

            return matches;
        }

        static void collectHelper(TrieNode root, Set<String> matches) {
            System.out.println("Helper Called");
            for (int i = 0; i < root.children.length; i++) {

                if (root.isEndOfWord)
                    matches.add(root.fullName);

                if (root.children[i] == null)
                    continue;

                collectHelper(root.children[i], matches);
            }
        }

        private static class TrieNode {
            TrieNode[] children = new TrieNode[SIZE];
            boolean isEndOfWord;
            String fullName;

            TrieNode() {
                for (TrieNode node : children)
                    node = null;
                isEndOfWord = false;
            }
        }
    }


}
