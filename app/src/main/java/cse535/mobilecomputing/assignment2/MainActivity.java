package cse535.mobilecomputing.assignment2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import db.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private String tableName = null;
    private RelativeLayout inputLayout;
    private LinearLayout graphLayout;
    private RelativeLayout serverLayout;
    private EditText nameEditText;
    private EditText ageEditText;
    private EditText IDEditText;
    private RadioButton maleRadio;
    private RadioButton femaleRadio;
    RadioGroup sexRadioGroup;
    private Button stopButton;
    private Button runButton;
    private Button submitButton;
    private Button uploadButton;
    private Button downloadActivityButton;
    private static DatabaseHelper dbHelper;
    private GraphView graphView;
    private UploadDatabase uploadDatabaseTask;

    List<float[]> valueList;
    List<float[]> emptyValueList;

    private float[] xValues = new float[10];
    private float[] yValues = new float[10];
    private float[] zValues = new float[10];

    TimerTask timerTask;
    Timer timer;
    boolean running = false;

    private static final String sURLBase = "https://impact.asu.edu/Appenstance/";
    private static final String sURLUpload = sURLBase + "UploadToServerGPS.php";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputLayout = (RelativeLayout) findViewById(R.id.input_layout);
        serverLayout = (RelativeLayout) findViewById(R.id.server_layout);
        graphLayout = (LinearLayout) findViewById(R.id.graph_layout);
        nameEditText = (EditText) findViewById(R.id.nameEditText);
        IDEditText = (EditText) findViewById(R.id.idEditText);
        ageEditText = (EditText) findViewById(R.id.ageEditText);
        sexRadioGroup = (RadioGroup) findViewById(R.id.sex_radio_group);
        maleRadio = (RadioButton) findViewById(R.id.male_radio);
        femaleRadio = (RadioButton) findViewById(R.id.female_radio);
        stopButton = (Button) findViewById(R.id.stop_button);
        runButton = (Button) findViewById(R.id.run_button);
        submitButton = (Button) findViewById(R.id.submitInputBtn);
        uploadButton = (Button) findViewById(R.id.upload_btn);
        downloadActivityButton = (Button) findViewById(R.id.download_activity_btn);
        graphLayout.setVisibility(View.GONE);
        inputLayout.setVisibility(View.VISIBLE);

        emptyValueList = new ArrayList<float[]>();
        emptyValueList.add(new float[0]);

        graphView = new GraphView(getApplicationContext(), emptyValueList, "Accelerometer Graph", null, null, GraphView.LINE);
        graphView.setLabels(10, 0);
        graphView.invalidate();
        graphView.setBackgroundColor(getResources().getColor(android.R.color.black));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 7f);
        graphView.setLayoutParams(params);
        graphLayout.addView(graphView);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString();
                String ID = IDEditText.getText().toString();
                String age = ageEditText.getText().toString();
                String sex = "";
                if (sexRadioGroup.getCheckedRadioButtonId() > -1) {
                    switch (sexRadioGroup.getCheckedRadioButtonId()) {
                        case R.id.male_radio:
                            sex = maleRadio.getText().toString();
                            break;
                        case R.id.female_radio:
                            sex = femaleRadio.getText().toString();
                            break;
                        default:
                            break;
                    }
                }
                if (!name.equals("") & !ID.equals("") & !sex.equals("") & !age.equals("")) {
                    tableName = name + "_" + ID + "_" + age + "_" + sex;
                    inputLayout.setVisibility(View.GONE);
                    graphLayout.setVisibility(View.VISIBLE);
                    startService(new Intent(MainActivity.this, AccelerometerService.class));
                    SharedPreferences prefs = getApplicationContext().getSharedPreferences("application_settings", 0);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("table_name", tableName);
                    editor.commit();
                    dbHelper = DatabaseHelper.getInstance(getApplicationContext());
                    serverLayout.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), "Service Started", Toast.LENGTH_SHORT);
                } else
                    Toast.makeText(getApplicationContext(), "Please fill out all fields", Toast.LENGTH_SHORT);

            }
        });
        runButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startTimer();
            }
        });
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
                clearGraph();
            }
        });
        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadDatabaseTask = new UploadDatabase();
                uploadDatabaseTask.execute();
            }
        });
        downloadActivityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopTimer();
                clearGraph();
                Intent i = new Intent(MainActivity.this, DownloadDatabaseActivity.class);
                startActivity(i);
            }
        });
    }

    public static String getStoredTableName(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("application_settings", 0);
        String tn = prefs.getString("table_name", null);
        return tn;
    }

    final Runnable refreshGraphRunnable = new Runnable() {
        @Override
        public void run() {
            extractMostRecentAxisValues();
            refreshGraphValues();
        }
    };
    final Handler mHandler = new Handler();

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                mHandler.post(refreshGraphRunnable);
            }
        };
    }

    private void startTimer() {
        if (running)
            return;
        running = true;
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 0, 1000);
    }

    private void stopTimer() {
        if (!running)
            return;
        running = false;
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }


    private void refreshGraphValues() {
        graphView.setValueList(valueList);
        graphView.setLabels(10, 4);
        graphView.invalidate();
    }

    private void clearGraph() {
        graphView.setValueList(emptyValueList);
        graphView.setLabels(10,0);
        graphView.invalidate();
    }

    private void extractMostRecentAxisValues() {
        List<Record> recordList = getMostRecentRecords();
        Log.i("MainActivity", "list: " + recordList);
        if (recordList != null & recordList.size() == 10) {
            Log.i("MainActivity", "New Set");
            int i = 0;
            for (Record r : recordList) {
                Log.i("MainActivity", "Record values: " + r.timestamp + " " + r.xVal + " " + r.yVal + " " + r.zVal);
                xValues[i] = r.xVal;
                yValues[i] = r.yVal;
                zValues[i] = r.zVal;
                i++;
            }
            valueList = new ArrayList<float[]>();
            valueList.add(xValues);
            valueList.add(yValues);
            valueList.add(zValues);
        }
    }

    private List<Record> getMostRecentRecords() {
        List<Record> recordList = null;
        if (dbHelper != null) {
            recordList = dbHelper.getMostRecentRecords();
        }
        Log.i("MainActivity", "db: " + dbHelper + " list: " + recordList);
        return recordList;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, AccelerometerService.class));
    }

    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
        stopService(new Intent(this, AccelerometerService.class));
    }

    @Override
    public void onResume() {
        super.onResume();
        tableName = getStoredTableName(getApplicationContext());
        dbHelper = DatabaseHelper.getInstance(getApplicationContext());
        Log.i("MainActivity", "Resume Values: " + tableName + " " + dbHelper);
        if (tableName != null & dbHelper != null) {
            inputLayout.setVisibility(View.GONE);
            graphLayout.setVisibility(View.VISIBLE);
            serverLayout.setVisibility(View.VISIBLE);
            startService(new Intent(this, AccelerometerService.class));
        }
    }

    private class UploadDatabase extends AsyncTask<Void, Void, String> {

        Context context = MainActivity.this;
        File db = context.getDatabasePath(DatabaseHelper.DATABASE_NAME);
        String fileName = db.getPath();
        InputStream input = null;
        DataOutputStream output = null;
        HttpsURLConnection connection = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";
        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        String response = null;

        @Override
        protected String doInBackground(Void... params) {

            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    // Not implemented
                }

                @Override
                public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
                    // Not implemented
                }
            }};

            try {
                SSLContext sc = SSLContext.getInstance("TLS");

                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            try {
                URL url = new URL(sURLUpload);
                connection = (HttpsURLConnection) url.openConnection();
                connection.setDoInput(true); // Allow Inputs
                connection.setDoOutput(true); // Allow Outputs
                connection.setUseCaches(false); // Don't use a Cached Copy
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file", fileName);

//                // this will be useful to display download percentage
//                // might be -1: server did not report the length

                input = new FileInputStream(db);
                output = new DataOutputStream(connection.getOutputStream());

                output.writeBytes(twoHyphens + boundary + lineEnd);
                output.writeBytes("Content-Disposition: form-data; name=uploaded_file;" + "filename="
                        + fileName + " " + lineEnd);
                Log.i("UploadDB", "Upload Filename: " + fileName);
                output.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = input.available();

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = input.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    output.write(buffer, 0, bufferSize);
                    bytesAvailable = input.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = input.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                output.writeBytes(lineEnd);
                output.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                //expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                response = connection.getResponseMessage();
                Log.i("UploadDb", "response: " + response);

                //close the streams //
                input.close();
                output.flush();
                output.close();

            } catch (MalformedURLException ex) {


                ex.printStackTrace();


                Log.e("Upload file to server", "error: " + ex.getMessage(), ex);
            } catch (Exception e) {


                e.printStackTrace();


                Log.e("Upload Exception", "Exception : " + e.getMessage(), e);
            }

            return response;
        }

        @Override
        protected void onPostExecute(String result) {
            Log.i("UploadDb", "Upload message: " + result);
            Toast.makeText(context, "Upload Result: "+result, Toast.LENGTH_SHORT).show();
        }
    }

}
