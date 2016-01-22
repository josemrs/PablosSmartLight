package es.jmrs.pablossmartlight;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import android.widget.Button;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import layout.ColorsFragment;

public class MainActivity extends AppCompatActivity {
    private TextView text;
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment implements ColorPicker.OnColorChangedListener {

        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        private View m_rootView = null;
        private ColorPicker m_colorPicker;
        private SaturationBar m_saturationBar;
        private ValueBar m_valueBar;

        private LEDConfig m_ledConfig;

        @Override
        public void onColorChanged(int color) {
            m_ledConfig = new LEDConfig(PSLControl.COMMAND.STATIC, color, PSLControl.DEFAULT_BRIGHTNESS, 0);
        }

        public PlaceholderFragment() {
        }

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            m_rootView = inflater.inflate(R.layout.fragment_debug_and_test, container, false);

            TextView textView = (TextView) m_rootView.findViewById(R.id.section_label);
            textView.setText(getString(R.string.section_format, getArguments().getInt(ARG_SECTION_NUMBER)));

            m_colorPicker = (ColorPicker) m_rootView.findViewById(R.id.colorPicker);
            m_colorPicker.setOnColorChangedListener(this);

            m_saturationBar = (SaturationBar) m_rootView.findViewById(R.id.saturationBar);
            m_valueBar = (ValueBar) m_rootView.findViewById(R.id.valueBar);

            m_colorPicker.addSaturationBar(m_saturationBar);
            m_colorPicker.addValueBar(m_valueBar);

            Button button = (Button) m_rootView.findViewById(R.id.buttonChangeColor);
            final TextView statusTextView = (TextView) m_rootView.findViewById(R.id.led_status);
            button.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    m_colorPicker.setOldCenterColor(m_colorPicker.getColor());
                    m_ledConfig = new LEDConfig(PSLControl.COMMAND.STATIC, m_colorPicker.getColor(), PSLControl.DEFAULT_BRIGHTNESS, 0);
                    //statusTextView.setText(m_ledConfig.toString());
                    //new CheckLEDsStatus().execute("http://192.168.1.10:4567/leds");
                    new PSLControl.SetLEDsConfig().execute(m_ledConfig);
                }
            });

            return m_rootView;
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            Log.d("Tag selected", ""+position);

            switch (position) {
                case 0:
                    return ColorsFragment.newInstance("","");
                case 1:
                    return SensorsFragment.newInstance("", "");
                default:
                    break;
            }

            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return "Color selection and effects";
                case 1:
                    return "Sensors";
                case 2:
                    return "Debug and testing";
            }
            return null;
        }
    }
}
