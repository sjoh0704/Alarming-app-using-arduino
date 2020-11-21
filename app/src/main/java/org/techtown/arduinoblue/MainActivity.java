package org.techtown.arduinoblue;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.Camera;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, SurfaceHolder.Callback {

    private Button mBtnBluetoothOn;   // 블루투스온 버튼
    private Button mBtnBluetoothOff;  // 블루투스 꺼짐 버튼
    private Button mBtnConnect;  // 아두이노와 블루투스연결 버튼
    private Button btnSavePhoneNo;  // 핸드폰 번호 save 버튼
    private TextView textViewPhone, textViewPhone2;  // personal contact 텍스트뷰, emergency contact 텍스트뷰
    private Camera camera;  // 레코딩 카메라
    private MediaRecorder mediaRecorder;  //  비디오레코더
    private Button btn_record, btn_upload;  // 영상 레코딩하기 버튼, 영상 업로드하기 버튼(파이어베이스, 외부저장소에)
    private SurfaceView surfaceView;   // 카메라 보여주는 화면
    private SurfaceHolder surfaceHolder;  //
    private boolean recording = false;  // recording 상태나타내기
    private String filename = null;  // 만들어진 비디오 파일이름
    private String dirPath;  // 저장된 파일경로
    private Button btn_send;  // sms 메시지 전송 버튼
    private EditText textPhoneNo;  //
    private  ProgressDialog progressDialog;
    private int warningValue = 0;
    private TextView status_textView;

    private GpsTracker gpsTracker;
    private List phoneNoBasic;
    private List phoneNoEmergency;

    private RadioButton radioButton1, radioButton2;



    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> mPairedDevices;
    List<String> mListPairedDevices;

    Handler mBluetoothHandler;
    ConnectedBluetoothThread mThreadConnectedBluetooth;
    BluetoothDevice mBluetoothDevice;
    BluetoothSocket mBluetoothSocket;

    final static int BT_REQUEST_ENABLE = 1;
    final static int BT_MESSAGE_READ = 2;
    final static int BT_CONNECTING_STATUS = 3;
    final static UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle("Vehicle Accident Notification");
        allowPermission();  //Ted permission으로 권한 얻어오기
        makeDir();

        phoneNoBasic = new ArrayList();
        phoneNoEmergency = new ArrayList();
        status_textView = findViewById(R.id.status);
        textViewPhone = findViewById(R.id.textViewPhone);
        textViewPhone2 = findViewById(R.id.textViewPhone2);

        radioButton1 = findViewById(R.id.basicBtn);
        radioButton2 = findViewById(R.id.emergencyBtn);
        btn_record = findViewById(R.id.btn_record);
        btn_upload = findViewById(R.id.btn_upload);
        btn_send = findViewById(R.id.btn_send);
        btnSavePhoneNo = findViewById(R.id.savePhoneNumber);
        btnSavePhoneNo.setOnClickListener(this);
        btn_record.setOnClickListener(this);
        btn_upload.setOnClickListener(this);
        btn_send.setOnClickListener(this);
        textPhoneNo = findViewById(R.id.editTextPhoneNumber);
        radioButton1.setOnClickListener(this);
        radioButton2.setOnClickListener(this);


        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please waiting...");
        progressDialog.setCancelable(false);
        progressDialog.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal);


        mBtnBluetoothOn = (Button)findViewById(R.id.btnBluetoothOn);
        mBtnBluetoothOff = (Button)findViewById(R.id.btnBluetoothOff);
        mBtnConnect = (Button)findViewById(R.id.btnConnect);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        mBtnBluetoothOn.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOn();
            }
        });
        mBtnBluetoothOff.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                bluetoothOff();
            }
        });
        mBtnConnect.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                listPairedDevices();
            }
        });


        mBluetoothHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == BT_MESSAGE_READ){
                    String readMessage = null;

                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    Log.e("readMessage: ",readMessage.substring(0,0) );
                    warningValue = Integer.parseInt(readMessage.substring(0,1));
                    if(warningValue == 2){
                        if(recording) {
                            btn_record.callOnClick();

                        }
                        status_textView.setText("severe");
                        Toast.makeText(MainActivity.this, "severe", Toast.LENGTH_SHORT).show();

                    }
                    else if(warningValue == 1){
                        if(recording) {
                            btn_record.callOnClick();


                        }
                        status_textView.setText("danger");
                        Toast.makeText(MainActivity.this, "danger", Toast.LENGTH_SHORT).show();
                    }else{
//                        Log.e("status: ", "안전");
                        status_textView.setText("normal");
                    }

                    
                    
                    
                }
            }
        };
    }
    void bluetoothOn() {
        if(mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "This device does not support Bluetooth.", Toast.LENGTH_LONG).show();
        }
        else {
            if (mBluetoothAdapter.isEnabled()) {
                Toast.makeText(getApplicationContext(), "Bluetooth has already been activated.", Toast.LENGTH_LONG).show();

            }
            else {
                Toast.makeText(getApplicationContext(), "Bluetooth is not activated.", Toast.LENGTH_LONG).show();
                Intent intentBluetoothEnable = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(intentBluetoothEnable, BT_REQUEST_ENABLE);
            }
        }
    }
    void bluetoothOff() {
        if (mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.disable();
            Toast.makeText(getApplicationContext(), "Bluetooth is deactivated", Toast.LENGTH_SHORT).show();

        }
        else {
            Toast.makeText(getApplicationContext(), "Bluetooth has already been deactivated.", Toast.LENGTH_SHORT).show();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BT_REQUEST_ENABLE:
                if (resultCode == RESULT_OK) { // 블루투스 활성화를 확인을 클릭하였다면
                    Toast.makeText(getApplicationContext(), "Activate Bluetooth", Toast.LENGTH_LONG).show();

                } else if (resultCode == RESULT_CANCELED) { // 블루투스 활성화를 취소를 클릭하였다면
                    Toast.makeText(getApplicationContext(), "Cancel", Toast.LENGTH_LONG).show();

                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    void listPairedDevices() {
        if (mBluetoothAdapter.isEnabled()) {
            mPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (mPairedDevices.size() > 0) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("DEVICES");

                mListPairedDevices = new ArrayList<String>();
                for (BluetoothDevice device : mPairedDevices) {
                    mListPairedDevices.add(device.getName());
                    //mListPairedDevices.add(device.getName() + "\n" + device.getAddress());
                }
                final CharSequence[] items = mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);
                mListPairedDevices.toArray(new CharSequence[mListPairedDevices.size()]);

                builder.setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int item) {
                        connectSelectedDevice(items[item].toString());
                    }
                });
                AlertDialog alert = builder.create();
                alert.show();
            } else {
                Toast.makeText(getApplicationContext(), "Unpaired Devices", Toast.LENGTH_LONG).show();
            }
        }
        else {
            Toast.makeText(getApplicationContext(), "Bluetooth is Deactivated.", Toast.LENGTH_SHORT).show();
        }
    }
    void connectSelectedDevice(String selectedDeviceName) {
        for(BluetoothDevice tempDevice : mPairedDevices) {
            if (selectedDeviceName.equals(tempDevice.getName())) {
                mBluetoothDevice = tempDevice;
                break;
            }
        }
        try {
            mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(BT_UUID);
            mBluetoothSocket.connect();
            mThreadConnectedBluetooth = new ConnectedBluetoothThread(mBluetoothSocket);
            mThreadConnectedBluetooth.start();
            mBluetoothHandler.obtainMessage(BT_CONNECTING_STATUS, 1, -1).sendToTarget();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Error Establishing Connection", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onClick(View view) {



        switch(view.getId()) {

            case R.id.btn_record:
                if (recording) {

                    mediaRecorder.stop();
                    mediaRecorder.release();
                    camera.lock();
                    recording = false;

                    Toast.makeText(MainActivity.this, "Please wait_Uploading to Firebase", Toast.LENGTH_SHORT).show();

                    btn_upload.callOnClick();
                    btn_send.callOnClick();


                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if (phoneNoBasic.size()!= 0 || phoneNoEmergency.size()!=0 ) {

                                //과부화도 덜되고 동영상 처리는 여기서 하는게 좋다
                                try {
                                    Toast.makeText(MainActivity.this, "Recording", Toast.LENGTH_SHORT).show();


                                    mediaRecorder = new MediaRecorder();
                                    camera.unlock();
                                    mediaRecorder.setCamera(camera);
                                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
                                    mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
                                    mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_480P));
                                    mediaRecorder.setOrientationHint(90);

                                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                    Date now = new Date();
                                    filename = formatter.format(now) + ".mp4";


                                    mediaRecorder.setOutputFile(dirPath + "/" + filename);
                                    mediaRecorder.setPreviewDisplay(surfaceHolder.getSurface());
                                    mediaRecorder.prepare();
                                    mediaRecorder.start();
                                    recording = true;

//
//                                    MediaScanner ms = MediaScanner.newInstance(MainActivity.this);
//                                    try {
//                                        ms.mediaScanning(dirPath + "/" + filename);
//                                    } catch (Exception e) {
//                                        e.printStackTrace();
//                                        Log.d("MediaScan", "ERROR" + e);
//                                    } finally {
//                                    }
//                                galleryAddPic();


                                } catch (Exception e) {
                                    e.printStackTrace();
                                    mediaRecorder.release();
                                }
                            } else {
                                Toast.makeText(MainActivity.this, "Not Registered", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }


                break;
            case R.id.btn_upload:
                if (filename != null) {

                    Log.e("Verification File: ", filename);

                    FirebaseStorage storage = FirebaseStorage.getInstance();

                    Uri file = Uri.fromFile(new File(dirPath + "/" + filename));
                    StorageReference storageRef = storage.getReferenceFromUrl("gs://arduinovideo-5c84c.appspot.com/").child(dirPath + "/" + filename);
                    //storage url 적는란


                    Log.e("Verification url: ", String.valueOf(file));
                    storageRef.putFile(file)
                            .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onProgress(@NonNull UploadTask.TaskSnapshot taskSnapshot) {
                                    double progress = (100.0 * taskSnapshot.getBytesTransferred());
                                    progressDialog.show();
                                    if (progress >= 100)
                                        progressDialog.dismiss();
                                }
                            })

                            .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Toast.makeText(MainActivity.this, "Upload Success ", Toast.LENGTH_SHORT).show();
                                    filename = null;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Toast.makeText(MainActivity.this, "Upload Fail", Toast.LENGTH_SHORT).show();
                                }
                            });


                } else {
                    Toast.makeText(MainActivity.this, "No File", Toast.LENGTH_SHORT).show();
                }
                break;


            case R.id.btn_send:
                if(phoneNoEmergency.size()!= 0 || phoneNoBasic.size()!=0) {
                    Log.e("SMS: ", "check");
                    gpsTracker = new GpsTracker(this);
                    double latitude = gpsTracker.getLatitude();
                    double longtitude = gpsTracker.getLongitude();
                    String address = getCurrentAddress(latitude, longtitude);
                    String location = "\n위도: " + latitude + "\n경도: " + longtitude;
                    Log.e("WarningValue: ", String.valueOf(warningValue));
                    Log.e("basic: ", String.valueOf(phoneNoBasic.size()));
                    Log.e("emergency: ", String.valueOf(phoneNoEmergency.size()));


                    if (warningValue >= 1) {
                        for (Object object : phoneNoBasic) {

                            sendSms((String) object, address + location);
                        }
                    }
                    if (warningValue >= 2) {
                        for (Object object : phoneNoEmergency) {
                            sendSms((String) object, address + location);
                        }
                    }


                    Toast.makeText(this, "Text Message", Toast.LENGTH_SHORT).show();
                }
                else{
                    Toast.makeText(this, "Register Mobile Number", Toast.LENGTH_SHORT).show();
                }

                break;
            case R.id.basicBtn:

                btnSavePhoneNo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String number = textPhoneNo.getText().toString();
                if(number.length() != 11){
                    Toast.makeText(MainActivity.this, "Register appropriate Number", Toast.LENGTH_SHORT).show();
                }
                else{
                    phoneNoBasic.add(number);
                }

                String phoneList = "";
                for(Object n: phoneNoBasic){
                    String element = (String)n;
                    phoneList += element + "\n";
                }
                textViewPhone.setText(phoneList);
                textPhoneNo.setText("");



                    }
                });


                break;

            case R.id.emergencyBtn:

                btnSavePhoneNo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {

                        String number = textPhoneNo.getText().toString();
                        if(number.length() != 11 ){
                            Toast.makeText(MainActivity.this, "Register appropriate Number", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            phoneNoEmergency.add(number);
                        }

                        String phoneList = "";
                        for(Object n: phoneNoEmergency){
                            String element = (String)n;
                            phoneList += element + "\n";
                        }
                        textViewPhone2.setText(phoneList);
                        textPhoneNo.setText("");



                    }
                });

                break;

