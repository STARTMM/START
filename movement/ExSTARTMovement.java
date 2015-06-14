package movement;

import core.Coord;
import core.Settings;
import core.SimClock;
import movement.map.DijkstraPathFinder;
import movement.map.MapNode;
import movement.map.PointsOfInterest;
import movement.operation.RegionManager;
import movement.operation.SpeedManager;

import java.util.List;

/**
 * Created by ywj on 15/5/5.
 * <p>
 * 扩展了STARTMovement，添加了时间字段，用来表示仿真是从什么时刻开始的
 */
public class ExSTARTMovement extends MapBasedMovement implements SwitchableMovement {

    /**
     * 区分车辆状态
     */
    private int status;

    /**
     * 记录节点的速度
     */
    private double speed;

    /**
     * 引入仿真开始时刻
     */
    private static final String BEGIN_TIME = "beginTime";
    private static int beginTime = -1;

    /** 其他参数和配置不变   */

    public static RegionManager regionManager = null;
    public static SpeedManager speedManager = null;

    private DijkstraPathFinder pathFinder;
    private PointsOfInterest pois;

    public DijkstraPathFinder getPathFinder() {
        return this.pathFinder;
    }
   
    public ExSTARTMovement(Settings settings) {
        super(settings);
        this.pathFinder = new DijkstraPathFinder(getOkMapNodeTypes());
		this.pois = new PointsOfInterest(getMap(), getOkMapNodeTypes(),
				settings, rng);
        if(beginTime==-1)
        {
        	beginTime = settings.getInt(BEGIN_TIME);
        	//System.out.println("begin time:"+this.beginTime);
        }
        if(_settings == null)
        {
        	_settings = settings;
        	
        }
        
        if(regionManager==null)
        {
            regionManager = RegionManager.getInstance(settings);

        	regionManager.scene.map = getMap();
        	regionManager.scene.loadRegion2MapNode();//在获取了map之后赋值
        	
        }
        if(speedManager==null)
            speedManager = SpeedManager.getInstance(settings);
    }

    /**
     * @param mbm 原来的STARTMovement
     */
    public ExSTARTMovement(ExSTARTMovement mbm) {
        super(mbm);
        // TODO Auto-generated constructor stub
        this.status = rng.nextInt(2);
        this.pathFinder = mbm.pathFinder;
    }
    
    public static Settings _settings = null;


    @Override
    public Path getPath() {

        //每次getPath就转换状态
        reverseStatus();//change the status

        Path p = new Path();

        MapNode to = getNextMapNode();
        List<MapNode> nodePath = getPathFinder().getShortestPath(this.lastMapNode, to);

        // this assertion should never fire if the map is checked in read phase
        assert nodePath.size() > 0 : "No path from " + this.lastMapNode + " to " +
                to + ". The simulation map isn't fully connected";

        double dis=0;
        MapNode source = this.lastMapNode;
        MapNode bknode = source;
        for (MapNode node : nodePath) { // create a Path from the shortest path
            dis+=distance(bknode.getLocation(),node.getLocation());//计算实际距离
            p.addWaypoint(node.getLocation());
        }

        double minSpeed = dis/3600;//最多跑一个小时
//        System.out.println("最小速度:"+minSpeed);
        minSpeed = minSpeed<10?minSpeed:0;

        //在此处设置速度
        double generateSpeed = speedManager.generateSpeed(this.status);
        this.speed = generateSpeed<minSpeed?minSpeed:generateSpeed;

        p.setSpeed(this.speed);

        lastMapNode = to;
        return p;
    }


    /**
     * 计算两个地点之间的距离
     * 为了防止速度过小
     * @param location 地点1
     * @param location2 地点2
     * @return
     */
    private static double distance(Coord location, Coord location2) {
        double x = Math.pow(location.getX()-location2.getX(), 2);
        double y = Math.pow(location.getY()-location2.getY(), 2);
        return Math.sqrt(x+y);
    }

    /**
     * 反转状态
     */
    public void reverseStatus() {
        this.status = this.status == 0 ? 1 : 0;
    }

    /**
     * 由当前位置状态获取下一个地点
     * @return MapNode 目的地点的地图节点
     */
    public MapNode getNextMapNode() {

        int _time = beginTime + (int) Math.floor(SimClock.getIntTime() / 3600);
        
        if(_time>regionManager.scene.timeFlag)
        {
        	regionManager.scene.loadTransProb(_settings, _time);
        	regionManager.scene.timeFlag = _time;
        }
        
        
        return regionManager.fromCoordToNextMapNode(_time, this.status, this.lastMapNode.getLocation());
    }

    @Override
    public ExSTARTMovement replicate() {
        return new ExSTARTMovement(this);
    }


}
