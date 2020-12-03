package org.tensorflow.lite.examples.superresolution;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

public class RegisterActivity extends AppCompatActivity {
    EditText emailId;
    EditText Password;
    Button signup;
    TextView tvsignin;
    EditText name;
    FirebaseAuth mFirebaseAuth;
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mFirebaseAuth.getCurrentUser();
        if(mFirebaseAuth.getCurrentUser()!=null){
            //display
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mFirebaseAuth = FirebaseAuth.getInstance();
        name= (EditText)findViewById(R.id.name);
        emailId = (EditText)findViewById(R.id.emailId);
        Password = (EditText)findViewById(R.id.Password);
        signup = (Button)findViewById(R.id.signup);
        tvsignin = (TextView)findViewById(R.id.tvsignin);

        signup.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String Username = name.getText().toString();
                final String email = emailId.getText().toString();
                String pwd = Password.getText().toString();
                if (Username.isEmpty()) {
                    name.setError("Please Enter a valid Email");
                    name.requestFocus();
                }
                else if (email.isEmpty()) {
                    emailId.setError("Please Enter a valid Email");
                    emailId.requestFocus();
                } else if (pwd.isEmpty()) {
                    Password.setError("Please enter a password");
                        Password.requestFocus();}
                else if(pwd.length()<6){
                    Password.setError("the password size should be more");
                    Password.requestFocus();
                }

                else if (email.isEmpty() && Username.isEmpty() && pwd.isEmpty()) {
                    Toast.makeText(org.tensorflow.lite.examples.superresolution.RegisterActivity.this, "Enter your email, Username, and Password", Toast.LENGTH_SHORT).show();
                } else if (!(email.isEmpty() && pwd.isEmpty())) {
                    mFirebaseAuth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(org.tensorflow.lite.examples.superresolution.RegisterActivity.this, new OnCompleteListener<AuthResult>() {
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                User user = new User(
                                        Username,email
                                );
                                FirebaseDatabase.getInstance().getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(user)
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful()){
                                            Toast.makeText(org.tensorflow.lite.examples.superresolution.RegisterActivity.this, "Sign Up successful!!", Toast.LENGTH_SHORT).show();
                                            startActivity(new Intent(org.tensorflow.lite.examples.superresolution.RegisterActivity.this, LoginActivity.class));
                                        }
                                        else{
                                            Toast.makeText(org.tensorflow.lite.examples.superresolution.RegisterActivity.this, "Sign Up Unsuccessful, Please try again!!", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });

                            } else {
                                Toast.makeText(org.tensorflow.lite.examples.superresolution.RegisterActivity.this, "Signup unsuccessful",Toast.LENGTH_SHORT).show();
                            }

                        }
                    });
                }

            }
        });
        tvsignin.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(org.tensorflow.lite.examples.superresolution.RegisterActivity.this, LoginActivity.class);
                startActivity(i);
            }
        });
    }

}









