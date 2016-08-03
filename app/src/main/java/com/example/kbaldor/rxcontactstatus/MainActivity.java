package com.example.kbaldor.rxcontactstatus;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import com.example.kbaldor.rxcontactstatus.adapters.ContactListViewAdapter;
import com.example.kbaldor.rxcontactstatus.adapters.UserInformation;
import com.example.kbaldor.rxcontactstatus.stages.GetChallengeStage;
import com.example.kbaldor.rxcontactstatus.stages.GetServerKeyStage;
import com.example.kbaldor.rxcontactstatus.stages.LogInStage;
import com.example.kbaldor.rxcontactstatus.stages.RegisterContactsStage;
import com.example.kbaldor.rxcontactstatus.stages.RegistrationStage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private ListView mListView;
    private EditText mNewUserEdTxt;
    private Button mSaveButton;

    Crypto myCrypto;
    String username = "kavya";
    String server_name = "http://129.115.27.54:25666";

    ArrayList<UserInformation> contacts;
    ArrayList<String> contactNames;

    Object mutex;

    ContactListViewAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mutex = new Object();

        contacts = new ArrayList<>();
        contacts.add(new UserInformation("alice", false));
        contacts.add(new UserInformation("bob", false));

        contactNames = new ArrayList<>();
        for(UserInformation userInfo : contacts) {
            contactNames.add(userInfo.getUserName());
        }

        adapter = new ContactListViewAdapter(this, contacts, contactNames, mutex);

        initializeUI();

        myCrypto = new Crypto(getPreferences(Context.MODE_PRIVATE));

        Observable.just(0) // the value doesn't matter, it just kicks things off
                .observeOn(Schedulers.newThread())
                .subscribeOn(Schedulers.newThread())
                .flatMap(new GetServerKeyStage(server_name))
                .flatMap(new RegistrationStage(server_name, username,
                                               getBase64Image(), myCrypto.getPublicKeyString()))
                .flatMap(new GetChallengeStage(server_name,username,myCrypto))
                .flatMap(new LogInStage(server_name, username))
                .flatMap(new RegisterContactsStage(server_name, username, contactNames))
                .subscribe(new Observer<Notification>() {
                    @Override
                    public void onCompleted() {

                        // now that we have the initial state, start polling for updates

                        Observable.interval(0, 1, TimeUnit.SECONDS, Schedulers.newThread())
                                //   .take(5) // would only poll five times
                                //   .takeWhile( <predicate> ) // could stop based on a flag variable
                                .subscribe(new Observer<Long>() {
                                    @Override
                                    public void onCompleted() {
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                    }

                                    @Override
                                    public void onNext(Long numTicks) {
                                        Log.d("POLL", "Polling " + numTicks);
                                        checkForNotifications(contactNames);
                                    }
                                });
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("LOG", "Error: ", e);
                    }

                    @Override
                    public void onNext(Notification notification) {
                        // handle initial state here
                        Log.d("LOG", "Next " + notification);
                        if (notification instanceof Notification.LogIn) {
                            Log.d("LOG", "User " + ((Notification.LogIn) notification).username + " is logged in");
                        }
                        if (notification instanceof Notification.LogOut) {
                            Log.d("LOG", "User " + ((Notification.LogOut) notification).username + " is logged out");
                        }
                    }
                });

    }

    private void initializeUI() {
        mNewUserEdTxt = (EditText) findViewById(R.id.newUserTxt);

        mSaveButton = (Button) findViewById(R.id.saveButton);
        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = mNewUserEdTxt.getText().toString().trim();

                if(!name.isEmpty() && !contactNames.contains(name)) {
                    synchronized (mutex) {
                        contacts.add(new UserInformation(name, false));
                        contactNames.add(name);

                        // Update changes in ListView
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                adapter.notifyDataSetChanged();

                            }
                        });
                    }
                }
            }

        });

        mListView = (ListView) findViewById(R.id.contactsList);
        mListView.setAdapter(adapter);

    }

    private void checkForNotifications(ArrayList<String> contactNames) {
        try {
            JSONObject json = new JSONObject();
            json.put("username", username);
            json.put("friends", new JSONArray(contactNames));
            JSONObject response = WebHelper.JSONPut(server_name + "/register-friends", json);

            JSONObject status = response.getJSONObject("friend-status-map");

            for (String contact : contactNames) {
                Log.d("POLL RESULTS ", contact + " : " + status.getString(contact));
            }

            synchronized (mutex) {
                for (UserInformation userInfo : contacts) {
                    String onlineStatus = status.getString(userInfo.getUserName());

                    if (onlineStatus == null) {
                        Log.d("UPDATE CONTACTS STATUS ", "Can't get status for <" + userInfo.getUserName() + ">");
                    } else {
                        if (onlineStatus.equals("logged-in")) {
                            userInfo.setIsOnline(true);
                        } else {
                            userInfo.setIsOnline(false);
                        }
                    }
                }
            }

            // Update changes in ListView
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    adapter.notifyDataSetChanged();

                }
            });


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    String getBase64Image(){
        InputStream is;
        byte[] buffer = new byte[0];
        try {
            is = getAssets().open("images/ic_android_black_24dp.png");
            buffer = new byte[is.available()];
            is.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Base64.encodeToString(buffer,Base64.DEFAULT).trim();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        Schedulers.shutdown();

        finish();
    }
}
