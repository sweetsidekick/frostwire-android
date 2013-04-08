package com.frostwire.android.tests;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.widget.TextView;

public class LeakyActivity extends Activity {
    
    private TextView textViewFormatterDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaky);
        textViewFormatterDisplay = (TextView) findViewById(R.id.text_leaky_format);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_leaky, menu);
        return true;
    }

    public TextView getTextView() {
        return textViewFormatterDisplay;
    }
}
