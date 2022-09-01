package aces.webctrl.scripts.terminalunits;
import aces.webctrl.scripts.commissioning.core.*;
import aces.webctrl.scripts.commissioning.web.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.function.*;
import java.util.concurrent.atomic.*;
public class TerminalUnitTest extends Script {
  private volatile boolean exited = false;
  private volatile Data[] data;
  private final AtomicInteger index = new AtomicInteger();
  private volatile boolean testDampers = false;
  private volatile boolean testFans = false;
  private volatile boolean testHeating = false;
  private volatile boolean initialized = false;
  private volatile String outputString = null;
  @Override public String getDescription(){
    return "<a href=\"https://github.com/automatic-controls/commissioning-scripts/tree/main/samples/TerminalUnitTest\" target=\"_blank\" style=\"border:none;\">Terminal Unit Commissioning Script</a> v0.1.0<br>Evaluates performance of fans, dampers, and heating components.";
  }
  @Override public String[] getParamNames(){
    return new String[]{"Dampers", "Fans", "Heating"};
  }
  @Override public void exit(){
    exited = true;
  }
  @Override public void init() throws Throwable {
    data = new Data[this.testsTotal];
    Arrays.fill(data,null);
    testDampers = this.params.getOrDefault("Dampers",false);
    testFans = this.params.getOrDefault("Fans",false);
    testHeating = this.params.getOrDefault("Heating",false);
    outputString = Utility.loadResourceAsString(TerminalUnitTest.class.getClassLoader(), "aces/webctrl/scripts/terminalunits/Output.html");
    initialized = true;
  }
  @Override public void exec(ResolvedTestingUnit x) throws Throwable {
    final int index = this.index.getAndIncrement();
    if (index>=data.length){
      Initializer.log(new ArrayIndexOutOfBoundsException("Index exceeded expected capacity: "+index+">="+data.length));
      return;
    }
    data[index] = new Data(x);
  }
  @Override public String getOutput(boolean email) throws Throwable {
    if (!initialized){
      return null;
    }
    final boolean notExited = !exited;
    return outputString.replace(
      "__TEST_FANS__",
      String.valueOf(testFans)
    ).replace(
      "__TEST_DAMPERS__",
      String.valueOf(testDampers)
    ).replace(
      "__TEST_HEATING__",
      String.valueOf(testHeating)
    ).replace(
      "__NOT_EXITED__",
      String.valueOf(notExited)
    ).replace(
      "__PROGRESS_BAR__",
      notExited ? (
        "<br><br>\n"+
        "<div style=\"position:relative;top:0;left:15%;width:70%;height:1em\">\n"+
        "<div class=\"bar\"></div>\n"+
        Utility.format("<div id=\"testsStartedBar\" class=\"bar\" style=\"background-color:indigo;width:$0%\"></div>\n", 100*this.testsStarted.get()/this.testsTotal)+
        Utility.format("<div id=\"testsCompletedBar\" class=\"bar\" style=\"background-color:blue;width:$0%\"></div>\n", 100*this.testsCompleted.get()/this.testsTotal)+
        "</div>\n"+
        "<br id=\"dataAfterBreak\">"
      ):(
        this.test.isKilled() ? (
          "<h3 id=\"dataAfterBreak\" style=\"color:crimson\">Foribly Terminated Before Natural Completion</h3>"
        ):(
          "<br id=\"dataAfterBreak\">"
        )
      )
    ).replace(
      "__CSS__",
      email ? (
        "<style>\n"+ProviderCSS.getCSS()+"\n</style>"
      ):(
        "<link rel=\"stylesheet\" type=\"text/css\" href=\"main.css\"/>"
      )
    ).replace(
      "__DATA__",
      printData(null,false).toString()
    );
  }
  @Override public void updateAJAX(HttpServletRequest req, HttpServletResponse res) throws Throwable {
    res.setContentType("text/plain");
    final StringBuilder sb = new StringBuilder(4096);
    sb.append(Utility.format("{\"started\":$0,\"completed\":$1,\"data\":", 100*this.testsStarted.get()/this.testsTotal, 100*this.testsCompleted.get()/this.testsTotal));
    printData(sb,true);
    sb.append('}');
    res.getWriter().print(ExpansionUtils.expandLinks(sb.toString(),req));
  }
  private StringBuilder printData(StringBuilder sb, final boolean JSON){
    final ArrayList<Data> list = new ArrayList<Data>(data.length);
    {
      Data d;
      for (int i=0;i<data.length;++i){
        d = data[i];
        if (d!=null){
          list.add(d);
        }
      }
    }
    list.sort(null);
    final int size = list.size();
    if (sb==null){
      sb = new StringBuilder(8+(size<<9));
    }
    if (size==0){
      sb.append("[]");
    }else{
      sb.append("[\n");
      boolean first = true;
      int group = -1;
      for (Data d:list){
        if (d.group==group){
          sb.append(",\n");
        }else{
          group = d.group;
          if (first){
            first = false;
          }else{
            sb.append("\n]\n},\n");
          }
          sb.append(Utility.format("{\n\"name\":\"$0\",\n\"equipment\":[\n", escape(JSON,this.mapping.groupNames.getOrDefault(group,"(Deleted Group)"))));
        }
        d.print(sb,JSON);
      }
      sb.append("]}]");
    }
    return sb;
  }
  private static String escape(boolean JSON, String s){
    return JSON?Utility.escapeJSON(s):Utility.escapeJS(s);
  }
  public final static int[] airflowX = new int[]{0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100};
  class Data implements Comparable<Data> {
    private final static String tagAirflow = "airflow";
    private final static String tagAirflowMaxHeat = "airflow_max_heat";
    private final static String tagAirflowMaxCool = "airflow_max_cool";
    private final static String tagAirflowLockFlag = "airflow_lock_flag";
    private final static String tagAirflowLockValue = "airflow_lock_value";
    private final static String tagDamperLockFlag = "damper_lock_flag";
    private final static String tagDamperLockValue = "damper_lock_value";
    private final static String tagDamperPosition = "damper_position";
    private final static String tagEAT = "eat";
    private final static String tagLAT = "lat";
    private final static String tagSFSSLockFlag = "sfss_lock_flag";
    private final static String tagSFSSLockValue = "sfss_lock_value";
    private final static String tagSFST = "sfst";
    private final static String tagHeatAOLockFlag = "heating_AO_lock_flag";
    private final static String tagHeatAOLockValue = "heating_AO_lock_value";
    private final static String tagHeatAOPosition = "heating_AO_position";
    private final static String tagPumpStatus = "pump_status";
    private final static String tagPumpCmdLockFlag = "pump_cmd_lock_flag";
    private final static String tagPumpCmdLockValue = "pump_cmd_lock_value";
    private final static String tagPumpRevLockFlag = "pump_rev_lock_flag";
    private final static String tagPumpRevLockValue = "pump_rev_lock_value";
    private final static double lowLimit = 40.0;
    private final static double highLimit = 120.0;
    public volatile ResolvedTestingUnit x;
    public volatile int group;
    public volatile String path;
    public volatile String link;
    public volatile long start = -1;
    public volatile long end = -1;
    public volatile boolean hasFan;
    public volatile boolean hasDamper;
    public volatile boolean hasHeatAO;
    public volatile boolean hasEAT;
    public volatile boolean hasPump;
    public volatile boolean fanError = false;
    public volatile boolean fanStopTest = false;
    public volatile boolean fanStartTest = false;
    public volatile boolean damperError = false;
    public volatile boolean damperResponse = true;
    public volatile Stats[] airflowY;
    private final static int airflowSamples = 5;
    public volatile double airflowMaxCoolValue = 0;
    public volatile double airflowMaxHeatValue = 0;
    public volatile boolean heatingError = false;
    public volatile boolean heatingFailure = false;
    public volatile String heatingFailMsg = "Failure";
    public volatile double[] heatingX;
    public volatile double[] heatingY;
    public volatile int heatingSamples;
    public Data(ResolvedTestingUnit x) throws Throwable {
      start = System.currentTimeMillis();
      this.x = x;
      group = x.getGroup();
      path = x.getDisplayPath();
      link = x.getPersistentLink();
      Boolean b;
      String s=null,t=null;
      Stats st;
      String airflowMaxHeat = null;
      String airflowMaxCool = null;
      hasFan = x.hasMapping(tagSFSSLockFlag)
        && x.hasMapping(tagSFSSLockValue)
        && x.hasMapping(tagSFST);
      hasDamper = x.hasMapping(tagAirflow)
        && x.hasMapping(tagAirflowLockFlag)
        && x.hasMapping(tagAirflowLockValue)
        && x.hasMapping(tagAirflowMaxCool)
        && x.hasMapping(tagAirflowMaxHeat)
        && x.hasMapping(tagDamperLockFlag)
        && x.hasMapping(tagDamperLockValue)
        && x.hasMapping(tagDamperPosition);
      hasHeatAO = x.hasMapping(tagHeatAOLockFlag)
        && x.hasMapping(tagHeatAOLockValue)
        && x.hasMapping(tagHeatAOPosition)
        && x.hasMapping(tagLAT);
      hasPump = x.hasMapping(tagPumpCmdLockFlag)
        && x.hasMapping(tagPumpCmdLockValue)
        && x.hasMapping(tagPumpRevLockFlag)
        && x.hasMapping(tagPumpRevLockValue)
        && x.hasMapping(tagPumpStatus)
        && x.hasMapping(tagLAT);
      hasEAT = x.hasMapping(tagEAT);
      if (hasFan && testFans || hasDamper && testDampers){
        // If we're going to test fans or dampers, first we make an attempt to shut-off all heating and cooling elements
        if (hasHeatAO){
          x.setValueAutoMark(tagHeatAOLockValue,0);
          x.setValueAutoMark(tagHeatAOLockFlag,true);
        }
        if (hasPump){
          x.setValueAutoMark(tagPumpCmdLockValue,0);
          x.setValueAutoMark(tagPumpCmdLockFlag,true);
        }
        if (hasHeatAO){
          Thread.sleep(30000);
        }
        if (hasPump){
          waitFor(180000, 3000, tagPumpStatus, new Predicate<String>(){
            public boolean test(String s){
              return s.equals("0");
            }
          });
        }
      }
      if (hasFan && testFans) fanTest: {
        // Test the fan by locking it off and then on
        if (!x.setValueAutoMark(tagSFSSLockValue, 0) || !x.setValueAutoMark(tagSFSSLockFlag, true)){
          fanError = true; break fanTest;
        }
        b = waitFor(180000, 3000, tagSFST, new Predicate<String>(){
          public boolean test(String s){
            return s.equals("0");
          }
        });
        if (b==null){
          fanError = true; break fanTest;
        }
        fanStopTest = b;
        if (!x.setValueAutoMark(tagSFSSLockValue, 1)){
          fanError = true; break fanTest;
        }
        b = waitFor(180000, 3000, tagSFST, new Predicate<String>(){
          public boolean test(String s){
            return s.equals("1");
          }
        });
        if (b==null){
          fanError = true; break fanTest;
        }
        fanStartTest = b;
      } else if (hasFan && (hasDamper && testDampers || hasHeatAO && testHeating || hasPump && testHeating)){
        // If we're going to test dampers or heating/cooling elements, then we lock the fan on
        if (x.setValueAutoMark(tagSFSSLockValue, 1) && x.setValueAutoMark(tagSFSSLockFlag, true)){
          b = waitFor(180000, 3000, tagSFST, new Predicate<String>(){
            public boolean test(String s){
              return s.equals("1");
            }
          });
          if (b!=null && b){
            fanStartTest = true;
          }
        }
      }
      if (hasDamper && testDampers) damperTest: {
        // Test the damper by locking it to 0%, and then measuring the airflow at 5% increment intervals
        try{
          if (airflowMaxCool==null && (airflowMaxCool=x.getValue(tagAirflowMaxCool))==null){
            damperError = true; break damperTest;
          }
          airflowMaxCoolValue = Double.parseDouble(airflowMaxCool);
          airflowY = new Stats[airflowX.length];
          for (int i=0;i<airflowX.length;++i){
            final int pos = airflowX[i];
            if (i==0){
              if (!x.setValueAutoMark(tagDamperLockValue, pos) || !x.setValueAutoMark(tagDamperLockFlag, true)){
                damperError = true; break damperTest;
              }
            }else{
              if (!x.setValueAutoMark(tagDamperLockValue, pos)){
                damperError = true; break damperTest;
              }
            }
            b = waitFor(240000, 3000, tagDamperPosition, new Predicate<String>(){
              public boolean test(String s){
                return Math.abs(Double.parseDouble(s)-pos)<1;
              }
            });
            if (b==null){
              damperError = true; break damperTest;
            }else if (!b){
              damperResponse = false; break damperTest;
            }
            if ((st = evaluate(airflowSamples, 2000, tagAirflow))==null){
              damperError = true; break damperTest;
            }
            airflowY[i] = st;
          }
        }catch(NumberFormatException e){
          Initializer.log(e);
          damperError = true;
        }
      }
      if (hasHeatAO && testHeating) heatTest: {
        // Test heating AO by locking to 0%, waiting for stabilization, locking to 100%, and measuring the temperature at 10 second intervals
        // If a damper exists, we lock it to the heating maximum airflow parameter
        // We ensure there is sufficient airflow at every interval of the test (e.g, to prevent fires / overheating)
        try{
          if (!fanStartTest && !hasDamper){
            heatingFailure = true;
            heatingFailMsg = "Loss of Airflow";
            break heatTest;
          }
          if ((s=x.getValue(tagHeatAOPosition))==null){
            heatingError = true; break heatTest;
          }
          final double initialPosition = Double.parseDouble(s);
          if (!x.setValueAutoMark(tagHeatAOLockValue,0) || !x.setValueAutoMark(tagHeatAOLockFlag,true)){
            heatingError = true; break heatTest;
          }
          if (hasDamper){
            final long lim = System.currentTimeMillis()+30000L+(long)(initialPosition*6000);
            if (airflowMaxHeat==null && (airflowMaxHeat=x.getValue(tagAirflowMaxHeat))==null){
              heatingError = true; break heatTest;
            }
            airflowMaxHeatValue = Math.max(Double.parseDouble(airflowMaxHeat),150);
            if (!x.setValueAutoMark(tagAirflowLockValue, airflowMaxHeatValue) || !x.setValueAutoMark(tagDamperLockFlag, false) || !x.setValueAutoMark(tagAirflowLockFlag, true)){
              heatingError = true; break heatTest;
            }
            waitFor(240000, 3000, tagAirflow, new Predicate<String>(){
              public boolean test(String s){
                return Math.abs(Double.parseDouble(s)-airflowMaxHeatValue)<50;
              }
            });
            final long dif = lim-System.currentTimeMillis();
            if (dif>0){
              Thread.sleep(dif);
            }
          }else{
            Thread.sleep(30000L+(long)(initialPosition*6000));
          }
          s = "0";
          if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
            heatingError = true; break heatTest;
          }
          final long startTime = System.currentTimeMillis();
          heatingX = new double[61];
          heatingY = new double[heatingX.length];
          heatingSamples = heatingX.length;
          heatingX[0] = 0;
          heatingY[0] = Double.parseDouble(t)-Double.parseDouble(s);
          if (!x.setValueAutoMark(tagHeatAOLockValue,100)){
            heatingError = true; break heatTest;
          }
          double yMax = 0;
          double yAvg = 0;
          double lat = 0;
          long time = 0;
          for (int i=1,j;i<heatingX.length;++i){
            Thread.sleep(10000L);
            time = System.currentTimeMillis();
            if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              heatingError = true; break heatTest;
            }else if ((b=checkAirflow())==null){
              heatingError = true; break heatTest;
            }else if (!b){
              heatingFailure = true;
              heatingFailMsg = "Loss of Airflow";
              break heatTest;
            }
            lat = Double.parseDouble(t);
            heatingX[i] = (time-startTime)/60000.0;
            heatingY[i] = lat-Double.parseDouble(s)-heatingY[0];
            if (lat>highLimit){
              heatingSamples = i+1;
              break;
            }
            if (heatingY[i]>yMax){
              yMax = heatingY[i];
            }
            if (i>21){
              yAvg = 0;
              for (j=0;j<8;++j){
                yAvg+=heatingY[i-j-3];
              }
              yAvg/=8;
              if (yAvg+yMax/50>(heatingY[i]+heatingY[i-1]+heatingY[i-2])/3){
                heatingSamples = i+1;
                break;
              }
            }
          }
          heatingY[0] = 0;
        }catch(NumberFormatException e){
          Initializer.log(e);
          heatingError = true;
        }finally{
          x.setValueAutoMark(tagHeatAOLockValue,0);
        }
      }
      if (hasPump && testHeating && !hasHeatAO) heatTest: {
        try{
          if (!fanStartTest && !hasDamper){
            heatingFailure = true;
            heatingFailMsg = "Loss of Airflow";
            break heatTest;
          }
          if (!x.setValueAutoMark(tagPumpRevLockValue,0) || !x.setValueAutoMark(tagPumpRevLockFlag,true) || !x.setValueAutoMark(tagPumpCmdLockValue,0) || !x.setValueAutoMark(tagPumpCmdLockFlag,true)){
            heatingError = true; break heatTest;
          }
          b = waitFor(180000, 3000, tagPumpStatus, new Predicate<String>(){
            public boolean test(String s){
              return s.equals("0");
            }
          });
          if (b==null){
            heatingError = true; break heatTest;
          }else if (!b){
            heatingFailure = true;
            heatingFailMsg = "Compressor Stop Failure";
            break heatTest;
          }
          Thread.sleep(480000L);
          s = "0";
          heatingX = new double[181];
          heatingY = new double[heatingX.length];
          heatingSamples = heatingX.length;
          heatingX[0] = 0;
          heatingY[0] = 0;
          for (int i=0;i<4;++i){
            Thread.sleep(3000);
            if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              heatingError = true; break heatTest;
            }
            heatingY[0]+=Double.parseDouble(t)-Double.parseDouble(s);
          }
          heatingY[0]/=4;
          final long startTime = System.currentTimeMillis();
          if (!x.setValueAutoMark(tagPumpCmdLockValue,1)){
            heatingError = true; break heatTest;
          }
          String u = "";
          int pumpStart = -1;
          int swap = -1;
          boolean swapped = false;
          boolean firstCooling = true;
          double yMax=0;
          double yMin=0;
          double sum=0;
          double yAvg=0;
          double yAvg2=0;
          long time = 0;
          double lat = 0;
          for (int i=1,j;i<heatingX.length;++i){
            Thread.sleep(10000L);
            if (pumpStart==-1){
              if ((u=x.getValue(tagPumpStatus))==null){
                heatingError = true;
                break heatTest;
              }else if (u.equals("1")){
                pumpStart = i+30;
                swap = i+((180-i)>>1)-5;
              }else if (i>18){
                heatingFailure = true;
                heatingFailMsg = "Compressor Start Failure";
                break heatTest;
              }
            }
            if (!swapped && i>=swap && swap!=-1){
              swapped = true;
              swap = i+30;
              firstCooling = sum<=0;
              if (!x.setValueAutoMark(tagPumpRevLockValue,1)){
                heatingError = true; break heatTest;
              }
            }
            time = System.currentTimeMillis();
            if (hasEAT && (s=x.getValue(tagEAT))==null || (t=x.getValue(tagLAT))==null){
              heatingError = true; break heatTest;
            }else if ((b=checkAirflow())==null){
              heatingError = true; break heatTest;
            }else if (!b){
              heatingFailure = true;
              heatingFailMsg = "Loss of Airflow";
              break heatTest;
            }
            lat = Double.parseDouble(t);
            heatingX[i] = (time-startTime)/60000.0;
            heatingY[i] = lat-Double.parseDouble(s)-heatingY[0];
            if (pumpStart!=-1 && (lat>highLimit || lat<lowLimit)){
              if (swapped){
                heatingSamples = i+1;
                break;
              }else{
                swapped = true;
                swap = i+30;
                firstCooling = sum<=0;
                if (!x.setValueAutoMark(tagPumpRevLockValue,1)){
                  heatingError = true; break heatTest;
                }
              }
            }
            sum+=heatingY[i];
            if (heatingY[i]>yMax){
              yMax = heatingY[i];
            }
            if (heatingY[i]<yMin){
              yMin = heatingY[i];
            }
            if (!swapped && i>pumpStart && pumpStart!=-1 || swapped && i>swap){
              yAvg = 0;
              for (j=0;j<10;++j){
                yAvg+=heatingY[i-j-5];
              }
              yAvg/=10;
              yAvg2 = (heatingY[i]+heatingY[i-1]+heatingY[i-2]+heatingY[i-3]+heatingY[i-4])/5;
              if (swapped){
                if (firstCooling && yAvg+yMax/50>yAvg2 || !firstCooling && yAvg+yMin/50<yAvg2){
                  heatingSamples = i+1;
                  break;
                }
              }else{
                if (sum>0 && yAvg+yMax/50>yAvg2 || sum<=0 && yAvg+yMin/50<yAvg2){
                  swapped = true;
                  swap = i+30;
                  firstCooling = sum<=0;
                  if (!x.setValueAutoMark(tagPumpRevLockValue,1)){
                    heatingError = true; break heatTest;
                  }
                }
              }
            }
          }
          heatingY[0] = 0;
        }catch(NumberFormatException e){
          Initializer.log(e);
          heatingError = true;
        }finally{
          x.setValueAutoMark(tagPumpCmdLockValue,0);
        }
      }
      end = System.currentTimeMillis();
    }
    private Boolean checkAirflow() throws Throwable {
      if (hasFan || hasDamper){
        String s=null;
        for (int i=0;i<3;++i){
          if (hasFan){
            if ((s=x.getValue(tagSFST))==null){
              return null;
            }
            if (s.equals("1")){
              return true;
            }
          }
          if (hasDamper){
            if ((s=x.getValue(tagAirflow))==null){
              return null;
            }
            if (Double.parseDouble(s)>90){
              return true;
            }
          }
          Thread.sleep(2000);
        }
      }
      return false;
    }
    private Boolean waitFor(long timeout, long interval, String tag, Predicate<String> test) throws Throwable {
      final long lim = System.currentTimeMillis()+timeout;
      String s;
      do {
        if ((s=x.getValue(tag))==null){
          return null;
        }
        if (test.test(s)){
          return true;
        }
        if (System.currentTimeMillis()>=lim){
          break;
        }
        Thread.sleep(interval);
      } while (System.currentTimeMillis()<lim);
      return false;
    }
    private Stats evaluate(int times, long interval, String tag) throws Throwable {
      final double[] arr = new double[times];
      String s;
      for (int i=0;i<times;++i){
        Thread.sleep(interval);
        if ((s=x.getValue(tag))==null){
          return null;
        }
        arr[i] = Double.parseDouble(s);
      }
      return new Stats(arr);
    }
    public void print(final StringBuilder sb, final boolean JSON){
      boolean first;
      int i,j;
      sb.append("{\n");
      sb.append(Utility.format("\"path\":\"$0\",\n", escape(JSON,path)));
      sb.append(Utility.format("\"link\":\"$0\",\n", link));
      sb.append(Utility.format("\"start\":\"$0\",\n", escape(JSON,Utility.getDateString(start))));
      sb.append(Utility.format("\"stop\":\"$0\",\n", escape(JSON,Utility.getDateString(end))));
      sb.append("\"duration\":").append((end-start)/60000.0);
      if (hasFan && testFans){
        sb.append(",\n\"fan\":{\n");
        sb.append("\"error\":").append(fanError);
        if (!fanError){
          sb.append(",\n\"start\":").append(fanStartTest);
          sb.append(",\n\"stop\":").append(fanStopTest);
        }
        sb.append("\n}");
      }
      if (hasDamper && testDampers){
        sb.append(",\n\"damper\":{\n");
        sb.append("\"error\":").append(damperError);
        if (!damperError){
          sb.append(",\n\"response\":").append(damperResponse);
          if (damperResponse){
            sb.append(",\n\"maxCool\":").append(airflowMaxCoolValue);
            sb.append(",\n\"x\":[");
            first = true;
            for (j=0;j<airflowX.length;++j){
              for (i=0;i<airflowSamples;++i){
                if (first){
                  first = false;
                }else{
                  sb.append(',');
                }
                sb.append(airflowX[j]);
              }
            }
            sb.append("],\n\"y\":[");
            first = true;
            for (Stats st:airflowY){
              for (j=0;j<st.arr.length;++j){
                if (first){
                  first = false;
                }else{
                  sb.append(',');
                }
                sb.append(st.arr[j]);
              }
            }
            sb.append("],\n\"xFit\":[");
            first = true;
            for (j=0;j<airflowX.length;++j){
              if (first){
                first = false;
              }else{
                sb.append(',');
              }
              sb.append(airflowX[j]);
            }
            sb.append("],\n\"yFit\":[");
            first = true;
            for (Stats st:airflowY){
              if (first){
                first = false;
              }else{
                sb.append(',');
              }
              sb.append(st.mean);
            }
            sb.append(']');
          }
        }
        sb.append("\n}");
      }
      if (hasHeatAO && testHeating || hasPump && testHeating){
        sb.append(",\n\"heatAO\":{\n");
        sb.append("\"error\":").append(heatingError);
        sb.append(",\n\"failure\":").append(heatingFailure);
        sb.append(",\n\"failMsg\":\"").append(escape(JSON,heatingFailMsg)).append('"');
        if (!heatingError && !heatingFailure){
          sb.append(",\n\"cooling\":").append(hasPump && !hasHeatAO);
          sb.append(",\n\"x\":[");
          first = true;
          for (j=0;j<heatingSamples;++j){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append(heatingX[j]);
          }
          sb.append("],\n\"y\":[");
          first = true;
          for (j=0;j<heatingSamples;++j){
            if (first){
              first = false;
            }else{
              sb.append(',');
            }
            sb.append(heatingY[j]);
          }
          sb.append(']');
        }
        sb.append("\n}");
      }
      sb.append("\n}");
    }
    @Override public int compareTo(Data d){
      if (group==d.group){
        return path.compareTo(d.path);
      }else{
        return group-d.group;
      }
    }
  }
}