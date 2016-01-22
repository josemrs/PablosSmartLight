package es.jmrs.pablossmartlight;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PSLControl {

    private static final String CONTROL_IP = "192.168.1.17";
    private static final String CONTROL_PORT = "4567";
    private static final String LEDS_URL = "/leds";
    private static final String SENSORS_URL = "/sensors";

    public static final int DEFAULT_BRIGHTNESS = 100;

    public static final class COMMAND
    {
        public static final String STATIC = "/color";
        public static final String CHASE = "/chaser";
        public static final String RANDOM = "/random";
        public static final String RANGE = "/range";
    }

    public static final class CHASER_DELAY
    {
        public static final int VERY_SLOW = 150;
        public static final int SLOW = 100;
        public static final int NORMAL = 50;
    }
    public static final class RANDOM_DELAY
    {
        public static final int VERY_SLOW = 1500;
        public static final int SLOW = 1000;
        public static final int NORMAL = 500;
    }
    public static final class BRIGHTNESS
    {
        public static final int VERY_LOW = 15;
        public static final int LOW = 25;
        public static final int NORMAL = 50;
    }

    public static class SetLEDsConfig extends AsyncTask<LEDConfig, Void, Integer> {

        private static String URL = "http://" + CONTROL_IP + ":" + CONTROL_PORT + LEDS_URL;

        protected Integer doInBackground(LEDConfig... params) {

            HttpClient client = HttpClientBuilder.create().build();

            LEDConfig ledConfig = params[0];

            HttpPut putRequest = new HttpPut(URL + ledConfig.getCommmand());
            putRequest.addHeader("Content-Type", "application/json");
            putRequest.addHeader("Accept", "application/json");

            JSONObject putArguments = new JSONObject();
            try {
                putArguments.put("RGB", ledConfig.getRGB());
                putArguments.put("brightness", ledConfig.getBrightness());
                putArguments.put("delay", ledConfig.getDelay());

                StringEntity entity = new StringEntity(putArguments.toString());
                putRequest.setEntity(entity);

                return client.execute(putRequest).getStatusLine().getStatusCode();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return -1;
        }

    }

    public static class GetSensorsInformation extends AsyncTask<Void, Void, SensorsInformation> {

        private static String URL = "http://" + CONTROL_IP + ":" + CONTROL_PORT + SENSORS_URL;

        private Context m_context;
        private View m_rootView;

        public GetSensorsInformation (Context context, View rootView)
        {
            m_context = context;
            m_rootView = rootView;
        }

        @Override
        protected SensorsInformation doInBackground(Void... params) {

            HttpClient client = HttpClientBuilder.create().build();
            HttpGet getRequest = new HttpGet(URL);

            try {
                HttpResponse response = client.execute(getRequest);
                String information = EntityUtils.toString(response.getEntity());

                JSONObject jsonInformation = new JSONObject(information);

                String infoField = jsonInformation.getString(SensorsInformation.JSON_TAG.DHT_VALID);
                Boolean dhtValid = Boolean.parseBoolean(infoField);

                float temperature = 0, humidity = 0, pressure = 0, bmpTemperature = 0;

                Log.d("response", jsonInformation.toString());
                if (dhtValid) {
                    infoField = jsonInformation.getString(SensorsInformation.JSON_TAG.TEMPERATURE);
                    temperature = Float.parseFloat(infoField) / SensorsInformation.FACTOR;
                    infoField = jsonInformation.getString(SensorsInformation.JSON_TAG.HUMIDITY);
                    humidity = Float.parseFloat(infoField) / SensorsInformation.FACTOR;
                }

                infoField = jsonInformation.getString(SensorsInformation.JSON_TAG.BMP_VALID);
                Boolean bmpValid = Boolean.parseBoolean(infoField);
                if (bmpValid) {
                    infoField = jsonInformation.getString(SensorsInformation.JSON_TAG.PRESSURE);
                    pressure = Float.parseFloat(infoField) / SensorsInformation.FACTOR;
                    infoField = jsonInformation.getString(SensorsInformation.JSON_TAG.BMT_TEMPERATURE);
                    bmpTemperature = Float.parseFloat(infoField) / SensorsInformation.FACTOR;
                }

                return new SensorsInformation(dhtValid, temperature, humidity, bmpValid, pressure, bmpTemperature);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return new SensorsInformation();
        }

        @Override
        protected void onPostExecute(SensorsInformation info)
        {
            String failure = "Failed to retrieve information: ";
            TextView textView;
            String timeStamp = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(Calendar.getInstance().getTime());

            if (info.IsDhtValid()) {
                textView = (TextView) m_rootView.findViewById(R.id.textViewTemperature);
                textView.setText(String.format("Temperature: %.2f °C", info.getTemperature()));
                textView = (TextView) m_rootView.findViewById(R.id.textViewHumidity);
                textView.setText(String.format("Humidity: %.2f %%", info.getHumidity()));
            }
            else {
                failure += "Temp/Humid ";
            }

            if (info.IsBmpValid()) {

                textView = (TextView) m_rootView.findViewById(R.id.textViewPressure);
                textView.setText(String.format("Pressure: %.2f hPa", info.getPressure()));
                textView = (TextView) m_rootView.findViewById(R.id.textViewBmpTemperature);
                textView.setText(String.format("Internal temperature: %.2f °C", info.getBmpTemperature()));
            }
            else {
                failure += "Press";
            }

            textView = (TextView) m_rootView.findViewById(R.id.textViewStatus);

            if (info.IsDhtValid() && info.IsBmpValid()) {
                textView.setText("Updated at " + timeStamp);
            }
            else
            {
                textView.setText(failure);
            }

            int fakeColor = SensorsInformation.MAX_TEMPERATURE << 16;
            fakeColor = fakeColor | ((int)Math.round(info.getTemperature()) << 8);
            fakeColor = fakeColor | SensorsInformation.MIN_TEMPERATURE;
            new PSLControl.SetLEDsConfig().execute(new LEDConfig(PSLControl.COMMAND.RANGE, fakeColor, 100, 0));


            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            fakeColor = SensorsInformation.MIN_HUMIDITY << 16;
            fakeColor = fakeColor | ((int)Math.round(info.getHumidity()) << 8);
            fakeColor = fakeColor | SensorsInformation.MAX_HUMIDITY;
            new PSLControl.SetLEDsConfig().execute(new LEDConfig(PSLControl.COMMAND.RANGE, fakeColor, 100, 0));
        }
    }

}
