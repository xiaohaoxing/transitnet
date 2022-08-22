package whu.edu.cs.transitnet.index;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import whu.edu.cs.transitnet.realtime.Entity;
import whu.edu.cs.transitnet.realtime.RealtimeService;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class PostingList {
     ConcurrentHashMap<Integer, HashSet<String>> GT = new ConcurrentHashMap<>();
     ConcurrentHashMap<String, Entity> TlP = new ConcurrentHashMap<>();
     ConcurrentHashMap<String, List<Entity>> entityCache = new ConcurrentHashMap<>();

    @Autowired
    Encoder encoder;

    @Autowired
    RealtimeService realtimeService;

    @PostConstruct
    public void initIndexAndCache(){
        updateIndexAndCache(realtimeService.getLatestEntities());
    }

    /**
     * 定期更新索引
     */
    @Scheduled(cron = "0/20 * * * * ?")
    public void scheduledUpdate() {
        List<Entity> entities = realtimeService.getLatestEntities();
        updateIndexAndCache(entities);
    }


    @Transactional
    public void updateIndexAndCache(List<Entity> entities){
        for(Entity entity : entities){
            String tid = entity.getTripId() + entity.getStartDate();

            //更新GT
            int gid = encoder.getGid(entity.getLat(),entity.getLon());
            HashSet<String> tids = GT.getOrDefault(gid,new HashSet<>());
            if(tids.add(tid)){GT.put(gid,tids);}

            //更新TlP
            TlP.put(tid, entity);

            //更新cache
            List<Entity> entities1 = entityCache.getOrDefault(tid, new ArrayList<Entity>());
            entities1.add(entity);
            entityCache.putIfAbsent(tid, entities1);
        }
        AtomicInteger count = new AtomicInteger();
        entityCache.forEach((k,v) -> {
            count.addAndGet(v.size());
        });
    }

    public HashSet<String> getTidsByGid(int gid){return GT.getOrDefault(gid, null);}

    public Entity getEntityByTid(String tid){return TlP.getOrDefault(tid,null);}

    /**
     * 检查TlP中的最新位置是否过期(五分钟之前)
     * @param tid
     * @return
     */
    public boolean isExpired(String tid){
        if(!TlP.containsKey(tid)){return false;}
        Entity entity = TlP.get(tid);
        long currTime = System.currentTimeMillis()/1000;
        return entity.getTimestamp() + 300 <= currTime;
    }

    public void removeFromTlP(String tid){
        TlP.remove(tid);
    }

    public void removeTidFromGid(int gid, String tid){
        GT.getOrDefault(gid, new HashSet<>()).remove(tid);
    }

    public int getCachedEntityCount(){
        AtomicInteger count = new AtomicInteger();
        entityCache.forEach((k,v)->{
            count.addAndGet(v.size());
        });
        return count.intValue();
    }

    public int getLatestEntityCount(){
        AtomicInteger count = new AtomicInteger();
        GT.forEach((k,v)->{
            count.addAndGet(v.size());
        });
        return count.intValue();
    }

    public int getIndexedGridCount(){
        return GT.size();
    }

    public int getLiveTrajsCount(){
        return TlP.size();
    }
}
