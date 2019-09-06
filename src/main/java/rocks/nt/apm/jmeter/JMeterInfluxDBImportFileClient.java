/*     */ package rocks.nt.apm.jmeter;
/*     */ 
/*     */ import java.io.BufferedWriter;
/*     */ import java.io.File;
/*     */ import java.io.FileWriter;
/*     */ import java.io.IOException;
/*     */ import java.util.HashSet;
/*     */ import java.util.List;
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
/*     */ import org.influxdb.dto.Point;
/*     */ import org.influxdb.dto.Point.Builder;
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
/*     */ public class JMeterInfluxDBImportFileClient
/*     */   extends AbstractBackendListenerClient
/*     */   implements Runnable
/*     */ {
/*  39 */   private static final Logger LOGGER = ;
/*     */   
/*     */ 
/*     */ 
/*     */   private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
/*     */   
/*     */ 
/*     */ 
/*     */   private static final String KEY_TEST_NAME = "testName";
/*     */   
/*     */ 
/*     */ 
/*     */   private static final String KEY_SAMPLERS_LIST = "samplersList";
/*     */   
/*     */ 
/*     */ 
/*     */   private static final String KEY_FILE_PATH = "filePath";
/*     */   
/*     */ 
/*     */ 
/*     */   private static final String SEPARATOR = ";";
/*     */   
/*     */ 
/*     */ 
/*     */   private ScheduledExecutorService scheduler;
/*     */   
/*     */ 
/*     */   private String testName;
/*     */   
/*     */ 
/*     */   private BufferedWriter exportFileWriter;
/*     */   
/*     */ 
/*  72 */   private String samplersList = "";
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   private String regexForSamplerList;
/*     */   
/*     */ 
/*     */ 
/*     */   private Set<String> samplersToFilter;
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context)
/*     */   {
/*  88 */     for (SampleResult sampleResult : sampleResults) {
/*  89 */       getUserMetrics().add(sampleResult);
/*     */       
/*  91 */       if (((this.regexForSamplerList != null) && (sampleResult.getSampleLabel().matches(this.regexForSamplerList))) || (this.samplersToFilter.contains(sampleResult.getSampleLabel()))) {
/*  92 */         Point point = Point.measurement("requestsRaw").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
/*  93 */           .tag("requestName", sampleResult.getSampleLabel()).addField("errorCount", sampleResult.getErrorCount())
/*  94 */           .addField("responseTime", sampleResult.getTime()).build();
/*     */         try {
/*  96 */           this.exportFileWriter.append(point.lineProtocol());
/*  97 */           this.exportFileWriter.newLine();
/*     */         } catch (IOException e) {
/*  99 */           throw new RuntimeException(e);
/*     */         }
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */   public Arguments getDefaultParameters()
/*     */   {
/* 107 */     Arguments arguments = new Arguments();
/* 108 */     arguments.addArgument("testName", "Test");
/* 109 */     arguments.addArgument("filePath", "influxDBExport.txt");
/* 110 */     arguments.addArgument("samplersList", ".*");
/* 111 */     arguments.addArgument("useRegexForSamplerList", "true");
/* 112 */     return arguments;
/*     */   }
/*     */   
/*     */   public void setupTest(BackendListenerContext context) throws Exception
/*     */   {
/* 117 */     this.testName = context.getParameter("testName", "Test");
/*     */     
/* 119 */     File exportFile = new File(context.getParameter("filePath", "influxDBExport.txt"));
/*     */     
/* 121 */     if ((exportFile.getParentFile() != null) && (!exportFile.getParentFile().exists())) {
/* 122 */       exportFile.getParentFile().mkdirs();
/*     */     }
/*     */     
/* 125 */     if (exportFile.exists()) {
/* 126 */       exportFile.delete();
/* 127 */       boolean created = exportFile.createNewFile();
/* 128 */       if (!created) {
/* 129 */         throw new RuntimeException("Export file could not be created!");
/*     */       }
/*     */     }
/*     */     
/* 133 */     this.exportFileWriter = new BufferedWriter(new FileWriter(exportFile));
/*     */     
/* 135 */     Point startPoint = Point.measurement("testStartEnd").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
/* 136 */       .tag("type", "started").addField("testName", this.testName).build();
/* 137 */     this.exportFileWriter.append(startPoint.lineProtocol());
/* 138 */     this.exportFileWriter.newLine();
/*     */     
/* 140 */     parseSamplers(context);
/* 141 */     this.scheduler = Executors.newScheduledThreadPool(1);
/*     */     
/* 143 */     this.scheduler.scheduleAtFixedRate(this, 1L, 1L, TimeUnit.SECONDS);
/*     */   }
/*     */   
/*     */   public void teardownTest(BackendListenerContext context) throws Exception
/*     */   {
/* 148 */     LOGGER.info("Shutting down influxDB scheduler...");
/* 149 */     this.scheduler.shutdown();
/*     */     
/* 151 */     addVirtualUsersMetrics(0, 0, 0, 0, JMeterContextService.getThreadCounts().finishedThreads);
/* 152 */     Point endPoint = Point.measurement("testStartEnd").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
/* 153 */       .tag("type", "finished").addField("testName", this.testName).build();
/*     */     
/* 155 */     this.exportFileWriter.append(endPoint.lineProtocol());
/* 156 */     this.exportFileWriter.newLine();
/*     */     try
/*     */     {
/* 159 */       this.scheduler.awaitTermination(30L, TimeUnit.SECONDS);
/* 160 */       LOGGER.info("influxDB scheduler terminated!");
/*     */     } catch (InterruptedException e) {
/* 162 */       LOGGER.error("Error waiting for end of scheduler");
/*     */     }
/*     */     
/* 165 */     this.samplersToFilter.clear();
/* 166 */     this.exportFileWriter.close();
/* 167 */     super.teardownTest(context);
/*     */   }
/*     */   
/*     */ 
/*     */   public void run()
/*     */   {
/*     */     try
/*     */     {
/* 175 */       JMeterContextService.ThreadCounts tc = JMeterContextService.getThreadCounts();
/* 176 */       addVirtualUsersMetrics(getUserMetrics().getMinActiveThreads(), getUserMetrics().getMeanActiveThreads(), getUserMetrics().getMaxActiveThreads(), tc.startedThreads, tc.finishedThreads);
/*     */     } catch (Exception e) {
/* 178 */       LOGGER.error("Failed writing to influx", e);
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   private void parseSamplers(BackendListenerContext context)
/*     */   {
/* 189 */     this.samplersList = context.getParameter("samplersList", "");
/* 190 */     this.samplersToFilter = new HashSet();
/* 191 */     if (context.getBooleanParameter("useRegexForSamplerList", false)) {
/* 192 */       this.regexForSamplerList = this.samplersList;
/*     */     } else {
/* 194 */       this.regexForSamplerList = null;
/* 195 */       String[] samplers = this.samplersList.split(";");
/* 196 */       this.samplersToFilter = new HashSet();
/* 197 */       String[] arrayOfString1; int j = (arrayOfString1 = samplers).length; for (int i = 0; i < j; i++) { String samplerName = arrayOfString1[i];
/* 198 */         this.samplersToFilter.add(samplerName);
/*     */       }
/*     */     }
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   private void addVirtualUsersMetrics(int minActiveThreads, int meanActiveThreads, int maxActiveThreads, int startedThreads, int finishedThreads)
/*     */   {
/* 207 */     Point.Builder builder = Point.measurement("virtualUsers").time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
/* 208 */     builder.addField("minActiveThreads", minActiveThreads);
/* 209 */     builder.addField("maxActiveThreads", maxActiveThreads);
/* 210 */     builder.addField("meanActiveThreads", meanActiveThreads);
/* 211 */     builder.addField("startedThreads", startedThreads);
/* 212 */     builder.addField("finishedThreads", finishedThreads);
/*     */     try {
/* 214 */       this.exportFileWriter.append(builder.build().lineProtocol());
/* 215 */       this.exportFileWriter.newLine();
/*     */     } catch (IOException e) {
/* 217 */       throw new RuntimeException(e);
/*     */     }
/*     */   }
/*     */ }


/* Location:              D:\Work\Download\1\JMeter-InfluxDB-Writer-v-1.2_main_my\JMeter-InfluxDB-Writer-v-1.2_main_my.jar!\rocks\nt\apm\jmeter\JMeterInfluxDBImportFileClient.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */