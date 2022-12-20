package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates a fan which may or may not have a VFD.
 */
public class Fan extends Component {
  public volatile Boolean startTest = null;
  public volatile Boolean stopTest = null;
  private volatile BinaryInput status;
  private volatile Output fan;
  private volatile Output vfd;
  private volatile boolean defined;
  public volatile String id;
  public Fan(Data d, String prefix, String id) throws InterruptedException {
    super(d);
    this.id = id;
    status = new BinaryInput(d, prefix+"_status");
    fan = new Output(d, prefix);
    vfd = new Output(d, prefix+"_vfd");
    defined = fan.isDefined() || vfd.isDefined();
  }
  /**
   * Turns the fan on or off.
   * @return whether the command signal was successfully applied.
   */
  public boolean set(boolean signal) throws InterruptedException {
    return defined && (!fan.isDefined() || fan.set(signal)) && (!vfd.isDefined() || vfd.set(signal));
  }
  /**
   * @return the {@code BinaryInput} which may be used to determine whether this fan has status.
   */
  public BinaryInput status(){
    return status;
  }
  /**
   * @return whether or not sufficient tag mappings exist to set the output value for this component.
   */
  @Override public boolean isDefined(){
    return defined;
  }
  /**
   * @return whether or not the VFD tag mappings exist.
   */
  public boolean hasVFD(){
    return vfd.isDefined();
  }
  /**
   * {@inheritDoc}
   */
  @Override protected boolean fault() throws InterruptedException {
    return !defined || fan.isDefined() && fan.hasFault() || vfd.isDefined() && vfd.hasFault();
  }
  /**
   * {@inheritDoc}
   */
  @Override public boolean hasProblem(){
    return fan.hasProblem() || vfd.hasProblem() || status.hasProblem();
  }
}