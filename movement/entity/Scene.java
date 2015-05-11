package movement.entity;

import core.Coord;
import core.Settings;
import core.SettingsError;
import movement.map.MapNode;
import movement.map.SimMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * 涓�涓崟渚嬫ā寮�
 * Created by ywj on 15/5/7.
 */
public class Scene {


    /**
     * REGION SETTINGS
     */
    private static final String SCENE_MANAGER_NS = "Scene";
    public static final String NROF_EVENT0_REGIONFILES_S = "nrofEvent0RegionFiles";
    public static final String NROF_EVENT1_REGIONFILES_S = "nrofEvent1RegionFiles";
    public static final String EVENT0_REGION_PREFIX = "event0Region";
    public static final String EVENT1_REGION_PREFIX = "event1Region";

    //鏈夊灏慐vent0 region
    //杞藉叆澶氬皯Event0 region
    public int nrofEvent0RegionFiles;
    public String[] event0Regions;

    public int nrofEvent1RegionFiles;
    public String[] event1Regions;


    //CVS鏍煎紡鏁版嵁
    public static final String EVENT0_REGION_TIMES_PREFIX = "event0RegionTimes";
    public static final String EVENT1_REGION_TIMES_PREFIX = "event1RegionTimes";

    public List<int[]> event0RegionTimes;
    public List<int[]> event1RegionTimes;

    //璇诲彇鍖哄煙杞Щ姒傜巼鐭╅樀
    public static final String FILE_TRANS_PROB_S = "transProbFile";//杈撳叆鏂囦欢

    public static String transProbFileName;


    private static Scene ourInstance = null;

    public static Scene getInstance(Settings settings) {
        if (ourInstance == null) {
            return new Scene(settings);
        }
        return ourInstance;
    }

    public static SimMap map=null;

    /**
     * grids x,y 鏂瑰悜鐨勪釜鏁�
     */
    public static final String SCENE_SCALE = "sceneScale";
    public static int grids_x, grids_y;


    /**
     * grid鐨剎,y闀垮害
     */
    public static final String GRID_SIZE = "gridSize";
    public static int glen_x, glen_y;
    //鑾峰彇浜嬩欢鍜屽尯鍩熺殑瀵瑰簲鍏崇郴


    public Hashtable<String, ExtGrid> grids = null;
    public Hashtable<String, ExtRegion> regionPool = new Hashtable<String, ExtRegion>();
    public Hashtable<String, Hashtable<String, ExtRegion>> timeEventRegionSets = null;//寤虹珛time锛峳egion鐨勫叧绯�
    public Hashtable<String, ExtRegion> timeGrid2Region = null;

    public Hashtable<String, List<MapNode>>region2MapNode = null;

    //TODO 璇诲叆鍖哄煙杞Щ姒傜巼鐭╅樀
    // key鏄痶imeFromRegionKey,閲囩敤 time鍜宺egionID鎷兼帴鑰屾垚
    public Hashtable<String, Hashtable<String, Double>> timeRegionTransProbs = null;//璁板綍鍖哄煙杞Щ姒傜巼鐭╅樀涔嬮棿鐨勫叧绯�

    /***************end of 鍙傛暟鍖�*******************/
    /**
     * 鍒濆鍖栬幏鍙杇rid锛宺egion锛宺egionset
     */
    private Scene(Settings settings) {
        int [] scale = settings.getCsvInts(SCENE_SCALE);
        grids_x = scale[0];
        grids_y = scale[1];

        int []size = settings.getCsvInts(GRID_SIZE);
        glen_x = size[0];
        glen_y = size[1];
        //TODO 璇诲彇璁剧疆
        initGrid();

        initRegions(settings);

        loadTransProb(settings);//璇诲叆鍖哄煙杞Щ姒傜巼

        loadRegion2MapNode();
    }

