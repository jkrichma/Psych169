package com.example.android.cookies;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    /**
     * Called when the cookie should be eaten.
     */
    public void eatCookie(View view) {
        ImageView cookieImage = (ImageView) findViewById(R.id.android_cookie_image_view);;
        cookieImage.setImageResource(R.drawable.after_cookie);

        TextView eatCookieText = (TextView)findViewById(R.id.status_text_view);
        eatCookieText.setText("I'm so full!");
        // TODO: Find a reference to the TextView in the layout. Change the text.

    }
}
