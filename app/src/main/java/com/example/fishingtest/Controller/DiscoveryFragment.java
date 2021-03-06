package com.example.fishingtest.Controller;


import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.example.fishingtest.Adapter.DiscAdapter;
import com.example.fishingtest.Model.Common;
import com.example.fishingtest.Model.Competition;
import com.example.fishingtest.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

/**
 * Completed by Kan Bu on 8/06/2019.
 *
 * The fragment residing in "HomePageActivity"
 * to display up-coming competitions for users to view and register
 */

public class DiscoveryFragment extends Fragment {
    // Local variables
    public static final String TAG = "Discovery Fragment";
    ArrayList<Competition> comps;
    private DatabaseReference databaseComps;
    private FirebaseUser user;

    // UI views
    RadioGroup radioGroup;
    RecyclerView recyclerView;
    DiscAdapter dAdapter;
    FloatingActionButton fab;

    //Constructor
    public DiscoveryFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Get current signed-in user ID
        final String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_discovery, container, false);

        // Get a reference to recyclerView
        recyclerView =  view.findViewById(R.id.recyclerView_discovery);
        // Set layoutManager
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setHasFixedSize(true);
        // Create an adapter
        comps = new ArrayList<>();

        // Get Competitions unregistered by the user from Firebase
        databaseComps = FirebaseDatabase.getInstance().getReference("Competitions");
        databaseComps.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                dAdapter.clearCompList();
                for(DataSnapshot compSnapshot : dataSnapshot.getChildren()){
                    Competition comp = compSnapshot.getValue(Competition.class);

                    // Only find competitions yet to start
                    String compStartAt = comp.getDate().trim() + " " + comp.getStartTime().trim() + " GMT+08:00"; // Competition Time is based on AEST by default
                    long result = Common.timeToCompStart(compStartAt);
                    if(result >= 0.0){
                        if(comp.getAttendants() == null) {
                            dAdapter.addComp(comp);
                        } else{
                            if(!comp.getAttendants().contains(userID)){
                                dAdapter.addComp(comp);
                            }
                        }
                    }
                }

                // Keep competition sorting order consistent after Comp List updated by Firebase
                switch(Common.DISCOVERY_SORT_ORDER){
                    case 0:
                        dAdapter.sortByName();
                        break;
                    case 1:
                        dAdapter.sortByDate();
                        break;
                    case 2:
                        dAdapter.sortByReward();
                        break;
                    default:
                        dAdapter.sortByName();
                }
                // Notify the changes
                dAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Radio button
        radioGroup = view.findViewById(R.id.disc_sort_group);

        // Sorting options
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {

                switch(checkedId){
                    case R.id.disc_sort_by_name:
                        dAdapter.sortByName();
                        Common.DISCOVERY_SORT_ORDER = 0;
                        break;
                    case R.id.disc_sort_by_date:
                        dAdapter.sortByDate();
                        Common.DISCOVERY_SORT_ORDER = 1;
                        break;
                    case R.id.disc_sort_by_reward:
                        dAdapter.sortByReward();
                        Common.DISCOVERY_SORT_ORDER = 2;
                        break;
                    default:
                        dAdapter.sortByName();
                        Common.DISCOVERY_SORT_ORDER = 0;
                }

                dAdapter.notifyDataSetChanged();
            }
        });

        // Set adaptor to the Recycler View
        dAdapter = new DiscAdapter(comps,getContext());
        recyclerView.setAdapter(dAdapter);

        // Click on floating action button to view the selected competition in details
        fab = (FloatingActionButton) view.findViewById(R.id.floating_button_discovery);
        fab.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                // Check if an Item have been selected
                if(dAdapter.row_index >= 0){
                    Intent myIntent = new Intent(getActivity(), ViewCompDetailsActivity.class);
                    getActivity().startActivity(myIntent);
                }
                else{
                    Toast.makeText(getContext(), "Please select a competition for full detail review", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return view;
    }





}
