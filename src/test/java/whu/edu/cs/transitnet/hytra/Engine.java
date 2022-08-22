package whu.edu.cs.transitnet.hytra;



import whu.edu.cs.transitnet.hytra.encoding.Encoder;
import whu.edu.cs.transitnet.hytra.merge.Generator;
import whu.edu.cs.transitnet.hytra.model.Point;
import whu.edu.cs.transitnet.hytra.model.PostingList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Engine {
    static HashMap<String, Object> Params = new HashMap();

    static HashMap<Integer, List<Point>> trajDataBase = new HashMap<>();

    //用于相似度查询
    static HashMap<Integer,List<String>> TG = new HashMap<>();


    public static void main(String[] args) throws IOException {
        //纽约参数
//        Params.put("city","nyc");
//        Params.put("spatialDomain", new double[]{40.502873,-74.252339,40.93372,-73.701241});
//        Params.put("resolution",7);
//        Params.put("separator", "@");
//        Params.put("epsilon", 30);
//        Params.put("dataSize",(int) 1.8e7 * 3);

        //悉尼参数
        Params.put("city","sydney");
        Params.put("spatialDomain", new double[]{-34,150.6,-33.6,151.3});
        Params.put("resolution",8);
        Params.put("separator", "@");
        Params.put("epsilon", 50);
        Params.put("dataSize",(int) 7.2e7);

        Encoder.setup(Params);
        Generator.setup(Params);

        //0：生成配置文件
//        buildTrajDB((String) Params.get("city"), "jun");
//        IndexingTime.setup(trajDataBase,Params);
//        IndexingTime.hytra();
//        System.out.println(PostingList.CP.size());
//        Generator.generateMap();
//        Generator.writeLsmConfig(String.format("/mnt/hyk/config/%s/lsm_config.config",Params.get("city")));
//        Generator.writeKV(String.format("/mnt/hyk/config/%s/put.txt",Params.get("city")));

        //0.1 生成TmC，TG,TC
        build((String) Params.get("city"), "jun");
        writeTC(String.format("/mnt/hyk/sim/%s/tmc_all_in_%d.txt", (String) Params.get("city"),(int) Params.get("resolution")));
//        Generator.generateMap();
//        Generator.writeTCWithCompaction("/mnt/hyk/sim/sydney/tc_all_in_4.txt");
        writeTG(String.format("/mnt/hyk/sim/%s/tg_all_in_%d.txt", (String) Params.get("city"),
                (int) Params.get("resolution")));


        //实验1：indexing time
        //1.1 缓存TrajDatabase（不用构建posting list）
//        buildTrajDB((String) Params.get("city"), "jun");
//        IndexingTime.setup(trajDataBase,Params);
        //1.2 执行xz2+和xzt
//        IndexingTime.xz2Plus();
//        IndexingTime.xzt();
        //1.3 执行hytra
//        IndexingTime.hytra();
        //1.4 执行R-Tree
//        IndexingTime.rtreeTraj();
//        IndexingTime.rtreePoint();

        //实验2：real-time range query
        //2.1：需要先缓存traj database（并构建GT和TlP）
//        buildTrajDB((String) Params.get("city"), "jun");
//        //2.2：设置查询参数：query range
//        RealtimeRange.setup(trajDataBase,Params,5000);
////        //2.3：构建xz2+索引
//        IndexingTime.setup(trajDataBase,Params);
//        HashMap<String, Integer> xz2Plus = IndexingTime.xz2Plus();
////        //2.4：执行trajmesa
//        RealtimeRange.TrajMesa(xz2Plus);
////        //2.5: 执行torch
//        RealtimeRange.torch(PostingList.GT,PostingList.TlP);
//        //2.6: 执行hytra
//        RealtimeRange.hytra(PostingList.GT,PostingList.TlP);
//        //2.7: 构建rtree
//        IndexingTime.setup(trajDataBase,Params);
//        RTree<Integer, com.github.davidmoten.rtree.geometry.Point> rtree = IndexingTime.rtreePoint();
//        //2.8: 执行rtree
//        RealtimeRange.rtree(rtree);

//        //实验3：real-time kNN
//        //3.1：需要先缓存traj database（并构建integer版GT和TG）
//        buildTrajDB((String) Params.get("city"), "jun");
//        //3.2：设置查询参数：query range
//        RealtimekNN.setup(trajDataBase,Params,20);
//        //3.3: 执行hytra
//        RealtimekNN.hytra(PostingList.GT);
//        //3.4: 执行trajmesa
//        RealtimekNN.trajmesa(PostingList.GT, TG);
//        //3.5: 执行torch
//        RealtimekNN.torch(PostingList.GT, TG);
//        //3.6: 构建rtree
//        IndexingTime.setup(trajDataBase,Params);
//        RTree<Integer, com.github.davidmoten.rtree.geometry.Point> rtree = IndexingTime.rtreePoint();
//        //3.7: 执行r-tree
//        RealtimekNN.rtree(rtree);


    }

    public static void build(String city, String tableName){
        Connection conn = null;
        try {
            Class.forName("org.sqlite.JDBC");
            conn = DriverManager.getConnection(String.format("jdbc:sqlite:/mnt/hyk/data/bus/%s/ready/gtfs_rt_%s.db", city, city.substring(0,3)));
            String sql = String.format("select * from %s where pid < %d", tableName,(int)Params.get("dataSize"));
            if((int)Params.get("dataSize") == -1){sql = String.format("select * from %s", tableName);}
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                ResultSet rs = pstmt.executeQuery();
                if (rs.isClosed()) {
                    System.out.println("no result is found!");
                }
                while (rs.next()){
                    int pid = rs.getInt("pid");
                    double lat = rs.getDouble("lat");
                    double lon = rs.getDouble("lon");
                    String datetime = rs.getString("datetime");
                    int tid = rs.getInt("tid");
                    Point p = new Point(pid, lat,lon,datetime,tid);
                    String cid = Encoder.encodeCube(p);
                    //tmc
                    if(!PostingList.TC.containsKey(tid)){
                        PostingList.TC.put(tid, new ArrayList<>());
                        PostingList.TC.get(tid).add(cid);
                    }
                    else{
                        //检查上一个点
                        int size = PostingList.TC.get(tid).size();
                        String last = PostingList.TC.get(tid).get(size - 1);
                        if(!last.equals(cid)){
                            PostingList.TC.get(tid).add(cid);
                        }
                    }
                    //tg
                    String gid = Encoder.encodeGrid(p);
                    if(!TG.containsKey(tid)){
                        TG.put(tid, new ArrayList<>());
                        TG.get(tid).add(gid);
                    }
                    else{
                        //检查上一个点
                        int size = TG.get(tid).size();
                        String last = TG.get(tid).get(size - 1);
                        if(!last.equals(gid)){
                            TG.get(tid).add(gid);
                        }
                    }
                    //ct
//                    if(!PostingList.CT.containsKey(cid)){
//                        PostingList.CT.put(cid, new HashSet<>());
//                    }
//                    PostingList.CT.get(cid).add(tid);
                }
                rs.close();
            } catch (SQLException e) {
                throw new IllegalStateException(e.getMessage());

            }
        } catch ( Exception e ) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        System.out.println("Opened database successfully");
    }

