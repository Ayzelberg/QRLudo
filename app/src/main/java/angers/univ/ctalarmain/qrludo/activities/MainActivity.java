package angers.univ.ctalarmain.qrludo.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import angers.univ.ctalarmain.qrludo.R;
import angers.univ.ctalarmain.qrludo.utils.BasicImageDownloader;
import angers.univ.ctalarmain.qrludo.utils.OnSwipeTouchListener;
import angers.univ.ctalarmain.qrludo.utils.QDCResponse;
import angers.univ.ctalarmain.qrludo.utils.QuestionDelayCounter;

import static java.lang.String.valueOf;

/**
 * @author Corentin Talarmain
 * MainActivity is the main activity of the application, this is where the user will be able to detect QRCodes and hear the question / answer.
 */
public class MainActivity extends AppCompatActivity implements SensorEventListener, QDCResponse {
    /**
     * The name of the file containing the user's settings.
     */
    public static final String PREFS_NAME = "MyPrefsFile";

    /**
     * The default language that will be applied to the Text To Speech engine.
     */
    public static final Locale LOCALE_DEFAULT = Locale.FRANCE;

    /**
     * The default speed applied to the Text To Speech Engine.
     */
    public static final float SPEEDSPEECH_DEFAULT = (float)1.2;

    /**
     * The default mode applied to the Text To Speech engine.
     */
    public static final int DEFAULT_MODE = TextToSpeech.QUEUE_ADD;

    /**
     * The default delay for question to get resetted.
     */
    public static final int DEFAULT_QUESTION_RESET_TIME = 60;

    /**
     * The integer corresponding to the request for phone vibration authorization
     */
    private static final int VIBRATE_REQUEST = 80;

    /**
     * The integer corresping to the code identifying the option intent used to launch the option activity.
     * @see OptionActivity
     */
    static final int OPTION_REQUEST = 90;  // The request code

    /**
     * The integer corresponding to the request for the camera authorization.
     */
    private static final int CAMERA_REQUEST = 10;

    /**
     * The integer corresponding to the request for the internet access authorization
     */
    private static final int INTERNET_REQUEST = 20;

    private static final int MULTIPLE_QUESTIONS_DETECTED = 100;

    public static final int DEFAULT_MULTIPLE_DETECTION_TIME = 4;


    /*
     * -----------------------------------------CAMERA STATES -----------------------------------------
     */

    /**
     *  The integer corresponding to the start state of the application.
     *  This state is corresponding to the camera being inactive.
     */
    private final int START_STATE = 30;

    /**
     * The integer corresponding to the detecting state of the application
     * This state is corresponing to the camera being active.
     */
    private final int DETECTING_STATE = 40;

    /*
     * -----------------------------------------QUESTION STATES -----------------------------------------
     */

    /**
     * The integer corresponding to the no question printed state of the application
     * This state corresponds to the state where no question is printed.
     */
    private final int NO_QUESTION_PRINTED_STATE = 50;

    /**
     * The integer corresponding to the question printed state of the application
     * This state corresponds to the state where a question is printed
     */
    private final int QUESTION_PRINTED_STATE = 60;


    /**
     * The integer corresponding to the reponse printed state of the application
     * This state is planned for a futur use of QRCodes
     */
    private final int REPONSE_PRINTED_STATE = 70;

    /**
     * The integer corresponding to the current camera state
     */
    private int cameraState;

    /**
     * The integer corresponding to the current question state
     */
    private int questionState;

    /**
     * The integer corresponding to the speed of the text to speech engine
     */
    private float speechSpeed;

    /**
     * The current language of the text to speech engine.
     */
    private Locale ttslanguage;

    /**
     * The current mode of the text to speech engine.
     */
    private int speechMode;

    /**
     * The current delay between reset of the questions
     */

    private int question_reset_time;

    /*
    *--------------------------------------Layouts--------------------------------------
     */

    /**
     * The main layout where all happens
     */
    private RelativeLayout mainLayout;

    /**
     * The layout used to display a picture
     * Not used at the moment, for a later purpose
     */
    private RelativeLayout imageLayout;

    /**
     * The layout used to display the question / answer
     */
    private LinearLayout contentLayout;

    /**
     * The text view containing the current question / answer
     */
    private TextView text_space;

    /**
     * The image view containing the current picture to show
     */
    private ImageView image_space;

    /**
     * The view containing the camera preview and detector
     */
    private SurfaceView cameraView;

    /**
     * The barcode detector
     */
    private BarcodeDetector detector;

    /**
     * The object used to get the camera
     */
    private CameraSource cameraSource;

    /**
     * The text to speech engine object
     */
    private TextToSpeech ttobj;

    /**
     * The progress bar for the text to speech loading.
     */
    private ProgressBar ttsprogress;

    /**
     * The text view indicating the text to speech loading
     */
    private TextView text_progress;

