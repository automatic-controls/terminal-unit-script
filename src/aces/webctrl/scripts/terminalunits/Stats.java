package aces.webctrl.scripts.terminalunits;
public class Stats {
  public volatile double[] arr;
  public volatile double mean = 0;
  //public volatile double absoluteDeviation = 0;
  public Stats(double... arr){
    this.arr = arr;
    if (arr.length>0){
      for (int i=0;i<arr.length;++i){
        mean+=arr[i];
      }
      mean/=arr.length;
      /*
      for (int i=0;i<arr.length;++i){
        absoluteDeviation+=Math.abs(arr[i]-mean);
      }
      absoluteDeviation/=arr.length;
      */
    }
  }
}