//    public static void buildTrajDB(String city, String tableName){
//        Connection conn = null;
//        try {
//            Class.forName("org.sqlite.JDBC");
//            conn = DriverManager.getConnection(String.format("jdbc:sqlite:/mnt/hyk/data/bus/%s/ready/gtfs_rt_%s.db", city, city.substring(0,3)));
//            int dataSize = (int)Params.get("dataSize");
//            String sql = String.format("select * from %s where pid < %d", tableName,(int)Params.get("dataSize"));
//            if(dataSize == -1){sql = String.format("select * from %s", tableName);}
//            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
//                ResultSet rs = pstmt.executeQuery();
//                if (rs.isClosed()) {
//                    System.out.println("no result is found!");
//                }
//                while (rs.next()){
//                    int pid = rs.getInt("pid");
//                    double lat = rs.getDouble("lat");
//                    double lon = rs.getDouble("lon");
//                    String datetime = rs.getString("datetime");
//                    int tid = rs.getInt("tid");
//                    Point p = new Point(pid, lat,lon,datetime,tid);
//                    if(!trajDataBase.containsKey(tid)){
//                        trajDataBase.put(tid, new ArrayList<Point>());
//                    }
//                    trajDataBase.get(tid).add(p);
//                    int gid = Encoder.encodeGrid(lat,lon);
//                    PostingList.TlP.putIfAbsent(tid,pid);
//                    if(!PostingList.GT.containsKey(gid)){
//                        PostingList.GT.put(gid, new HashSet<Integer>());
//                    }
//                    PostingList.GT.get(gid).add(tid);
//                    if(!TG.containsKey(tid)){
//                        TG.put(tid, new ArrayList<>());
//                        TG.get(tid).add(gid);
//                    }
//                    int size = TG.get(tid).size();
//                    if(gid != TG.get(tid).get(size-1)){
//                        TG.get(tid).add(gid);
//                    }
//
//                }
//                rs.close();
//            } catch (SQLException e) {
//                throw new IllegalStateException(e.getMessage());
//
//            }
//        } catch ( Exception e ) {
//            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
//            System.exit(0);
//        }
//        System.out.println("Opened database successfully");
//    }

    public static void writeTC(String filePath) {
        File f = new File(filePath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(f, false);
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            StringBuilder sb = new StringBuilder();
            PostingList.TC.entrySet().forEach(entry->sb.append(entry).append("\n"));
            writer.write(sb.toString());
            writer.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeTG(String filePath) {
        File f = new File(filePath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(f, false);
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            StringBuilder sb = new StringBuilder();
            TG.entrySet().forEach(entry->sb.append(entry).append("\n"));
            writer.write(sb.toString());
            writer.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void writeTCWithCompaction(String filePath) {
        File f = new File(filePath);
        FileOutputStream out;
        try {
            out = new FileOutputStream(f, false);
            OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");
            StringBuilder sb = new StringBuilder();
            PostingList.TC.entrySet().forEach(entry->sb.append(entry).append("\n"));
            writer.write(sb.toString());
            writer.close();

        }catch (IOException e) {
            e.printStackTrace();
        }
    }

}
