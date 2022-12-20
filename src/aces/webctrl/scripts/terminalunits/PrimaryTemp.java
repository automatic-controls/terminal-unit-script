package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates the entering and leaving air temperature.
 */
public class PrimaryTemp extends Component {
  public final static double lowLimit = 40.0;
  public final static double highLimit = 120.0;
  private volatile boolean beyondLimits = false;
  public volatile boolean lowLimitPass = false;
  public volatile boolean highLimitPass = false;
  private volatile double mark = 0.0;
  private final static String lat = "lat";
  private final static String lat_status = "lat_status";
  private final static String lat_fault = "lat_fault";
  private volatile boolean hasLAT = false;
  private volatile boolean hasStatusLAT = false;
  private volatile boolean hasFaultLAT = false;
  private final static String eat = "eat";
  private final static String eat_status = "eat_status";
  private final static String eat_fault = "eat_fault";
  private volatile boolean hasEAT = false;
  private volatile boolean hasStatusEAT = false;
  private volatile boolean hasFaultEAT = false;
  public PrimaryTemp(Data d){
    super(d);
    if (hasLAT=d.x.hasMapping(lat)){
      hasStatusLAT = d.x.hasMapping(lat_status);
      hasFaultLAT = d.x.hasMapping(lat_fault);
    }
    if (hasEAT=d.x.hasMapping(eat)){
      hasStatusEAT = d.x.hasMapping(eat_status);
      hasFaultEAT = d.x.hasMapping(eat_fault);
    }
  }
  /**
   * @return whether dangerous temperatures have been detected by the last invokation of {@link #getLAT()} or {@link #getLAT(int, long)}.
   */
  public boolean danger(){
    return beyondLimits;
  }
  /**
   * Sets the current marked value (defaults to zero).
   * @see {@link #getValue()}
   */
  public void setMark(double mark){
    this.mark = mark;
  }
  /**
   * @return the current marked value (defaults to zero).
   * @see {@link #getValue()}
   */
  public double getMark(){
    return mark;
  }
  /**
   * Gets an average reading of {@link #getValue()} over time.
   * It is suggested to invoke {@link #danger()} after every invokation of this method to check whether dangerous temperatures have been detected.
   * @param times specifies how many measurements to take.
   * @param interval specifies how long to wait between each measurement (in milliseconds).
   * @return the averaged result or {@code null} if it cannot be retrieved for any reason.
   * @see {@link #setMark(double)}
   * @see {@link #getMark()}
   */
  public Double getValue(int times, long interval) throws InterruptedException {
    if (times<=0 || interval<0){
      return null;
    }
    double x = 0;
    Double y;
    boolean danger = false;
    boolean high = false;
    boolean low = false;
    for (int i=0;i<times;++i){
      if (i!=0){
        Thread.sleep(interval);
      }
      if ((y=getValue())==null){
        return null;
      }
      danger|=danger();
      high|=highLimitPass;
      low|=lowLimitPass;
      x+=y;
    }
    beyondLimits = danger;
    highLimitPass = high;
    lowLimitPass = low;
    return x/times;
  }
  /**
   * Gets an average reading of leaving air temperature over time.
   * It is suggested to invoke {@link #danger()} after every invokation of this method to check whether dangerous temperatures have been detected.
   * @param times specifies how many measurements to take.
   * @param interval specifies how long to wait between each measurement (in milliseconds).
   * @return leaving air temperature or {@code null} if the value cannot be retrieved for any reason.
   */
  public Double getLAT(int times, long interval) throws InterruptedException {
    if (times<=0 || interval<0){
      return null;
    }
    double x = 0;
    Double y;
    boolean danger = false;
    boolean high = false;
    boolean low = false;
    for (int i=0;i<times;++i){
      if (i!=0){
        Thread.sleep(interval);
      }
      if ((y=getLAT())==null){
        return null;
      }
      danger|=danger();
      high|=highLimitPass;
      low|=lowLimitPass;
      x+=y;
    }
    beyondLimits = danger;
    highLimitPass = high;
    lowLimitPass = low;
    return x/times;
  }
  /**
   * Gets an average reading of entering air temperature over time.
   * @param times specifies how many measurements to take.
   * @param interval specifies how long to wait between each measurement (in milliseconds).
   * @return entering air temperature or {@code null} if the value cannot be retrieved for any reason.
   */
  public Double getEAT(int times, long interval) throws InterruptedException {
    if (times<=0 || interval<0){
      return null;
    }
    double x = 0;
    Double y;
    for (int i=0;i<times;++i){
      if (i!=0){
        Thread.sleep(interval);
      }
      if ((y=getEAT())==null){
        return null;
      }
      x+=y;
    }
    return x/times;
  }
  /**
   * Retrieves the difference between leaving air temperature and entering air temperature.
   * If entering air temperature does not exist, this method merely retrieves the leaving air temperature reading.
   * Then the result is adjusted by the marked value (defaults to zero).
   * Assuming everything is defined, the final return value will be {@code (lat-eat)-mark}.
   * It is suggested to invoke {@link #danger()} after every invokation of this method to check whether dangerous temperatures have been detected.
   * @return the adjusted result or {@code null} if it cannot be retrieved for any reason.
   * @see {@link #setMark(double)}
   * @see {@link #getMark()}
   */
  public Double getValue() throws InterruptedException {
    Double val = getLAT();
    if (val==null){
      return null;
    }
    val-=mark;
    if (hasEAT){
      Double x = getEAT();
      return x==null?null:val-x;
    }else{
      return val;
    }
  }
  /**
   * It is suggested to invoke {@link #danger()} after every invokation of this method to check whether dangerous temperatures have been detected.
   * @return leaving air temperature or {@code null} if the value cannot be retrieved for any reason.
   */
  public Double getLAT() throws InterruptedException {
    if (!hasLAT){
      return null;
    }
    String s;
    if ((s=d.x.getValue(lat))==null){
      problem = true;
      return null;
    }
    try{
      double x = Double.parseDouble(s);
      lowLimitPass = x<lowLimit;
      highLimitPass = x>highLimit;
      beyondLimits = lowLimitPass || highLimitPass;
      return x;
    }catch(NumberFormatException e){
      return null;
    }
  }
  /**
   * @return entering air temperature or {@code null} if the value cannot be retrieved for any reason.
   */
  public Double getEAT() throws InterruptedException {
    if (!hasEAT){
      return null;
    }
    String s;
    if ((s=d.x.getValue(eat))==null){
      problem = true;
      return null;
    }
    try{
      return Double.parseDouble(s);
    }catch(NumberFormatException e){
      return null;
    }
  }
  /**
   * @return whether or not leaving air temperature (LAT) has been mapped.
   */
  @Override public boolean isDefined(){
    return hasLAT;
  }
  /**
   * @return whether or not entering air temperature (EAT) has been mapped.
   */
  public boolean hasEAT(){
    return hasEAT;
  }
  /**
   * {@inheritDoc}
   */
  @Override protected boolean fault() throws InterruptedException {
    String s;
    if (hasLAT){
      if (hasStatusLAT){
        if ((s=d.x.getValue(lat_status))==null){
          problem = true;
          return true;
        }else if (!parseBoolean(s)){
          return true;
        }
      }
      if (hasFaultLAT){
        if ((s=d.x.getValue(lat_fault))==null){
          problem = true;
          return true;
        }else if (parseBoolean(s)){
          return true;
        }
      }
    }else{
      return true;
    }
    if (hasEAT){
      if (hasStatusEAT){
        if ((s=d.x.getValue(eat_status))==null){
          problem = true;
          return true;
        }else if (!parseBoolean(s)){
          return true;
        }
      }
      if (hasFaultEAT){
        if ((s=d.x.getValue(eat_fault))==null){
          problem = true;
          return true;
        }else if (parseBoolean(s)){
          return true;
        }
      }
    }
    return false;
  }
}