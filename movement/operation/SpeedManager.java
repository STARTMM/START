package movement.operation;

import core.Settings;
import core.SimClock;

import java.util.Random;

/**
 * Created by ywj on 15/5/7.
 */
public class SpeedManager {

    //public static double A0 = 0.11798;
    //public static double A1 = 0.0058637;

    public static double a= 0.00211644;
    public static double b= 1.69768;
    public static double c= 0.646143;

    private static SpeedManager ourInstance = null;
    private static Random rng = new Random(SimClock.getIntTime());

    public static SpeedManager getInstance(Settings settings)
    {
        if(ourInstance==null)
        {
            ourInstance = new SpeedManager(settings);
        }
        return ourInstance;
    }

    private SpeedManager(Settings settings) {
    }

    public double generateSpeed(int status) {
        //TODO 实现按函数分布生成速度
        if(status==0)
            return generateSpeedForStatus0();
        else
            return generateSpeedForStatus1();
    }

    protected double generateSpeed(double status)
    {
        // TODO get speed by the status
        if(status==0)
            return generateSpeedForStatus0();
        else
            return generateSpeedForStatus1();

    }

    private double generateSpeedForStatus0() {
        double seed = rng.nextDouble()*speed_dis_for_status0(22.22);
        double sp = reverse_speed_for_status0(seed);
//		if(sp<0||sp>44.4)
//			System.out.println(sp);
//		if(sp>10)
//			System.out.print(sp);
        return sp;

    }

    private double generateSpeedForStatus1() {
        double seed = rng.nextDouble()*speed_dis_for_status1(22.22);
        //System.out.println(seed);
        double sp = reverse_speed_for_status1(seed);
//		if(sp<0||sp>44.4)
//			System.out.println(sp);
//		if(sp>10)
//			System.out.print(sp);
        return sp;
    }


    private double speed_dis_for_status0(double x){
        return 1-1/Math.exp(-a*Math.pow(x, b)-c);
    }

    private double speed_dis_for_status1(double x)
    {
        return 1-1/Math.exp(-a*Math.pow(x, b));
    }

    private double reverse_speed_for_status0(double result)
    {
        return Math.pow(Math.log(1/(1-result))/A0,1/1.5);
    }

    private double reverse_speed_for_status1(double result)
    {
        return Math.pow(Math.log(1/(1-result))/A1,1/2.5);
    }



}
