package rocks.nt.apm.jmeter.config.influxdb;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.visualizers.backend.BackendListenerContext;

/**
 * Configuration for influxDB.
 * 
 * @author Alexander Wert
 *
 */
public class InfluxDBConfig {

	/**
	 * Default database name.
	 */
	public static final String DEFAULT_DATABASE = "jmeter";

	/**
	 * Default retention policy name.
	 */
	public static final String DEFAULT_RETENTION_POLICY = "autogen";

	/**
	 * Default http scheme name.
	 */
	public static final String DEFAULT_HTTP_SCHEME = "http";
	
	/**
	 * Default port.
	 */
	public static final int DEFAULT_PORT = 8086;

	/**
	 * Config key for database name.
	 */
	public static final String KEY_INFLUX_DB_DATABASE = "influxDBDatabase";

	/**
	 * Config key for password.
	 */
	public static final String KEY_INFLUX_DB_PASSWORD = "influxDBPassword";

	/**
	 * Config key for user name.
	 */
	public static final String KEY_INFLUX_DB_USER = "influxDBUser";

	/**
	 * Config key for port.
	 */
	public static final String KEY_INFLUX_DB_PORT = "influxDBPort";

	/**
	 * Config key for host.
	 */
	public static final String KEY_INFLUX_DB_HOST = "influxDBHost";

	/**
	 * Config key for retention policy name.
	 */
	public static final String KEY_RETENTION_POLICY = "retentionPolicy";
	
	/**
	 * Config key for http scheme.
	 */
	public static final String KEY_HTTP_SCHEME = "influxHTTPScheme";

	/**
	 * InfluxDB Host.
	 */
	private String influxDBHost;

	/**
	 * InfluxDB User.
	 */
	private String influxUser;

	/**
	 * InfluxDB Password.
	 */
	private String influxPassword;

	/**
	 * InfluxDB database name.
	 */
	private String influxDatabase;

	/**
	 * InfluxDB database retention policy.
	 */
	private String influxRetentionPolicy;

	/**
	 * InfluxDB Port.
	 */
	private int influxDBPort;
	
	/**
	 * InfluxDB database retention policy.
	 */
	private String influxHTTPScheme;

	public InfluxDBConfig(BackendListenerContext context) {
		String influxDBHost = context.getParameter(KEY_INFLUX_DB_HOST);
		if (StringUtils.isEmpty(influxDBHost)) {
			throw new IllegalArgumentException(KEY_INFLUX_DB_HOST + "must not be empty!");
		}
		setInfluxDBHost(influxDBHost);

		int influxDBPort = context.getIntParameter(KEY_INFLUX_DB_PORT, InfluxDBConfig.DEFAULT_PORT);
		setInfluxDBPort(influxDBPort);

		String influxUser = context.getParameter(KEY_INFLUX_DB_USER);
		setInfluxUser(influxUser);

		String influxPassword = context.getParameter(KEY_INFLUX_DB_PASSWORD);
		setInfluxPassword(influxPassword);

		String influxDatabase = context.getParameter(KEY_INFLUX_DB_DATABASE);
		if (StringUtils.isEmpty(influxDatabase)) {
			throw new IllegalArgumentException(KEY_INFLUX_DB_DATABASE + "must not be empty!");
		}
		setInfluxDatabase(influxDatabase);

		String influxRetentionPolicy = context.getParameter(KEY_RETENTION_POLICY, DEFAULT_RETENTION_POLICY);
		if (StringUtils.isEmpty(influxRetentionPolicy)) {
			influxRetentionPolicy = DEFAULT_RETENTION_POLICY;
		}
		setInfluxRetentionPolicy(influxRetentionPolicy);
		
		String influxHTTPScheme = context.getParameter(KEY_HTTP_SCHEME, DEFAULT_HTTP_SCHEME);
		if (StringUtils.isEmpty(influxHTTPScheme)) {
			influxHTTPScheme = DEFAULT_HTTP_SCHEME;
		}
		// TODO: no checks but should be only "http" and "https"
		setInfluxHTTPScheme(influxHTTPScheme);
	}

	/**
	 * Builds URL to influxDB.
	 * 
	 * @return influxDB URL.
	 */
	public String getInfluxDBURL() {
		return influxHTTPScheme + "://" + influxDBHost + ":" + influxDBPort;
	}

	/**
	 * @return the influxDBHost
	 */
	public String getInfluxDBHost() {
		return influxDBHost;
	}

	/**
	 * @param influxDBHost
	 *            the influxDBHost to set
	 */
	public void setInfluxDBHost(String influxDBHost) {
		this.influxDBHost = influxDBHost;
	}

	/**
	 * @return the influxUser
	 */
	public String getInfluxUser() {
		return influxUser;
	}

	/**
	 * @param influxUser
	 *            the influxUser to set
	 */
	public void setInfluxUser(String influxUser) {
		this.influxUser = influxUser;
	}

	/**
	 * @return the influxPassword
	 */
	public String getInfluxPassword() {
		return influxPassword;
	}

	/**
	 * @param influxPassword
	 *            the influxPassword to set
	 */
	public void setInfluxPassword(String influxPassword) {
		this.influxPassword = influxPassword;
	}

	/**
	 * @return the influxDatabase
	 */
	public String getInfluxDatabase() {
		return influxDatabase;
	}

	/**
	 * @param influxDatabase
	 *            the influxDatabase to set
	 */
	public void setInfluxDatabase(String influxDatabase) {
		this.influxDatabase = influxDatabase;
	}

	/**
	 * @return the influxRetentionPolicy
	 */
	public String getInfluxRetentionPolicy() {
		return influxRetentionPolicy;
	}

	/**
	 * @param influxRetentionPolicy
	 *            the influxRetentionPolicy to set
	 */
	public void setInfluxRetentionPolicy(String influxRetentionPolicy) {
		this.influxRetentionPolicy = influxRetentionPolicy;
	}

	/**
	 * @param influxHTTPScheme
	 *            the influxHTTPScheme to set
	 */
	public void setInfluxHTTPScheme(String influxHTTPScheme) {
		this.influxHTTPScheme = influxHTTPScheme;
	}

	/**
	 * @return the influxDBPort
	 */
	public int getInfluxDBPort() {
		return influxDBPort;
	}

	/**
	 * @param influxDBPort
	 *            the influxDBPort to set
	 */
	public void setInfluxDBPort(int influxDBPort) {
		this.influxDBPort = influxDBPort;
	}
}
