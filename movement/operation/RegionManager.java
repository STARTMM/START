package movement.operation;

import core.Coord;
import core.Settings;
import movement.Region;
import movement.entity.ExtGrid;
import movement.entity.Scene;
import movement.map.MapNode;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

/**
 * 目的是由当前区域找到对应地图上的目的节点
 * TODO 从coord找到region
 * TODO 从to region中按概率选区
 * TODO 从to region中选取选取Coord
 *
 * Created by ywj on 15/5/7.
 */
public class RegionManager {

    private static RegionManager ourInstance = null;

    public static RegionManager getInstance(Settings settings) {
        if(ourInstance==null)
        {
            ourInstance = new RegionManager(settings);
        }
        return ourInstance;
    }

    public Scene scene = null;
    public static Random random = new Random();


    private RegionManager(Settings settings) {
        this.scene = Scene.getInstance(settings);
    }


    /**
     *  * TODO 从to region中按概率选区
     * @param time 时刻
     * @param gridKey 格子的key
     * @return Region Key
     */
    public String fromTimeEventGridToRegion(int time, int event, String gridKey)
    {
        String timeEventGridKey = Scene.getTimeEventGridKey(time, event,gridKey);
        return scene.timeGrid2Region.get(timeEventGridKey).region_key;
    }

    /**
     *  TODO 从to region中按概率选区
     */
    public String toRegion(int time, int event,String fromRegion_id) throws Exception {
        double prob = random.nextDouble();

        String timeEventKey = Scene.getTimeFromRegionKey(time, fromRegion_id);
        Hashtable<String,Double>toRegionProbs = this.scene.timeRegionTransProbs.get(timeEventKey);

        for(String key:toRegionProbs.keySet())
        {
            if(prob>toRegionProbs.get(key))
            {
                prob -= toRegionProbs.get(key);
            }
            else{
                return key;
            }
        }

        throw new Exception("No region select:timeEventFromKey:"+timeEventKey);
    }

    /**
     * 由目的区域找到对应的地图点
     * @param toRegionId
     * @return
     * @throws Exception
     */
    public MapNode toMapNode(String toRegionId) throws Exception {
        if(this.scene.region2MapNode.containsKey(toRegionId)) {
            List<MapNode> mapNodes = this.scene.region2MapNode.get(toRegionId);
            int index = random.nextInt(mapNodes.size());
            return mapNodes.get(index);
        }
        System.out.println(toRegionId+"这个区域没有地图点呢");
        throw new Exception("WARNING:*RegionManager:toMapNode:"+toRegionId+"has no mapnode!");
    }

    /**
     *
     * @param time 由当前时刻1
     * @param event 当前事件值0
     * @param coord 当前位置0-201
     * @return 目的的地图
     */
    public MapNode fromCoordToNextMapNode(int time,int event,Coord coord)
    {
        String gridKey = this.scene.fromCoordToGrid(coord);
        String region_key = fromTimeEventGridToRegion(time, event, gridKey);
        try {
            String toRegion_key = toRegion(time,event,region_key);
            MapNode mapNode = toMapNode(toRegion_key);
            return mapNode;

        } catch (Exception e) {
//            e.printStackTrace();
        	//System.out.println(e.getMessage());
        	System.out.println("fromCoordToNextMapNode**"+e.getMessage());
            return scene.randomGetMapNode();
        }
    }

}
