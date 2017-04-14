package rocks.nt.apm.jmeter.config.influxdb;

/**
 * Constants (Tag, Field, Measurement) names for the measurement that denotes start and end points of a load test.
 * 
 * @author Alexander Wert
 *
 */
public interface TestStartEndMeasurement {

	/**
	 * Measurement name.
	 */
	String MEASUREMENT_NAME = "testStartEnd";

	/**
	 * Tags.
	 * 
	 * @author Alexander Wert
	 *
	 */
	public interface Tags {
		/**
		 * Start or End type tag.
		 */
		String TYPE = "type";
	}
	
	/**
	 * Fields.
	 * 
	 * @author Alexander Wert
	 *
	 */
	public interface Fields {	
		/**
		 * Test name field.
		 */
		String TEST_NAME = "testName";

		/**
		 * Node name field
		 */
		String NODE_NAME = "nodeName";
	}
	
	/**
	 * Values.
	 * 
	 * @author Alexander Wert
	 *
	 */
	public interface Values {
		/**
		 * Finished.
		 */
		String FINISHED = "finished";
		/**
		 * Started.
		 */
		String STARTED = "started";
	}
}
