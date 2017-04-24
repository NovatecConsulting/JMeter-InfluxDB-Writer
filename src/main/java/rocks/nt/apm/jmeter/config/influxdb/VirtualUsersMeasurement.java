package rocks.nt.apm.jmeter.config.influxdb;

/**
 * Constants (Tag, Field, Measurement) names for the virtual users measurement.
 * 
 * @author Alexander Wert
 *
 */
public interface VirtualUsersMeasurement {

	/**
	 * Measurement name.
	 */
	String MEASUREMENT_NAME = "virtualUsers";

	/**
	 * Tags.
	 * 
	 * @author Alexander Wert
	 *
	 */
	public interface Tags {
		/**
		 * Node name field
		 */
		String NODE_NAME = "nodeName";
	}

	/**
	 * Fields.
	 * 
	 * @author Alexander Wert
	 *
	 */
	public interface Fields {
		/**
		 * Minimum active threads field.
		 */
		String MIN_ACTIVE_THREADS = "minActiveThreads";
		/**
		 * Maximum active threads field.
		 */
		String MAX_ACTIVE_THREADS = "maxActiveThreads";

		/**
		 * Mean active threads field.
		 */
		String MEAN_ACTIVE_THREADS = "meanActiveThreads";

		/**
		 * Started threads field.
		 */
		String STARTED_THREADS = "startedThreads";

		/**
		 * Finished threads field.
		 */
		String FINISHED_THREADS = "finishedThreads";
	}
}
