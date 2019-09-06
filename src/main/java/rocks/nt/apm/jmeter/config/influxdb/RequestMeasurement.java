package rocks.nt.apm.jmeter.config.influxdb;

public abstract interface RequestMeasurement
{
  public static final String MEASUREMENT_NAME = "requestsRaw";
  
  public static abstract interface Fields
  {
    public static final String RESPONSE_TIME = "responseTime";
    public static final String RESPONSE_BYTES = "responseBytes";
    public static final String RESPONSE_LATENCY = "responseLatency";
    public static final String REQUEST_CONNECT = "connectTime";
    public static final String RESPONSE_CODE = "responseCode";
    public static final String ERROR_COUNT = "errorCount";
    public static final String THREAD_NAME = "threadName";
    public static final String TEST_NAME = "testName";
    public static final String NODE_NAME = "nodeName";
  }
  
  public static abstract interface Tags
  {
    public static final String REQUEST_NAME = "requestName";
  }
}


/* Location:              D:\Work\Download\1\JMeter-InfluxDB-Writer-v-1.2_main_my\JMeter-InfluxDB-Writer-v-1.2_main_my.jar!\rocks\nt\apm\jmeter\config\influxdb\RequestMeasurement.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */