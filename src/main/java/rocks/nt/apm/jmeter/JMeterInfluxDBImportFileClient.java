package rocks.nt.apm.jmeter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterContextService.ThreadCounts;
import org.apache.jmeter.visualizers.backend.AbstractBackendListenerClient;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;
import org.influxdb.dto.Point;
import org.influxdb.dto.Point.Builder;

import rocks.nt.apm.jmeter.config.influxdb.RequestMeasurement;
import rocks.nt.apm.jmeter.config.influxdb.TestStartEndMeasurement;
import rocks.nt.apm.jmeter.config.influxdb.VirtualUsersMeasurement;

/**
 * Backend listener that writes JMeter metrics to influxDB directly.
 * 
 * @author Alexander Wert
 *
 */
public class JMeterInfluxDBImportFileClient extends AbstractBackendListenerClient implements Runnable {
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LoggingManager.getLoggerForClass();

	/**
	 * Parameter Keys.
	 */
	private static final String KEY_USE_REGEX_FOR_SAMPLER_LIST = "useRegexForSamplerList";
	private static final String KEY_TEST_NAME = "testName";
	private static final String KEY_SAMPLERS_LIST = "samplersList";
	private static final String KEY_FILE_PATH = "filePath";

	/**
	 * Constants.
	 */
	private static final String SEPARATOR = ";";

	/**
	 * Scheduler for periodic metric aggregation.
	 */
	private ScheduledExecutorService scheduler;

	/**
	 * Name of the test.
	 */
	private String testName;

	/**
	 * Export File Writer.
	 */
	private BufferedWriter exportFileWriter;

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
	 * Processes sampler results.
	 */
	public void handleSampleResults(List<SampleResult> sampleResults, BackendListenerContext context) {
		for (SampleResult sampleResult : sampleResults) {
			getUserMetrics().add(sampleResult);

			if ((null != regexForSamplerList && sampleResult.getSampleLabel().matches(regexForSamplerList)) || samplersToFilter.contains(sampleResult.getSampleLabel())) {
				Point point = Point.measurement(RequestMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
						.tag(RequestMeasurement.Tags.REQUEST_NAME, sampleResult.getSampleLabel()).addField(RequestMeasurement.Fields.ERROR_COUNT, sampleResult.getErrorCount())
						.addField(RequestMeasurement.Fields.RESPONSE_TIME, sampleResult.getTime()).build();
				try {
					exportFileWriter.append(point.lineProtocol());
					exportFileWriter.newLine();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	@Override
	public Arguments getDefaultParameters() {
		Arguments arguments = new Arguments();
		arguments.addArgument(KEY_TEST_NAME, "Test");
		arguments.addArgument(KEY_FILE_PATH, "influxDBExport.txt");
		arguments.addArgument(KEY_SAMPLERS_LIST, ".*");
		arguments.addArgument(KEY_USE_REGEX_FOR_SAMPLER_LIST, "true");
		return arguments;
	}

	@Override
	public void setupTest(BackendListenerContext context) throws Exception {
		testName = context.getParameter(KEY_TEST_NAME, "Test");

		File exportFile = new File(context.getParameter(KEY_FILE_PATH, "influxDBExport.txt"));

		if (exportFile.getParentFile() != null && !exportFile.getParentFile().exists()) {
			exportFile.getParentFile().mkdirs();
		}

		if (exportFile.exists()) {
			exportFile.delete();
			boolean created = exportFile.createNewFile();
			if (!created) {
				throw new RuntimeException("Export file could not be created!");
			}
		}

		exportFileWriter = new BufferedWriter(new FileWriter(exportFile));

		Point startPoint = Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.STARTED).tag(TestStartEndMeasurement.Tags.TEST_NAME, testName).build();
		exportFileWriter.append(startPoint.lineProtocol());
		exportFileWriter.newLine();

		parseSamplers(context);
		scheduler = Executors.newScheduledThreadPool(1);

		scheduler.scheduleAtFixedRate(this, 1, 1, TimeUnit.SECONDS);
	}

	@Override
	public void teardownTest(BackendListenerContext context) throws Exception {
		LOGGER.info("Shutting down influxDB scheduler...");
		scheduler.shutdown();

		addVirtualUsersMetrics(0, 0, 0, 0, JMeterContextService.getThreadCounts().finishedThreads);
		Point endPoint = Point.measurement(TestStartEndMeasurement.MEASUREMENT_NAME).time(System.currentTimeMillis(), TimeUnit.MILLISECONDS)
				.tag(TestStartEndMeasurement.Tags.TYPE, TestStartEndMeasurement.Values.FINISHED).tag(TestStartEndMeasurement.Tags.TEST_NAME, testName).build();

		exportFileWriter.append(endPoint.lineProtocol());
		exportFileWriter.newLine();
		
		try {
			scheduler.awaitTermination(30, TimeUnit.SECONDS);
			LOGGER.info("influxDB scheduler terminated!");
		} catch (InterruptedException e) {
			LOGGER.error("Error waiting for end of scheduler");
		}

		samplersToFilter.clear();
		exportFileWriter.close();
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
		try {
			exportFileWriter.append(builder.build().lineProtocol());
			exportFileWriter.newLine();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
