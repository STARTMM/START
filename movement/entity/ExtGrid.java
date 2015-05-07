package movement.entity;

/**
 * Created by ywj on 15/5/7.
 */
public class ExtGrid {
    public String grid_id;
    public int x;
    public int y;
    public ExtGrid(int x, int y)
    {
        this.grid_id = getKeyForGrid(x,y);
        this.x = x;
        this.y = y;
    }

    public static String getKeyForGrid(int x,int y)
    {
        return x+"-"+y;
    }
}
