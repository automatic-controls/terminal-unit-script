package aces.webctrl.scripts.terminalunits;
import aces.webctrl.scripts.commissioning.core.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;
public class Data implements Comparable<Data> {
  private final static DecimalFormat df = new DecimalFormat("0.#");
  static { df.setMaximumFractionDigits(5); }
  private final static Pattern fanMatcher = Pattern.compile("(fan(\\d++))(?:_vfd)?_lock_flag");
  private final static Pattern elementMatcher = Pattern.compile("(([ech])(\\d++)(?:_(\\d++))?)_lock_flag");
  private final static Pattern onMatcher = Pattern.compile("(on\\d++)_lock_flag");
  private final static Pattern offMatcher = Pattern.compile("(off\\d++)_lock_flag");
  public volatile ResolvedTestingUnit x;
  public volatile Params p;
  public volatile int group;
  public volatile String path;
  public volatile String link;
  public volatile long start = -1;
  public volatile long end = -1;
  private volatile BinaryInput alarm;
  private volatile PrimaryTemp temp;
  private volatile Humidity hum;
  private volatile Output dehum;
  private volatile BinaryInput dehumStatus;
  private volatile OATemp oat;
  private volatile OADamper oad;
  private volatile Airflow air;
  private volatile Fan fans[];
  private volatile boolean hasFans = false;
  private volatile Element elements[];
  private volatile int numElements = 0;
  private volatile boolean dehumStartTest = false;
  private volatile boolean dehumStopTest = false;
  private volatile boolean alarmTriggered = false;
  private volatile boolean lossOfAirflow = false;
  private volatile Trend tempTrend = null;
  private volatile Trend humTrend = null;
  private volatile double baseTemp = 0;
  private volatile String desc = "Untested";
  private final static int timeMult = 26;
  private final static long timeout = timeMult*10000L;
  public Data(ResolvedTestingUnit x, Params p) throws Throwable {
    start = System.currentTimeMillis();
    try{
      this.x = x;
      this.p = p;
      group = x.getGroup();
      path = x.getDisplayPath();
      link = x.getPersistentLink();
      final boolean ctrl = p.ctrlDampers || p.ctrlFans || p.ctrlTemp;
      alarm = new BinaryInput(this, "alarm");
      temp = new PrimaryTemp(this);
      hum = new Humidity(this);
      dehum = new Output(this, "dehum");
      dehumStatus = new BinaryInput(this, "dehum_status");
      oat = new OATemp(this);
      oad = new OADamper(this);
      air = new Airflow(this);
      if (!alarm.isDefined()){ alarm = null; }
      if (!temp.isDefined()){ temp = null; }
      if (!hum.isDefined()){ hum = null; }
      if (!dehum.isDefined()){ dehum = null; }
      if (!dehumStatus.isDefined()){ dehumStatus = null; }
      if (!oat.isDefined()){ oat = null; }
      if (!oad.isDefined()){ oad = null; }
      if (!air.isDefined()){ air = null; }
      {
        Set<String> tags = x.getTags();
        int n = 0, m = 0;
        for (String s:tags){
          if (fanMatcher.matcher(s).matches()){
            ++n;
          }else if (elementMatcher.matcher(s).matches()){
            ++m;
          }
        }
        fans = new Fan[n];
        elements = new Element[m];
        n = 0;
        m = 0;
        String ss;
        Matcher mm;
        for (String s:tags){
          if ((mm=fanMatcher.matcher(s)).matches()){
            fans[n] = new Fan(this, mm.group(1), mm.group(2));
            if (fans[n].isDefined()){
              hasFans = true;
            }else{
              fans[n] = null;
            }
            ++n;
          }else if ((mm=elementMatcher.matcher(s)).matches()){
            try{
              s = mm.group(2);
              ss = mm.group(4);
              elements[m] = new Element(this, mm.group(1), ctrl, Integer.parseInt(mm.group(3)), ss==null?1:Integer.parseInt(ss), s.equals("e")?null:s.equals("c"));
              if (elements[m].isDefined()){
                ++numElements;
              }else{
                elements[m] = null;
              }
              ++m;
            }catch(NumberFormatException e){
              elements[m] = null;
            }
          }else if (ctrl){
            if ((mm=onMatcher.matcher(s)).matches()){
              new Output(this, mm.group(1)).set(true);
            }else if ((mm=offMatcher.matcher(s)).matches()){
              new Output(this, mm.group(1)).set(false);
            }
          }
        }
      }
      Arrays.sort(elements, new Comparator<Element>(){
        public int compare(Element a, Element b){
          if (a==b){
            return 0;
          }else if (a==null){
            return -1;
          }else if (b==null){
            return 1;
          }else if (eq(a.predictCool,b.predictCool)){
            if (a.elem==b.elem){
              return a.stage-b.stage;
            }else{
              return a.elem-b.elem;
            }
          }else if (a.predictCool==null){
            return -1;
          }else if (b.predictCool==null){
            return 1;
          }else{
            return a.predictCool.booleanValue()?1:-1;
          }
        }
      });
      int i,j;
      {
        boolean a,b;
        for (i=0;i<elements.length;++i){
          if (elements[i]!=null){
            a = i-1>=0 && elements[i-1]!=null && elements[i].elem==elements[i-1].elem;
            b = i+1<elements.length && elements[i+1]!=null && elements[i].elem==elements[i+1].elem;
            elements[i].staged = a || b;
            elements[i].higherStages = a && elements[i].stage<elements[i-1].stage || b && elements[i].stage<elements[i+1].stage;
            elements[i].lowerStages = a && elements[i].stage>elements[i-1].stage || b && elements[i].stage>elements[i+1].stage;
          }
        }
      }
      if (ctrl) testing:{
        final Pause pause = new Pause();
        if (alarm!=null && alarm.get(false)){
          alarmTriggered = true;
          break testing;
        }
        if (p.ctrlTemp){
          final long end = System.currentTimeMillis()+timeout;
          for (i=0;i<elements.length;++i){
            if (elements[i]!=null && elements[i].status().isDefined() && !elements[i].hasFault()){
              elements[i].stopTest = elements[i].status().waitFor(false, end-System.currentTimeMillis(), 3000L);
              if (alarm!=null && alarm.get(false)){
                alarmTriggered = true;
                break testing;
              }
            }
          }
          pause.reset(timeout);
        }
        if (air!=null && !air.hasFault()){
          air.setDamperPosition(100);
        }
        if (oad!=null && !oad.hasFault()){
          oad.set(0);
        }
        if (dehum!=null && !dehum.hasFault()){
          dehum.set(false);
        }
        if (p.ctrlFans){
          for (i=0;i<fans.length;++i){
            if (fans[i]!=null && !fans[i].hasFault() && fans[i].status().isDefined()){
              fans[i].stopTest = fans[i].set(false);
            }
          }
          long end = System.currentTimeMillis()+timeout;
          for (i=0;i<fans.length;++i){
            if (fans[i]!=null && fans[i].stopTest!=null && fans[i].stopTest){
              fans[i].stopTest&=fans[i].status().waitFor(false, end-System.currentTimeMillis(), 3000);
            }
          }
          if (alarm!=null && alarm.get(false)){
            alarmTriggered = true;
            break testing;
          }
          for (i=0;i<fans.length;++i){
            if (fans[i]!=null && !fans[i].hasFault()){
              if (fans[i].status().isDefined()){
                fans[i].startTest = fans[i].set(true);
              }else{
                fans[i].set(true);
              }
            }
          }
          end = System.currentTimeMillis()+timeout;
          for (i=0;i<fans.length;++i){
            if (fans[i]!=null && fans[i].startTest!=null && fans[i].startTest){
              fans[i].startTest&=fans[i].status().waitFor(true, end-System.currentTimeMillis(), 3000);
            }
          }
          if (alarm!=null && alarm.get(false)){
            alarmTriggered = true;
            break testing;
          }
        }else{
          for (i=0;i<fans.length;++i){
            if (fans[i]!=null && !fans[i].hasFault()){
              fans[i].set(true);
            }
          }
          if (alarm!=null && alarm.get(false)){
            alarmTriggered = true;
            break testing;
          }
        }
        if (p.ctrlDampers && air!=null && !air.encounteredFault() && !air.hasProblem()){
          air.readings = new double[21];
          Arrays.fill(air.readings, 0.0);
          Double z;
          for (i=100,j=20;i>=0;i-=5,--j){
            if (!air.hasFault() && air.setDamperPosition(i) && air.getDamperPosition().waitFor(i, 1, timeout, 3000L) && (z=air.getAirflow().get(5, 2000L))!=null){
              air.readings[j] = z;
            }else{
              air.readings = null;
              break;
            }
            if (alarm!=null && alarm.get(false)){
              alarmTriggered = true;
              air.readings = null;
              break testing;
            }
          }
        }
        if (p.ctrlTemp && temp!=null && !temp.hasFault() && numElements>0){
          if (air!=null && !air.hasFault() && air.setDamperPosition(100)){
            air.getDamperPosition().waitForGtr(80, timeout, 3000);
          }
          if (!hasAirflow()){
            lossOfAirflow = true;
            break testing;
          }
          if (alarm!=null && alarm.get(false)){
            alarmTriggered = true;
            break testing;
          }
          pause.pause();
          Double t;
          tempTrend = new Trend(128);
          {
            double w = 0, v = 0;
            for (j=0;j<30;++j){
              if ((t=temp.getValue())==null){
                break testing;
              }
              if (alarm!=null && alarm.get(false)){
                alarmTriggered = true;
                break testing;
              }
              tempTrend.add(t);
              if (j>25){
                w+=t;
                if ((t=temp.getLAT())==null){
                  break testing;
                }
                v+=t;
              }
              if (j!=29){
                Thread.sleep(10000L);
              }
            }
            baseTemp = v/4;
            temp.setMark(w/=4);
            for (i=0;i<tempTrend.len;++i){
              tempTrend.y[i]-=w;
            }
            tempTrend.min-=w;
            tempTrend.max-=w;
            tempTrend.sum-=w*tempTrend.len;
          }
          tempTrend.reset("Off");
          long end, dangerZone = 0;
          Boolean b;
          Segment seg;
          for (i=0;i<elements.length;++i){
            if (elements[i]!=null){
              if (elements[i].hasFault() || !elements[i].set(true)){
                if (elements[i].staged && !elements[i].higherStages){
                  for (j=0;j<elements.length;++j){
                    if (elements[j]!=null && elements[i].elem==elements[j].elem){
                      elements[j].set(false);
                    }
                  }
                  for (j=0;j<timeMult;++j){
                    Thread.sleep(10000L);
                    if ((t=temp.getValue())==null){
                      break testing;
                    }
                    if (alarm!=null && alarm.get(false)){
                      alarmTriggered = true;
                      break testing;
                    }
                    tempTrend.add(t);
                  }
                  tempTrend.reset("Off");
                }
                continue;
              }
              if (elements[i].lowerStages && temp.danger()){
                continue;
              }
              if (air!=null && !air.hasFault()){
                if (elements[i].predictCool==null){
                  air.setDamperPosition(100);
                }else{
                  air.setAirflow(elements[i].predictCool?air.getCoolingAirflow():air.getHeatingAirflow());
                }
              }
              if (alarm!=null && alarm.get(false)){
                alarmTriggered = true;
                break testing;
              }
              if (elements[i].status().isDefined()){
                elementStatusWait:{
                  j = 0;
                  end = System.currentTimeMillis()+timeout;
                  while (true){
                    if ((b=elements[i].status().get())==null){
                      elements[i].startTest = false;
                      break elementStatusWait;
                    }else if (b){
                      elements[i].startTest = true;
                      break elementStatusWait;
                    }
                    if (end<=System.currentTimeMillis()){
                      break;
                    }
                    Thread.sleep(3000);
                    if (++j==3){
                      j = 0;
                      if ((t=temp.getValue())==null){
                        break testing;
                      }
                      tempTrend.add(t);
                    }
                  }
                  elements[i].startTest = false;
                }
                if (!elements[i].startTest){
                  elements[i].set(false);
                  continue;
                }
                if (alarm!=null && alarm.get(false)){
                  alarmTriggered = true;
                  break testing;
                }
              }
              while (true){
                Thread.sleep(10000L);
                if (!hasAirflow()){
                  lossOfAirflow = true;
                  break testing;
                }
                if ((t=temp.getValue())==null){
                  break testing;
                }
                if (alarm!=null && alarm.get(false)){
                  alarmTriggered = true;
                  break testing;
                }
                if (!tempTrend.add(t) || temp.danger() && dangerZone<System.currentTimeMillis()){
                  seg = tempTrend.reset(null);
                  boolean heat = elements[i].predictCool==null||elements[i].isInvertible()?seg.increasing:!elements[i].predictCool;
                  seg.name = elements[i].ID+" - "+(heat?"Heating":"Cooling");
                  seg.lowerStages = elements[i].lowerStages;
                  seg.lowDanger = temp.lowLimitPass;
                  seg.highDanger = temp.highLimitPass;
                  if (heat){
                    elements[i].saveHeating();
                  }else{
                    elements[i].saveCooling();
                  }
                  if (temp.danger()){
                    dangerZone = System.currentTimeMillis()+60000L;
                  }
                  break;
                }
              }
              if (!elements[i].staged && !elements[i].hasFault() && elements[i].isInvertible() && elements[i].invert()){
                while (true){
                  Thread.sleep(10000L);
                  if (!hasAirflow()){
                    lossOfAirflow = true;
                    break testing;
                  }
                  if ((t=temp.getValue())==null){
                    break testing;
                  }
                  if (alarm!=null && alarm.get(false)){
                    alarmTriggered = true;
                    break testing;
                  }
                  if (!tempTrend.add(t) || temp.danger() && dangerZone<System.currentTimeMillis()){
                    seg = tempTrend.reset(null);
                    seg.name = elements[i].ID+" - "+(seg.increasing?"Heating":"Cooling");
                    seg.lowDanger = temp.lowLimitPass;
                    seg.highDanger = temp.highLimitPass;
                    if (temp.danger()){
                      dangerZone = System.currentTimeMillis()+60000L;
                    }
                    break;
                  }
                }
              }
              if (!elements[i].higherStages){
                if (elements[i].staged){
                  for (j=0;j<elements.length;++j){
                    if (elements[j]!=null && elements[i].elem==elements[j].elem){
                      elements[j].set(false);
                    }
                  }
                }else{
                  elements[i].set(false);
                }
                for (j=0;j<timeMult;++j){
                  Thread.sleep(10000L);
                  if ((t=temp.getValue())==null){
                    break testing;
                  }
                  if (alarm!=null && alarm.get(false)){
                    alarmTriggered = true;
                    break testing;
                  }
                  tempTrend.add(t);
                }
                tempTrend.reset("Off");
              }else if (elements[i].staged){
                dangerZone = 0L;
              }
            }
          }
          if (dehum!=null && hum!=null && !dehum.hasFault() && !hum.hasFault()){
            for (i=0;i<elements.length;++i){
              if (elements[i]!=null){
                elements[i].setCooling();
              }
            }
            if ((t=hum.get(5, 2000))==null){
              break testing;
            }
            if (dehumStatus!=null && !dehumStatus.get(true)){
              dehumStopTest = true;
            }
            if (dehum.set(true)){
              humTrend = new Trend(64);
              humTrend.offset = 45;
              humTrend.add(t);
              while (true){
                Thread.sleep(10000L);
                if (!hasAirflow()){
                  lossOfAirflow = true;
                  break testing;
                }
                if (alarm!=null && alarm.get(false)){
                  alarmTriggered = true;
                  break testing;
                }
                if ((t=temp.getValue())==null){
                  break testing;
                }
                tempTrend.add(t);
                if ((t=hum.get())==null){
                  break testing;
                }
                if (!humTrend.add(t) || temp.danger() && dangerZone<System.currentTimeMillis()){
                  humTrend.reset("Dehumidification");
                  seg = tempTrend.reset("Dehumidification");
                  seg.lowDanger = temp.lowLimitPass;
                  seg.highDanger = temp.highLimitPass;
                  break;
                }
                if (dehumStatus!=null && !dehumStartTest && !dehumStatus.hasFault() && dehumStatus.get(false)){
                  dehumStartTest = true;
                }
              }
            }
          }
        }
      }
      final StringBuilder sb = new StringBuilder(256);
      if (alarm!=null){
        sb.append("<br>Alarm");
      }
      if (temp!=null){
        sb.append("<br>Leaving&nbsp;Air&nbsp;Temperature");
        if (temp.hasEAT()){
          sb.append("<br>Entering&nbsp;Air&nbsp;Temperature");
        }
      }
      if (oat!=null){
        sb.append("<br>Outdoor&nbsp;Air&nbsp;Temperature");
      }
      if (hum!=null){
        sb.append("<br>Humidity&nbsp;Sensor");
      }
      if (dehum!=null){
        sb.append("<br>Dehumidification&nbsp;Command");
        if (dehumStatus!=null){
          sb.append("&nbsp;+&nbsp;Status");
        }
      }
      if (oad!=null){
        sb.append("<br>Outdoor&nbsp;Air&nbsp;Damper");
        if (oad.hasRAD()){
          sb.append("<br>Return&nbsp;Air&nbsp;Damper");
        }
      }
      if (air!=null){
        sb.append("<br>Airflow&nbsp;Microblock");
      }
      if (hasFans){
        for (i=0;i<fans.length;++i){
          if (fans[i]!=null){
            sb.append("<br>Fan&nbsp;["+fans[i].id+"]");
            if (fans[i].status().isDefined()){
              sb.append("&nbsp;+&nbsp;Status");
            }
            if (fans[i].hasVFD()){
              sb.append("&nbsp;+&nbsp;VFD");
            }
          }
        }
      }
      if (numElements>0){
        for (i=0;i<elements.length;++i){
          if (elements[i]!=null){
            sb.append("<br>");
            if (elements[i].name!=null){
              sb.append(elements[i].name.replace(" ", "&nbsp;")).append("&nbsp;");
            }
            sb.append(elements[i].ID);
            if (elements[i].status().isDefined()){
              sb.append("&nbsp;+&nbsp;Status");
            }
            if (elements[i].name==null){
              if (elements[i].isInvertible()){
                sb.append("&nbsp;+&nbsp;Dual");
              }else if (elements[i].isSaved()){
                if (elements[i].isCooling()){
                  sb.append("&nbsp;+&nbsp;Cooling");
                }else{
                  sb.append("&nbsp;+&nbsp;Heating");
                }
              }else if (elements[i].predictCool!=null){
                if (elements[i].predictCool){
                  sb.append("&nbsp;+&nbsp;Cooling");
                }else{
                  sb.append("&nbsp;+&nbsp;Heating");
                }
              }
            }
          }
        }
      }
      if (sb.length()>0){
        sb.delete(0,4);
      }
      desc = sb.toString();
    }finally{
      if (tempTrend!=null && tempTrend.getSegmentLength()>0){
        tempTrend.reset("Unknown");
      }
      if (humTrend!=null && humTrend.getSegmentLength()>0){
        humTrend.reset("Unknown");
      }
      end = System.currentTimeMillis();
    }
  }
  private boolean hasAirflow() throws InterruptedException {
    if (air!=null && !air.hasFault() && air.getAirflow().get(0)>90){
      return true;
    }
    if (hasFans){
      for (int i=0;i<fans.length;++i){
        if (fans[i]!=null && !fans[i].hasFault() && fans[i].status().get(false)){
          return true;
        }
      }
    }
    return air==null && !hasFans;
  }
  public void print(final StringBuilder sb, final boolean JSON){
    int i;
    boolean prefix;
    sb.append("{\n");
    sb.append(Utility.format("\"description\":\"$0\",\n", escape(JSON,desc)));
    sb.append(Utility.format("\"path\":\"$0\",\n", escape(JSON,path)));
    sb.append(Utility.format("\"link\":\"$0\",\n", link));
    sb.append(Utility.format("\"start\":\"$0\",\n", escape(JSON,Utility.getDateString(start))));
    sb.append(Utility.format("\"stop\":\"$0\",\n", escape(JSON,Utility.getDateString(end))));
    sb.append(Utility.format("\"duration\":$0,\n", df.format((end-start)/60000.0)));
    sb.append(Utility.format("\"alarmTriggered\":$0,\n", alarmTriggered));
    sb.append(Utility.format("\"lossOfAirflow\":$0,\n", lossOfAirflow));
    prefix = false;
    sb.append("\"problems\":[\n");
    prefix|=addProblem(sb, alarm, "Alarm", prefix);
    prefix|=addProblem(sb, temp, "Primary Temperature Sensor", prefix);
    prefix|=addProblem(sb, hum, "Humidity Sensor", prefix);
    prefix|=addProblem(sb, dehum, "Dehumidification Command", prefix);
    prefix|=addProblem(sb, dehumStatus, "Dehumidification Status", prefix);
    prefix|=addProblem(sb, oat, "OAT Sensor", prefix);
    prefix|=addProblem(sb, oad, "OAD Command", prefix);
    prefix|=addProblem(sb, air, "Airflow Microblock", prefix);
    if (hasFans){
      for (i=0;i<fans.length;++i){
        prefix|=addProblem(sb, fans[i], "Fan["+fans[i].id+"]", prefix);
      }
    }
    if (numElements>0){
      for (i=0;i<elements.length;++i){
        prefix|=addProblem(sb, elements[i], elements[i].ID, prefix);
      }
    }
    sb.append("\n],\n");
    prefix = false;
    sb.append("\"faults\":[\n");
    prefix|=addFault(sb, alarm, "Alarm", prefix);
    prefix|=addFault(sb, temp, "Primary Temperature Sensor", prefix);
    prefix|=addFault(sb, hum, "Humidity Sensor", prefix);
    prefix|=addFault(sb, dehum, "Dehumidification Command", prefix);
    prefix|=addFault(sb, dehumStatus, "Dehumidification Status", prefix);
    prefix|=addFault(sb, oat, "OAT Sensor", prefix);
    prefix|=addFault(sb, oad, "OAD Command", prefix);
    prefix|=addFault(sb, air, "Airflow Microblock", prefix);
    if (air!=null && p.ctrlDampers && !alarmTriggered && !air.encounteredFault() && !air.hasProblem() && air.readings==null){
      if (prefix){
        sb.append(",\n");
      }
      sb.append("\"Airflow Damper Unresponsive\"");
      prefix = true;
    }
    if (hasFans){
      for (i=0;i<fans.length;++i){
        prefix|=addFault(sb, fans[i], "Fan["+fans[i].id+"]", prefix);
      }
    }
    if (numElements>0){
      for (i=0;i<elements.length;++i){
        prefix|=addFault(sb, elements[i], elements[i].ID, prefix);
      }
    }
    sb.append("\n]");
    if (tempTrend!=null){
      prefix = false;
      sb.append(",\n\"baseTemp\":").append(df.format(baseTemp));
      sb.append(",\n\"lowLimit\":").append(df.format(PrimaryTemp.lowLimit));
      sb.append(",\n\"highLimit\":").append(df.format(PrimaryTemp.highLimit));
      sb.append(",\n\"tempTrend\":[\n");
      for (Segment seg:tempTrend.segments){
        if (seg.end>seg.start){
          if (prefix){
            sb.append(",\n");
          }else{
            prefix = true;
          }
          sb.append("{\n");
          sb.append(Utility.format("\"name\":\"$0\",\n", escape(JSON, seg.name)));
          sb.append(Utility.format("\"lowerStages\":$0,\n", seg.lowerStages));
          sb.append(Utility.format("\"min\":$0,\n", df.format(seg.min)));
          sb.append(Utility.format("\"max\":$0,\n", df.format(seg.max)));
          sb.append(Utility.format("\"increasing\":$0,\n", seg.increasing));
          sb.append(Utility.format("\"lowDanger\":$0,\n", seg.lowDanger));
          sb.append(Utility.format("\"highDanger\":$0,\n", seg.highDanger));
          sb.append("\"x\":[");
          for (i=seg.start;i<seg.end;++i){
            if (i>seg.start){
              sb.append(',');
            }
            sb.append(df.format(tempTrend.x[i]/60000.0));
          }
          sb.append("],\n\"y\":[");
          for (i=seg.start;i<seg.end;++i){
            if (i>seg.start){
              sb.append(',');
            }
            sb.append(df.format(tempTrend.y[i]));
          }
          sb.append("]\n}");
        }
      }
      sb.append("\n]");
    }
    if (humTrend!=null){
      prefix = false;
      sb.append(",\n\"humTrend\":[\n");
      for (Segment seg:humTrend.segments){
        if (seg.end>seg.start){
          if (prefix){
            sb.append(",\n");
          }else{
            prefix = true;
          }
          sb.append("{\n");
          sb.append(Utility.format("\"name\":\"$0\",\n", escape(JSON, seg.name)));
          sb.append(Utility.format("\"min\":$0,\n", df.format(seg.min)));
          sb.append(Utility.format("\"max\":$0,\n", df.format(seg.max)));
          sb.append(Utility.format("\"increasing\":$0,\n", seg.increasing));
          sb.append("\"x\":[");
          for (i=seg.start;i<seg.end;++i){
            if (i>seg.start){
              sb.append(',');
            }
            sb.append(df.format(humTrend.x[i]/60000.0));
          }
          sb.append("],\n\"y\":[");
          for (i=seg.start;i<seg.end;++i){
            if (i>seg.start){
              sb.append(',');
            }
            sb.append(df.format(humTrend.y[i]));
          }
          sb.append("]\n}");
        }
      }
      sb.append("\n]");
    }
    sb.append(",\n\"commandTests\":[\n");
    prefix = false;
    if (humTrend!=null && dehumStatus!=null){
      if (prefix){
        sb.append(",\n");
      }else{
        prefix = true;
      }
      sb.append("{\n\"name\":\"Dehum\",\n");
      sb.append("\"start\":").append(dehumStartTest).append(",\n");
      sb.append("\"stop\":").append(dehumStopTest).append("\n}");
    }
    if (hasFans && p.ctrlFans){
      for (i=0;i<fans.length;++i){
        if (fans[i]!=null && fans[i].status().isDefined()){
          if (prefix){
            sb.append(",\n");
          }else{
            prefix = true;
          }
          sb.append("{\n\"name\":\"Fan[").append(fans[i].id).append("]\",\n");
          sb.append("\"start\":").append(fans[i].startTest).append(",\n");
          sb.append("\"stop\":").append(fans[i].stopTest).append("\n}");
        }
      }
    }
    if (numElements>0 && p.ctrlTemp){
      for (i=0;i<elements.length;++i){
        if (elements[i]!=null && elements[i].status().isDefined()){
          if (prefix){
            sb.append(",\n");
          }else{
            prefix = true;
          }
          sb.append("{\n\"name\":\"").append(elements[i].ID).append("\",\n");
          sb.append("\"start\":").append(elements[i].startTest).append(",\n");
          sb.append("\"stop\":").append(elements[i].stopTest).append("\n}");
        }
      }
    }
    sb.append("\n]");
    if (air!=null && air.readings!=null){
      sb.append(",\n\"airflow\":{\n");
      sb.append("\"maxCooling\":").append(air.getCoolingAirflow()).append(",\n");
      sb.append("\"x\":[0,5,10,15,20,25,30,35,40,45,50,55,60,65,70,75,80,85,90,95,100],\n");
      sb.append("\"y\":[");
      prefix = false;
      for (i=0;i<21;++i){
        if (prefix){
          sb.append(',');
        }else{
          prefix = true;
        }
        sb.append(df.format(air.readings[i]));
      }
      sb.append("]\n}");
    }
    sb.append("\n}");
  }
  private static boolean addProblem(StringBuilder sb, Component c, String name, boolean prefix){
    if (c!=null && name!=null && c.hasProblem()){
      if (prefix){
        sb.append(",\n");
      }
      sb.append('"').append(name).append('"');
      return true;
    }
    return false;
  }
  private static boolean addFault(StringBuilder sb, Component c, String name, boolean prefix){
    if (c!=null && name!=null && c.encounteredFault()){
      if (prefix){
        sb.append(",\n");
      }
      sb.append('"').append(name).append('"');
      return true;
    }
    return false;
  }
  public static String escape(boolean JSON, String s){
    return JSON?Utility.escapeJSON(s):Utility.escapeJS(s);
  }
  @Override public int compareTo(Data d){
    if (group==d.group){
      return path.compareTo(d.path);
    }else{
      return group-d.group;
    }
  }
  public static boolean eq(Boolean a, Boolean b){
    if (a==b){
      return true;
    }else if (a==null || b==null){
      return false;
    }else{
      return a.booleanValue()==b.booleanValue();
    }
  }
}