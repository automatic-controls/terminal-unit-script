package aces.webctrl.scripts.terminalunits;
import aces.webctrl.scripts.commissioning.core.*;
import aces.webctrl.scripts.commissioning.web.*;
import javax.servlet.http.*;
import java.util.*;
import java.util.concurrent.atomic.*;
public class TerminalUnitTest extends Script {
  private volatile boolean exited = false;
  private volatile Data[] data;
  private final AtomicInteger index = new AtomicInteger();
  private volatile boolean initialized = false;
  private volatile String outputString = null;
  private volatile Params p = null;
  @Override public String getDescription(){
    return "<a href=\"https://github.com/automatic-controls/commissioning-scripts/tree/main/samples/TerminalUnitTest\" target=\"_blank\" style=\"border:none;\">Terminal Unit Commissioning Script</a> v0.2.1<br>Evaluates performance of fans, dampers, heating, cooling, and dehumidification components.";
  }
  @Override public String[] getParamNames(){
    return new String[]{"Dampers", "Fans", "Heating / Cooling"};
  }
  @Override public void exit(){
    exited = true;
  }
  @Override public void init() throws Throwable {
    data = new Data[this.testsTotal];
    Arrays.fill(data,null);
    p = new Params();
    p.ctrlDampers = this.params.getOrDefault("Dampers",false);
    p.ctrlFans = this.params.getOrDefault("Fans",false);
    p.ctrlTemp = this.params.getOrDefault("Heating / Cooling",false);
    outputString = Utility.loadResourceAsString(TerminalUnitTest.class.getClassLoader(), "aces/webctrl/scripts/terminalunits/Output.html");
    initialized = true;
  }
  @Override public void exec(ResolvedTestingUnit x) throws Throwable {
    final int index = this.index.getAndIncrement();
    if (index>=data.length){
      Initializer.log(new ArrayIndexOutOfBoundsException("Index exceeded expected capacity: "+index+">="+data.length));
      return;
    }
    x.tries = 5;
    x.failedAttemptTimeout = 500L;
    data[index] = new Data(x,p);
  }
  @Override public String getOutput(boolean email) throws Throwable {
    if (!initialized){
      return null;
    }
    final boolean notExited = !exited;
    return outputString.replace(
      "__CTRL_FANS__",
      String.valueOf(p.ctrlFans)
    ).replace(
      "__CTRL_DAMPERS__",
      String.valueOf(p.ctrlDampers)
    ).replace(
      "__CTRL_TEMP__",
      String.valueOf(p.ctrlTemp)
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
          "<h3 id=\"dataAfterBreak\" style=\"color:crimson\">Foribly Aborted</h3>"
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
          sb.append(Utility.format("{\n\"name\":\"$0\",\n\"equipment\":[\n", Data.escape(JSON,this.mapping.groupNames.getOrDefault(group,"(Deleted Group)"))));
        }
        d.print(sb,JSON);
      }
      sb.append("]}]");
    }
    return sb;
  }
}