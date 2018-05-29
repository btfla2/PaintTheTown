package com.example.btfla2.paintthetown;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    Button startPaintingButton;
    Button viewMapButton;
    Button startWorkoutButton;
    Button planRouteButton;
    FloatingActionButton centerFAB;
    TextView rightBarTextView;
    TextView leftBarTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startPaintingButton = (Button) findViewById(R.id.startPaintingButton);
        startPaintingButton.setOnClickListener(this);
        viewMapButton = (Button) findViewById(R.id.viewMapButton);
        viewMapButton.setOnClickListener(this);
        startWorkoutButton = (Button) findViewById(R.id.startWorkoutButton);
        startWorkoutButton.setOnClickListener(this);
        planRouteButton = (Button) findViewById(R.id.planRouteButton);
        planRouteButton.setOnClickListener(this);
        centerFAB = (FloatingActionButton) findViewById(R.id.centerFAB);
        centerFAB.setOnClickListener(this);

        rightBarTextView = (TextView) findViewById(R.id.rightTextView);
        leftBarTextView = (TextView) findViewById(R.id.leftTextView);

        setBarText();
    }

    private void setBarText() {
        rightBarTextView.setText("");
        leftBarTextView.setText("");
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.startPaintingButton) {
            Intent newIntent = new Intent(this, MapsActivity.class);
            newIntent.putExtra("painting", true);

            startActivity(newIntent);

        } else if (v.getId() == R.id.viewMapButton) {
            Intent newIntent = new Intent(this, MapsActivity.class);
            newIntent.putExtra("painting", false);

            startActivity(newIntent);

        } else if (v.getId() == R.id.startWorkoutButton) {
            Intent newIntent = new Intent(this, WorkoutActivity.class);

            startActivity(newIntent);

        } else if (v.getId() == R.id.planRouteButton) {
            Intent newIntent = new Intent(this, MapsActivity.class);

            startActivity(newIntent);

        } else if (v.getId() == R.id.centerFAB) {
            Intent newIntent = new Intent(this, MapsActivity.class);
            newIntent.putExtra("painting", true);

            startActivity(newIntent);

        }

    }
}
