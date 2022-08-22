package whu.edu.cs.transitnet.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import whu.edu.cs.transitnet.realtime.Entity;
import whu.edu.cs.transitnet.realtime.QueryService;

import java.util.HashSet;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/query/realtime/")
public class QueryController {
    @Autowired
    QueryService queryService;

    @PostMapping("/range")
    public String rtRangeSearch(@RequestParam("minLat") float minLat,
                                @RequestParam("minLon") float minLon,
                                @RequestParam("maxLat") float maxLat,
                                @RequestParam("maxLon") float maxLon) throws JsonProcessingException {

        HashSet<Entity> entities = queryService.rtRangeSearch(minLat, minLon, maxLat, maxLon);
        return new ObjectMapper().writeValueAsString(entities);
    }

    @PostMapping("/kNN")
    public String rtKNNSearch(@RequestParam("tid") String tid, @RequestParam("K") int K) throws JsonProcessingException {
        List<Entity> entities = queryService.rtkNNSearch(tid, K);
        return new ObjectMapper().writeValueAsString(entities);
    }


}
