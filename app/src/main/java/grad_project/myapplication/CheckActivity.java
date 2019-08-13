package grad_project.myapplication;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class CheckActivity extends AppCompatActivity {
    private SharedPreferences infoData;
    long l_nowTime, l_startTime, l_endTime, l_elapseTime;
    long l_delayTime = 0;
    String s_nowTime, s_startTime, s_endTime, s_elapseTime, s_id;
    boolean is_start, is_finish;
    TextView tv_nowTime, tv_startTime, tv_endTime, tv_elapsedTime;
    private String[] exhibitionState = new String[6];      // 전시관 오픈 여부(1 : open, 0 : close)
    LinearLayout[] ll_ex = new LinearLayout[6];
    ImageView[] iv_ex = new ImageView[6];
    boolean[] is_success = new boolean[6];
    Button bt_toMap, bt_finish;
    boolean connectState = true;

    TimerHandler timerhandler;
    private static int MESSAGE_TIMER_START = 100;
    private static int REFRESH_TIMER_START = 200;

    /***** php 통신 *****/
    private static final String BASE_PATH = "http://35.221.108.183/android/";

    public static final String GET_ISSTART = BASE_PATH + "get_isStart.php";              //시작여부(성공 1, 실패 0 반환)
    public static final String GET_EXHIBITION = BASE_PATH + "get_exhibition.php";          //각 전시관 별 개설 여부(JSON 형식) - ex) { "number": "1", "isOpen": "1" }
    public static final String SET_ISEND = BASE_PATH + "set_isEnd.php";    //전시 종료 값 보내기(성공 1, 실패 0 반환)
    public static final String GET_ISEND = BASE_PATH + "get_isEnd.php";    //전시 종료 여부 받기(종료됨 : 시간, 종료안됨 : 0)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar == null) {
            throw new NullPointerException("Null ActionBar");
        } else {
            actionBar.hide();
        }
        RelativeLayout bt_back_layout = findViewById(R.id.bt_back_layout);
        bt_back_layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        infoData = getSharedPreferences("infoData", MODE_PRIVATE);
        loadData();

        tv_nowTime = findViewById(R.id.tv_nowTime);
        tv_startTime = findViewById(R.id.tv_startTime);
        tv_endTime = findViewById(R.id.tv_endTime);
        tv_elapsedTime = findViewById(R.id.tv_elapsedTime);
        ll_ex[0] = findViewById(R.id.layout_ex_1);
        ll_ex[1] = findViewById(R.id.layout_ex_2);
        ll_ex[2] = findViewById(R.id.layout_ex_3);
        ll_ex[3] = findViewById(R.id.layout_ex_4);
        ll_ex[4] = findViewById(R.id.layout_ex_5);
        ll_ex[5] = findViewById(R.id.layout_ex_6);
        iv_ex[0] = findViewById(R.id.iv_ex_1);
        iv_ex[1] = findViewById(R.id.iv_ex_2);
        iv_ex[2] = findViewById(R.id.iv_ex_3);
        iv_ex[3] = findViewById(R.id.iv_ex_4);
        iv_ex[4] = findViewById(R.id.iv_ex_5);
        iv_ex[5] = findViewById(R.id.iv_ex_6);
        bt_toMap = findViewById(R.id.bt_toMap);
        bt_finish = findViewById(R.id.bt_finish); 

        l_startTime = getStartTime();
        getTimeRefresh();
        getTimeData();
        getExhibitionData();

        timerhandler = new TimerHandler();
    }

    @Override
    protected void onResume() {
        super.onResume();

        loadData();

        String endResult = isAlreadyEnd();

        if (endResult.equals("0")) {
            if (connectState) {
                if (is_start) {
                    timerhandler.sendEmptyMessage(MESSAGE_TIMER_START);
                    for (int i = 0; i < 6; i++) {
                        if (exhibitionState[i].equals("0")) {
                            iv_ex[i].setImageResource(R.drawable.closed);
                            ll_ex[i].setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(getApplicationContext(), "개방되지 않은 전시관입니다.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            if (is_success[i]) {
                                iv_ex[i].setImageResource(R.drawable.complete);
                                ll_ex[i].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(getApplicationContext(), "관람 완료된 전시관입니다.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                iv_ex[i].setImageResource(R.drawable.progress);
                                ll_ex[i].setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        Toast.makeText(getApplicationContext(), "관람하지 않은 전시관입니다.", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                    }
                } else {
                    for (int i = 0; i < 6; i++) {
                        ll_ex[i].setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Toast.makeText(getApplicationContext(), "관람 시작 전입니다.", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                    timerhandler.sendEmptyMessage(REFRESH_TIMER_START);
                }

                if (getFinish()) {
                    bt_finish.setEnabled(true);
                } else {
                    bt_finish.setEnabled(false);
                }
            } else {
                Toast.makeText(getApplicationContext(), "서버와의 통신에 실패하였습니다.", Toast.LENGTH_SHORT).show();
                bt_finish.setEnabled(false);
            }
        } else {
            Toast.makeText(getApplicationContext(), "이미 관람 완료된 사용자입니다.", Toast.LENGTH_SHORT).show();
            for (int i = 0; i < 6; i++) {
                iv_ex[i].setImageResource(R.drawable.complete);
                ll_ex[i].setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getApplicationContext(), "관람 완료된 전시관입니다.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            bt_finish.setEnabled(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        timerhandler.removeMessages(MESSAGE_TIMER_START);
        timerhandler.removeMessages(REFRESH_TIMER_START);
    }

    // 설문조사 요청
    public void surveyDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("설문조사에 참여해주세요!");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.i815.or.kr/"));
                        startActivity(intent);
                        finish();
                    }
                });
        builder.setCancelable(false);
        builder.show();
    }


    public void onBack(View v) {
        if (v == findViewById(R.id.bt_back)) {
            finish();
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.bt_toMap :
                if (is_start) {
                    Intent intent = new Intent(CheckActivity.this, NormalActivity.class);
                    intent.putExtra("Time", l_startTime);
                    startActivity(intent);
                } else {
                    Intent intent = new Intent(CheckActivity.this, HelpActivity.class);
                    startActivity(intent);
                }
//                finish();
                break;
            case R.id.bt_finish :
                if (getFinish()) {
                    FinishTask finishTask = new FinishTask(this);
                    try {
                        String result = finishTask.execute(SET_ISEND, s_id).get();
                        if (result.equals("0")) {
                            Toast.makeText(getApplicationContext(), "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                            connectState = true;
                            break;
                        }
                        else if (result.equals("1")) {
                            Toast.makeText(getApplicationContext(), "관람 종료 확인이 되었습니다.", Toast.LENGTH_SHORT).show();
                            connectState = true;
                            surveyDialog();
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        connectState = false;
                    }
                } else {
                    Toast.makeText(getApplicationContext(), "아직 관람이 끝나지 않았습니다.", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public String isAlreadyEnd() {
        FinishTask finishTask = new FinishTask(this);
        try {
            return finishTask.execute(GET_ISEND, s_id).get();
        } catch (Exception e) {
            e.printStackTrace();
            connectState = false;
        }
        return "-1";
    }

    public void getTimeData() {
        if (!is_start) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_startTime.setText("관람 시작 전입니다.");
                    tv_endTime.setText("");
                    tv_elapsedTime.setText("");
                }
            });
        } else {
            Date startDate = new Date(l_startTime);
            SimpleDateFormat sdfStart = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
            s_startTime = sdfStart.format(startDate);
            Log.d("START TIME", s_startTime);

            l_endTime = l_startTime + 7200000 + l_delayTime;
            Date endDate = new Date(l_endTime);
            SimpleDateFormat sdfEnd = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
            s_endTime = sdfEnd.format(endDate);
            Log.d("END TIME", s_endTime);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_startTime.setText(s_startTime);
                    tv_endTime.setText(s_endTime);
                }
            });
        }

    }

    public void getTimeRefresh() {
        getNowTime();

        if (is_start) {
            l_elapseTime = l_nowTime - l_startTime - 32400000;
            Date ElapseDate = new Date(l_elapseTime);
            SimpleDateFormat sdfElapse = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
            s_elapseTime = sdfElapse.format(ElapseDate);
            Log.d("ELAPSE TIME", s_elapseTime);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_nowTime.setText(s_nowTime);
                    tv_elapsedTime.setText(s_elapseTime);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tv_nowTime.setText(s_nowTime);
                    tv_elapsedTime.setText("");
                }
            });
        }
    }

    public void getNowTime() {
        l_nowTime = System.currentTimeMillis();
        Date nowDate = new Date(l_nowTime);
        SimpleDateFormat sdfNow = new SimpleDateFormat("HH:mm:ss", Locale.KOREA);
        s_nowTime = sdfNow.format(nowDate);
        Log.d("CURRENT TIME", s_nowTime);
    }
    // 3초 단위로 시작 여부 서버에서 받아옴
    private class TimerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            if  (msg.what == MESSAGE_TIMER_START) {
                getTimeRefresh();
                this.sendEmptyMessageDelayed(MESSAGE_TIMER_START, 1000);
            }
            else if (msg.what == REFRESH_TIMER_START) {
                GetIsStartTask startTask = new GetIsStartTask(CheckActivity.this);
                try {
                    String result = startTask.execute(GET_ISSTART, s_id).get();
                    is_start = !result.equals("0");
                    getTimeRefresh();
                    if (is_start) {
                        timerhandler.removeMessages(REFRESH_TIMER_START);
                        onPause();
                        l_startTime = getStartTime();
                        getTimeData();
                        onResume();
                        Log.d("REFRESH","RUNNING");
                    } else {
                        this.sendEmptyMessageDelayed(REFRESH_TIMER_START, 1000);
                    }
                    Log.d("ISSTART", Boolean.toString(is_start));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean getFinish() {
        boolean state = false;

        if (l_endTime <= l_nowTime) {
            state = true;
        }
        for (int i = 0; i < 6; i++) {
            if (exhibitionState[i].equals("1")) {
                if (!is_success[i]) {
                    state = false;
                    break;
                }
            }
        }

        return state;
    }

    public void loadData() {
        s_id = infoData.getString("ID", "");
        for (int i = 1; i < 7; i++) {
            is_success[i-1] = infoData.getBoolean("IS_CHECK_" + i, false);
        }
    }

    /* DB-서버 통신 파트 */
    // 관람 시작이 되었는지 여부 받아오는 메소드
    public long getStartTime() {
        // 관람 시작 여부
        GetIsStartTask startTask = new GetIsStartTask(this);
        try {
            String result = startTask.execute(GET_ISSTART, s_id).get();
            is_start = !result.equals("0");
            if (is_start) {
                SimpleDateFormat sdfStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.KOREA);
                long time = sdfStart.parse(result).getTime();
                Log.d("ISSTART", Long.toString(time));
                connectState = true;
                return time;
            }
        } catch (Exception e) {
            e.printStackTrace();
            connectState = false;
        }
        return -1;
    }
    // DB 전시관 데이터 받아오기
    public void getExhibitionData() {
        // 전시관 오픈 여부
        GetExhibitionTask task = new GetExhibitionTask(this);
        try {
            String result = task.execute(GET_EXHIBITION).get();
            Log.d("Exhibition", result);
            JSONObject jResult = new JSONObject(result);
            JSONArray jArray = jResult.getJSONArray("result");
            Log.d("ARRAY LENGTH", Integer.toString(jArray.length()));
            for (int i = 0; i < jArray.length(); i++) {
                JSONObject jObject = jArray.getJSONObject(i);
                exhibitionState[i] = jObject.getString("isOpen");
                Log.d("EXHIBITION", i + " : " + exhibitionState[i]);
                connectState = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            connectState = false;
        }
    }
    /***** 서버 통신 *****/
    // 관람 시작 여부 받아오는 부분
    public static class GetIsStartTask extends AsyncTask<String, Void, String> {
        private WeakReference<CheckActivity> activityReference;
        ProgressDialog progressDialog;

        GetIsStartTask(CheckActivity context) {
            activityReference = new WeakReference<>(context);
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(activityReference.get(),
                    "Please Wait", null, true, true);
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            /*출력값*/
        }
        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String id = params[1];
            String postParameters = "&id=" + id;
            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();
                int responseStatusCode = httpURLConnection.getResponseCode();
                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
    }

    // 전시관 오픈 여부 받아오는 부분
    public static class GetExhibitionTask extends AsyncTask<String, Void, String> {
        private WeakReference<CheckActivity> activityReference;
        ProgressDialog progressDialog;

        GetExhibitionTask(CheckActivity context) {
            activityReference = new WeakReference<>(context);
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(activityReference.get(),
                    "Please Wait", null, true, true);
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
            /*출력값*/
        }
        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();
                int responseStatusCode = httpURLConnection.getResponseCode();
                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
    }

    // 관람 종료시 데이터 전송, 관람 종료 여부 정보 받아오기
    public static class FinishTask extends AsyncTask<String, Void, String> {
        private WeakReference<CheckActivity> activityReference;
        ProgressDialog progressDialog;

        FinishTask(CheckActivity context) {
            activityReference = new WeakReference<>(context);
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = ProgressDialog.show(activityReference.get(),
                    "Please Wait", null, true, true);
        }
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            progressDialog.dismiss();
        }
        @Override
        protected String doInBackground(String... params) {
            String serverURL = params[0];
            String id = params[1];
            String postParameters = "&id=" + id;
            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();
                int responseStatusCode = httpURLConnection.getResponseCode();
                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder sb = new StringBuilder();
                String line;
                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString();
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
    }
}
