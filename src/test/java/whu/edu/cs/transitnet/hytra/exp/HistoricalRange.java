package whu.edu.cs.transitnet.hytra.exp;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import whu.edu.cs.transitnet.hytra.encoding.Decoder;
import whu.edu.cs.transitnet.hytra.encoding.Encoder;
import whu.edu.cs.transitnet.hytra.util.DateUtil;
import whu.edu.cs.transitnet.hytra.util.GeoUtil;

import java.util.HashMap;
import java.util.HashSet;

public class HistoricalRange {
    static private Logger logger = LoggerFactory.getLogger(HistoricalRange.class);
    static double[] spatial_range = new double[4];
    static HashMap<String, Object> passedParams;

    static long t_length;

    public static void generateQr(HashMap<String, Object> Params, int s_length, long temporal_length) {
        passedParams = Params;
        double[] S = (double[]) Params.get("spatialDomain");
        double lat = (S[0] + S[2]) / 2;
        double lon = (S[1] + S[3]) / 2;
        double lat1 = GeoUtil.increaseLat(lat, s_length);
        double lon1 = GeoUtil.increaseLng(lat,lon,s_length);
        spatial_range = new double[] {lat,lon,lat1,lon1};
        t_length = temporal_length;
    }

    public static void spatial_hytra(HashMap<String, HashMap<Integer, HashSet<String>>> planes){
        //decode spatial range
        long start = System.currentTimeMillis();
        int resolution = (int) passedParams.get("resolution");
        int[] ij_s = Decoder.decodeZ2(Encoder.encodeGrid(spatial_range[0],spatial_range[1]));
        int[] ij_e = Decoder.decodeZ2(Encoder.encodeGrid(spatial_range[2],spatial_range[3]));
        //encode time range
        String day = "2022-06-01";
        int t_s = 3600 * 9, t_e = 3600 * 13;
        double delta_t = 86400 / Math.pow(2, resolution);
        int k_s = (int)(t_s/delta_t), k_e = (int) (t_e/delta_t);

        //union
        HashSet<String> planes_i = new HashSet<>(), planes_j = new HashSet<>(), planes_k = new HashSet<>();
        for(int i = ij_s[0]; i <= ij_e[0]; i++) {
            planes_i.addAll(planes.get(day).get(i));
        }
        for(int j = ij_s[1] + (int) Math.pow(2,resolution); j <= ij_e[1] + (int) Math.pow(2,resolution); j++) {
            planes_j.addAll(planes.get(day).get(j));
        }
        for(int k = k_s + (int) Math.pow(2,resolution+1); k <= k_e + (int) Math.pow(2,resolution+1); k++) {
            planes_k.addAll(planes.get(day).get(k));
        }

        //intersection
        planes_i.retainAll(planes_j);
        planes_i.retainAll(planes_k);
        long end = System.currentTimeMillis();
        logger.info("[RQ Time] (Hytra spatial) --- " + (end-start)/1e3);
        System.out.println(planes_i.size());

    }

    public static void temporal_hytra(HashMap<String, HashMap<Integer, HashSet<String>>> planes) {
        long start = System.currentTimeMillis();
        //固定spatial range
        double[] S = (double[]) passedParams.get("spatialDomain");
        double lat = (S[0] + S[2]) / 2;
        double lon = (S[1] + S[3]) / 2;
        double lat1 = GeoUtil.increaseLat(lat, 3000);
        double lon1 = GeoUtil.increaseLng(lat,lon,3000);
        int[] ij_s = Decoder.decodeZ2(Encoder.encodeGrid(lat,lon));
        int[] ij_e = Decoder.decodeZ2(Encoder.encodeGrid(lat1,lon1));
        //变化temporal range
        long t_s = DateUtil.dateToTimeStamp("2022-06-01 09:00:00");
        long t_e = t_s + t_length;
        String datetime = DateUtil.timestampToDate(t_e);
        int D = Integer.parseInt(datetime.split(" ")[0].split("-")[2]);
        int resolution = (int) passedParams.get("resolution");
        double delta_t = 86400 / Math.pow(2, resolution);
        //
        if(D == 1){
            int k_s = (int)(3600 * 9/delta_t), k_e = (int) (3600 * 9 + t_length/delta_t);
            //union
            HashSet<String> planes_i = new HashSet<>(), planes_j = new HashSet<>(), planes_k = new HashSet<>();
            for(int i = ij_s[0]; i <= ij_e[0]; i++) {
                planes_i.addAll(planes.get("2022-06-01").get(i));
            }
            for(int j = ij_s[1] + (int) Math.pow(2,resolution); j <= ij_e[1] + (int) Math.pow(2,resolution); j++) {
                planes_j.addAll(planes.get("2022-06-01").get(j));
            }
            for(int k = k_s + (int) Math.pow(2,resolution+1); k <= k_e + (int) Math.pow(2,resolution+1); k++) {
                planes_k.addAll(planes.get("2022-06-01").get(k));
            }

            //intersection
            planes_i.retainAll(planes_j);
            planes_i.retainAll(planes_k);
            return;
        }

        HashSet<String> planes_I = new HashSet<>();
        for(int d = 1; d <= D; d++){
            String day;
            if(d < 10) {day = "2022-06-0" + d;}
            else {day = "2022-06-" + d;}
            HashSet<String> planes_i = new HashSet<>(), planes_j = new HashSet<>(), planes_k = new HashSet<>();
            for(int i = ij_s[0]; i <= ij_e[0]; i++) {
                planes_i.addAll(planes.get(day).get(i));
            }
            for(int j = ij_s[1] + (int) Math.pow(2,resolution); j <= ij_e[1] + (int) Math.pow(2,resolution); j++) {
                planes_j.addAll(planes.get(day).get(j));
            }
            for(int k = (int) Math.pow(2,resolution+1); k < (int) 3*Math.pow(2,resolution); k++) {
                planes_k.addAll(planes.get(day).get(k));
            }

            //intersection
            planes_i.retainAll(planes_j);
            planes_i.retainAll(planes_k);
            planes_I.addAll(planes_i);
        }
        long end = System.currentTimeMillis();
        logger.info("[RQ Time] (Hytra spatial) --- " + (end-start)/1e3);
        System.out.println(planes_I.size());
    }

    public static void trajmesa() {

    }
}
