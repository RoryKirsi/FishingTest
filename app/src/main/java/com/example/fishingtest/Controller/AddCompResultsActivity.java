package com.example.fishingtest.Controller;

import android.content.Intent;
import android.os.Bundle;
import android.os.TestLooperManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.fishingtest.Adapter.EditCompListAdapter;
import com.example.fishingtest.Model.Common;
import com.example.fishingtest.Model.Competition;
import com.example.fishingtest.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import org.w3c.dom.Text;

import java.util.ArrayList;

public class AddCompResultsActivity extends AppCompatActivity {

    final static String WAIT_FOR_RESULTS_STATUS = "2";


    // UI
    ListView cListView;
    TextView cName;
    TextView cDate;
    TextView cStartTime;
    TextView cStopTime;
    TextView cReward;
    TextView cType;
    Button btn_select_comp;


    // Firebase
    DatabaseReference databaseComps;
    ArrayList<Competition> compList;

    // Competition selected
    String compID;
    String compName;
    String cStatus;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_comp_results);

        compID = new String();

        //Fetch "Waiting-for-Results"competitions from Firebase and store into compList
        databaseComps = FirebaseDatabase.getInstance().getReference("Competitions");

        databaseComps.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                compList.clear();
                for(DataSnapshot compSnapshot: dataSnapshot.getChildren()){
                    Competition comp = compSnapshot.getValue(Competition.class);
                    if(comp.getcStatus().equals(WAIT_FOR_RESULTS_STATUS)){
                        comp.checkArrayList();
                        if(comp.getWinner().equals(Common.NA) && comp.getResults().equals(Common.NA) && comp.getcStatus().equals("2"))
                            compList.add(comp);
                    }

                }

                EditCompListAdapter compListAdapter = new EditCompListAdapter(AddCompResultsActivity.this, compList);
                cListView.setAdapter(compListAdapter);

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


        // UI
        cListView = (ListView)findViewById(R.id.add_comp_result_comp_list);
        cName =(TextView)findViewById(R.id.add_comp_result_comp_name);
        cDate = (TextView)findViewById(R.id.update_comp_date);
        cStartTime = (TextView)findViewById(R.id.update_comp_start_time);
        cStopTime = (TextView)findViewById(R.id.update_comp_stop_time);
        cReward = (TextView)findViewById(R.id.update_comp_reward);
        cType = (TextView)findViewById(R.id.add_comp_result_comp_type);
        btn_select_comp = (Button)findViewById(R.id.admin_button_select_comp_for_results);

        // Set up List of Competition
        compList = new ArrayList<>();

        cListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Competition comp = compList.get(position);

                cName.setText(comp.getCname());
                cDate.setText(comp.getDate());
                cStartTime.setText(comp.getStartTime());
                cStopTime.setText(comp.getStopTime());
                cReward.setText(Integer.toString(comp.getReward()));

                String[] compTypes = getBaseContext().getResources().getStringArray(R.array.comp_type);
                cType.setText(compTypes[comp.getCompType()]);

                // Hold these invisible values
                compID = comp.getCompID();
                compName = comp.getCname();
                cStatus = comp.getcStatus();     // TODO: DO WE NEED IT?
            }
        });

        // Click "View All the Posts" Buton
        btn_select_comp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent selectResultIntent = new Intent(AddCompResultsActivity.this, SelectCompWinnerActivity.class);
                selectResultIntent.putExtra(Common.compID,compID);
                selectResultIntent.putExtra(Common.compName, compName);
                startActivity(selectResultIntent);

            }
        });

    }


}