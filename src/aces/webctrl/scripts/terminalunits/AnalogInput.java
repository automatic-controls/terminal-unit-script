package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates a analog input.
 */
public class AnalogInput extends Component {
  private volatile String tag = null;
  private volatile boolean has = false;
  public AnalogInput(Data d, String tag){
    super(d);
    this.tag = tag;
    has = d.x.hasMapping(tag);
  }
  /**
   * Gets an average reading over time.
   * @param times specifies how many measurements to take.
   * @param interval specifies how long to wait between each measurement (in milliseconds).
   * @return the averaged value or {@code null} if the average cannot be retrieved for any reason.
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
   * Gets an average reading over time.
   * @param times specifies how many measurements to take.
   * @param interval specifies how long to wait between each measurement (in milliseconds).
   * @param def specifies a default value which is assumed when an individual measurement fails for any reason.
   * @return the averaged value.
   */
  public double get(int times, long interval, double def) throws InterruptedException {
    if (times<=0 || interval<0){
      return def;
    }
    double x = 0;
    for (int i=0;i<times;++i){
      if (i!=0){
        Thread.sleep(interval);
      }
      x+=get(def);
    }
    return x/times;
  }
  /**
   * @return the retrieved value, or {@code null} if any error occurs.
   */
  public Double get() throws InterruptedException {
    if (!has){
      return null;
    }
    String s;
    try{
      if ((s=d.x.getValue(tag))==null){
        problem = true;
        return null;
      }else{
        return Double.parseDouble(s);
      }
    }catch(NumberFormatException e){
      return null;
    }
  }
  /**
   * @return the retrieved value, or the given default value {@code def} if any error occurs.
   */
  public double get(double def) throws InterruptedException {
    if (!has){
      return def;
    }
    String s;
    try{
      if ((s=d.x.getValue(tag))==null){
        problem = true;
        return def;
      }else{
        return Double.parseDouble(s);
      }
    }catch(NumberFormatException e){
      return def;
    }
  }
  /**
   * Waits for this input to register a specific value.
   * @param value is what we're waiting for.
   * @param threshold specifies how close we need to get.
   * @param timeout specifies the maximum time to wait in milliseconds.
   * @param interval specifies the time in milliseconds to wait between value validations.
   * @return whether the value was successfully achieved during the specified timeout.
   */
  public boolean waitFor(double value, double threshold, long timeout, long interval) throws InterruptedException {
    if (!has){
      return false;
    }
    final long end = System.currentTimeMillis()+timeout;
    long dif;
    Double b;
    while (true){
      if ((b=get())==null){
        return false;
      }else if (Math.abs(b-value)<=threshold){
        return true;
      }
      dif = Math.min(interval, end-System.currentTimeMillis());
      if (dif<=0){
        break;
      }
      Thread.sleep(dif);
    }
    return false;
  }
  /**
   * Waits for this input to register a value greater than or equal to what is specified.
   * @param value is the minimum acceptable reading.
   * @param timeout specifies the maximum time to wait in milliseconds.
   * @param interval specifies the time in milliseconds to wait between value validations.
   * @return whether the value was successfully achieved during the specified timeout.
   */
  public boolean waitForGtr(double value, long timeout, long interval) throws InterruptedException {
    if (!has){
      return false;
    }
    final long end = System.currentTimeMillis()+timeout;
    long dif;
    Double b;
    while (true){
      if ((b=get())==null){
        return false;
      }else if (b>=value){
        return true;
      }
      dif = Math.min(interval, end-System.currentTimeMillis());
      if (dif<=0){
        break;
      }
      Thread.sleep(dif);
    }
    return false;
  }
  /**
   * Waits for this input to register a value less than or equal to what is specified.
   * @param value is the maximum acceptable reading.
   * @param timeout specifies the maximum time to wait in milliseconds.
   * @param interval specifies the time in milliseconds to wait between value validations.
   * @return whether the value was successfully achieved during the specified timeout.
   */
  public boolean waitForLss(double value, long timeout, long interval) throws InterruptedException {
    if (!has){
      return false;
    }
    final long end = System.currentTimeMillis()+timeout;
    long dif;
    Double b;
    while (true){
      if ((b=get())==null){
        return false;
      }else if (b<=value){
        return true;
      }
      dif = Math.min(interval, end-System.currentTimeMillis());
      if (dif<=0){
        break;
      }
      Thread.sleep(dif);
    }
    return false;
  }
  /**
   * @return whether the mapping has been defined.
   */
  @Override public boolean isDefined(){
    return has;
  }
  /**
   * @return whether the mapping is undefined.
   */
  @Override protected boolean fault(){
    return !has;
  }
}