package whu.edu.cs.transitnet.index;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import whu.edu.cs.transitnet.realtime.Entity;

@Component
public class Encoder {

    double[] S; //x_min, y_min, x_min, y_max (默认纽约)

    int RESOLUTION;
    private double deltaX;
    private double deltaY;
    private double deltaT;
    private String split = "@";

    public Encoder( @Value("${nyc.boundingbox}") double[] s,  @Value("${nyc.resolution}")int RESOLUTION) {
        S = s;
        this.RESOLUTION = RESOLUTION;
        deltaX = (S[2] - S[0]) / Math.pow(2,RESOLUTION);
        deltaY = (S[3] - S[1]) / Math.pow(2,RESOLUTION);
        deltaT =  3600 * 24 / Math.pow(2,RESOLUTION);
    }

    public int getRESOLUTION() {
        return RESOLUTION;
    }

    //    /**
//     * 加载配置
//     * @param Params
//     */
//    public static void setup(HashMap<String, Object> Params) {
//        RESOLUTION = (int) Params.get("resolution");
//        S = (double[]) Params.get("spatialDomain");
//        deltaX = (S[2] - S[0]) / Math.pow(2,RESOLUTION);
//        deltaY = (S[3] - S[1]) / Math.pow(2,RESOLUTION);
//        deltaT = 3600 * 24 / Math.pow(2,RESOLUTION);
//        split = (String) Params.get("separator");
//    }

//    /**
//     * 对输入的point进行cube encoding （D+Z+L）
//     * @param p
//     * @return
//     */
//    public static String encodeCube(Point p) {
//        String[] date_time = p.getDatetime().split(" ");
//        String[] hour_min_sec = date_time[1].split(":");
//        double t = Integer.parseInt(hour_min_sec[0]) * 3600 + Integer.parseInt(hour_min_sec[1]) * 60 + Integer.parseInt(hour_min_sec[2]);
//        int i = (int) ((p.getLat()-S[0]) / deltaX), j = (int) ((p.getLon()-S[1]) / deltaY), k = (int) (t/deltaT);
//        return date_time[0] + split + combine3(i,j,k,RESOLUTION) + split + "0";
//    }
//
//    /**
//     * 对输入的point进行grid encoding (D+Z)
//     * @param p
//     * @return
//     */
//    public static String encodeGrid(Point p) {
//        String[] date_time = p.getDatetime().split(" ");
//        int i = (int) ((p.getLat()-S[0]) / deltaX), j = (int) ((p.getLon()-S[1]) / deltaY);
//        return date_time[0] + split + combine2(i,j,RESOLUTION);
//    }

    public String encodeGrid(String datetime, double lat, double lon) {
        String[] date_time = datetime.split(" ");
        int i = (int) ((lat-S[0]) / deltaX), j = (int) ((lon-S[1]) / deltaY);
        return date_time[0] + split + combine2(i,j);
    }

    /**
     * 对输入的经纬度进行grid encoding (Z)
     * @param lat
     * @param lon
     * @return
     */
    public int getGid(float lat, float lon) {
        int i = (int) ((lat-S[0]) / deltaX), j = (int) ((lon-S[1]) / deltaY);
        return combine2(i,j);
    }

    public int getGid(Entity entity) {
        float lat = entity.getLat();
        float lon = entity.getLon();
        int i = (int) ((lat-S[0]) / deltaX), j = (int) ((lon-S[1]) / deltaY);
        return combine2(i,j);
    }


    public int[] getGridCoordinate(float lat, float lon) {
        int i = (int) ((lat-S[0]) / deltaX), j = (int) ((lon-S[1]) / deltaY);
        return new int[]{i,j};
    }

    public int encodeGrid(double lat, double lon) {
        int i = (int) ((lat-S[0]) / deltaX), j = (int) ((lon-S[1]) / deltaY);
        return combine2(i,j);
    }


    /**
     * 三维Z-curve编码
     * @param aid
     * @param bid
     * @param cid
     * @param lengtho
     * @return
     */
    public int combine3(int aid, int bid, int cid, int lengtho){
        int length = lengtho;
        int[] a =new int[length];
        int[] b =new int[length];
        int[] c =new int[length];
        //convert aid,bid and cid to binary bits
        while(length-- >= 1){
            a[length] = aid%2;
            aid /=2;
            b[length] = bid%2;
            bid /=2;
            c[length] = cid%2;
            cid /=2;
        }
        //
        int com[] = new int[3*lengtho];
        for(int i = 0; i<lengtho; i++){
            com[3*i]= a[i];
            com[3*i+1] = b[i];
            com[3*i+2] = c[i];
        }
        return bitToint(com, 3*lengtho);
    }

    /**
     * 二维Z-curve编码
     * @param aid
     * @param bid
     * @return
     */
    public int combine2(int aid, int bid){
        int length = RESOLUTION;
        int[] a =new int[length];
        int[] b =new int[length];
        //convert aid,bid and cid to binary bits
        while(length-- >= 1){
            a[length] = aid%2;
            aid /=2;
            b[length] = bid%2;
            bid /=2;
        }
        //
        int com[] = new int[2*RESOLUTION];
        for(int i = 0; i<RESOLUTION; i++){
            com[2*i]= a[i];
            com[2*i+1] = b[i];
        }
        return bitToint(com, 2*RESOLUTION);
    }

    public int bitToint(int[] a, int length){
        int sum = 0;
        for(int i=0; i<length; i++){
            sum += a[i]*Math.pow(2, length-i-1);
        }
        return sum;
    }

    public int bitToint(String bits){
        int sum = 0;
        int length = bits.length();
        for(int i = 0; i < length; i++){
            sum += Integer.parseInt(String.valueOf(bits.charAt(i))) * Math.pow(2, length - i-1);
        }
        return sum;
    }

    /**
     * 将十进制数转换为一定位数的二进制，并在高位补零
     * @param number 十进制数
     * @param digits 位数
     * @return 二进制数
     */
    public String padding(int number, int digits){
        StringBuilder bits = new StringBuilder(Integer.toBinaryString(number));
        for(int i = 0; i < digits-bits.length();i++){
            bits.insert(0, "0");
        }
        return bits.toString();
    }



}
