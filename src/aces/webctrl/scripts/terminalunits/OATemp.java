package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates the outdoor air temperature.
 */
public class OATemp extends Component {
  private final static String oat = "oat";
  private final static String oat_status = "oat_status";
  private final static String oat_fault = "oat_fault";
  private volatile boolean has = false;
  private volatile boolean hasStatus = false;
  private volatile boolean hasFault = false;
  public OATemp(Data d){
    super(d);
    if (has=d.x.hasMapping(oat)){
      hasStatus = d.x.hasMapping(oat_status);
      hasFault = d.x.hasMapping(oat_fault);
    }
  }
  /**
   * Gets an average reading of outdoor air temperature over time.
   * @param times specifies how many measurements to take.
   * @param interval specifies how long to wait between each measurement (in milliseconds).
   * @return outdoor air temperature or {@code null} if the value cannot be retrieved for any reason.
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
   * @return outdoor air temperature or {@code null} if the value cannot be retrieved for any reason.
   */
  public Double get() throws InterruptedException {
    if (!has){
      return null;
    }
    String s;
    if ((s=d.x.getValue(oat))==null){
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
   * @return whether or not outdoor air temperature (OAT) has been mapped.
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
        if ((s=d.x.getValue(oat_status))==null){
          problem = true;
          return true;
        }else if (!parseBoolean(s)){
          return true;
        }
      }
      if (hasFault){
        if ((s=d.x.getValue(oat_fault))==null){
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