    /**
     * The boolean indicating that the sdk version of the phone is greater than marshamallow (API 23)
     */
    private boolean marshmallow;

    /**
     * The boolean indicating if the camera permission has been granted
     */
    private boolean camera;

    /**
     * The boolean indicating if the internet permission has been granted
     */
    private boolean internet;

    /**
     * The processor of the detector, where the events from the detector are handled
     */
    private Detector.Processor<Barcode> detector_processor;

    /**
     * The boolean indicating if the sdk version of the phone is greater than lollipop
     */
    private boolean lollipop;

    /**
     * The boolean indicating if the sdk version of the phone is greater than ice cream
     */
    private boolean ice_cream;

    /**
     * The boolean indicating if the text to speech engine is done initializing
     */
    private boolean ttsready;

    private boolean multiple_detecting;

    /**
     * This object is used to make the "bip" sound when a QRCode is detected
     */
    ToneGenerator toneGen;

    /**
     * This bundle is used to pass a list of languages available for the text to speech engine
     */
    private Bundle locals;

    /**
     * This string store the last code scanned, so the detected won't scan it again over and over
     */
    String lastBarcode;

    /**
     * The current question scanned
     */
    private String question;

    /**
     * The current answer scanned
     */
    private String reponse;

    private ArrayList<String> m_barcodes;

    private int m_nbrCodes;

    private int currQuest;

    /**
     * The asynchronous task used to countdown the time before the last question has to get resetted
     */
    private QuestionDelayCounter qdc;

    /**
     * The image url, for a later purpose with the presence of pictures
     */
    private String image_url;

    /**
     * The boolean indicating the presence of a picture in the code, for a later purpose
     */
    private boolean image;

    /**
     * The boolean indicating the presence of a sound in the code, for a later purpose
     */
    private boolean musique;


    /**
     * This object is used to manage the different sensors used
     */
    private SensorManager sensorManager;

    /**
     * The accelerometer sensor, used to manage the movements of the phone
     */
    private Sensor accelerometer;

    /**
     * The proximity sensor, used to manage the proximity of the phone from another object
     */
    private Sensor proximity;

    /**
     * The electro magnetic sensor, used to manage the electro magnetic field of the phone
     */
    private Sensor magnetic;

    private boolean hasAccelerometer;

    private boolean hasProximity;

    private boolean hasMagnetic;


    /**
     * The vector used to gather datas from the accelerometer sensor
     */
    private float[] acceleromterVector = new float[3];

    /**
     * The vector used to gather data from the magnetic sensor
     */
    private float[] magneticVector = new float[3];


    private float[] mResultMatric = new float[9];
    private float[] resultMatrix = new float[9];


    MultipleDetectionTimer mdt;

    /**
     * Float used for the proximity sensor
     */
    float p;

    /**
     * long used to know when the last sensor event occurred
     */
    private long lastUpdate = 0;

    private int lastInclination = -1;

    private int inclinationCounter = 0;
    /**
     * The boolean used to indicate if the phone vibrating permission has been granted
     */
    private boolean vibrate;

    /**
     * The object used to make the phone vibrate
     */
    private Vibrator vibrator;

    GoogleApiAvailability gga;

    private final int GOOGLE_SERVICES_REQUEST = 500;

    private int multiple_detection_time;


    /**
     * The OnCreate event of the main activity, called at the creation.
     * Initializes all the attributes and check for a bundle to restore a defined state.
     * Initializes also the listeners and check for the permissions.
     * @param savedInstanceState The bundle saving data if a state has to be saved
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        gga = GoogleApiAvailability.getInstance();
        int status = gga.isGooglePlayServicesAvailable(getApplicationContext());
        if(status == ConnectionResult.SUCCESS) {
            //alarm to go and install Google Play Services
        }else{

            Dialog errorDia = gga.getErrorDialog(this,status,GOOGLE_SERVICES_REQUEST);
            errorDia.show();
        }
        initializeAttributes();

        if(savedInstanceState != null) {
            if (savedInstanceState.containsKey("QUESTION"))
                question = savedInstanceState.getString("QUESTION");
            if (savedInstanceState.containsKey("REPONSE"))
                reponse = savedInstanceState.getString("REPONSE");
            musique = savedInstanceState.getBoolean("MUSIQUE");
            cameraState = savedInstanceState.getInt("STATE");
            switch (cameraState) {
                case DETECTING_STATE: {
                    startDetection();
                    break;
                }
            }
        }

        initializeListeners();


        checkPermissions();


    }

    /**
     * Method defined from the QDCResponse interface.
     * Called when the QuestionDelayCounter AsyncTask is over
     * @see QuestionDelayCounter
     * @see QDCResponse
     * @param output The output of the Async Task, true if the question has to be reset, false otherwise
     */
    @Override
    public void processFinish(Boolean output) {
        if(output){
           resetQuestion();
            lastBarcode = "";
        }
    }

    /**
     * The AsyncTask used to detect when the Text To Speech engine talk for the first time
     */
    private class detectionOnTTSInitialization extends AsyncTask<TextToSpeech,Void,Integer>{

        /**
         * Get out of a while waiting for the text to speech to speak, and then initiate the application
         * detector
         * @param params The Text To Speech engine on 0
         * @return An integer meaning it's ok
         */
        @Override
        protected Integer doInBackground(TextToSpeech... params) {
            while(!params[0].isSpeaking()){
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ttsprogress.setVisibility(View.GONE);
                    text_progress.setVisibility(View.GONE);
                }
            });
            ttsready = true;
            return 1;
        }
    }

    /**
     * The method setting up the text to speech engine.
     */
    private void SetUpTTS() {
        /*Intent checkIntent = new Intent();
        checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkIntent, MY_DATA_CHECK_CODE);*/

        //The text to speech
        ttobj = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {

                ttobj.setLanguage(ttslanguage);
                ttobj.setSpeechRate(speechSpeed);
                new detectionOnTTSInitialization().execute(ttobj);
                toSpeech("Application lancée.", TextToSpeech.QUEUE_FLUSH);
                fillLocals();

            }
        });


        //This OnUtteranceProgressListener is used for the synthetize
        ttobj.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                if (utteranceId.equals("synthetizeKey")) {
                    Log.d("UTTERANCE", "Start");
                }
            }

            @Override
            public void onDone(String utteranceId) {
                if (utteranceId.equals("synthetizeKey")) {
                    Log.d("UTTERANCE", "Done");
                    printQuestion();
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e("UTTERANCE", "Error while trying to synthesize sample text");
            }
        });
    }

    private void fillLocals() {

        Locale[] locales = Locale.getAvailableLocales();


            for (Locale l : locales) {
                try {
                    if (ttobj.isLanguageAvailable(l) >= TextToSpeech.LANG_COUNTRY_AVAILABLE) {
                        locals.putString(l.getCountry(), l.getLanguage());
                    }
                }
                catch(IllegalArgumentException e){
                }
            }

    }


    /**
     * OnPause event for the activity, unregister the sensors
     */
    @Override
    protected void onPause() {
        // unregister the sensor (désenregistrer le capteur)
        if(hasAccelerometer)
        sensorManager.unregisterListener(this, accelerometer);
        if(hasProximity)
        sensorManager.unregisterListener(this, proximity);
        super.onPause();
    }

    /**
     * The OnResume event for the activity
     * Register the sensors
     */
    @Override
    protected void onResume() {
        /* Ce qu'en dit Google&#160;dans le cas de l'accéléromètre :
         * «&#160; Ce n'est pas nécessaire d'avoir les évènements des capteurs à un rythme trop rapide.
         * En utilisant un rythme moins rapide (SENSOR_DELAY_UI), nous obtenons un filtre
         * automatique de bas-niveau qui "extrait" la gravité  de l'accélération.
         * Un autre bénéfice étant que l'on utilise moins d'énergie et de CPU.&#160;»
         */
        if(hasAccelerometer)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        if(hasProximity)
        sensorManager.registerListener(this, proximity,SensorManager.SENSOR_DELAY_UI);
        super.onResume();
    }

    /**
     * Initialization of the attributes.
     */
    private void initializeAttributes() {

        /*
        * --------------- LAYOUTS ---------------
         */

        ttsprogress = (ProgressBar) findViewById(R.id.tts_progress);

        text_progress = (TextView) findViewById(R.id.text_loading);

        mainLayout = (RelativeLayout) findViewById(R.id.main_layout);

        //Image layout
        imageLayout = (RelativeLayout) findViewById(R.id.image_layout);

        //Content Layout
        contentLayout = (LinearLayout) findViewById(R.id.content_layout);

        text_space = (TextView) findViewById(R.id.text_space);

        image_space = (ImageView) findViewById(R.id.image);

        cameraView = (SurfaceView) findViewById(R.id.camera_view);

        cameraView.setVisibility(View.INVISIBLE);

        ttsready = false;

        locals = new Bundle();

        vibrator = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

        speechSpeed = settings.getFloat("speechSpeed", SPEEDSPEECH_DEFAULT);

        ttslanguage = new Locale(settings.getString("speechLanguage", LOCALE_DEFAULT.getLanguage()),settings.getString("speechCountry", LOCALE_DEFAULT.getCountry()));

        speechMode = settings.getInt("speechMode", DEFAULT_MODE);

        question_reset_time = settings.getInt("resetTime",DEFAULT_QUESTION_RESET_TIME);

        multiple_detection_time = settings.getInt("MDTime",DEFAULT_MULTIPLE_DETECTION_TIME);

        SetUpTTS();

        PackageManager manager = getPackageManager();

        hasAccelerometer = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER);


        hasProximity = manager.hasSystemFeature(PackageManager.FEATURE_SENSOR_PROXIMITY);

        // Instancier le gestionnaire des capteurs, le SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        // Instancier l'accéléromètre
        if(hasAccelerometer)
         accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if(hasProximity)
         proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


        m_barcodes = new ArrayList<>();

        lastUpdate = System.currentTimeMillis();
        //Main Layout


        toneGen = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        cameraState = START_STATE;

        questionState = NO_QUESTION_PRINTED_STATE;


        lastBarcode = "";

        marshmallow = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;

        lollipop = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ;

        ice_cream = android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1;

        multiple_detecting = false;

        internet = false;

        camera = false;

        setUpDetector();
    }

    private void setUpDetector() {
        detector = new BarcodeDetector.Builder(getApplicationContext())
                .setBarcodeFormats(Barcode.DATA_MATRIX | Barcode.QR_CODE)
                .build();

        if (!detector.isOperational()) {
            Log.e("DETECTOR","Could not set up detector.");
            return;
        }

        cameraSource = new CameraSource
                .Builder(this, detector)
                .setRequestedPreviewSize(640, 480)
                .setAutoFocusEnabled(true)
                .build();



        cameraView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {

                    cameraSource.start(cameraView.getHolder());
                    cameraState = DETECTING_STATE;

                    Log.d("CAMERA_START", "Camera started");
                } catch(SecurityException se)
                {
                    Log.e("CAMERA SECURITY", se.getMessage());
                }
                catch (Exception e)
                {
                    Log.e("CAMERA SOURCE", e.getMessage());
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                try {

                    if(cameraState == DETECTING_STATE) {

                        cameraState = START_STATE;
                    }
                    cameraSource.stop();
                } catch (Exception e)
                {
                    Log.e("CAMERA SOURCE", e.getMessage());
                }
            }
        });


        detector_processor = new Detector.Processor<Barcode>() {
            int lastBarcodesSize;

            @Override
            public void release() {
            }

            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections) {
                final SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() != 0) {
                        for(int i = 0 ; i < barcodes.size(); i ++) {
                            if (!m_barcodes.contains(barcodes.valueAt(i).rawValue)) {
                                m_barcodes.add(barcodes.valueAt(i).rawValue);
                                m_nbrCodes++;
                                if (!multiple_detecting) {
                                    multiple_detecting = true;
                                    mdt = new MultipleDetectionTimer();
                                    mdt.execute(multiple_detection_time * 1000);
                                    parseJSON(barcodes.valueAt(0).rawValue);
                                    currQuest = 0;
                                    toneGen.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 150);
                                    questionState = QUESTION_PRINTED_STATE;
                                } else if (questionState != MULTIPLE_QUESTIONS_DETECTED) {
                                    questionState = MULTIPLE_QUESTIONS_DETECTED;
                                }
                                if (multiple_detecting) {
                                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150);
                                }


                            }
                        }
                    lastBarcodesSize = barcodes.size();
                    //state = CODE_DETECTED_STATE;
                    //stopDetection();
                   /* for(int i = 0 ; i < barcodes.size(); i++)
                    {
                        toneGen.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                       // parseJSON(barcodes.valueAt(i).rawValue);

                    }*/
                   /* Log.d("COUCOU","--------------------STOP--------------");
                    boolean image = false, musique = false;
                    for (int i = 0; i < barcodes.size(); i++) {
                        JSONObject object;
                        try {
                            object = new JSONObject(barcodes.valueAt(i).rawValue);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                       /switch (barcode.valueFormat) {
                            case Barcode.URL:
                                Log.d("COUCOU","--------------------URL--------------");
                                if (internet) {
                                    URLConnection connection = null;
                                    boolean image = false;
                                    try {
                                        connection = new URL(barcode.rawValue).openConnection();
                                        String contentType = connection.getHeaderField("Content-Type");
                                        image = contentType.startsWith("image/");
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    if(image)
                                    {
                                        Intent intent = new Intent(getApplicationContext(), ImageDialog.class);
                                        intent.putExtra("EXTRA_HTML", barcode.rawValue);
                                        startActivity(intent);
                                    }
                                } else {
                                    printToast("Can't open link, internet use not permitted.");
                                }
                                break;
                            case Barcode.TEXT:
                                Log.d("COUCOU","--------------------TEXT--------------");
                                toSpeech(barcodes.valueAt(i).rawValue);
                                break;
                        }


                    }*/


                }
            }
        };


        detector.setProcessor(detector_processor);
    }


    /**
     * The AsyncTask used to detect when the Text To Speech engine talk for the first time
     */
    private class MultipleDetectionTimer extends AsyncTask<Integer,Void,Boolean>{

        /**
         * Get out of a while waiting for the text to speech to speak, and then initiate the application
         * detector
         * @param params The Text To Speech engine on 0
         * @return An integer meaning it's ok
         */
        @Override
        protected Boolean doInBackground(Integer... params) {
                try {
                    Thread.sleep(params[0]);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(questionState == MULTIPLE_QUESTIONS_DETECTED) {
                    stopDetection();
                }else if(questionState == QUESTION_PRINTED_STATE){
                    m_barcodes.clear();
                    m_nbrCodes = 0;
                }
            multiple_detecting = false;
            return true;
        }
    }



    private void resetQuestion() {
        question = "";

        reponse = "";
        text_space.setVisibility(View.INVISIBLE);
        contentLayout.setVisibility(View.INVISIBLE);
        questionState = NO_QUESTION_PRINTED_STATE;
        m_barcodes.clear();
        m_nbrCodes = 0;
        currQuest = 0;
        multiple_detecting = false;

    }

    private void initializeListeners() {
    /*    mainLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @OverrideQuestion
            public boolean onLongClick(View v) {
                Log.d("LISTENER","COUCOU C'EST LE LONG CLICK LISTENER");
                switch(state)
                {
                    case START_STATE:
                    {
                        startDetection();
                        return true;
                    }
                    case DETECTING_STATE:
                    {
                        stopDetection();
                        return true;
                    }
                    case CODE_DETECTED_STATE:
                    {
                        printQuestion();
                        return true;
                    }
                    case QUESTION_PRINTED_STATE:
                        toSpeech(question,TextToSpeech.QUEUE_ADD);
                        return true;
                }
                return false;
            }
        });*/
        mainLayout.setOnTouchListener(new OnSwipeTouchListener(MainActivity.this) {
            public void onSwipeTop() {
                if(questionState == QUESTION_PRINTED_STATE || questionState == MULTIPLE_QUESTIONS_DETECTED) {
                    toSpeech(question, TextToSpeech.QUEUE_FLUSH);

                }
            }
            public void onSwipeRight() {
                /*if(questionState == REPONSE_PRINTED_STATE && question != "") {
                    printQuestion();
                }*/
                if(questionState == MULTIPLE_QUESTIONS_DETECTED && !multiple_detecting)
                {
                    if(currQuest > 0)
                    {
                        currQuest--;
                        question = m_barcodes.get(currQuest);
                        printQuestion();
                        if(currQuest == 0)
                        {
                            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SLS, 25);
                        }
                    }
                }
            }
            public void onSwipeLeft() {
                Log.d("state",valueOf(questionState == QUESTION_PRINTED_STATE));
                /*if(questionState == QUESTION_PRINTED_STATE && reponse != "") {
                    printReponse();
                }*/
                if(questionState == MULTIPLE_QUESTIONS_DETECTED)
                {
                    Log.d("ah","ah");
                    if(multiple_detecting )
                    {
                        multiple_detecting = false;
                        mdt.cancel(true);
                        stopDetection();
                    }
                    if(currQuest+1 < m_nbrCodes)
                    {
                        currQuest++;
                        question = m_barcodes.get(currQuest);
                        if(currQuest+1 == m_nbrCodes) {
                            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SLS, 25);
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            toneGen.startTone(ToneGenerator.TONE_CDMA_HIGH_PBX_SLS, 25);
                        }
                        printQuestion();
                    }
                    else if(currQuest+1 == m_nbrCodes)
                    {
                        questionState = NO_QUESTION_PRINTED_STATE;
                        m_barcodes.clear();
                        m_nbrCodes = 0;
                        currQuest = 0;
                        startDetection();
                    }
                }
            }
            public void onSwipeBottom() {

                    if(ttobj.isSpeaking())
                    {
                        ttobj.stop();
                    }

            }


        });

    }

    @TargetApi(Build.VERSION_CODES.M)
    private void checkPermissions() {
        if (marshmallow) {
            Log.d("PERMISSION_CHECK","---------Marshallow----------");

            // Here, thisActivity is the current activity
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION_CHECK","---------CheckPermission----------");
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    Log.d("PERMISSION_CHECK","---------Explanation----------");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    Log.d("PERMISSION_CHECK","---------NoExplanation----------");
                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_REQUEST);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else{
                camera = true;
            }
            /*if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.INTERNET)
                    != PackageManager.PERMISSION_GRANTED)
            {
                Log.d("PERMISSION_CHECK","---------CheckPermission----------");
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.INTERNET)) {
                    Log.d("PERMISSION_CHECK","---------Explanation----------");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    Log.d("PERMISSION_CHECK","---------NoExplanation----------");
                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.INTERNET},
                            INTERNET_REQUEST);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else
                internet = true;*/
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.VIBRATE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d("PERMISSION_CHECK","---------CheckPermission----------");
                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.VIBRATE)) {
                    Log.d("PERMISSION_CHECK","---------Explanation----------");
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {
                    Log.d("PERMISSION_CHECK","---------NoExplanation----------");
                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            CAMERA_REQUEST);

                    // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                    // app-defined int constant. The callback method gets the
                    // result of the request.
                }
            }else{
                vibrate = true;
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            saveState();
            Intent pickOptionIntent = new Intent(getApplicationContext(), OptionActivity.class);
            /*if(ice_cream && !lollipop) {
                pickOptionIntent.putExtra("DefaultsEnforced", ttobj.areDefaultsEnforced());
            }*/
            pickOptionIntent.putExtra("languages",locals);
            startActivityForResult(pickOptionIntent, OPTION_REQUEST);
            /*Intent intent = new Intent(getApplicationContext(), OptionActivity.class);
            startActivity(intent);*/
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == OPTION_REQUEST) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);

                float tmp = speechSpeed;
                speechSpeed = settings.getFloat("speechSpeed", SPEEDSPEECH_DEFAULT);

                ttslanguage = new Locale(settings.getString("speechLanguage",LOCALE_DEFAULT.getLanguage()),settings.getString("speechCountry",LOCALE_DEFAULT.getCountry()));
                int i = ttobj.setSpeechRate(speechSpeed);
                ttobj.setLanguage(ttslanguage);


                speechMode = settings.getInt("speechMode",DEFAULT_MODE);

                question_reset_time = settings.getInt("resetTime",DEFAULT_QUESTION_RESET_TIME);

                multiple_detection_time = settings.getInt("MDTime",DEFAULT_MULTIPLE_DETECTION_TIME);
                // Do something with the contact here (bigger example below)
            }
        }/*
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                // success, create the TTS instance

            } else {
                // missing data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
            }
        }*/
    }

    private void saveState() {

    }



    private void printQuestion() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {

                text_space.setText(question);
                /*if(image)
                {
                    Bitmap bmp = BitmapFactory.decodeFile(image_url);
                    image_space.setImageBitmap(bmp);
                    image_space.setVisibility(View.VISIBLE);
                }else
                {
                    image_space.setVisibility(View.INVISIBLE);
                }*/
                text_space.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.VISIBLE);
                toSpeech(question, speechMode);
                if(!(questionState == MULTIPLE_QUESTIONS_DETECTED))
                {
                    questionState = QUESTION_PRINTED_STATE;
                }

                if(question_reset_time > 0) {
                    if (qdc != null)
                        qdc.cancel(true);
                    qdc = new QuestionDelayCounter();
                    qdc.delegate = (QDCResponse) activity;
                    qdc.execute(question_reset_time);
                }
            }
        });
    }

    private void printReponse() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                text_space.setText(reponse);
                //image_space.setVisibility(View.INVISIBLE);
                text_space.setVisibility(View.VISIBLE);
                contentLayout.setVisibility(View.VISIBLE);
                toSpeech(reponse,TextToSpeech.QUEUE_FLUSH);
                questionState = REPONSE_PRINTED_STATE;
            }
        });

    }


    private void stopDetection() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                if(ttsready) {
                    cameraView.setVisibility(View.INVISIBLE);
                }else{
                    cameraState = START_STATE;
                }
            }
        });
    }

    private void startDetection() {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {

                if(ttsprogress.getVisibility() == View.VISIBLE){
                    ttsprogress.setVisibility(View.GONE);
                    text_progress.setVisibility(View.GONE);
                }
                if(ttsready) {
                    cameraView.setVisibility(View.VISIBLE);
                }else{
                    cameraState = DETECTING_STATE;
                }
            }
        });
    }

    private void parseJSON(String rawValue) {
        JSONObject object;
        try {
            object = new JSONObject(rawValue);
            question = object.getString("question");
            reponse = object.getString("reponse");
            //image = object.getBoolean("picture");
            musique = object.getBoolean("music");

            /*if(image){
                JSONObject image_json = object.getJSONObject("Picture");
                String image_name = image_json.getString("name");
                Uri uri = Uri.parse(image_json.getString("url"));
                String type = uri.getScheme();
                image_url = Environment.getExternalStorageDirectory().getAbsolutePath() +
                        File.separator + "QRCodeForGames" + File.separator +  image_name + ".jpeg";
                if(!(new File(image_url).exists())) {
                    if (type.equals("http") || type.equals("https")) {
                        if (internet) {
                            String url = uri.toString();
                            URLConnection connection = null;
                            boolean html_image = false;
                            try {
                                connection = new URL(url).openConnection();
                                String contentType = connection.getHeaderField("Content-Type");
                                html_image = contentType.startsWith("image/");
                                Log.d("CONTENT", contentType);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (html_image) {
                                if(write) {
                                    downloadPicture(image_name, url);
                                }else{
                                    toSpeech("L'écriture sur le stockage externe n'a pas été accordée.",TextToSpeech.QUEUE_ADD);
                                }
                            }else{
                                toSpeech("L'URL internet n'est pas une image.",TextToSpeech.QUEUE_ADD);
                            }
                        } else {
                            toSpeech("Erreur : Une ressource internet est demandée, mais non permis.", TextToSpeech.QUEUE_ADD);
                        }

                    } else if(type.equals("file") ){
                        File f = new File(uri.getSchemeSpecificPart());
                        if(f.exists()){
                            image_url = uri.getSchemeSpecificPart();
                        }else
                        {
                            toSpeech("L'url indiquée pour le fichier est erronnée.",TextToSpeech.QUEUE_ADD);
                        }
                    }
                }else{
                    printQuestion();
                }

            }*/
            if(musique)
            {
                toSpeech("Il y a une musique.",TextToSpeech.QUEUE_ADD);
            }

        } catch (JSONException e) {
            question = rawValue;

            //image = false;
            musique = false;
            printQuestion();
        }



    }

    private void downloadPicture(String image_name, String url) {
        final String i_name = image_name;
        final BasicImageDownloader downloader = new BasicImageDownloader(new BasicImageDownloader.OnImageLoaderListener() {
            @Override
            public void onError(BasicImageDownloader.ImageError error) {
                Toast.makeText(MainActivity.this, "Error code " + error.getErrorCode() + ": " +
                        error.getMessage(), Toast.LENGTH_LONG).show();
                error.printStackTrace();
                //image_space.setImageResource(RES_ERROR);
            }

            @Override
            public void onProgressChange(int percent) {

            }

            @Override
            public void onComplete(Bitmap result) {
                        /* save the image - I'm gonna use JPEG */
                final Bitmap.CompressFormat mFormat = Bitmap.CompressFormat.JPEG;
                        /* don't forget to include the extension into the file name */
                final File myImageFile = new File(getApplicationContext().getFilesDir().getAbsolutePath() +
                        File.separator + "images" + File.separator +  i_name + "." + mFormat.name().toLowerCase());
                Log.d("FILE", myImageFile.getAbsolutePath());
                BasicImageDownloader.writeToDisk(myImageFile, result, new BasicImageDownloader.OnBitmapSaveListener() {
                    @Override
                    public void onBitmapSaved() {
                        Toast.makeText(MainActivity.this, "Image saved as: " + myImageFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                        printQuestion();
                    }

                    @Override
                    public void onBitmapSaveError(BasicImageDownloader.ImageError error) {
                        Toast.makeText(MainActivity.this, "Error code " + error.getErrorCode() + ": " +
                                error.getMessage(), Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }


                }, mFormat, false);

            }
        });
        downloader.download(url, true);

    }

    private void printPicture(String image_name, String url, boolean b) {
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.d("PERMISSION_RESULT","---------START----------");
        switch (requestCode) {
            case CAMERA_REQUEST: {
                Log.d("PERMISSION_RESULT","---------1----------");

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION_RESULT","---------GRANTED----------");
                    camera = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d("PERMISSION_RESULT","---------DENIED----------");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to use the camera", Toast.LENGTH_SHORT).show();
                }
                break;
            }
           /* case INTERNET_REQUEST: {
                Log.d("PERMISSION_RESULT", "---------INTERNET_REQUEST----------");

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION_RESULT", "---------GRANTED----------");
                    internet = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d("PERMISSION_RESULT", "---------DENIED----------");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to use internet", Toast.LENGTH_SHORT).show();
                }
                break;
            }*/
            case VIBRATE_REQUEST: {
                Log.d("PERMISSION_RESULT", "---------INTERNET_REQUEST----------");

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("PERMISSION_RESULT", "---------GRANTED----------");
                    vibrate = true;
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    Log.d("PERMISSION_RESULT", "---------DENIED----------");
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied to use vibrations", Toast.LENGTH_SHORT).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void printToast(final String str) {
        final AppCompatActivity activity = this;
        activity.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(activity, str, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @SuppressWarnings("deprecation")
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private void toSpeech20(String str, int queue)
    {
        ttobj.speak(str, queue, null);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void toSpeech21(String str, int queue)
    {
        String utteranceId = this.hashCode() + "";
        ttobj.speak(str, queue, null, utteranceId);
    }

    private void toSpeech(String str, int queue) {
        if(((AudioManager)getSystemService(getApplicationContext().AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_MUSIC) == 0){
            vibrator.vibrate(1000);
            Toast.makeText(getApplicationContext(),"Le volume est à 0.",Toast.LENGTH_SHORT).show();
        }
        if (lollipop) {
            toSpeech21(str, queue);
        } else{
            toSpeech20(str,queue);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        // Récupérer les valeurs du capteur
        float x, y, z;
        /*if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];

            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 3000) {
                long diffTime = (curTime - lastUpdate);
                lastUpdate = curTime;

                float speed = Math.abs(x + y + z - last_x - last_y - last_z) / diffTime * 10000;

                if (speed > SHAKE_THRESHOLD) {

                    Log.d("SHAKED","SHAKEDSHAKED");
                }
                Log.d("X",valueOf(x));
                Log.d("Y",valueOf(y));
                Log.d("Z",valueOf(z));
                last_x = x;
                last_y = y;
                last_z = z;
            }
        }*/

       /* if(hasAccelerometer && magnetic != null) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                acceleromterVector = event.values;
            } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticVector = event.values;
            }

            // Demander au sensorManager la matric de Rotation (resultMatric)
            long curTime = System.currentTimeMillis();

            if ((curTime - lastUpdate) > 2000) {
                long diffTime = (curTime - lastUpdate);

                SensorManager.getRotationMatrix(resultMatrix, mResultMatric, acceleromterVector, magneticVector);

                float[] values =  new float[3];
                // Demander au SensorManager le vecteur d'orientation associé (values)
                SensorManager.getOrientation(resultMatrix, values);

                int inclination = (int) SensorManager.getInclination(mResultMatric);

                Log.d("inclinationGarvity",valueOf(inclination));
                if(lastInclination == -1){
                    lastInclination = inclination;
                }else{
                    if(Math.abs(inclination - lastInclination) < 75){
                        if (inclination < 40 && cameraState == START_STATE && ttsready && questionState != MULTIPLE_QUESTIONS_DETECTED) {
                            startDetection();
                            lastUpdate = curTime;
                        } else if (inclination > 140 && cameraState == DETECTING_STATE && ttsready && questionState != MULTIPLE_QUESTIONS_DETECTED) {
                            stopDetection();
                            lastUpdate = curTime;
                        }
                    }
                }


            }
        }else */if(hasAccelerometer)
        {
            long curTime = System.currentTimeMillis();
            if ((curTime - lastUpdate) > 2000) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    acceleromterVector = event.values.clone();

                    double norm_Of_g = Math.sqrt(acceleromterVector[0] * acceleromterVector[0] + acceleromterVector[1] * acceleromterVector[1] + acceleromterVector[2] * acceleromterVector[2]);

// Normalize the accelerometer vector
                    acceleromterVector[0] = acceleromterVector[0] / (float)norm_Of_g;
                    acceleromterVector[1] = acceleromterVector[1] / (float)norm_Of_g;
                    acceleromterVector[2] = acceleromterVector[2] / (float)norm_Of_g;

                    int inclination = (int) Math.round(Math.toDegrees(Math.acos(acceleromterVector[2])));
                    if(lastInclination == -1){
                        lastInclination = inclination;
                    }else{
                        if(Math.abs(inclination - lastInclination) < 75){
                            if (inclination < 40 && cameraState == START_STATE && ttsready && questionState != MULTIPLE_QUESTIONS_DETECTED) {
                                startDetection();
                                toSpeech("Détection en cours.",TextToSpeech.QUEUE_ADD);
                                lastUpdate = curTime;
                            } else if (inclination > 140 && cameraState == DETECTING_STATE && ttsready && questionState != MULTIPLE_QUESTIONS_DETECTED) {
                                stopDetection();
                                toSpeech("Détection interrompue.",TextToSpeech.QUEUE_ADD);
                                lastUpdate = curTime;
                            }
                        }
                    }
                    if(inclinationCounter == 7){
                        lastInclination = inclination;
                        inclinationCounter = 0;
                    }else{
                        inclinationCounter++;
                    }

                }
            }
        }else if(hasProximity) {
            if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                long curTime = System.currentTimeMillis();

                lastUpdate = curTime;
                // La valeur de la lumière
                p = event.values[0];
                if (p < 5 && cameraState == DETECTING_STATE) {
                    stopDetection();

                } else if (p >= 5 && cameraState == START_STATE) {
                    startDetection();
                }
            }
        }else{
            if(cameraState == START_STATE && ttsready) {
                startDetection();
            }
        }

    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

  /*  private void toFile(String str){
        if(lollipop)
        {
            toFile21(str);
        }else{
            toFile20(str);
        }
    }

    @SuppressWarnings("deprecation")
    private void toFile20(String str) {
        HashMap<String, String> utteranceMap = new HashMap<String,String>();
        utteranceMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "synthetizeKey");
        ttobj.synthesizeToFile(str,utteranceMap,questionFile.getAbsolutePath());
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void toFile21(String str) {
        Bundle utteranceBundle = new Bundle();
        utteranceBundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "synthetizeKey");
        ttobj.synthesizeToFile(str,utteranceBundle,questionFile,"synthetizeKey");
    }
*/
}


