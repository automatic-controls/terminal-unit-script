package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates the outdoor and return air dampers.
 */
public class OADamper extends Component {
  public volatile Boolean test = null;
  private volatile Output oad;
  private volatile Output rad;
  private volatile double oa_min;
  public OADamper(Data d) throws InterruptedException {
    super(d);
    {
      AnalogInput ai;
      oa_min = (ai=new AnalogInput(d, "oa_damper_min_position")).get(20)/100;
      problem|=ai.problem;
    }
    oad = new Output(d, "oa_damper");
    rad = new Output(d, "ra_damper");
  }
  /**
   * @return the minimum OA damper position (typically used when heating or cooling elements are active).
   */
  public double getMinimum(){
    return oa_min;
  }
  /**
   * Sets the damper position.
   * @param value is a number between 0 and 1 which represents a percentage damper position for the OAD.
   * @return whether the damper position was set successfully.
   */
  public boolean set(double value) throws InterruptedException {
    return oad.set(value) && (!rad.isDefined() || rad.set(1-value));
  }
  /**
   * @return whether or not sufficient tag mappings exist to set the output value for this component.
   */
  @Override public boolean isDefined(){
    return oad.isDefined();
  }
  /**
   * @return whether or not the RAD mappings exist.
   */
  public boolean hasRAD(){
    return rad.isDefined();
  }
  /**
   * {@inheritDoc}
   */
  @Override protected boolean fault() throws InterruptedException {
    return oad.hasFault() || rad.isDefined() && rad.hasFault();
  }
  /**
   * {@inheritDoc}
   */
  @Override public boolean hasProblem(){
    return problem || oad.hasProblem() || rad.hasProblem();
  }
}