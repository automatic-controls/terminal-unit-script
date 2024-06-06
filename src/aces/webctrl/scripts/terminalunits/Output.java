package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates a binary or analog output.
 */
public class Output extends Component {
  private volatile String lock_flag = "_lock_flag";
  private volatile String lock_value = "_lock_value";
  private volatile String lock_min = "_lock_min";
  private volatile String lock_max = "_lock_max";
  private volatile String fault = "_fault";
  private volatile boolean hasLock = false;
  private volatile boolean hasLockFlag = false;
  private volatile boolean hasBounds = false;
  private volatile boolean hasFault = false;
  private volatile double min = 0;
  private volatile double max = 100;
  private volatile boolean literal = false;
  private volatile boolean locked = false;
  private volatile String prefix = null;
  public Output(Data d, String prefix) throws InterruptedException {
    super(d);
    this.prefix = prefix;
    lock_flag = prefix+lock_flag;
    lock_value = prefix+lock_value;
    lock_min = prefix+lock_min;
    lock_max = prefix+lock_max;
    fault = prefix+fault;
    hasLock = d.x.hasMapping(lock_value);
    hasLockFlag = d.x.hasMapping(lock_flag);
    if (hasLock){
      String s;
      if (hasLockFlag){
        s = d.x.getValue(lock_flag);
        if (s==null){ problem = true; }
        locked = parseBoolean(s);
      }
      hasFault = d.x.hasMapping(fault);
      hasBounds = d.x.hasMapping(lock_min) && d.x.hasMapping(lock_max);
      if (hasBounds){
        try{
          Double x=null,y=null;
          if ((s=d.x.getValue(lock_min))!=null){
            x = Double.parseDouble(s);
          }
          if ((s=d.x.getValue(lock_max))!=null){
            y = Double.parseDouble(s);
          }
          s = null;
          if (x==null || y==null){
            problem = true;
          }else if (x.equals(y)){
            literal = true;
          }else{
            min = Math.min(x,y);
            max = Math.max(x,y);
          }
        }catch(NumberFormatException e){}
      }
    }
  }
  public Output(Data d, String prefix, double min, double max) throws InterruptedException {
    super(d);
    lock_flag = prefix+lock_flag;
    lock_value = prefix+lock_value;
    lock_min = prefix+lock_min;
    lock_max = prefix+lock_max;
    fault = prefix+fault;
    hasLock = d.x.hasMapping(lock_value);
    hasLockFlag = d.x.hasMapping(lock_flag);
    if (hasLock){
      String s;
      if (hasLockFlag){
        s = d.x.getValue(lock_flag);
        if (s==null){ problem = true; }
        locked = parseBoolean(s);
      }
      hasFault = d.x.hasMapping(fault);
      hasBounds = true;
      this.min = min;
      this.max = max;
      if (min==max){
        literal = true;
      }
    }
  }
  /**
   * Releases the lock on this output.
   * @return whether the lock was released successfully.
   */
  public boolean unlock() throws InterruptedException {
    if (!hasLockFlag){
      locked = false;
      return true;
    }
    if (locked){
      if (d.x.setValueAutoMark(lock_flag, false)){
        locked = false;
      }else{
        problem = true;
      }
    }
    return !locked;
  }
  /**
   * Acquires a lock on this output.
   * @return whether the lock was acquired successfully.
   */
  public boolean lock() throws InterruptedException {
    if (!hasLockFlag){
      locked = true;
      return true;
    }
    if (!locked){
      if (d.x.setValueAutoMark(lock_flag, true)){
        locked = true;
      }else{
        problem = true;
      }
    }
    return locked;
  }
  private static String doubleToSafeString(double x){
    final String s = String.valueOf(x);
    return s.endsWith(".0")?s.substring(0,s.length()-2):s;
  }
  /**
   * Sets the value of this output. If necessary, a lock is acquired on this output.
   * @return whether the output was set successfully.
   */
  public boolean set(boolean value) throws InterruptedException {
    if (!hasLock){
      return false;
    }
    if (d.x.setValueAutoMark(lock_value, literal?value:(hasBounds?doubleToSafeString(value?max:min):(value?"1":"0")))){
      if (lock()){
        return true;
      }
    }else{
      problem = true;
    }
    return false;
  }
  /**
   * Sets the value of this output. If necessary, a lock is acquired on this output.
   * @param value is a number between 0 and 1 which represents a percentage.
   * @return whether the output was set successfully.
   */
  public boolean set(double value) throws InterruptedException {
    if (!hasLock){
      return false;
    }
    if (d.x.setValueAutoMark(lock_value, literal?value:(hasBounds?doubleToSafeString(min+value*(max-min)):(value>=0.5?"1":"0")))){
      if (lock()){
        return true;
      }
    }else{
      problem = true;
    }
    return false;
  }
  /**
   * @return whether this object has been detected to be an analog output.
   */
  public boolean isAnalog(){
    return hasBounds || literal;
  }
  /**
   * @return whether or not sufficient tag mappings exist to set the output value for this component.
   */
  @Override public boolean isDefined(){
    return hasLock;
  }
  /**
   * {@inheritDoc}
   */
  @Override protected boolean fault() throws InterruptedException {
    if (!hasLock){
      return true;
    }
    if (hasFault){
      String s = d.x.getValue(fault);
      if (s==null){ problem = true; }
      return parseBoolean(s,true);
    }
    return false;
  }
  /**
   * @return the tag prefix used to construct this output.
   */
  public String getPrefix(){
    return prefix;
  }
}