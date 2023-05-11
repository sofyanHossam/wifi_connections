package com.example.wificonnection.Server;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.wificonnection.R;
import com.vkpapps.apmanager.APManager;
import com.vkpapps.apmanager.DefaultFailureListener;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerActivity extends AppCompatActivity implements APManager.OnSuccessListener{

    String Pssid,Ppassword;
    Button send_but;
    EditText messagespace;
    TextView chatspace;
    final Context context = this;
    String ip,name="admin";
    static Socket arr[] = new Socket[100];
    static int num = 0;
    Handler handler = new Handler();
    private ChatArrayAdapter chatArrayAdapter;
    private ListView listView;
    private boolean side = false;
    ServerSocket ss;

    TextView ip_address,passwordTV,ssidTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_server);



        messagespace = (EditText) findViewById(R.id.messagespace);
        send_but = (Button) findViewById(R.id.send_but);
         ip_address= (TextView) findViewById(R.id.ipAddress);
         passwordTV=findViewById(R.id.password);
         ssidTV=findViewById(R.id.ssid);

      //  progressDialog=new ProgressDialog(this);


        listView = (ListView) findViewById(R.id.msgview);

        chatArrayAdapter = new ChatArrayAdapter(getApplicationContext(), R.layout.right);
        listView.setAdapter(chatArrayAdapter);

        listView.setTranscriptMode(AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL);
        listView.setAdapter(chatArrayAdapter);

        //to scroll the list view to bottom on data change
        chatArrayAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(chatArrayAdapter.getCount() - 1);
            }
        });


       // final EditText uname = (EditText) promptsView.findViewById(R.id.uname);
        APManager apManager = APManager.getApManager(this);
        apManager.turnOnHotspot(this, this, new DefaultFailureListener(this));


    }

    private void showAlert(String ssid, String password) {
        LayoutInflater li = LayoutInflater.from(context);

        View promptsView = li.inflate(R.layout.prompt_server, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);

        alertDialogBuilder.setView(promptsView);



       //ip_address.append(ip);
       passwordTV.append(password);
       ssidTV.append(ssid);

        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,int id) {
                                name = "Admin";
                                //uname.getText().toString();
                                Thread sc = new Thread(new StartCommunication());
                                sc.start();
                            }
                        })
                .setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        for(int i=0;i<num;i++) {
            Socket temp = arr[i];
            SendToAll thread = new SendToAll(temp,"exit");
            thread.start();
        }
        try{ss.close();}catch(Exception e){}
    }

    private boolean sendChatMessage(String str) {
        String arr[] = str.split(":");
        if(arr.length == 1) {
            if(str.contains("Server started") || str.contains("Joined"))
                chatArrayAdapter.add(new ChatMessage(false, "<font color='#00AA00'>*** " + str + "***</font>"));
            else chatArrayAdapter.add(new ChatMessage(false, "<font color='#AA0000'>*** " + str + "***</font>"));
        }
        else if (!arr[0].equals(name))
            chatArrayAdapter.add(new ChatMessage(false, "<font color='#0077CC'>" + arr[0] + "</font><br/>" + arr[1]));
        else
            chatArrayAdapter.add(new ChatMessage(true, arr[1]));
        return true;
    }

    @Override
    public void onSuccess(@NonNull String ssid, @NonNull String password) {
        Toast.makeText(context, ""+ssid, Toast.LENGTH_SHORT).show();
        Ppassword=password;
        Pssid=ssid;
        ssidTV.append(ssid);
        passwordTV.append(password);
        ip = Utils.getIPAddress(true);
        //showAlert(Pssid,Ppassword);
        Thread sc = new Thread(new StartCommunication());
        sc.start();

    }


    class StartCommunication implements Runnable {

        @Override
        public void run()
        {
            try {
                ss = new ServerSocket(55555);
                ip_address.append(ip);
                sendChatMessage("Server started at " + ip + " !!\n");

                send_but.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String message = messagespace.getText().toString();
                        messagespace.setText("");
                        message = name + ": " + message;
                        final String mes = message;
                        sendChatMessage(mes + "\n");
                        for(int i=0;i<num;i++) {
                            Socket temp = arr[i];
                            SendToAll thread = new SendToAll(temp,message);
                            thread.start();
                        }
                    }
                });

                while (true){
                    Socket clientSocket = ss.accept( );
                    ServerThread thread = new ServerThread(clientSocket);
                    arr[num++] = clientSocket;
                    thread.start( );
                }

            }catch(final Exception e){

                sendChatMessage(e.toString());

            }
        }
    }

    class SendToAll extends Thread {

        Socket s;
        String msg;
        SendToAll(Socket s,String msg)
        {
            this.s = s;
            this.msg = msg;
        }

        public void run() {
            try{
                PrintStream ps = new PrintStream(s.getOutputStream( ));
                ps.println(msg);
                if(msg.equalsIgnoreCase("exit"))
                    for(int i=0;i<num;i++) {
                        if(arr[i] == s)
                        {
                            s.close();
                            break;
                        }
                    }
                ps.flush();
            }catch(final Exception e){
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        chatspace.append(e.toString());
                    }
                });
            }
        }
    }

    class ServerThread extends Thread {

        Socket clientSocket;

        ServerThread(Socket cs){
            clientSocket = cs;
        }

        public void run(){
            try{
                String str;
                BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                while(true)
                {
                    str = br.readLine( );
                    if(str.startsWith("Ex1+:"))
                    {
                        str = str.substring(5,str.length()) + " Left";
                        for(int i=0;i<num;i++) {
                            if(arr[i] == clientSocket)
                                for(int j=i;j<num - 1;j++)
                                    arr[j] = arr[j+1];
                            num--;

                        }
                        clientSocket.close();
                        for(int i=0;i<num;i++) {
                            Socket temp = arr[i];
                            SendToAll thread = new SendToAll(temp,str);
                            thread.start();
                        }
                        sendChatMessage(str + "\n");
                        break;
                    }
                    if(str.substring(0,6).equals("j01ne6"))
                        str = str.substring(7,str.length()) + " Joined";
                    sendChatMessage(str + "\n");
                    for(int i=0;i<num;i++) {
                        Socket temp = arr[i];
                        SendToAll thread = new SendToAll(temp,str);
                        thread.start();
                    }
                }
            }
            catch(final Exception e){
                try{

                    for(int i=0;i<num;i++) {
                        if(arr[i] == clientSocket)
                            for(int j=i;j<num - 1;j++)
                                arr[j] = arr[j+1];
                    }
                    num--;
                    clientSocket.close();
                }catch(Exception ex){ex.printStackTrace();}}
        }
    }
}