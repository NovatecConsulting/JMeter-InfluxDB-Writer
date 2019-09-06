package rocks.nt.apm.jmeter.config.influxdb;

public abstract interface TestStartEndMeasurement
{
  public static final String MEASUREMENT_NAME = "testStartEnd";
  
  public static abstract interface Fields
  {
    public static final String TEST_NAME = "testName";
  }
  
  public static abstract interface Tags
  {
    public static final String TYPE = "type";
    public static final String NODE_NAME = "nodeName";
  }
  
  public static abstract interface Values
  {
    public static final String FINISHED = "finished";
    public static final String STARTED = "started";
  }
}


/* Location:              D:\Work\Download\1\JMeter-InfluxDB-Writer-v-1.2_main_my\JMeter-InfluxDB-Writer-v-1.2_main_my.jar!\rocks\nt\apm\jmeter\config\influxdb\TestStartEndMeasurement.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */