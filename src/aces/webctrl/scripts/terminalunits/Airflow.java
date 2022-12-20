package aces.webctrl.scripts.terminalunits;
/**
 * Encapsulates an airflow microblock.
 */
public class Airflow extends Component {
  public volatile double[] readings = null;
  private final static String measured = "airflow_measured";
  private final static String setp_lock_flag = "airflow_setp_lock_flag";
  private final static String setp_lock_value = "airflow_setp_lock_value";
  private final static String max_cool = "airflow_for_max_cool";
  private final static String max_heat = "airflow_for_max_heat";
  private final static String damper_lock_flag = "airflow_damper_lock_flag";
  private final static String damper_lock_value = "airflow_damper_lock_value";
  private final static String damper_position = "airflow_damper_position";
  private final static String fault = "airflow_fault";
  private volatile AnalogInput airflowActual;
  private volatile Output airflowSetpoint;
  private volatile AnalogInput damperActual;
  private volatile Output damperSetpoint;
  private volatile boolean defined;
  private volatile int maxCool = -1;
  private volatile int maxHeat = -1;
  public Airflow(Data d) throws InterruptedException {
    super(d);
    defined = d.x.hasMapping(measured)
      && d.x.hasMapping(setp_lock_flag)
      && d.x.hasMapping(setp_lock_value)
      && d.x.hasMapping(max_cool)
      && d.x.hasMapping(max_heat)
      && d.x.hasMapping(damper_lock_flag)
      && d.x.hasMapping(damper_lock_value)
      && d.x.hasMapping(damper_position)
      && d.x.hasMapping(fault);
    if (defined){
      try{
        String s;
        if ((s=d.x.getValue(max_heat))==null){
          maxHeat = 150;
          problem = true;
        }else{
          maxHeat = Math.max((int)Double.parseDouble(s),150);
        }
        if ((s=d.x.getValue(max_cool))==null){
          maxCool = maxHeat;
          problem = true;
        }else{
          maxCool = Math.max((int)Double.parseDouble(s),150);
        }
      }catch(NumberFormatException e){
        if (maxHeat==-1){
          maxHeat = 150;
        }
        maxCool = maxHeat;
      }
      airflowActual = new AnalogInput(d, measured);
      damperActual = new AnalogInput(d, damper_position);
      airflowSetpoint = new Output(d, "airflow_setp", 0, 0);
      damperSetpoint = new Output(d, "airflow_damper", 0, 0);
    }
  }
  /**
   * Controls the airflow setpoint.
   * @return whether the airflow setpoint was successfully set.
   */
  public boolean setAirflow(int x) throws InterruptedException {
    return x>=0 && damperSetpoint.unlock() && airflowSetpoint.set(x);
  }
  /**
   * Controls the damper position setpoint.
   * @param x should be between 0 and 100.
   * @return whether the damper position setpoint was successfully set.
   */
  public boolean setDamperPosition(double x) throws InterruptedException {
    return x>=0 && x<=100 && airflowSetpoint.unlock() && damperSetpoint.set(x);
  }
  /**
   * @return the ideal airflow for when cooling elements are cranked to 100%.
   */
  public int getCoolingAirflow(){
    return maxCool;
  }
  /**
   * @return the ideal airflow for when heating elements are cranked to 100%.
   */
  public int getHeatingAirflow(){
    return maxHeat;
  }
  /**
   * @return the {@code AnalogInput} corresponding to measured airflow.
   */
  public AnalogInput getAirflow(){
    return airflowActual;
  }
  /**
   * @return the {@code AnalogInput} corresponding to the actual damper position.
   */
  public AnalogInput getDamperPosition(){
    return damperActual;
  }
  /**
   * @return whether or not sufficient tag mappings exist to set the output value for this component.
   */
  @Override public boolean isDefined(){
    return defined;
  }
  /**
   * {@inheritDoc}
   */
  @Override protected boolean fault() throws InterruptedException {
    if (!defined){
      return true;
    }
    String s = d.x.getValue(fault);
    if (s==null){ problem = true; }
    return parseBoolean(s,true);
  }
  /**
   * {@inheritDoc}
   */
  @Override public boolean hasProblem(){
    return problem || airflowActual.hasProblem() || airflowSetpoint.hasProblem() || damperActual.hasProblem() || damperSetpoint.hasProblem();
  }
}