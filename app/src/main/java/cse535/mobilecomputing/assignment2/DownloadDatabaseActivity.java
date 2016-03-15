package cse535.mobilecomputing.assignment2;

import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import db.DownloadDatabaseHelper;

public class DownloadDatabaseActivity extends AppCompatActivity {


    GraphView graphView;
    private Button downloadDatabaseButton;
    private static DownloadDatabaseHelper dbHelper;
    private DownloadDatabase downloadDatabaseTask;

    List<float[]> valueList;
    List<float[]> emptyValueList;

    private float[] xValues = new float[10];
    private float[] yValues = new float[10];
    private float[] zValues = new float[10];


    private static final String sURLBase = "https://impact.asu.edu/Appenstance/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        downloadDatabaseButton = (Button)findViewById(R.id.download_btn);

        emptyValueList = new ArrayList<float[]>();
        emptyValueList.add(new float[0]);
        graphView = new GraphView(getApplicationContext(), emptyValueList, "Downloaded Data", null, null, GraphView.LINE);
        graphView.setBackgroundColor(getResources().getColor(android.R.color.black));
        graphView.setLabels(10,0);
        graphView.invalidate();

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 9f);
        graphView.setLayoutParams(params);
        LinearLayout rootView = (LinearLayout)findViewById(R.id.root);
        rootView.addView(graphView);
        downloadDatabaseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadDatabaseTask = new DownloadDatabase();
                downloadDatabaseTask.execute();
            }
        });
    }

    private void refreshGraphValues() {
        graphView.setValueList(valueList);
        graphView.setLabels(10,4);
        graphView.invalidate();
    }

    private void clearGraph() {
        graphView.setValueList(emptyValueList);
        graphView.invalidate();
    }

    private void extractMostRecentAxisValues() {
        List<Record> recordList = getMostRecentRecords();
        Log.i("MainActivity", "list: " + recordList);
        if (recordList != null && recordList.size() == 10) {
            Log.i("DownloadActivity", "New Set");
            int i = 0;
            for (Record r : recordList) {
                Log.i("DownloadActivity", "Record values: " + r.timestamp + " " + r.xVal + " " + r.yVal + " " + r.zVal);
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
        Log.i("DownloadActivity", "db: " + dbHelper + " list: " + recordList);
        return recordList;
    }

    private class DownloadDatabase extends AsyncTask<Void, Void, Boolean> {

        String response = null;
        String code = null;

        @Override
        protected Boolean doInBackground(Void... params) {
            InputStream input = null;
            OutputStream output = null;
            HttpsURLConnection connection = null;
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
                String tableName = MainActivity.getStoredTableName(DownloadDatabaseActivity.this);
                URL url = new URL(sURLBase + DatabaseHelper.DATABASE_NAME);
                connection = (HttpsURLConnection) url.openConnection();

                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
                    response =  "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                    return true;
                }
                response = connection.getResponseMessage();

                input = connection.getInputStream();
                output = new FileOutputStream(DownloadDatabaseHelper.DATABASE_PATH + DatabaseHelper.DATABASE_NAME);
                byte data[] = new byte[4096];
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return false;
                    }
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                e.printStackTrace();
                code = "Exception";
                return true;
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean err) {
            Log.i("DownloadDb", "Download message: " + response + code);
            Toast.makeText(DownloadDatabaseActivity.this, "Download Result: "+response, Toast.LENGTH_SHORT).show();

            if(!err){
                Log.i("DownloadDb", "No error");
                dbHelper = DownloadDatabaseHelper.getInstance(DownloadDatabaseActivity.this);
                extractMostRecentAxisValues();
                refreshGraphValues();
            }
        }
    }
}
