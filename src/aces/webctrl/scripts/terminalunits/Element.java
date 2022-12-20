package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates a heating or cooling component.
 */
public class Element extends Component {
  public volatile Boolean startTest = null;
  public volatile Boolean stopTest = null;
  private volatile BinaryInput status;
  private volatile Output main;
  private volatile Output rv;
  private volatile boolean saved = false;
  public volatile int elem;
  public volatile int stage;
  public volatile String ID;
  public volatile String name = null;
  public volatile boolean staged = false;
  public volatile boolean higherStages = false;
  public volatile boolean lowerStages = false;
  public volatile Boolean predictCool;
  /** Represents the status of the reversing switch */
  private volatile boolean config = false;
  /**
   * When {@code config==cooling}, one can assume this element is in cooling mode.
   * Otherwise, this element is in heating mode.
   */
  private volatile boolean cooling = false;
  public Element(Data d, String prefix, boolean ctrl, int elem, int stage, Boolean predictCool) throws InterruptedException {
    super(d);
    this.predictCool = predictCool;
    this.elem = elem;
    this.stage = stage;
    ID = "["+elem+"."+stage+"]";
    status = new BinaryInput(d, prefix+"_status");
    main = new Output(d, prefix);
    rv = new Output(d, prefix+"_rv");
    if (ctrl){
      main.set(false);
      rv.set(false);
    }
    name = d.x.getValue(prefix+"_name");
  }
  /**
   * Turns this element on or off.
   * @return whether this operation is successful.
   */
  public boolean set(boolean command) throws InterruptedException {
    return main.set(command);
  }
  /**
   * Records that this element is currently in cooling mode.
   * Either this method or {@link #saveHeating()} should be invoked before
   * any attempts to read or write the mode of this element.
   */
  public void saveCooling(){
    cooling = config;
    saved = true;
  }
  /**
   * Records that this element is currently in heating mode.
   * Either this method or {@link #saveCooling()} should be invoked before
   * any attempts to read or write the mode of this element.
   */
  public void saveHeating(){
    cooling = !config;
    saved = true;
  }
  /**
   * @return whether the heating / cooling configuration has been saved.
   */
  public boolean isSaved(){
    return saved;
  }
  /**
   * @return whether this element is in cooling mode.
   * @see {@link #saveCooling()}
   */
  public boolean isCooling(){
    return cooling==config;
  }
  /**
   * @return whether this element is in heating mode.
   * @see {@link #saveHeating()}
   */
  public boolean isHeating(){
    return cooling!=config;
  }
  /**
   * Attempts to put this element in cooling mode.
   * @return whether this operation was successful.
   * @see {@link #saveCooling()}
   */
  public boolean setCooling() throws InterruptedException {
    return isCooling() || invert();
  }
  /**
   * Attempts to put this element in heating mode.
   * @return whether this operation was successful.
   * @see {@link #saveHeating()}
   */
  public boolean setHeating() throws InterruptedException {
    return isHeating() || invert();
  }
  /**
   * Attempts to flip the mode of this element from cooling to heating or vice versa.
   * @return whether this operation was successful.
   */
  public boolean invert() throws InterruptedException {
    if (isInvertible() && rv.set(!config)){
      config^=true;
      return true;
    }
    return false;
  }
  /**
   * @return whether the reversing output mapping is defined, allowing this element to switch between both heating and cooling functions.
   */
  public boolean isInvertible(){
    return rv.isDefined();
  }
  /**
   * @return the {@code BinaryInput} which may be used to determine whether this element has status.
   */
  public BinaryInput status(){
    return status;
  }
  /**
   * @return whether or not sufficient tag mappings exist to set the output value for this component.
   */
  @Override public boolean isDefined(){
    return main.isDefined();
  }
  /**
   * {@inheritDoc}
   */
  @Override protected boolean fault() throws InterruptedException {
    return main.hasFault() || rv.isDefined() && rv.hasFault();
  }
  /**
   * {@inheritDoc}
   */
  @Override public boolean hasProblem(){
    return main.hasProblem() || rv.hasProblem() || status.hasProblem();
  }
}