import java.util.HashMap;
import java.util.Map;


/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {

    /*
        To-Do: Break getMapRaster() into multiple functions for readability of code and add error handling.
     */

    private String[][] renderGrid;
    private static  double[] lonDPPs;   //  Resolution at each of the 8 depth levels. Pre-calculated.

    static {
        lonDPPs = new double[]{
                0.00034332275390625, 0.000171661376953125, 0.0000858306884765625, 0.00004291534423828125,
                0.000021457672119140625, 0.000010728836059570312, 0.000005364418029785156, 0.00000268220901489258
                };
    }

    public Rasterer() {

    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {

        /*
            To Do: Break into multiple functions. Hard to read.

         */

        double currentLonDPP = getLonDPP(params.get("lrlon"), params.get("ullon"), params.get("w"));
        int currentDepth = getDepthLevel(currentLonDPP);


        //  Get longitude bounds for the current depth
        double prev = MapServer.ROOT_ULLON;
        double next = calculateNext(currentDepth,MapServer.ROOT_ULLON, MapServer.ROOT_LRLON);
        double stride = next - prev;

        int lon_start = 0;
        int lon_end = 0;
        double raster_ul_lon = 0.0;
        double raster_lr_lon = 0.0;


        int numberOfImgs = (int) (Math.pow(2, currentDepth) - 1);

        //  Get raster_ul_lon for current depth
        for (int i = 0; i <= numberOfImgs; i++) {
            if (params.get("ullon") >= prev && params.get("ullon") <= next) {
                lon_start = i;
                raster_ul_lon = prev;
                break;
            }
            else {
                prev = next;
                next += stride;
            }
        }

        //  Get raster_lr_lon for the current depth
        for (int i = lon_start; i <= numberOfImgs; i++) {
            if (params.get("lrlon") >= prev && params.get("lrlon") <= next) {

                lon_end = i;
                raster_lr_lon = next;
                break;
            }
            else {
                prev = next;
                next += stride;
            }
        }

        //  Get latitude bounds for the current depth
        prev = MapServer.ROOT_ULLAT;
        next = calculateNext(currentDepth, MapServer.ROOT_ULLAT, MapServer.ROOT_LRLAT);
        stride = prev - next;

        int lat_start = 0;
        int lat_end = 0;
        double raster_ul_lat = 0.0;
        double raster_lr_lat = 0.0;

        //  Get raster_ul_lat
        for (int j = 0; j <= numberOfImgs; j++) {
            if (params.get("ullat") <= prev && params.get("ullat") >= next) {
                lat_start = j;
                raster_ul_lat = prev;
                break;
            }
            else {
                prev = next;
                next -= stride;
            }
        }

        //  Get raster_lr_lat
        for (int j = lat_start; j <= numberOfImgs; j++) {
            if (params.get("lrlat") <= prev && params.get("lrlat") >= next) {
                lat_end = j;
                raster_lr_lat = next;
                break;
            }
            else {
                prev = next;
                next -= stride;
            }
        }

        renderGrid = new String[lat_end - lat_start + 1][lon_end - lon_start + 1];
        int r_pos = 0;

        //  Build the render grid containing the appropriate file names
        for (int i = lat_start; i <= lat_end; i++) {
            String[] row = new String[lon_end - lon_start + 1];
            int c_pos = 0;
            for (int j = lon_start; j <= lon_end; j++) {
                String temp = buildString(i, j, currentDepth);
                row[c_pos++] = temp;
            }
            renderGrid[r_pos++] = row;
        }

        Map<String, Object> results = new HashMap<>();

        results.put("render_grid", renderGrid);
        results.put("raster_ul_lon", raster_ul_lon);
        results.put("raster_ul_lat", raster_ul_lat);
        results.put("raster_lr_lon", raster_lr_lon);
        results.put("raster_lr_lat", raster_lr_lat);
        results.put("depth", currentDepth);
        results.put("query_success", true);

        return results;
    }

    private double getLonDPP(Double lrlon, Double ullon, Double w) {
        return (lrlon.doubleValue() - ullon.doubleValue()) / w.doubleValue();
    }

    private int getDepthLevel(double currentLonDPP) {
        for (int i = 0; i < lonDPPs.length; i++) {
            if (lonDPPs[i] <= currentLonDPP)
                return i;
        }
        return 7;
    }

    private double calculateNext(int currentDepth, double s, double e) {
        double end = e;
        double temp = end;

        for (int i = 1; i <= currentDepth; i++) {
            temp = (s + end) / 2;
            end = temp;
        }
        return temp;
    }

    private String buildString(int i, int j, int d) {
        String temp = "d" + d + "_x" + j + "_y" + i + ".png";
        return temp;
    }
}