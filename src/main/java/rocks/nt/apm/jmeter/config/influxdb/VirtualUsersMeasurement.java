package rocks.nt.apm.jmeter.config.influxdb;

public abstract interface VirtualUsersMeasurement
{
  public static final String MEASUREMENT_NAME = "virtualUsers";
  
  public static abstract interface Fields
  {
    public static final String MIN_ACTIVE_THREADS = "minActiveThreads";
    public static final String MAX_ACTIVE_THREADS = "maxActiveThreads";
    public static final String MEAN_ACTIVE_THREADS = "meanActiveThreads";
    public static final String STARTED_THREADS = "startedThreads";
    public static final String FINISHED_THREADS = "finishedThreads";
  }
  
  public static abstract interface Tags
  {
    public static final String NODE_NAME = "nodeName";
  }
}


/* Location:              D:\Work\Download\1\JMeter-InfluxDB-Writer-v-1.2_main_my\JMeter-InfluxDB-Writer-v-1.2_main_my.jar!\rocks\nt\apm\jmeter\config\influxdb\VirtualUsersMeasurement.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */