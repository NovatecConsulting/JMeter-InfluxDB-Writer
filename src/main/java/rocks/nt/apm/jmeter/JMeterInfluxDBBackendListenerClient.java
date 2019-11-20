package rocks.nt.apm.jmeter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.AbstractThreadGroup;
import org.apache.jmeter.testelement.AbstractTestElement;
import org.apache.jmeter.util.JMeterUtils;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

import rocks.nt.apm.jmeter.config.influxdb.InfluxDBConfig;
import rocks.nt.apm.jmeter.config.influxdb.RequestMeasurement;
import rocks.nt.apm.jmeter.config.influxdb.TestStartEndMeasurement;
import rocks.nt.apm.jmeter.config.influxdb.VirtualUsersMeasurement;

/**
 * Backend listener that writes JMeter metrics to influxDB directly.
 * 
 * @author Alexander Wert
 *
 */
public class JMeterInfluxDBBackendListenerClient extends AbstractBackendListenerClient implements Runnable {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggingManager.getLoggerForClass();

	/**
	 * Parameter Keys.
	 */
	private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
	private static final String KEY_TEST_NAME = "component";
	private static final String KEY_NODE_NAME = "env";
	private static final String KEY_SAMPLERS_LIST = "samplersList";
	private static final String KEY_RECORD_SUB_SAMPLES = "recordSubSamples";

	/**
	 * Constants.
	 */
	private static final String SEPARATOR = ";";
	private static final int ONE_MS_IN_NANOSECONDS = 1000000;

	/**
	 * Scheduler for periodic metric aggregation.
	 */
	private ScheduledExecutorService scheduler;
		
	String old_tg_name;
	int old_tg_number = 0;

	/**
	 * Name of the name
	 */
	private String env;

/**
	 * Name of the system component
	 */
	private String component;
	

	/**
	 * List of samplers to record.
	 */
	private String samplersList = "";

	/**
	 * Regex if samplers are defined through regular expression.
	 */
	private String regexForSamplerList;

	/**
	 * Set of samplers to record.
	 */
	private Set<String> samplersToFilter;

	/**
	 * InfluxDB configuration.
	 */
	InfluxDBConfig influxDBConfig;

	/**
	 * influxDB client.
	 */
	private InfluxDB influxDB;

	/**
	 * Random number generator
	 */
	private Random randomNumberGenerator;

	/**
	 * Indicates whether to record Subsamples
	 */
	private boolean recordSubSamples;

	/**
	 * Processes sampler results.
	 */
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		// Gather all the listeners
		List<SampleResult> allSampleResults = new ArrayList<SampleResult>();
		for (SampleResult sampleResult : sampleResults) {
            allSampleResults.add(sampleResult);

            if(recordSubSamples) {
				for (SampleResult subResult : sampleResult.getSubResults()) {
					allSampleResults.add(subResult);
				}
			}
        }

