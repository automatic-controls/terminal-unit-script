package aces.webctrl.scripts.terminalunits;
import java.util.ArrayList;
/**
 * Records data over time.
 */
public class Trend {
  /** Larger values mean that trend stabilization will be detected earlier */
  public volatile double OFFSET_FACTOR = 0.018;
  public volatile int TRAILING_AVG = 5;
  public volatile int MIDDLE_AVG = 10;
  public volatile long[] x;
  public volatile double[] y;
  public volatile int len = 0;
  public volatile double min;
  public volatile double max;
  public volatile double sum = 0;
  public volatile long startTime;
  public volatile int start = 0;
  public volatile ArrayList<Segment> segments = new ArrayList<Segment>();
  public Trend(){
    this(0);
  }
  public Trend(int capacity){
    capacity = Math.max(capacity,MIDDLE_AVG+TRAILING_AVG+1);
    y = new double[capacity];
    x = new long[capacity];
    startTime = System.currentTimeMillis();
  }
  /**
   * @return the length of the segment started with an invokation of {@link #reset()}.
   */
  public int getSegmentLength(){
    return len-start;
  }
  /**
   * May be used when flipping from heating to cooling mode, for example.
   */
  public Segment reset(String segmentName){
    final Segment seg = new Segment(segmentName, start, len, isIncreasing(), min, max);
    start = len;
    min = 0;
    max = 0;
    sum = 0;
    segments.add(seg);
    return seg;
  }
  /**
   * @return whether to continue adding more data.
   */
  public boolean add(double yy){
    if (len==x.length){
      final int capacity = len<<1;
      final long[] newX = new long[capacity];
      final double[] newY = new double[capacity];
      System.arraycopy(x,0,newX,0,len);
      System.arraycopy(y,0,newY,0,len);
      x = newX;
      y = newY;
    }
    x[len] = System.currentTimeMillis()-startTime;
    y[len] = yy;
    sum+=yy;
    ++len;
    if (len==start+1){
      min = yy;
      max = yy;
    }else if (yy<min){
      min = yy;
    }else if (yy>max){
      max = yy;
    }
    return !hasStabilized();
  }
  /**
   * @return whether this trend is increasing.
   */
  public boolean isIncreasing(){
    final int ll = len-start;
    if (ll<2){
      return false;
    }
    double i_ = (start+len-1)/2.0;
    double y_ = sum/ll;
    double z = 0;
    for (int i=start;i<len;++i){
      z+=(y[i]-y_)*(i-i_);
    }
    return z>0;
  }
  /**
   * @return whether this trend appears to have stabalized.
   */
  public boolean hasStabilized(){
    if (len<start+MIDDLE_AVG+TRAILING_AVG+15){
      return false;
    }
    int i;
    double offset = (Math.max(max,0)-Math.min(min,0))*OFFSET_FACTOR;
    double a = 0;
    for (i=len-TRAILING_AVG;i<len;++i){
      a+=y[i];
    }
    a/=TRAILING_AVG;
    double b = 0;
    for (i=len-MIDDLE_AVG-TRAILING_AVG;i<len-TRAILING_AVG;++i){
      b+=y[i];
    }
    b/=MIDDLE_AVG;
    if (isIncreasing()){
      return b+offset>a;
    }else{
      return b-offset<a;
    }
  }
}
/**
 * Defines a single section of a trend.
 */
class Segment {
  public volatile String name;
  public volatile int start;
  public volatile int end;
  public volatile boolean increasing;
  public volatile double min;
  public volatile double max;
  public volatile boolean lowerStages = false;
  public volatile boolean lowDanger = false;
  public volatile boolean highDanger = false;
  public Segment(String name, int start, int end, boolean increasing, double min, double max){
    this.name = name;
    this.start = start;
    this.end = end;
    this.increasing = increasing;
    this.min = min;
    this.max = max;
  }
}