package whu.edu.cs.transitnet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import whu.edu.cs.transitnet.realtime.RealtimeService;
import whu.edu.cs.transitnet.realtime.Vehicle;

import java.util.List;
import java.util.Queue;

@Controller
public class RealTimeController {

    @Autowired
    RealtimeService realtimeService;

    @RequestMapping("/realtime/all")
    @ResponseBody
    Queue<List<Vehicle>> getAllRealtimeData() {
        return realtimeService.GetAll();
    }

    @RequestMapping("/realtime/update")
    @ResponseBody
    List<List<Vehicle>> getUpdateRealtimeData(@RequestParam(name = "time") long millisec) {
        return realtimeService.GetUpdate(millisec);
    }

    @RequestMapping("/realtime/latest")
    @ResponseBody
    List<Vehicle> getLatestRealtimeData(@RequestParam(name = "time") long millisec) {
        return realtimeService.GetLatest(millisec);
    }
}
