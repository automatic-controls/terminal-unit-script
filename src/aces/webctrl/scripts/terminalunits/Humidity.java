package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates a humidity sensor.
 */
public class Humidity extends Component {
  private final static String humidity = "humidity";
  private final static String humidity_status = "humidity_status";
  private final static String humidity_fault = "humidity_fault";
  private volatile boolean has = false;
  private volatile boolean hasStatus = false;
  private volatile boolean hasFault = false;
  public Humidity(Data d){
    super(d);
    if (has=d.x.hasMapping(humidity)){
      hasStatus = d.x.hasMapping(humidity_status);
      hasFault = d.x.hasMapping(humidity_fault);
    }
  }
  /**
   * Gets an average reading of humidity over time.
   * @param times specifies how many measurements to take.
   * @param interval specifies how long to wait between each measurement (in milliseconds).
   * @return humidity or {@code null} if the value cannot be retrieved for any reason.
   */
  public Double get(int times, long interval) throws InterruptedException {
    if (times<=0 || interval<0){
      return null;
    }
    double x = 0;
    Double y;
    for (int i=0;i<times;++i){
      if (i!=0){
        Thread.sleep(interval);
      }
      if ((y=get())==null){
        return null;
      }
      x+=y;
    }
    return x/times;
  }
  /**
   * @return humidity or {@code null} if the value cannot be retrieved for any reason.
   */
  public Double get() throws InterruptedException {
    if (!has){
      return null;
    }
    String s;
    if ((s=d.x.getValue(humidity))==null){
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
   * @return whether or not humidity has been mapped.
   */
  @Override public boolean isDefined(){
    return has;
  }
  /**
   * {@inheritDoc}
   */
  @Override protected boolean fault() throws InterruptedException {
    String s;
    if (has){
      if (hasStatus){
        if ((s=d.x.getValue(humidity_status))==null){
          problem = true;
          return true;
        }else if (!parseBoolean(s)){
          return true;
        }
      }
      if (hasFault){
        if ((s=d.x.getValue(humidity_fault))==null){
          problem = true;
          return true;
        }else if (parseBoolean(s)){
          return true;
        }
      }
    }else{
      return true;
    }
    return false;
  }
}