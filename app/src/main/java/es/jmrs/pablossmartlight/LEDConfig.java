package es.jmrs.pablossmartlight;

/**
 * Created by jmrodriguez on 19/12/2015.
 */
public class LEDConfig {

        private String m_command;
        private int m_RGB;
        private int m_brightness;
        private int m_delay;

        public LEDConfig(String command, int red, int green, int blue, int brightness, int delay)
        {
            m_command = command;
            m_RGB = (red << 16) | (green << 8) | blue;
            m_brightness = brightness;
            m_delay = delay;
        }

        public LEDConfig(String command, int RGB, int brightness, int delay)
        {
            m_command = command;
            m_RGB = RGB & 0x00FFFFFF;
            m_brightness = brightness;
            m_delay = delay;
        }

        String getCommmand() { return m_command; }
        String getRGB() { return String.format("0x%06X", m_RGB); }
        int getRedComponent() { return (m_RGB >> 16) & 0xFF; }
        int getGreenComponent()
        {
            return (m_RGB >> 8) & 0xFF;
        }
        int getBlueComponent()
        {
            return (m_RGB) & 0xFF;
        }
        int getBrightness() {
            return m_brightness;
        }
        int getDelay() {
            return m_delay;
        }

    public String toString()
    {
        return "Command " + getCommmand() + " RGB [" + getRedComponent() + ", " + getGreenComponent() + ", " +
                getBlueComponent() + "] Brightness " + getBrightness() + " Delay " + getDelay();
    }
}
