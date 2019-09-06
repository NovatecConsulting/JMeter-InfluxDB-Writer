/*     */ package rocks.nt.apm.jmeter;
/*     */ 
/*     */ import java.util.HashSet;
/*     */ import java.util.List;
/*     */ import java.util.Random;
/*     */ import java.util.Set;
/*     */ import java.util.concurrent.Executors;
/*     */ import java.util.concurrent.ScheduledExecutorService;
/*     */ import java.util.concurrent.TimeUnit;
/*     */ import org.apache.jmeter.config.Arguments;
/*     */ import org.apache.jmeter.samplers.SampleResult;
/*     */ import org.apache.jmeter.threads.JMeterContextService;
/*     */ import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
/*     */ import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
/*     */ import org.apache.jmeter.visualizers.backend.BackendListenerContext;
/*     */ import org.apache.jmeter.visualizers.backend.UserMetric;
/*     */ import org.apache.jorphan.logging.LoggingManager;
/*     */ import org.apache.log.Logger;
/*     */ import org.influxdb.InfluxDB;
/*     */ import org.influxdb.InfluxDBFactory;
/*     */ import org.influxdb.dto.Point;
/*     */ import org.influxdb.dto.Point.Builder;
/*     */ import rocks.nt.apm.jmeter.config.influxdb.InfluxDBConfig;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class JMeterInfluxDBBackendListenerClient
/*     */   extends AbstractBackendListenerClient
/*     */   implements Runnable
/*     */ {
/*  40 */   private static final Logger LOGGER = ;
/*     */   
/*     */ 
/*     */   private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
/*     */   
/*     */ 
/*     */   private static final String KEY_TEST_NAME = "testName";
/*     */   
/*     */ 
/*     */   private static final String KEY_NODE_NAME = "nodeName";
/*     */   
/*     */ 
/*     */   private static final String KEY_SAMPLERS_LIST = "samplersList";
/*     */   
/*     */ 
/*     */   private static final String SEPARATOR = ";";
/*     */   
/*     */ 
/*     */   private static final int ONE_MS_IN_NANOSECONDS = 1000000;
/*     */   
/*     */ 
/*     */   long startOfTest;
/*     */   
/*     */ 
/*     */   long currentTime;
/*     */   
/*     */ 
/*     */   long deltaTime;
/*     */   
/*     */   long counters;
/*     */   
/*     */   double rate;
/*     */   
/*     */   private ScheduledExecutorService scheduler;
/*     */   
/*     */   private String testName;
/*     */   
/*     */   private String nodeName;
/*     */   
/*  79 */   private String samplersList = "";
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private String regexForSamplerList;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private Set<String> samplersToFilter;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   InfluxDBConfig influxDBConfig;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private InfluxDB influxDB;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private Random randomNumberGenerator;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context)
/*     */   {
/* 114 */     for (SampleResult sampleResult : sampleResults) {
/* 115 */       getUserMetrics().add(sampleResult);
/*     */       
/*     */ 
/* 118 */       if (((this.regexForSamplerList != null) && (sampleResult.getSampleLabel().matches(this.regexForSamplerList))) || (this.samplersToFilter.contains(sampleResult.getSampleLabel()))) {
/* 119 */         Point point = Point.measurement("requestsRaw").time(System.currentTimeMillis() * 1000000L + getUniqueNumberForTheSamplerThread(), TimeUnit.NANOSECONDS)
/* 120 */           .tag("requestName", sampleResult.getSampleLabel())
/* 121 */           .addField("responseCode", sampleResult.getResponseCode())
/* 122 */           .addField("errorCount", sampleResult.getErrorCount())
/* 123 */           .addField("threadName", sampleResult.getThreadName())
/* 124 */           .addField("testName", this.testName)
/* 125 */           .addField("nodeName", this.nodeName)
/* 126 */           .addField("responseBytes", sampleResult.getBytesAsLong())
/* 127 */           .addField("responseLatency", sampleResult.getLatency())
/* 128 */           .addField("connectTime", sampleResult.getConnectTime())
/* 129 */           .addField("responseTime", sampleResult.getTime()).build();
/* 130 */         this.influxDB.write(this.influxDBConfig.getInfluxDatabase(), this.influxDBConfig.getInfluxRetentionPolicy(), point);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */   public Arguments getDefaultParameters()
/*     */   {
/* 138 */     Arguments arguments = new Arguments();
/* 139 */     arguments.addArgument("testName", "Test");
/* 140 */     arguments.addArgument("nodeName", "Test-Node");
/* 141 */     arguments.addArgument("influxDBHost", "localhost");
/* 142 */     arguments.addArgument("influxDBPort", Integer.toString(8086));
/* 143 */     arguments.addArgument("influxDBUser", "jmeter");
/* 144 */     arguments.addArgument("influxDBPassword", "");
/* 145 */     arguments.addArgument("influxDBDatabase", "jmeter");
/* 146 */     arguments.addArgument("retentionPolicy", "autogen");
/* 147 */     arguments.addArgument("samplersList", ".*");
/* 148 */     arguments.addArgument("useRegexForSamplerList", "true");
/* 149 */     return arguments;
/*     */   }
/*     */   
/*     */   public void setupTest(BackendListenerContext context) throws Exception
/*     */   {
/* 154 */     this.testName = context.getParameter("testName", "Test");
/* 155 */     this.randomNumberGenerator = new Random();
/* 156 */     this.nodeName = context.getParameter("nodeName", "Test-Node");
/* 157 */     this.startOfTest = System.currentTimeMillis();
/*     */     
/* 159 */     setupInfluxClient(context);
/* 160 */     this.influxDB.write(
/* 161 */       this.influxDBConfig.getInfluxDatabase(), 
/* 162 */       this.influxDBConfig.getInfluxRetentionPolicy(), 
/* 163 */       Point.measurement("testStartEnd").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
/* 164 */       .tag("type", "started")
/* 165 */       .tag("nodeName", this.nodeName)
/* 166 */       .addField("testName", this.testName)
/* 167 */       .build());
/*     */     
/*     */ 
/*     */ 
/* 171 */     parseSamplers(context);
/* 172 */     this.scheduler = Executors.newScheduledThreadPool(1);
/*     */     
/* 174 */     this.scheduler.scheduleAtFixedRate(this, 1L, 1L, TimeUnit.SECONDS);
/*     */   }
/*     */   
/*     */   private long time(long currentTimeMillis, TimeUnit milliseconds)
/*     */   {
/* 179 */     return 0L;
/*     */   }
/*     */   
/*     */   public void teardownTest(BackendListenerContext context) throws Exception
/*     */   {
/* 184 */     LOGGER.info("Shutting down influxDB scheduler...");
/* 185 */     this.scheduler.shutdown();
/*     */     
/* 187 */     addVirtualUsersMetrics(0, 0, 0, 0, JMeterContextService.getThreadCounts().finishedThreads);
/* 188 */     this.influxDB.write(
/* 189 */       this.influxDBConfig.getInfluxDatabase(), 
/* 190 */       this.influxDBConfig.getInfluxRetentionPolicy(), 
/* 191 */       Point.measurement("testStartEnd").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
/* 192 */       .tag("type", "finished")
/* 193 */       .tag("nodeName", this.nodeName)
/* 194 */       .addField("testName", this.testName)
/* 195 */       .build());
/*     */     
/* 197 */     this.influxDB.disableBatch();
/*     */     try {
/* 199 */       this.scheduler.awaitTermination(30L, TimeUnit.SECONDS);
/* 200 */       LOGGER.info("influxDB scheduler terminated!");
/*     */     } catch (InterruptedException e) {
/* 202 */       LOGGER.error("Error waiting for end of scheduler");
/*     */     }
/*     */     
/* 205 */     this.samplersToFilter.clear();
/* 206 */     super.teardownTest(context);
/*     */   }
/*     */   
/*     */ 
/*     */   public void run()
/*     */   {
/*     */     try
/*     */     {
/* 214 */       JMeterContextService.ThreadCounts tc = JMeterContextService.getThreadCounts();
/* 215 */       addVirtualUsersMetrics(getUserMetrics().getMinActiveThreads(), getUserMetrics().getMeanActiveThreads(), getUserMetrics().getMaxActiveThreads(), tc.startedThreads, tc.finishedThreads);
/*     */     } catch (Exception e) {
/* 217 */       LOGGER.error("Failed writing to influx", e);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void setupInfluxClient(BackendListenerContext context)
/*     */   {
/* 228 */     this.influxDBConfig = new InfluxDBConfig(context);
/* 229 */     this.influxDB = InfluxDBFactory.connect(this.influxDBConfig.getInfluxDBURL(), this.influxDBConfig.getInfluxUser(), this.influxDBConfig.getInfluxPassword());
/* 230 */     this.influxDB.enableBatch(100, 5, TimeUnit.SECONDS);
/* 231 */     createDatabaseIfNotExistent();
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void parseSamplers(BackendListenerContext context)
/*     */   {
/* 241 */     this.samplersList = context.getParameter("samplersList", "");
/* 242 */     this.samplersToFilter = new HashSet();
/* 243 */     if (context.getBooleanParameter("useRegexForSamplerList", false)) {
/* 244 */       this.regexForSamplerList = this.samplersList;
/*     */     } else {
/* 246 */       this.regexForSamplerList = null;
/* 247 */       String[] samplers = this.samplersList.split(";");
/* 248 */       this.samplersToFilter = new HashSet();
/* 249 */       String[] arrayOfString1; int j = (arrayOfString1 = samplers).length; for (int i = 0; i < j; i++) { String samplerName = arrayOfString1[i];
/* 250 */         this.samplersToFilter.add(samplerName);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void addVirtualUsersMetrics(int minActiveThreads, int meanActiveThreads, int maxActiveThreads, int startedThreads, int finishedThreads)
/*     */   {
/* 259 */     Point.Builder builder = Point.measurement("virtualUsers").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
/* 260 */     builder.addField("minActiveThreads", minActiveThreads);
/* 261 */     builder.addField("maxActiveThreads", maxActiveThreads);
/* 262 */     builder.addField("meanActiveThreads", meanActiveThreads);
/* 263 */     builder.addField("startedThreads", startedThreads);
/* 264 */     builder.addField("finishedThreads", finishedThreads);
/* 265 */     builder.tag("nodeName", this.nodeName);
/* 266 */     this.influxDB.write(this.influxDBConfig.getInfluxDatabase(), this.influxDBConfig.getInfluxRetentionPolicy(), builder.build());
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void createDatabaseIfNotExistent()
/*     */   {
/* 273 */     List<String> dbNames = this.influxDB.describeDatabases();
/* 274 */     if (!dbNames.contains(this.influxDBConfig.getInfluxDatabase())) {
/* 275 */       this.influxDB.createDatabase(this.influxDBConfig.getInfluxDatabase());
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private int getUniqueNumberForTheSamplerThread()
/*     */   {
/* 283 */     return this.randomNumberGenerator.nextInt(1000000);
/*     */   }
/*     */ }


/* Location:              D:\Work\Download\1\JMeter-InfluxDB-Writer-v-1.2_main_my\JMeter-InfluxDB-Writer-v-1.2_main_my.jar!\rocks\nt\apm\jmeter\JMeterInfluxDBBackendListenerClient.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */