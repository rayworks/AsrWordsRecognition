package com.rayworks.asrwordsrecognition;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.rayworks.asrwordsrecognition.view.ButtonRecorder;
import com.rayworks.asrwordsrecognition.view.MicrophoneVolumeView;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class WordsRecognitionActivity extends AppCompatActivity implements RecognitionListener {

    public static final int TIMEOUT = 10000;
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;

    /* Named searches allow to quickly reconfigure the decoder */
    private static final String KWS_SEARCH = "wakeup";
    private static final String NGRAM_SEARCH = "ngram_search";

    private SpeechRecognizer recognizer;

    private ButtonRecorder recordBtn;
    private MicrophoneVolumeView microphoneView;
    private TextView resultView;

    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler();
    private ProgressBar loadingView;
    private RelativeLayout asrParent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        microphoneView = (MicrophoneVolumeView) findViewById(R.id.record_volume);

        recordBtn = (ButtonRecorder) findViewById(R.id.recorder_button);
        recordBtn.setRecordActionListener(
                new ButtonRecorder.RecordActionListener() {
                    @Override
                    public boolean preparedForRecording() {
                        return true;
                    }

                    @Override
                    public void onRecordStarted() {
                        onRecordStart();
                    }

                    @Override
                    public void onRecordComplete() {
                        onRecordDone();
                    }
                });

        resultView = (TextView) findViewById(R.id.result);

        loadingView = (ProgressBar)findViewById(R.id.loading_view);
        asrParent = (RelativeLayout)findViewById(R.id.asr_parent);
        loadingView.setVisibility(View.VISIBLE);
        asrParent.setVisibility(View.GONE);

        // Check if user has given permission to record audio
        final Context context = getApplicationContext();
        int permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        int writtenCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permissionCheck == PackageManager.PERMISSION_DENIED || writtenCheck == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{
                            Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            runRecognizerSetup();
        }
    }

    private void onRecordStart() {
        microphoneView.setVisibility(View.VISIBLE);

        recognizer.startListening(NGRAM_SEARCH, TIMEOUT);
    }

    private void onRecordDone() {
        microphoneView.setVisibility(View.INVISIBLE);
        microphoneView.setProportion(0);

        stopRecognition();
    }

    private void runRecognizerSetup() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                showMsg("Preparing the recognizer...");

                try {
                    Assets assets = new Assets(WordsRecognitionActivity.this);
                    File assetDir = assets.syncAssets();
                    setupRecognizer(assetDir);

                    showMsg("Recognizer is ready");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loadingView.setVisibility(View.GONE);
                            asrParent.setVisibility(View.VISIBLE);
                        }
                    });

                } catch (IOException e) {
                    e.printStackTrace();

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            resultView.setText("Failed to setup recognizer!");
                        }
                    });
                }
            }
        });
    }

    private void showMsg(final String msg) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(WordsRecognitionActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupRecognizer(File assetsDir) throws IOException {
        // The recognizer can be configured to perform multiple searches
        // of different kind and switch between them

        recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(new File(assetsDir, "en-us-ptm"))
                .setDictionary(new File(assetsDir, "cmudict-en-us.dict"))

                .setRawLogDir(assetsDir) // To disable logging of raw audio comment out this call (takes a lot of space on the device)
                .setKeywordThreshold(1e-45f) // Threshold to tune for keyphrase to balance between false alarms and misses
                .setBoolean("-allphone_ci", true)  // Use context-independent phonetic search, context-dependent is too slow for mobile


                .getRecognizer();
        recognizer.addListener(this);

        // Create keyword-activation search with the specified language model.
        recognizer.addNgramSearch(NGRAM_SEARCH, new File(assetsDir, "6805.lm"));

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                runRecognizerSetup();
            } else {
                finish();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (recognizer != null) {
            recognizer.cancel();
            recognizer.shutdown();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBeginningOfSpeech() {

    }

    @Override
    public void onEndOfSpeech() {
        //stopRecognition();
    }

    /**
     * We stop recognizer here to get a final result
     */
    private void stopRecognition() {
        recognizer.stop();
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {

    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        if (hypothesis != null) {
            int score = hypothesis.getBestScore();
            String text = "<<< result :" + hypothesis.getHypstr() + " score : " + score;
            resultView.setText(text);
        } else {
            resultView.setText("<<< Sentence UnRecognized.");
        }
    }

    @Override
    public void onError(Exception e) {
        resultView.setText(e.getMessage());
    }

    @Override
    public void onTimeout() {
        showMsg("<<< timed out!!");
        stopRecognition();
    }
}
