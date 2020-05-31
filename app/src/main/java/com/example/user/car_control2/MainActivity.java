package com.example.user.car_control2;

import android.app.Activity;

import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
//import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Spinner;
import android.content.pm.ActivityInfo;

public class MainActivity extends Activity implements OnClickListener, AdapterView.OnItemSelectedListener {

    Button btnActAccelerometer, btnActWheel, btnActButtons, btnActMCU, btnActTouch, btnActAbout;
    private Spinner modelSpinner;
    private String selectedModelName;
    private String[] modelNames;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        Resources res = getResources();

        modelNames = res.getStringArray( R.array.tf_models );

        TextView textv = (TextView) findViewById(R.id.textView1);
        textv.setShadowLayer(1, 3, 3, Color.GRAY);

        btnActAccelerometer = (Button) findViewById(R.id.button_accel);
        btnActAccelerometer.setOnClickListener(this);

        btnActWheel = (Button) findViewById(R.id.button_wheel);
        btnActWheel.setOnClickListener(this);

        btnActButtons = (Button) findViewById(R.id.button_buttons);
        btnActButtons.setOnClickListener(this);

        btnActMCU = (Button) findViewById(R.id.button_mcu);
        btnActMCU.setOnClickListener(this);

        btnActTouch = (Button) findViewById(R.id.button_touch);
        btnActTouch.setOnClickListener(this);

        btnActAbout = (Button) findViewById(R.id.button_about);
        btnActAbout.setOnClickListener(this);

        modelSpinner = findViewById(R.id.model_spinner);
        modelSpinner.setOnItemSelectedListener(this);

        selectedModelName = modelNames[0];
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_accel:
                Intent intent_accel = new Intent(this, ActivityAccelerometer.class);
                intent_accel.putExtra("model_name", selectedModelName);
                startActivity(intent_accel);
                break;
            case R.id.button_wheel:
                Intent intent_wheel = new Intent(this, ActivityWheel.class);
                startActivity(intent_wheel);
                break;
            case R.id.button_buttons:
                Intent intent_buttons = new Intent(this, ActivityButtons.class);
                startActivity(intent_buttons);
                break;
            case R.id.button_mcu:
                Intent intent_mcu = new Intent(this, ActivityMCU.class);
                startActivity(intent_mcu);
                break;
            case R.id.button_touch:
                Intent intent_touch = new Intent(this, ActivityTouch.class);
                startActivity(intent_touch);
                break;
            case R.id.button_about:
                Intent intent_about = new Intent(this, ActivityAbout.class);
                startActivity(intent_about);
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent intent = new Intent();
        intent.setClass(MainActivity.this, SetPreferenceActivity.class);
        startActivityForResult(intent, 0);

        return true;
    }
    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        if (parent == modelSpinner) {
            int selectedPosition = modelSpinner.getSelectedItemPosition();
            selectedModelName = modelNames[selectedPosition];
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    // Do nothing.
    }

}