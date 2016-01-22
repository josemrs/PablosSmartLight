package layout;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.ToggleButton;

import es.jmrs.pablossmartlight.LEDConfig;
import es.jmrs.pablossmartlight.PSLControl;
import es.jmrs.pablossmartlight.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link ColorsFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link ColorsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ColorsFragment extends Fragment implements RadioGroup.OnCheckedChangeListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View m_fragmentView = null;
    private TextView m_selectionTextView = null;
    private ToggleButton m_toggle = null;
    private RadioGroup m_colorGroup = null;
    private RadioGroup m_effectGroup = null;
    private RadioGroup m_speedGroup = null;
    private RadioGroup m_brightnessGroup = null;

    private int m_color;
    private String m_effect;
    private int m_brightness;
    private int m_delay;

    private OnFragmentInteractionListener mListener;

    public ColorsFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ColorsFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ColorsFragment newInstance(String param1, String param2) {
        ColorsFragment fragment = new ColorsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        m_fragmentView = inflater.inflate(R.layout.fragment_colors, container, false);
        return m_fragmentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState)
    {
        m_colorGroup = (RadioGroup) m_fragmentView.findViewById(R.id.radioGroupColor);
        m_colorGroup.setOnCheckedChangeListener(this);
        m_effectGroup = (RadioGroup) m_fragmentView.findViewById(R.id.radioGroupEffect);
        m_effectGroup.setOnCheckedChangeListener(this);
        m_speedGroup = (RadioGroup) m_fragmentView.findViewById(R.id.radioGroupSpeed);
        m_speedGroup.setOnCheckedChangeListener(this);
        m_brightnessGroup = (RadioGroup) m_fragmentView.findViewById(R.id.radioGroupBrightness);
        m_brightnessGroup.setOnCheckedChangeListener(this);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedId) {

        int checked = m_colorGroup.getCheckedRadioButtonId();

        if(checked == R.id.radioButtonRed) {
            m_color = 0x00FF0000;
        } else if(checked == R.id.radioButtonGreen) {
            m_color = 0x0000FF00;
        } else if(checked == R.id.radioButtonBlue) {
            m_color = 0x000000FF;
        }

        checked = m_effectGroup.getCheckedRadioButtonId();

        if(checked == R.id.radioButtonStatic) {
            m_effect = PSLControl.COMMAND.STATIC;
        } else if(checked == R.id.radioButtonChaser) {
            m_effect = PSLControl.COMMAND.CHASE;
        } else if(checked == R.id.radioButtonRandom) {
            m_effect = PSLControl.COMMAND.RANDOM;
        }

        checked = m_speedGroup.getCheckedRadioButtonId();

        if (m_effect == PSLControl.COMMAND.STATIC) {
            m_delay = 0;
        } else if(checked == R.id.radioButtonVerySlow) {
            m_delay = m_effect == "chaser" ? PSLControl.CHASER_DELAY.VERY_SLOW : PSLControl.RANDOM_DELAY.VERY_SLOW;
        } else if(checked == R.id.radioButtonSlow) {
            m_delay = m_effect == "chaser" ? PSLControl.CHASER_DELAY.SLOW : PSLControl.RANDOM_DELAY.SLOW;
        } else if(checked == R.id.radioButtonNormalSpeed) {
            m_delay = m_effect == "chaser" ? PSLControl.CHASER_DELAY.NORMAL : PSLControl.RANDOM_DELAY.NORMAL;
        }

        checked = m_brightnessGroup.getCheckedRadioButtonId();

        if(checked == R.id.radioButtonVeryLow) {
            m_brightness = PSLControl.BRIGHTNESS.VERY_LOW;
        } else if(checked == R.id.radioButtonLow) {
            m_brightness = PSLControl.BRIGHTNESS.LOW;
        } else if(checked == R.id.radioButtonNormalBrightness) {
            m_brightness = PSLControl.BRIGHTNESS.NORMAL;
        }

        new PSLControl.SetLEDsConfig().execute(new LEDConfig(m_effect, m_color, m_brightness, m_delay));
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
