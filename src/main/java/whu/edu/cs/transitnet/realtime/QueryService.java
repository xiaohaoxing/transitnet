package whu.edu.cs.transitnet.realtime;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import whu.edu.cs.transitnet.index.Decoder;
import whu.edu.cs.transitnet.index.Encoder;
import whu.edu.cs.transitnet.index.PostingList;
import whu.edu.cs.transitnet.utils.GeoUtil;
import java.util.*;

@Slf4j
@Service
public class QueryService {

    @Autowired
     PostingList postingList;

    @Autowired
    Encoder encoder;

    @Autowired
    Decoder decoder;

    @Transactional
    public HashSet<Entity> rtRangeSearch(float minLat, float minLon, float maxLat, float maxLon){

        HashSet<Entity> res = new HashSet<>();

        int[] minCrdnt = decoder.getGridCoordinate(minLat,minLon);
        int[] maxCrdnt = decoder.getGridCoordinate(maxLat,maxLon);

        //k: gid, v:过期的tid
        HashMap<Integer,HashSet<String>> expired = new HashMap<>();
        //完全包括
        for (int i = minCrdnt[0] + 1; i < maxCrdnt[0]; i++) {
            for (int j = minCrdnt[1] + 1; j < maxCrdnt[1]; j++) {
                int gid = decoder.combine2(i,j);
                HashSet<String> canTids = postingList.getTidsByGid(gid);
                if(canTids == null){continue;}
                for (String tid : canTids){
                    //检查是否过期
                    if(postingList.isExpired(tid)){
                        HashSet<String> tids = expired.getOrDefault(gid, new HashSet<>());
                        tids.add(tid);
                        expired.putIfAbsent(gid,tids);
                    }
                    else{
                        Entity entity = postingList.getEntityByTid(tid);
                        if(entity != null){
                            res.add(entity);
                        }
                    }
                }
            }
        }

        //边界
        HashSet<Integer> border = new HashSet<>();
        for (int i = minCrdnt[0]; i <= maxCrdnt[0]; i++) {
            border.add(decoder.combine2(i,minCrdnt[1]));
            border.add(decoder.combine2(i,maxCrdnt[1]));
        }
        for (int j = minCrdnt[1]; j <= maxCrdnt[1]; j++) {
            border.add(decoder.combine2(minCrdnt[0],j));
            border.add(decoder.combine2(maxCrdnt[0],j));
        }

        for(int gid : border){
            HashSet<String> canTids = postingList.getTidsByGid(gid);
            if(canTids == null){continue;}
            for (String tid : canTids){
                //检查是否过期
                if(postingList.isExpired(tid)){
                    HashSet<String> tids = expired.getOrDefault(gid, new HashSet<>());
                    tids.add(tid);
                    expired.putIfAbsent(gid,tids);
                    continue;
                }

                Entity entity = postingList.getEntityByTid(tid);
                float lat = entity.getLat();
                float lon = entity.getLon();

                //检查范围
                if ((minLat > lat) || (lat > maxLat) ||
                        (minLon > lon) || (lon > maxLon)) {
                    continue;
                }

                res.add(entity);
            }
        }

        //删除过期轨迹
//        expired.forEach((gid,tids)->{
//            tids.forEach(tid ->{
//                postingList.removeTidFromGid(gid,tid);
//                postingList.removeFromTlP(tid);
//            });
//        });
        return res;
    }

    @Transactional
    public List<Entity> rtkNNSearch(String tid, int K){
        Entity queried = postingList.getEntityByTid(tid);

        Queue<Entity> queue = new PriorityQueue<>((e1, e2) -> {
            double d1 = GeoUtil.distance(queried, e1);
            double d2 = GeoUtil.distance(queried, e2);
            return Double.compare(d1,d2);
        });

        //k: gid, v:过期的tid
        HashMap<Integer,HashSet<String>> expired = new HashMap<>();

        //保存临时结果L
        HashSet<Entity> temp = new HashSet<>();

        //获取最大round
        int[] crdnt = decoder.getGridCoordinate(queried.getLat(),queried.getLon());
        int[] dis = new int[]{crdnt[0],crdnt[1],
                (1 << decoder.getRESOLUTION()) - 1 - crdnt[0],
                (1 << decoder.getRESOLUTION()) - 1 - crdnt[1]};
        Arrays.sort(dis);
        int maxRound = dis[3];

        int round = 0;
        while (temp.size() < K && round <= maxRound){
            HashSet<Integer> gids = IRS(queried, round);
            //检查每个网格中的candidate

            for(int gid : gids){
                HashSet<String> canTids = postingList.getTidsByGid(gid);
                System.out.println("gid: "+gid);
                if(canTids == null){continue;}
                for(String canTid : canTids){
                    System.out.println("tid: "+canTid);
                    //检查是否过期
                    if(postingList.isExpired(canTid)){
//                        HashSet<String> tids = expired.getOrDefault(gid, new HashSet<>());
//                        tids.add(canTid);
//                        expired.putIfAbsent(gid,tids);
                    }
                    else {
                        Entity entity = postingList.getEntityByTid(tid);
                        if(entity != null){
                            temp.add(entity);
                            System.out.println("vehicleid: " + entity.getVehicleId());
                        }
                    }
                }
            }
            round++;
        }

        System.out.println(temp.size());
        //删除过期轨迹
//        expired.forEach((gid,tids)->{
//            tids.forEach(expiredTid ->{
//                postingList.removeTidFromGid(gid,tid);
//                postingList.removeFromTlP(tid);
//            });
//        });

        queue.addAll(temp);
        List<Entity> res = new ArrayList<>();
        while (!queue.isEmpty()){res.add(queue.poll());}
        return res;
    }

    /**
     * Incremental Range Search, 返回gid
     * @param center
     * @param round
     * @return
     */
    private HashSet<Integer> IRS(Entity center, int round){
        log.error("IRS " + round);
        int[] crdnt = decoder.getGridCoordinate(center.getLat(),center.getLon());
        int minI = Math.max(0,crdnt[0] - round);
        int maxI = Math.min((1 << decoder.getRESOLUTION()) - 1,crdnt[0] + round);
        int minJ = Math.max(0,crdnt[1] - round);
        int maxJ = Math.min((1 << decoder.getRESOLUTION()) - 1,crdnt[1] + round);

        HashSet<Integer> grids = new HashSet<>();
        for (int i = minI; i <= maxI; i++) {
            grids.add(encoder.combine2(i,minJ));
            grids.add(encoder.combine2(i,maxJ));
        }
        for (int j = minJ; j <= maxJ; j++) {
            grids.add(encoder.combine2(minI,j));
            grids.add(encoder.combine2(maxI,j));
        }
        return grids;
    }


    public int getCachedEntityCount(){
        return postingList.getCachedEntityCount();
    }

    public int getLatestEntityCount(){
        return postingList.getLatestEntityCount();
    }
}





