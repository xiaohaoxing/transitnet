package whu.edu.cs.transitnet.realtime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Entity {
    //tid: trip_id + start_date
    //pid: vehicle_id + timestamp
    private String id; //entity id
    private String routeId; //路线id
    private String tripId; //行程id
    private String vehicleId; //车辆id
    private String startDate; //行程开始日期（yyyy-mm-dd）
    private float lon; //经度
    private float lat;  //纬度
    private float bearing; //车辆朝向
    private float speed; //行驶速度
    private long timestamp; //当前时刻

    public boolean equals(Entity e){
        return Objects.equals(e.getVehicleId(), this.vehicleId) && e.getTimestamp() == this.timestamp;
    }
}