//


        }}

    private void sendSms(String phoneNum, String msg) {

        SmsManager sms = SmsManager.getDefault();
        Log.e("SendSMS: ", phoneNum);
        Log.e("MSG: ",msg);
        sms.sendTextMessage(phoneNum, null, msg, null, null);


    }


    public String getCurrentAddress( double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {

            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "Geocoder is not available", Toast.LENGTH_LONG).show();
            return "Geocoder is not available";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "Wrong GPS Location", Toast.LENGTH_LONG).show();
            return "Wrong GPS Location";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "Address is not detected", Toast.LENGTH_LONG).show();
            return "Address is not detected";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";

    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {

    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int i, int i1, int i2) {

    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {

    }

    private class ConnectedBluetoothThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedBluetoothThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getApplicationContext(), "Error Establishing Socket Connection", Toast.LENGTH_LONG).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.available();
                    if (bytes != 0) {
                        SystemClock.sleep(100);
                        bytes = mmInStream.available();
                        bytes = mmInStream.read(buffer, 0, bytes);
                        mBluetoothHandler.obtainMessage(BT_MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (IOException e) {
                    break;
                }
            }
        }


    }

    private void makeDir() {
        String str = Environment.getExternalStorageState();
        if ( str.equals(Environment.MEDIA_MOUNTED)) {

            dirPath = "/sdcard/DCIM/BlackBox";
            File file = new File(dirPath);
            if( !file.exists() )  // 원하는 경로에 폴더가 있는지 확인
            {
                Log.e("TAG : ", "Create Directory");
                file.mkdirs();
            }
            else Log.e("TAG : ", "Directory has been created");

        }
        else
            Toast.makeText(this, " Failed SD Card Verification", Toast.LENGTH_SHORT).show();
    }
    PermissionListener permission = new PermissionListener() {
        @Override
        public void onPermissionGranted() {
            camera = Camera.open();
            camera.setDisplayOrientation(90);
            surfaceView = findViewById(R.id.surfaceView);
            surfaceHolder = surfaceView.getHolder();
            surfaceHolder.addCallback(MainActivity.this);
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        }

        @Override
        public void onPermissionDenied(ArrayList<String> deniedPermissions) {

        }
    };

    private void allowPermission() {

        TedPermission.with(this)
                .setPermissionListener(permission)
                .setRationaleMessage("Allow needed authentication")
                .setDeniedMessage("Authentification has been denied")
                .setPermissions(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION )
                .check();
    }

    private void refreshCamera(Camera camera) {
        if(surfaceHolder.getSurface() == null){
            return ;
        }
        try {
            camera.stopPreview();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        setCamera(camera);


    }

    private void setCamera(Camera cam) {
        camera = cam;

    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }




}