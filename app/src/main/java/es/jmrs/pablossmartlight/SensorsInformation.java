package es.jmrs.pablossmartlight;

/**
 * Created by jmrodriguez on 20/12/2015.
 */
public class SensorsInformation {

    public static final int MIN_TEMPERATURE = 16;
    public static final int MAX_TEMPERATURE = 22;
    public static final int MIN_HUMIDITY = 40;
    public static final int MAX_HUMIDITY = 60;

    private boolean m_dhtValid;
    private double m_temperature;
    private double m_humidity;
    private boolean m_bmpValid;
    private double m_pressure;
    private double m_bmpTemperature;

    public static final class JSON_TAG
    {
        public static final String DHT_VALID = "dhtvalid";
        public static final String TEMPERATURE = "temperature";
        public static final String HUMIDITY = "humidity";
        public static final String BMP_VALID = "bmpvalid";
        public static final String PRESSURE = "pressure";
        public static final String BMT_TEMPERATURE = "bmptemperature";
    }

    public static final float FACTOR = 10;

    public SensorsInformation()
    {
        m_dhtValid = m_bmpValid = false;
        m_temperature = m_humidity = m_pressure = m_bmpTemperature = 0;
    }

    public SensorsInformation(
            boolean dhtValid,
            double temperature,
            double humidity,
            boolean bmpValid,
            double pressure,
            double bmpTemperature
    )
    {
        m_dhtValid = dhtValid;
        m_temperature = temperature;
        m_humidity = humidity;
        m_bmpValid = bmpValid;
        m_pressure = pressure;
        m_bmpTemperature = bmpTemperature;
    }

    boolean IsDhtValid() { return m_dhtValid; }
    boolean IsBmpValid() { return m_bmpValid; }
    double getTemperature() { return m_temperature; }
    double getHumidity() { return m_humidity; }
    double getPressure() { return m_pressure; }
    double getBmpTemperature() { return m_bmpTemperature; }
}
