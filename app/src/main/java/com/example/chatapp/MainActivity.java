package com.example.chatapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import androidx.core.app.ActivityCompat;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.database.FirebaseListAdapter;
import com.firebase.ui.database.FirebaseListOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.text.DateFormat;

public class MainActivity extends AppCompatActivity {
    private static int SIGN_IN_REQUEST_CODE = 1;
    private FirebaseListAdapter<ChatMessage> adapter;
    private FirebaseAuth mAuth;
    private FloatingActionButton fab;
    ConstraintLayout activity_main;
    private StorageReference mStorageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        activity_main = findViewById(R.id.activity_main);
        fab = findViewById(R.id.fab);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        //set send function
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText input = findViewById(R.id.input_message);
                //push message to database
                FirebaseDatabase.getInstance().getReference().push().setValue(
                        new ChatMessage(input.getText().toString(), mAuth.getCurrentUser().getEmail()));
                input.setText("");//clear input box
                upload();
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
    private void upload(){
        //verify read permission
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            // this will request for permission when permission is not true
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
        Uri file = Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/mynotes.txt"));
        StorageReference notesRef = mStorageRef.child("notes/funnotes.txt");

        notesRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
                        Uri downloadUrl = taskSnapshot.getUploadSessionUri();
                        System.out.println("testing: "+ downloadUrl);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                        System.out.println("testing: "+ exception);
                    }
                });
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
        adapter.startListening();
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
        if(adapter!=null)
        adapter.startListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if(adapter!=null)
        adapter.stopListening();
    }
}