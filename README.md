# JMeter-InfluxDB-Writer
Plugin for JMeter that allows to write load test data on-the-fly to influxDB.

## Installation ##
After you have installed JMeter on your machine put a plugin JAR-file (for example *JMeter-InfluxDB-Writer-plugin-1.2.jar*) 
into the *[JMETER_HOME]/lib/ext* directory and then start JMeter.   

The plugin can bee seen when you add a Backend-Listener element to your test plan. In the JMeter's GUI you see 
a drop-down list named Backend Listener implementation. One of the entries must be 
**rocks.nt.apm.jmeter.JMeterInfluxDBBackendListenerClient**.

## Configuration of the Listener ##
It is recommended that you add only one Backend-Listener element for your whole test plan. Thus you get a single point 
of service for all your samplers. After you select from the implementation drop-down list the plugin you have 
to configure it.

Please note, when the plugin runs it creates automatically (on the very first run) three tables filled later on with
statistic performance data:
* requestsRaw - detailed information about performance of the application under test (its version, error count, sampler/request names, hardware hostname etc.)
* testStartEnd - keeps time stamps about start and end of specific test runs associated with run-Ids (see below _runId_).
* virtualUsers - keeps time stamps about virtual users (also called threads or parallel clients) in specific test runs associated with run-Ids.

The plugin has the following attributes to be filled out in:
#### testName ####
You may put here the name of you test plan.

#### nodeName #### 
For example, here you may place name of the host/machine where the test runs, thus having a specific information 
about the hardware and therefore additional performance data about the software.

#### runId ### 
Descriptor/Id for the test run

#### optionalTags #### 
This attribute is empty per default. But you may specify here additional tags for the InfluxDB-table _requestsRaw_ 
to have more descriptors (or querying possibilities) for your statistic data. This is a text with key-value pairs 
delimited by _coma, colon or semicolon_. Whereas key is a tag to be recorded in the InfluxDB-database and value is 
its value for the current measurement. 
	 
For ex. "appVersion=4.1.11;testdataVersion=3.0" means that the InfluxDB-table _requestsRaw_ gets two tags "appVersion" 
and "testdataVersion" and every measurement gets for these two tags values "4.1.11" and "3.0" correspondingly.

#### influxDBHost ####
Host where InfluxDB runs.
 
#### influxDBPort #### 
Port of the InfluxDB to be used.

#### influxDBUser #### 
Database user.

#### influxDBPassword ####
Database user's password.
 
#### influxDBDatabase #### 
Name of the database to be filled with the performance data. Note that it must be created before the test starts. 
Otherwise an error would be reported.

#### retentionPolicy #### 
Descriptor about how long the statistical data should be stored and when its compression starts. Read more in 
the documentation of the InfluxDB project.

#### samplersList #### 
A regex-string describing which samplers should be caught by the given Backend Listener.

#### useRegexForSamplerList ####
A flag (_true_ or _false_) indicating that the attribute **samplersList** (see above) is a regex or a simple text string.
 
#### recordSubSamples ####
A flag (_true_ or _false_) indicating whether sub-samplers should be caught by the given Backend Listener thus making +
the collected performance data more granular.