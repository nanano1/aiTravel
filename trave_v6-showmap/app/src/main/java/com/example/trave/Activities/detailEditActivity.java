package com.example.trave.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Adapters.detailEditADapter;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.R;

import java.util.ArrayList;

public class detailEditActivity extends AppCompatActivity {

    private TextView itineraryNameEditText;
    private TextView itineraryLocationEditText;
    private Button saveButton;
    private RecyclerView attractionsRecyclerView;

    private ArrayList<ItineraryAttraction> itineraryAttractionList=new ArrayList<>();
    private Long itineraryId;
    private detailEditADapter detailEditAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_itinerary);

        itineraryLocationEditText = findViewById(R.id.itineraryLocationText);
        itineraryNameEditText = findViewById(R.id.itineraryNameText);
        saveButton = findViewById(R.id.saveEdit);
        attractionsRecyclerView = findViewById(R.id.attractionsRecyclerView);

        Intent intent = getIntent();
        itineraryId = intent.getLongExtra("itineraryId", 0);
        Log.d("detailEditActivity", "itineraryId " + itineraryId);
        String itineraryTittle = intent.getStringExtra("itineraryTittle");
        String itineraryLocation = intent.getStringExtra("itineraryLocation");
        itineraryNameEditText.setText(itineraryTittle);
        itineraryLocationEditText.setText(itineraryLocation);
        itineraryAttractionList = intent.getParcelableArrayListExtra("itineraryAttractions");

        detailEditAdapter = new detailEditADapter(itineraryAttractionList);
        attractionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        attractionsRecyclerView.setAdapter(detailEditAdapter);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateItineraryAttractions();
                saveItinerary();
            }
        });
    }

    private void updateItineraryAttractions() {
        for (int i = 0; i < attractionsRecyclerView.getChildCount(); i++) {
            View itemView = attractionsRecyclerView.getChildAt(i);

            detailEditADapter.detailEditViewHolder holder = (detailEditADapter.detailEditViewHolder) attractionsRecyclerView.getChildViewHolder(itemView);
            ItineraryAttraction attraction = itineraryAttractionList.get(i);

            attraction.setAttractionName(holder.attractionNameEditText.getText().toString());
            attraction.setTransport(holder.transportEditText.getText().toString());

            try {
                attraction.setDayNumber(Integer.parseInt(holder.dayNumberEditText.getText().toString()));
            } catch (NumberFormatException e) {
                // Handle the exception
            }

            try {
                attraction.setVisitOrder(Integer.parseInt(holder.visitOrderEditText.getText().toString()));
            } catch (NumberFormatException e) {
                // Handle the exception
            }
        }
        for (ItineraryAttraction attraction : itineraryAttractionList) {
            Log.d("detailEditActivity", "Updated Attraction: " + attraction.getAttractionName());
        }
        detailEditAdapter.notifyDataSetChanged();
    }

    private void saveItinerary() {
        String itineraryName = itineraryNameEditText.getText().toString();

        if (itineraryName.isEmpty() || itineraryAttractionList.isEmpty()) {
            Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent resultIntent = new Intent();
        resultIntent.putExtra("itineraryId", itineraryId);
        Log.d("detailEditActivity", "ResultitineraryId " + itineraryId);

        resultIntent.putExtra("itineraryTittle", itineraryName);
        resultIntent.putParcelableArrayListExtra("itineraryAttractions", itineraryAttractionList);

        Log.d("detailEditActivity", "Returning Itinerary ID: " + itineraryId);
        Log.d("detailEditActivity", "Returning Attractions: " + itineraryAttractionList.size());

        setResult(RESULT_OK, resultIntent);
        finish();
    }

}

