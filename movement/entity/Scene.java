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
 * Singleton
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

    public static List<String>mnRegion = new ArrayList<String>();
    public static List<MapNode>validMapNodes = new ArrayList<MapNode>();
    
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
    public static final String FILE_TRANS_PROB_S = "transProbFileProfix";//杈撳叆鏂囦欢

    public static String transProbFileProfix;


    private static Scene ourInstance = null;

    public static Scene getInstance(Settings settings) {
        if (ourInstance == null) {
            return new Scene(settings);
        }
        return ourInstance;
    }

    public static SimMap map=null;

    /**
     * grids x,y 鏂瑰悜鐨勪釜鏁?
     */
    public static final String SCENE_SCALE = "sceneScale";
    public static int grids_x, grids_y;


    /**
     * grid length x y 
     */
    public static final String GRID_SIZE = "gridSize";
    public static int glen_x, glen_y;
   


    public Hashtable<String, ExtGrid> grids = null;
    public Hashtable<String, ExtRegion> regionPool = new Hashtable<String, ExtRegion>();
    public Hashtable<String, Hashtable<String, ExtRegion>> timeEventRegionSets = null;//寤虹珛time锛峳egion鐨勫叧绯?
    public Hashtable<String, ExtRegion> timeGrid2Region = new Hashtable<String,ExtRegion>();

    public Hashtable<String, List<MapNode>>region2MapNode = null;

    //TODO 璇诲叆鍖哄煙杞Щ姒傜巼鐭╅樀
    // key鏄痶imeFromRegionKey,閲囩敤 time鍜宺egionID鎷兼帴鑰屾垚
    public int timeFlag = -1;
    public Hashtable<String, Hashtable<String, Double>> timeRegionTransProbs = null;//璁板綍鍖哄煙杞Щ姒傜巼鐭╅樀涔嬮棿鐨勫叧绯?

  
    private Scene(Settings settings) {
        int [] scale = settings.getCsvInts(SCENE_SCALE);
        grids_x = scale[0];
        grids_y = scale[1];

        int []size = settings.getCsvInts(GRID_SIZE);
        glen_x = size[0];
        glen_y = size[1];
        
        initGrid();
        initRegions(settings);

        
    }

    /**
     * change to every time only load one hour transition prob
     * 鍒濆鍖栧尯鍩熻浆绉荤煩闃?
     * 杈撳叆鏍煎紡涓? event,regionf,hour,regionto, all transprob
     * 0	334		48	1	0.04166667
     * 0	296		57	1	111	0.009009009
     * 1	448		255	1	10	0.1
     * add a field to mark
     */
    public void loadTransProb(Settings settings, int time)
    {
        transProbFileProfix = settings.getSetting(FILE_TRANS_PROB_S);

        System.out.println("**Loading transition prob...");
        File inFile = new File(transProbFileProfix+time+".txt");
        Scanner scanner;
        try {
            scanner = new Scanner(inFile);
        } catch (FileNotFoundException e) {
            throw new SettingsError("Couldn't find transprob movement input " +
                    "file " + inFile);
        }
        
        this.timeRegionTransProbs = new Hashtable<String,Hashtable<String,Double>>();
        
        while(scanner.hasNextLine())
        {
            String nextLine = scanner.nextLine().trim();
            /**
             *    event,regionf,hour,regionto, event_num, all transprob
             */
            String s[] = nextLine.split("\t");
            
            
            int _event = Integer.parseInt(s[0]);
            int _regionFrom_id = Integer.parseInt(s[1]);
            int _time = Integer.parseInt(s[3]);
            int _regionTo_id = Integer.parseInt(s[2]);
            double _tansProb = Double.parseDouble(s[4]);

            String regionKey = ExtRegion.getRegionKey(_regionFrom_id, _event);
            if(!mnRegion.contains(regionKey))
            	continue;
           
            
            String _timeRegionKey = getTimeFromRegionKey(_time, regionKey);
            
            
            if (!this.timeRegionTransProbs.containsKey(_timeRegionKey)) {
                this.timeRegionTransProbs.put(_timeRegionKey, new Hashtable<String, Double>());
            }

            
            int _reverse_event = _event==0?1:0;
            String _regionToKey = ExtRegion.getRegionKey(_regionTo_id,_reverse_event);
            
            if(mnRegion.contains(_regionToKey))
            	this.timeRegionTransProbs.get(_timeRegionKey).put(_regionToKey, _tansProb);

        }
        
        normalizeTransProbs();
        
        System.out.println("fininsh loading transition prob...");
        scanner.close();
    }
    
    
    private void normalizeTransProbs()
    {
    	System.out.println("normalizing the TransProbs");
    	
    	List<String> invalidKeys = new ArrayList<String>();
    	
    	for(String key:this.timeRegionTransProbs.keySet())
    	{
    		Hashtable<String,Double>toRegionProbs = this.timeRegionTransProbs.get(key);
    		double sum = 0;
    		for(double prob:toRegionProbs.values())
    		{
    			sum += prob;
    		}
    		
    		if(sum==0)
    		{
    			invalidKeys.add(key);
    		}
    		else{
    			for(String subKey:toRegionProbs.keySet())
        		{
    				double prob = this.timeRegionTransProbs.get(key).get(subKey)/sum;
    				
        			this.timeRegionTransProbs.get(key).put(subKey, prob);
        		}
    		}
    	}
    	
    	for(int i=0;i<invalidKeys.size();i++)
    	{
    		this.timeRegionTransProbs.remove(invalidKeys.get(i));
    	}
    	
    }

    /**
     * init all the grids
     * grids belongs to ExGrid.
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
     * 鍒濆鍖栧尯鍩?
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

        loadGrid2Region2RegionSet(0);
        loadGrid2Region2RegionSet(1);

    }


    /**
     * 瀵瑰簲鍖哄煙id鍜宮apnode
     */
    public void loadRegion2MapNode() {
    	

        System.out.println("** size of Beijing2:" + map.getNodes().size());
        System.out.println("LoadRegions to MapNode");
        
        
        this.region2MapNode = new Hashtable<String , List<MapNode>>();
        

        for(MapNode mapNode:map.getNodes())
        {
            Coord coord = mapNode.getLocation();
            String grid_id = fromCoordToGrid(coord) ;
          
            for(ExtRegion _region:this.regionPool.values())
            {
                if(_region.grids.containsKey(grid_id))
                {
                    if(!this.region2MapNode.containsKey(_region.region_key))
                    {
                        this.region2MapNode.put(_region.region_key,new ArrayList<MapNode>());
                    }
                    this.region2MapNode.get(_region.region_key).add(mapNode);
                    
                    if(!mnRegion.contains(_region.region_key))
                    {
                    	mnRegion.add(_region.region_key);
                    }
                    if(!validMapNodes.contains(mapNode))
                    {
                    	validMapNodes.add(mapNode);
                    }
                }
            }
        }
        
    }


    public MapNode randomGetMapNode(){
        int len = validMapNodes.size();
        Random random = new Random();
        return validMapNodes.get(random.nextInt(len));
    }


    /**
     * loading the x, y region id 
     * to create the relations between event grid and regions 
     * @param event: 0 or 1
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

            Scanner scanner;
            try {
                scanner = new Scanner(inFile);
            } catch (FileNotFoundException e) {
                throw new SettingsError("Couldn't find external movement input " +
                        "file " + inFile);
            }


            while (scanner.hasNextLine()) {

                String nextLine = scanner.nextLine().trim();

                String s[] = nextLine.split(" ");
                if (s.length < 4) continue;
                int x = Integer.parseInt(s[0]);
                int y = Integer.parseInt(s[1]);

                int region_id = Integer.parseInt(s[3]);

                String gridKey = ExtGrid.getKeyForGrid(x, y);
                ExtGrid grid = this.grids.get(gridKey);

                ExtRegion region = this.getRegionFromPool(region_id, event);//get a region from the pool by the key

                region.grids.put(gridKey, grid);//add a grid to the region's grid

                /**
                 * 
                 * from time,grid to region
                 * error
                 */
                for (int j = 0; j < (regionTimes.get(i)).length; j++) {
                    String tgKey = getTimeEventGridKey(regionTimes.get(i)[j], event, gridKey);
                    this.timeGrid2Region.put(tgKey, region);
                }

            }
            
            scanner.close();

        }

    }

    /**
     * get a region from the pool
     * if the regionid is not in the region 
     * new a region for the key
     * @param region_id
     * @param event
     * @return
     */
    private ExtRegion getRegionFromPool(int region_id, int event) {
        String region_key = ExtRegion.getRegionKey(region_id, event);
        if (!this.regionPool.containsKey(region_key)) {
            ExtRegion region = new ExtRegion(region_id, event);
            this.regionPool.put(region_key, region);
        }
        return this.regionPool.get(region_key);
    }


    
    public static String getTimeEventKey(int time, int event) {
        return time + "-" + event;
    }


    public static String getTimeEventGridKey(int time, int event, String gridKey) {
        return time + "-" + event + "-" + gridKey;
    }


    public static String getTimeFromRegionKey(int time, String regionFrom_key) {
        return time + "-" + regionFrom_key;
    }

    
    public String fromCoordToGrid(Coord coord)
    {
        int x = (int) Math.floor(coord.getX()/glen_x);
        int y = (int) Math.floor(coord.getY()/glen_y);
        return this.grids.get(ExtGrid.getKeyForGrid(x,y)).grid_id;
    }
}
