package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.text.DateFormat;

public class MainActivity extends AppCompatActivity {
    private static int SIGN_IN_REQUEST_CODE = 1;
    private FirebaseListAdapter<ChatMessage> adapter;
    private FirebaseAuth mAuth;
    private FloatingActionButton fab;
    ConstraintLayout activity_main;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        activity_main = findViewById(R.id.activity_main);
        fab = findViewById(R.id.fab);
        //set send function
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = findViewById(R.id.input_message);
                //push message to database
                FirebaseDatabase.getInstance().getReference().push().setValue(
                        new ChatMessage(input.getText().toString(), mAuth.getCurrentUser().getEmail()));
                input.setText("");//clear input box
            }
        });
        //navigate to Signin page if not signed in
        if(mAuth.getCurrentUser()==null){
            startActivityForResult(AuthUI.getInstance().createSignInIntentBuilder().build(),
                    SIGN_IN_REQUEST_CODE);
        } else {
            Snackbar.make(activity_main,"Welcome"+mAuth.getCurrentUser().getEmail(),
                    Snackbar.LENGTH_SHORT).show();
            //load content
            displayChatMessage();
        }
    }

    private void displayChatMessage() {
        ListView messages = findViewById(R.id.messages);
        Query query = FirebaseDatabase.getInstance().getReference();
        FirebaseListOptions<ChatMessage> options = new FirebaseListOptions.Builder<ChatMessage>()
                .setLayout(R.layout.list_item_layout)
                .setQuery(query, ChatMessage.class)
                .build();
        //adapter = new FirebaseListAdapter<ChatMessage>(this, ChatMessage.class, R.layout.list_item_layout,FirebaseDatabase.getInstance().getReference() ) {
        adapter = new FirebaseListAdapter<ChatMessage>(options ) {

            protected void populateView(@NonNull View v, @NonNull ChatMessage model, int position) {
                TextView messageText, messageUser, messageTime;
                messageText = v.findViewById(R.id.message_text);
                messageUser = v.findViewById(R.id.message_user);
                messageTime = v.findViewById(R.id.message_time);

                messageText.setText(model.getMessageText());
                messageUser.setText(model.getMessageUser());
                messageTime.setText(DateFormat.getInstance().format(model.getMessagetime()));
            }
        };
        messages.setAdapter(adapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==SIGN_IN_REQUEST_CODE){
            if(resultCode==RESULT_OK){
                Snackbar.make(activity_main,"Successfully signed in. Welcome!",
                        Snackbar.LENGTH_SHORT).show();
                displayChatMessage();
            } else {
                Snackbar.make(activity_main,"Couldn't sign in.  Please try again.",
                        Snackbar.LENGTH_SHORT).show();
                finish();//exit
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId()==R.id.menu_sign_out){
            AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    Snackbar.make(activity_main,"You have been signed out.",
                            Snackbar.LENGTH_SHORT).show();
                    finish();//exit
                }
            });
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        adapter.stopListening();
    }
}