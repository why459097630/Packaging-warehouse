package com.ndjc.generated;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        String artifactName = getIntent().getStringExtra("artifactName");

        TextView nameTextView = findViewById(R.id.artifact_name);
        ImageView imageView = findViewById(R.id.artifact_image);
        TextView descriptionTextView = findViewById(R.id.artifact_description);

        nameTextView.setText(artifactName);
        // Set image and description based on artifactName
        // This is a placeholder implementation
        imageView.setImageResource(R.drawable.artifact_placeholder);
        descriptionTextView.setText("Description for " + artifactName);
    }
}