    /**
     * 鍒濆鍖栧尯鍩熻浆绉荤煩闃�
     * 杈撳叆鏍煎紡涓� event,regionf,hour,regionto, event, all transprob
     * 0	334	0	48	1	24	0.04166667
     * 0	296	0	57	1	111	0.009009009
     * 1	448	0	255	1	10	0.1
     */
    private void loadTransProb(Settings settings)
    {
        transProbFileName = settings.getSetting(FILE_TRANS_PROB_S);

        File inFile = new File(transProbFileName);
        Scanner scanner;
        try {
            scanner = new Scanner(inFile);
        } catch (FileNotFoundException e) {
            throw new SettingsError("Couldn't find transprob movement input " +
                    "file " + inFile);
        }
        System.out.println("Loading transition prob...");

        //鍒濆鍖栨暟鎹粨鏋�
        this.timeRegionTransProbs = new Hashtable<String,Hashtable<String,Double>>();
        //璇诲叆鏁版嵁
        while(scanner.hasNextLine())
        {
            String nextLine = scanner.nextLine().trim();
            /**
             *      * 杈撳叆鏍煎紡涓� event,regionf,hour,regionto, event_num, all transprob
             */
            String s[] = nextLine.split("\t");
            int _event = Integer.parseInt(s[0]);
            int _regionFrom_id = Integer.parseInt(s[1]);
            int _time = Integer.parseInt(s[2]);
            int _regionTo_id = Integer.parseInt(s[3]);
            double _tansProb = Double.parseDouble(s[6]);

            String regionKey = ExtRegion.getRegionKey(_regionFrom_id, _event);
            String _timeRegionKey = getTimeFromRegionKey(_time, regionKey);
            if (!this.timeRegionTransProbs.contains(_timeRegionKey)) {
                this.timeRegionTransProbs.put(_timeRegionKey, new Hashtable<String, Double>());
            }

            int _reverse_event = _event==0?1:0;
            String _regionToKey = ExtRegion.getRegionKey(_regionTo_id,_reverse_event);

            this.timeRegionTransProbs.get(_timeRegionKey).put(_regionToKey, _tansProb);
        }
        System.out.println("fininsh loading transition prob...");
        scanner.close();
    }

    /**
     * 鍒濆鍖朑rid
     */
    private void initGrid() {
        grids = new Hashtable<String, ExtGrid>();

        for (int i = 0; i < grids_x; i++) {
            for (int j = 0; j < grids_y; j++) {
                ExtGrid grid = new ExtGrid(i, j);
                grids.put(grid.grid_id, grid);
            }
        }
    }

    /*
     * 鍒濆鍖栧尯鍩�
     */
    private void initRegions(Settings settings) {

        this.nrofEvent0RegionFiles = Integer.parseInt(
                settings.getSetting(NROF_EVENT0_REGIONFILES_S));
        this.nrofEvent1RegionFiles = Integer.parseInt(
                settings.getSetting(NROF_EVENT1_REGIONFILES_S));

        this.event0Regions = new String[this.nrofEvent0RegionFiles];
        this.event1Regions = new String[this.nrofEvent1RegionFiles];

        this.event0RegionTimes = new ArrayList<int[]>(this.nrofEvent0RegionFiles);
        this.event1RegionTimes = new ArrayList<int[]>(this.nrofEvent1RegionFiles);

        for (int i = 0; i < this.nrofEvent0RegionFiles; i++) {
            this.event0Regions[i] = settings.getSetting(EVENT0_REGION_PREFIX + i);
            this.event0RegionTimes.add(settings.getCsvInts(EVENT0_REGION_TIMES_PREFIX+i));
        }

        for (int i = 0; i < this.nrofEvent1RegionFiles; i++) {
            this.event1Regions[i] = settings.getSetting(EVENT1_REGION_PREFIX + i);
            this.event1RegionTimes.add(settings.getCsvInts(EVENT1_REGION_TIMES_PREFIX+i));
        }

        System.out.println(this.event0Regions.length);
        System.out.println(this.event1RegionTimes.size());
        loadGrid2Region2RegionSet(0);//error
        loadGrid2Region2RegionSet(1);

    }


    /**
     * 瀵瑰簲鍖哄煙id鍜宮apnode
     */
    private void loadRegion2MapNode() {
        this.region2MapNode = new Hashtable<String , List<MapNode>>();

        System.out.println("** size of Beijing2:" + map.getNodes().size());
        System.out.println("LoadRegions to MapNode");


        for(MapNode mapNode:map.getNodes())
        {
            Coord coord = mapNode.getLocation();
            String grid_id = ExtGrid.getKeyForGrid((int)coord.getX(),(int)coord.getY());
            for(ExtRegion _region:this.regionPool.values())
            {
                if(_region.grids.contains(grid_id))
                {
                    if(!this.region2MapNode.contains(_region.region_key))
                    {
                        this.region2MapNode.put(_region.region_key,new ArrayList<MapNode>());
                    }
                    this.region2MapNode.get(_region.region_key).add(mapNode);
                }
            }
        }

    }


