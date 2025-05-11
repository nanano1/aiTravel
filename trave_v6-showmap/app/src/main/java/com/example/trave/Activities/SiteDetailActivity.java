package com.example.trave.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.trave.Adapters.detailEditADapter;
import com.example.trave.DatabaseHelper;
import com.example.trave.Domains.ItineraryAttraction;
import com.example.trave.Domains.Sites;
import com.example.trave.R;

import java.util.ArrayList;

public class SiteDetailActivity extends AppCompatActivity {
    private TextView SiteTittle;
    private TextView addressTxt;
    private TextView telTxt;
    private TextView desTxt;
    private Button saveButton;
    private RecyclerView attractionsRecyclerView;

    private ArrayList<ItineraryAttraction> itineraryAttractionList=new ArrayList<>();

    private detailEditADapter detailEditAdapter;
    private Long siteId;
    private DatabaseHelper dbHelper;
    private String address;
    private String tel;
    private String Tittle;
    private String des;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        dbHelper = new DatabaseHelper(this);
        addressTxt=findViewById(R.id.addressTxt);
        telTxt=findViewById(R.id.telTxt);
        desTxt=findViewById(R.id.desTxt);
        SiteTittle=findViewById(R.id.SiteTittle);

        Intent intent = getIntent();
        siteId=intent.getLongExtra("siteId",0);
        Sites sites=dbHelper.getSiteBySiteId(siteId);

        tel=sites.getTel();
        address=sites.getAddress();
        des=sites.getTypeDesc();
        telTxt.setText(tel);
        desTxt.setText(des);
        addressTxt.setText(address);
        SiteTittle.setText(sites.getName());



    }


}