		for(SampleResult sampleResult: allSampleResults) {
            getUserMetrics().add(sampleResult);

			if ((null != regexForSamplerList && sampleResult.getSampleLabel().matches(regexForSamplerList)) || samplersToFilter.contains(sampleResult.getSampleLabel())) {
				Point point = Point.measurement(RequestMeasurement.MEASUREMENT_NAME).time(
						System.currentTimeMillis() * ONE_MS_IN_NANOSECONDS + getUniqueNumberForTheSamplerThread(), TimeUnit.NANOSECONDS)
						.tag(RequestMeasurement.Tags.REQUEST_NAME, sampleResult.getSampleLabel())
						.tag(RequestMeasurement.Tags.RESPONSE_CODE, sampleResult.getResponseCode())
                        .addField(RequestMeasurement.Fields.ERROR_COUNT, sampleResult.getErrorCount())
						.addField(RequestMeasurement.Fields.THREAD_NAME, sampleResult.getThreadName())
						.tag(RequestMeasurement.Tags.TEST_NAME, component)
						.tag(RequestMeasurement.Tags.NODE_NAME, env)
						.addField(RequestMeasurement.Fields.RESPONSE_BYTES, sampleResult.getBytesAsLong())
						.addField(RequestMeasurement.Fields.RESPONSE_LATENCY, sampleResult.getLatency())
						.addField(RequestMeasurement.Fields.CONNECT_TIME, sampleResult.getConnectTime())
						.addField(RequestMeasurement.Fields.DOWNLOAD_TIME, (sampleResult.getTime() - sampleResult.getLatency()))
						.addField(RequestMeasurement.Fields.PROCESSING_TIME, (sampleResult.getTime() - sampleResult.getConnectTime()))
						.addField(RequestMeasurement.Fields.RESPONSE_TIME, sampleResult.getTime()).build();
				influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), point);
			}
		}
	}

	@Override
	public Arguments getDefaultParameters() {
		Arguments arguments = new Arguments();
		arguments.addArgument(KEY_TEST_NAME, "component");
		arguments.addArgument(KEY_NODE_NAME, "env");
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_HOST, "localhost");
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PORT, Integer.toString(InfluxDBConfig.DEFAULT_PORT));
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_USER, "");
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_PASSWORD, "");
		arguments.addArgument(InfluxDBConfig.KEY_INFLUX_DB_DATABASE, InfluxDBConfig.DEFAULT_DATABASE);
		arguments.addArgument(InfluxDBConfig.KEY_RETENTION_POLICY, InfluxDBConfig.DEFAULT_RETENTION_POLICY);
		arguments.addArgument(KEY_SAMPLERS_LIST, ".*");
		arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, "true");
		arguments.addArgument(KEY_RECORD_SUB_SAMPLES, "false");
		return arguments;
	}

	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		component = context.getParameter(KEY_TEST_NAME, "component");
		randomNumberGenerator = new Random();
		env = context.getParameter(KEY_NODE_NAME, "env");
		
		setupInfluxClient(context);
		influxDB.write(
				influxDBConfig.getInfluxDatabase(),
				influxDBConfig.getInfluxRetentionPolicy(),
				Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
						.tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.STARTED)
						.tag(TestStartEndMeasurement.Tags.NODE_NAME, env)
						.tag(TestStartEndMeasurement.Tags.TEST_NAME, component)
						.addField(TestStartEndMeasurement.Fields.PLACEHOLDER, "1")
						.build());

		parseSamplers(context);
		scheduler = Executors.newScheduledThreadPool(1);

		scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);

		// Indicates whether to write sub sample records to the database
		recordSubSamples = Boolean.parseBoolean(context.getParameter(KEY_RECORD_SUB_SAMPLES, "false"));
	}

	@Override
	public void teardownTest(BackendListenerContext context) throws Exception {
		LOGGER.info("Shutting down influxDB scheduler...");
		scheduler.shutdown();

		addVirtualUsersMetrics(0,0,0,0,JMeterContextService.getThreadCounts().finishedThreads);
		influxDB.write(
				influxDBConfig.getInfluxDatabase(),
				influxDBConfig.getInfluxRetentionPolicy(),
				Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
						.tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.FINISHED)
						.tag(TestStartEndMeasurement.Tags.NODE_NAME, env)
						.tag(TestStartEndMeasurement.Tags.TEST_NAME, component)
						.addField(TestStartEndMeasurement.Fields.PLACEHOLDER,"1")
						.build());

		influxDB.disableBatch();
		try {
			scheduler.awaitTermination(30, TimeUnit.SECONDS);
			LOGGER.info("influxDB scheduler terminated!");
		} catch (InterruptedException e) {
			LOGGER.error("Error waiting for end of scheduler");
		}

		samplersToFilter.clear();
		super.teardownTest(context);
	}

	/**
	 * Periodically writes virtual users metrics to influxDB.
	 */
	public void run() {
		try {
			ThreadCounts tc = JMeterContextService.getThreadCounts();
			addVirtualUsersMetrics(getUserMetrics().getMinActiveThreads(), getUserMetrics().getMeanActiveThreads(), getUserMetrics().getMaxActiveThreads(), tc.startedThreads, tc.finishedThreads);

		} catch (Exception e) {
			LOGGER.error("Failed writing to influx", e);
		}
	}

	/**
	 * Setup influxDB client.
	 * 
	 * @param context
	 *            {@link BackendListenerContext}.
	 */
	private void setupInfluxClient(BackendListenerContext context) {
		influxDBConfig = new InfluxDBConfig(context);
		influxDB = InfluxDBFactory.connect(influxDBConfig.getInfluxDBURL(), influxDBConfig.getInfluxUser(), influxDBConfig.getInfluxPassword());
		influxDB.enableBatch(100, 5, TimeUnit.SECONDS);
		createDatabaseIfNotExistent();
	}

	/**
	 * Parses list of samplers.
	 * 
	 * @param context
	 *            {@link BackendListenerContext}.
	 */
	private void parseSamplers(BackendListenerContext context) {
		samplersList = context.getParameter(KEY_SAMPLERS_LIST, "");
		samplersToFilter = new HashSet<String>();
		if (context.getBooleanParameter(KEY_USE_REGEX_FOR_SAMPLER_LIST, false)) {
			regexForSamplerList = samplersList;
		} else {
			regexForSamplerList = null;
			String[] samplers = samplersList.split(SEPARATOR);
			samplersToFilter = new HashSet<String>();
			for (String samplerName : samplers) {
				samplersToFilter.add(samplerName);
			}
		}
	}

	/**
	 * Write thread metrics.
	 */
	private void addVirtualUsersMetrics(int minActiveThreads, int meanActiveThreads, int maxActiveThreads, int startedThreads, int finishedThreads) {
		Builder builder = Point.measurement(VirtualUsersMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		builder.addField(VirtualUsersMeasurement.Fields.MIN_ACTIVE_THREADS, minActiveThreads);
		builder.addField(VirtualUsersMeasurement.Fields.MAX_ACTIVE_THREADS, maxActiveThreads);
		builder.addField(VirtualUsersMeasurement.Fields.MEAN_ACTIVE_THREADS, meanActiveThreads);
		builder.addField(VirtualUsersMeasurement.Fields.STARTED_THREADS, startedThreads);
		builder.addField(VirtualUsersMeasurement.Fields.FINISHED_THREADS, finishedThreads);
		builder.tag(VirtualUsersMeasurement.Tags.NODE_NAME, env);
		builder.tag(VirtualUsersMeasurement.Tags.TEST_NAME, component);
		influxDB.write(influxDBConfig.getInfluxDatabase(), influxDBConfig.getInfluxRetentionPolicy(), builder.build());
	}

	/**
	 * Creates the configured database in influx if it does not exist yet.
	 */
	private void createDatabaseIfNotExistent() {
		List<String> dbNames = influxDB.describeDatabases();
		if (!dbNames.contains(influxDBConfig.getInfluxDatabase())) {
			influxDB.createDatabase(influxDBConfig.getInfluxDatabase());
		}
	}

	/**
	 * Try to get a unique number for the sampler thread
	 */
	private int getUniqueNumberForTheSamplerThread() {
		return randomNumberGenerator.nextInt(ONE_MS_IN_NANOSECONDS);
	}
}