    public MapNode randomGetMapNode(){
        int len = map.getNodes().size();
        Random random = new Random();
        return map.getNodes().get(random.nextInt(len));
    }


    /**
     * 杞藉叆浜嬩欢瀵瑰簲鐨勫尯鍩熼泦鍚�
     */
    private void loadGrid2Region2RegionSet(int event) {
        int filesCount;
        String[] fileNames;
        List<int[]> regionTimes;

        if (event == 0) {
            filesCount = this.nrofEvent0RegionFiles;
            fileNames = this.event0Regions;
            regionTimes = this.event0RegionTimes;
        } else {
            filesCount = this.nrofEvent1RegionFiles;
            fileNames = this.event1Regions;
            regionTimes = this.event1RegionTimes;
        }

        //time gain size 1 hour
        for (int i = 0; i < filesCount; i++) {
            File inFile = new File(fileNames[i]);
            System.out.println("begin loading cells and region [" + fileNames[i] + "]...");

            Scanner scanner;
            try {
                scanner = new Scanner(inFile);
            } catch (FileNotFoundException e) {
                throw new SettingsError("Couldn't find external movement input " +
                        "file " + inFile);
            }

            Hashtable<String, ExtRegion> _regions = new Hashtable<String, ExtRegion>();

            while (scanner.hasNextLine()) {

                String nextLine = scanner.nextLine().trim();

                String s[] = nextLine.split(" ");
                if (s.length < 4) continue;
                int x = Integer.parseInt(s[0]);
                int y = Integer.parseInt(s[1]);

                int region_id = Integer.parseInt(s[3]);
                String gridKey = ExtGrid.getKeyForGrid(x, y);
                ExtGrid grid = this.grids.get(gridKey);

                ExtRegion region = this.getRegionFromPool(region_id, event);

                region.grids.put(gridKey, grid);
                _regions.put(ExtRegion.getRegionKey(region_id, event), region);

                /**
                 * 寤虹珛鍙嶅悜绱㈠紩
                 * from time,grid to region
                 * error
                 */
                for (int j = 0; j < (regionTimes.get(i)).length; j++) {
                    String tgKey = getTimeEventGridKey(regionTimes.get(i)[j], event, gridKey);
                    this.timeGrid2Region.put(tgKey, region);
                }

            }
            
            System.out.println("region size:"+regionTimes.get(i).length);

            for (int j = 0; j < regionTimes.get(i).length; j++) {
                int _time = regionTimes.get(i).length;
                String teKey = getTimeEventKey(_time, event);
                this.timeEventRegionSets.put(teKey, _regions);
            }

            System.out.println("fininsh loading cells and region...");
            scanner.close();

        }

    }

    /**
     * 浠嶳egion姹犱腑鑾峰彇鍒皉egion
     *
     * @param region_id
     * @param event
     * @return
     */
    private ExtRegion getRegionFromPool(int region_id, int event) {
        String region_key = ExtRegion.getRegionKey(region_id, event);
        if (!this.regionPool.contains(region_key)) {
            ExtRegion region = new ExtRegion(region_id, event);
            this.regionPool.put(region_key, region);
        }
        return this.regionPool.get(region_key);
    }


    /**
     * 鑾峰彇浠巘ime event鏋勬垚鐨勭储寮�
     *
     * @param time  鏃跺埢
     * @param event 浜嬩欢 0 1
     * @return 绱㈠紩
     */
    public static String getTimeEventKey(int time, int event) {
        return time + "-" + event;
    }


    /**
     * 鑾峰彇浠庢椂闂� 鏍煎瓙鍒板尯鍩熺殑绱㈠紩
     *
     * @param time    鏃堕棿
     * @param gridKey 鏍煎瓙key
     * @return 绱㈠紩
     */
    public static String getTimeEventGridKey(int time, int event, String gridKey) {
        return time + "-" + event + "-" + gridKey;
    }


    public static String getTimeFromRegionKey(int time, String regionFrom_key) {
        return time + "-" + regionFrom_key;
    }

    /**
     * 浠巆oord鎵惧埌region
     * @param coord
     * @return ExtGrid鐨刬d
     */
    public String fromCoordToGrid(Coord coord)
    {
        int x = (int) Math.floor(coord.getX()/glen_x);
        int y = (int) Math.floor(coord.getY()/glen_y);
        return this.grids.get(ExtGrid.getKeyForGrid(x,y)).grid_id;
    }
}
