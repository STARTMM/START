package report;

import core.Coord;
import core.DTNHost;
import core.MovementListener;
import core.Settings;

import java.util.List;

/**
 * Created by ywj on 15/5/17.
 */
public class MovementTraceReport extends Report implements MovementListener {
	/** node array's name -setting id ({@value})*/
	public static final String NODE_ARR_S = "nodeArray";
	/** ns command -setting id ({@value}) */
	public static final String NS_CMD_S = "nsCmd";
	/** default value for the array name ({@value})*/
	public static final String DEF_NODE_ARRAY = "$node_";
	/** default value for the ns command ({@value})*/
	public static final String DEF_NS_CMD = "$ns_";
	
	/** a value "close enough" to zero ({@value}). Used for fixing zero values*/
	public static final double EPSILON = 0.00001; 
	/** formatting string for coordinate values ({@value})*/
	public static final String COORD_FORMAT = "%.5f";
	
	private String nodeArray;
	private String nsCmd;
	
	/**
	 * Constructor. Reads {@link #NODE_ARR_S} and {@link #NS_CMD_S} settings 
	 * and uses those values as the name of the node array and ns command. 
	 * If the values aren't present, default values of 
	 * <CODE>{@value DEF_NODE_ARRAY}</CODE> and
	 * <CODE>{@value DEF_NS_CMD}</CODE> are used.
	 */
	public MovementTraceReport() {
		Settings settings = getSettings();

		if (settings.contains(NODE_ARR_S)) {
			nodeArray = settings.getSetting(NODE_ARR_S);
		}
		else {
			nodeArray = DEF_NODE_ARRAY;
		}
		if (settings.contains(NS_CMD_S)) {
			nsCmd = settings.getSetting(NS_CMD_S);
		}
		else {
			nsCmd = DEF_NS_CMD;
		}
		
		init();
	}

	public void initialLocation(DTNHost host, Coord location) {
        return;
	}

	@Override
	public void newPath(DTNHost host, List<Coord> path, double speed) {
		int id = host.getAddress();
        double time = getSimTime();
        if(path == null) return;

        int index = 0;

        write(String.format("%d %f %f %f %f", id, time, path.get(0).getX(), path.get(0).getY(), speed));
        if(speed==0)return;

        index++;
        while (index<path.size())
        {
            double currentTime = getTime(time,path.get(index-1),path.get(index),speed);

            write(String.format("%d %f %f %f %f", id, currentTime, path.get(index).getX(), path.get(index).getY(), speed));

            time = currentTime;
            index++;
        }
	}

    private double getTime(double time, Coord current, Coord next, double speed){
        double dis = distance(current,next);
        return time+dis/speed;
    }

    private double distance(Coord current, Coord next) {
        double x2 = Math.pow(current.getX()-next.getX(),2);
        double y2 = Math.pow(current.getY()-next.getY(),2);
        return Math.sqrt(x2+y2);
    }
    public void newDestination(DTNHost host, Coord dst, double speed) {
        return;
    }

	/** 
	 * Fixes and formats coordinate values suitable for Ns2 module. 
	 * I.e. converts zero-values to {@value EPSILON} and formats values
	 * with {@link #COORD_FORMAT}. 
	 * @param val The value to fix
	 * @return The fixed value
	 */
	private String fix(double val) {
		val = val == 0 ? EPSILON : val;
		return String.format(COORD_FORMAT, val);
	}
}
