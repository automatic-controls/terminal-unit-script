package aces.webctrl.scripts.terminalunits;
public class Pause {
  private volatile long end;
  public Pause(){
    reset();
  }
  public Pause(long duration){
    reset(duration);
  }
  public void reset(long duration){
    if (duration<=0){
      end = 0;
    }else{
      end = System.currentTimeMillis()+duration;
    }
  }
  public void reset(){
    end = 0;
  }
  public void pause() throws InterruptedException {
    if (end==0){
      return;
    }
    final long dif = end-System.currentTimeMillis();
    if (dif>0){
      Thread.sleep(dif);
    }
    end = 0;
  }
}