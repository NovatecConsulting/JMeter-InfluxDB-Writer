/*     */ package rocks.nt.apm.jmeter.config.influxdb;
/*     */ 
/*     */ import org.apache.commons.lang3.StringUtils;
/*     */ import org.apache.jmeter.visualizers.backend.BackendListenerContext;
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
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ public class InfluxDBConfig
/*     */ {
/*     */   public static final String DEFAULT_DATABASE = "jmeter";
/*     */   public static final String DEFAULT_RETENTION_POLICY = "autogen";
/*     */   public static final int DEFAULT_PORT = 8086;
/*     */   public static final String KEY_INFLUX_DB_DATABASE = "influxDBDatabase";
/*     */   public static final String KEY_INFLUX_DB_PASSWORD = "influxDBPassword";
/*     */   public static final String KEY_INFLUX_DB_USER = "influxDBUser";
/*     */   public static final String KEY_INFLUX_DB_PORT = "influxDBPort";
/*     */   public static final String KEY_INFLUX_DB_HOST = "influxDBHost";
/*     */   public static final String KEY_RETENTION_POLICY = "retentionPolicy";
/*     */   private String influxDBHost;
/*     */   private String influxUser;
/*     */   private String influxPassword;
/*     */   private String influxDatabase;
/*     */   private String influxRetentionPolicy;
/*     */   private int influxDBPort;
/*     */   
/*     */   public InfluxDBConfig(BackendListenerContext context)
/*     */   {
/*  90 */     String influxDBHost = context.getParameter("influxDBHost");
/*  91 */     if (StringUtils.isEmpty(influxDBHost)) {
/*  92 */       throw new IllegalArgumentException("influxDBHostmust not be empty!");
/*     */     }
/*  94 */     setInfluxDBHost(influxDBHost);
/*     */     
/*  96 */     int influxDBPort = context.getIntParameter("influxDBPort", 8086);
/*  97 */     setInfluxDBPort(influxDBPort);
/*     */     
/*  99 */     String influxUser = context.getParameter("influxDBUser");
/* 100 */     setInfluxUser(influxUser);
/*     */     
/* 102 */     String influxPassword = context.getParameter("influxDBPassword");
/* 103 */     setInfluxPassword(influxPassword);
/*     */     
/* 105 */     String influxDatabase = context.getParameter("influxDBDatabase");
/* 106 */     if (StringUtils.isEmpty(influxDatabase)) {
/* 107 */       throw new IllegalArgumentException("influxDBDatabasemust not be empty!");
/*     */     }
/* 109 */     setInfluxDatabase(influxDatabase);
/*     */     
/* 111 */     String influxRetentionPolicy = context.getParameter("retentionPolicy", "autogen");
/* 112 */     if (StringUtils.isEmpty(influxRetentionPolicy)) {
/* 113 */       influxRetentionPolicy = "autogen";
/*     */     }
/* 115 */     setInfluxRetentionPolicy(influxRetentionPolicy);
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   public String getInfluxDBURL()
/*     */   {
/* 124 */     return "http://" + this.influxDBHost + ":" + this.influxDBPort;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getInfluxDBHost()
/*     */   {
/* 131 */     return this.influxDBHost;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setInfluxDBHost(String influxDBHost)
/*     */   {
/* 139 */     this.influxDBHost = influxDBHost;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getInfluxUser()
/*     */   {
/* 146 */     return this.influxUser;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setInfluxUser(String influxUser)
/*     */   {
/* 154 */     this.influxUser = influxUser;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getInfluxPassword()
/*     */   {
/* 161 */     return this.influxPassword;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setInfluxPassword(String influxPassword)
/*     */   {
/* 169 */     this.influxPassword = influxPassword;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getInfluxDatabase()
/*     */   {
/* 176 */     return this.influxDatabase;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setInfluxDatabase(String influxDatabase)
/*     */   {
/* 184 */     this.influxDatabase = influxDatabase;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public String getInfluxRetentionPolicy()
/*     */   {
/* 191 */     return this.influxRetentionPolicy;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setInfluxRetentionPolicy(String influxRetentionPolicy)
/*     */   {
/* 199 */     this.influxRetentionPolicy = influxRetentionPolicy;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */   public int getInfluxDBPort()
/*     */   {
/* 206 */     return this.influxDBPort;
/*     */   }
/*     */   
/*     */ 
/*     */ 
/*     */ 
/*     */   public void setInfluxDBPort(int influxDBPort)
/*     */   {
/* 214 */     this.influxDBPort = influxDBPort;
/*     */   }
/*     */ }


/* Location:              D:\Work\Download\1\JMeter-InfluxDB-Writer-v-1.2_main_my\JMeter-InfluxDB-Writer-v-1.2_main_my.jar!\rocks\nt\apm\jmeter\config\influxdb\InfluxDBConfig.class
 * Java compiler version: 7 (51.0)
 * JD-Core Version:       0.7.1
 */