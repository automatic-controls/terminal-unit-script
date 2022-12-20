package aces.webctrl.scripts.terminalunits;
import java.util.regex.*;
public abstract class Component {
  protected volatile boolean problem = false;
  private volatile boolean fault = false;
  protected volatile Data d;
  public Component(Data d){
    this.d = d;
  }
  /**
   * @return whether or not this component can be successfully derived from the tag mapping list.
   */
  public abstract boolean isDefined();
  /**
   * @return whether this component is has ever been flagged to have some sort of internal error.
   */
  public boolean encounteredFault(){
    return fault;
  }
  /**
   * @return whether this component is flagged to have some sort of internal error.
   */
  public boolean hasFault() throws InterruptedException {
    final boolean b = fault();
    fault|=b;
    return b;
  }
  /**
   * @return whether this component is flagged to have some sort of internal error.
   */
  protected abstract boolean fault() throws InterruptedException;
  /**
   * @return whether an issue was encountered when getting or setting a value.
   */
  public boolean hasProblem(){
    return problem;
  }
  /**
   * Used to parse booelan values from strings.
   */
  private final static Pattern falsePattern = Pattern.compile("false|[0\\.]+", Pattern.CASE_INSENSITIVE);
  /**
   * @param s is the {@code String} to parse.
   * @param nil is the returned when {@code s} is {@code null}.
   * @return the {@code boolean} value parsed from the given {@code String}.
   */
  public static boolean parseBoolean(String s, boolean nil){
    if (s==null){
      return nil;
    }
    return !falsePattern.matcher(s).matches();
  }
  /**
   * This method returns {@code false} when {@code s} is {@code null}.
   * @param s is the {@code String} to parse.
   * @return the {@code boolean} value parsed from the given {@code String}.
   * @see {@link #parseBoolean(String, boolean)}
   */
  public static boolean parseBoolean(String s){
    return parseBoolean(s,false);
  }
}