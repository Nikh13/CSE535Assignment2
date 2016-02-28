package cse535.mobilecomputing.assignment2;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import db.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private String tableName = null;
    private RelativeLayout inputLayout;
    private LinearLayout graphLayout;
    private EditText nameEditText;
    private EditText ageEditText;
    private EditText IDEditText;
    private RadioButton maleRadio;
    private RadioButton femaleRadio;
    RadioGroup sexRadioGroup;
    private Button stopButton;
    private Button runButton;
    private Button submitButton;
    private static DatabaseHelper dbHelper;
    private GraphView xGraph;
//    private GraphView yGraph;
//    private GraphView zGraph;

    List<float[]> valueList;
    List<float[]> emptyValueList;

    private float[] xValues = new float[10];
    private float[] yValues = new float[10];
    private float[] zValues = new float[10];
    private float[] tsValues = new float[10];

    TimerTask timerTask;
    Timer timer;
    boolean running = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        inputLayout = (RelativeLayout) findViewById(R.id.input_layout);
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
        graphLayout.setVisibility(View.GONE);
        inputLayout.setVisibility(View.VISIBLE);

        emptyValueList = new ArrayList<float[]>();
        emptyValueList.add(new float[0]);

        xGraph = new GraphView(getApplicationContext(), emptyValueList, "X axis", null, null, GraphView.LINE);
//        yGraph = new GraphView(getApplicationContext(), new float[0], "Y axis", null, null, GraphView.LINE);
//        zGraph = new GraphView(getApplicationContext(), new float[0], "Z axis", null, null, GraphView.LINE);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 9f);
        xGraph.setLayoutParams(params);
//        yGraph.setLayoutParams(params);
//        zGraph.setLayoutParams(params);
        graphLayout.addView(xGraph);
//        graphLayout.addView(yGraph);
//        graphLayout.addView(zGraph);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String name = nameEditText.getText().toString();
                String ID = IDEditText.getText().toString();
                String age = ageEditText.getText().toString();
                String sex = "";
                if(sexRadioGroup.getCheckedRadioButtonId()>-1){
                    switch(sexRadioGroup.getCheckedRadioButtonId()){
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
                    editor.putString("table_name",tableName);
                    editor.commit();
                    dbHelper = DatabaseHelper.getInstance(getApplicationContext());
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
        xGraph.setValueList(valueList);
//        yGraph.setValueList(yValues);
//        zGraph.setValueList(zValues);
        xGraph.invalidate();
//        yGraph.invalidate();
//        zGraph.invalidate();
    }

    private void clearGraph() {
        xGraph.setValueList(emptyValueList);
//        yGraph.setValueList(new float[0]);
//        zGraph.setValueList(new float[0]);
        xGraph.invalidate();
//        yGraph.invalidate();
//        zGraph.invalidate();
    }

    private void extractMostRecentAxisValues() {
        List<Record> recordList = getMostRecentRecords();
        Log.i("MainActivity", "list: "+recordList);
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
        Log.i("MainActivity", "db: "+dbHelper+" list: "+recordList);
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
            startTimer();
            startService(new Intent(this, AccelerometerService.class));
        }
    }

}
