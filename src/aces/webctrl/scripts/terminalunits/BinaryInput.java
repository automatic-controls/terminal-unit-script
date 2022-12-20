package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates a binary input.
 */
public class BinaryInput extends Component {
  private volatile String tag = null;
  private volatile boolean has = false;
  public BinaryInput(Data d, String tag){
    super(d);
    this.tag = tag;
    has = d.x.hasMapping(tag);
  }
  /**
   * @return the retrieved value, or {@code null} if any error occurs.
   */
  public Boolean get() throws InterruptedException {
    if (!has){
      return null;
    }
    String s;
    if ((s=d.x.getValue(tag))==null){
      problem = true;
      return null;
    }else{
      return parseBoolean(s);
    }
  }
  /**
   * @return the retrieved value, or the given default value {@code def} if any error occurs.
   */
  public boolean get(boolean def) throws InterruptedException {
    if (!has){
      return def;
    }
    String s;
    if ((s=d.x.getValue(tag))==null){
      problem = true;
      return def;
    }else{
      return parseBoolean(s);
    }
  }
  /**
   * Waits for this input to register a specific value.
   * @param value is what we're waiting for.
   * @param timeout specifies the maximum time to wait in milliseconds.
   * @param interval specifies the time in milliseconds to wait between value validations.
   * @return whether the value was successfully achieved during the specified timeout.
   */
  public boolean waitFor(boolean value, long timeout, long interval) throws InterruptedException {
    if (!has){
      return false;
    }
    final long end = System.currentTimeMillis()+timeout;
    long dif;
    Boolean b;
    while (true){
      if ((b=get())==null){
        return false;
      }else if (b==value){
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