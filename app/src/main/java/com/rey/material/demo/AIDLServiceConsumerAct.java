package com.rey.material.demo;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mmx.service.ISensorService;
public class AIDLServiceConsumerAct extends Activity implements OnClickListener {
    private Button mBindServiceBtn;
    private Button mUnbindServiceBtn;

    private ISensorService mSensorService;
    private Bundle mThreadArgs;
    private Message mThreadMsg;
    private TextView mTextView;

    private final ServiceConnection mServiceConn = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v("SERVICE CONNECTED", "SERVICE CONNECTED");
            mSensorService = ISensorService.Stub.asInterface(service);
            Bundle data = new Bundle();
            data.putString("KEY_APP_ID", "");
            data.putString("KEY_AUTH_KEY", "");
            data.putString("KEY_DEVICE_ID", "");
            data.putString("KEY_APP_VERSION", "");

            try {
                mSensorService.sendRequiredDataToService(data);
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Service Connected !!!", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.v("SERVICE DISCONNECTED", "SERVICE DISCONNECTED");
            mSensorService = null;
            Toast.makeText(getApplicationContext(), "Service Disconnected !!!", Toast.LENGTH_SHORT).show();
        }
    };

    private volatile Looper mServiceLooper;
    private volatile ServiceHandler mServiceHandler;

    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Bundle args = (Bundle) msg.obj;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBindServiceBtn = (Button) findViewById(R.id.btnBindService);
        mBindServiceBtn.setOnClickListener(this);

        mUnbindServiceBtn = (Button) findViewById(R.id.btnUnbindService);
        mUnbindServiceBtn.setOnClickListener(this);
        mUnbindServiceBtn.setEnabled(false);

        HandlerThread hthr = new HandlerThread("StartedSensorServiceThread", Process.THREAD_PRIORITY_BACKGROUND);
        hthr.start();
        mServiceLooper = hthr.getLooper();
        mServiceHandler = new ServiceHandler(mServiceLooper);
        mThreadArgs = new Bundle();

        IntentFilter filter = new IntentFilter("com.snapone.service");
        SensorReceiver receiver = new SensorReceiver();
        registerReceiver(receiver, filter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnBindService:
                if (!bindService(new Intent(ISensorService.class.getName()), mServiceConn, Context.BIND_AUTO_CREATE)) {
                    Toast.makeText(getApplicationContext(), "Kindly Install the service", Toast.LENGTH_LONG).show();
                    break;
                }
                mBindServiceBtn.setEnabled(false);
                mUnbindServiceBtn.setEnabled(true);
                break;
            case R.id.btnUnbindService:
                unbindService(mServiceConn);
                mBindServiceBtn.setEnabled(true);
                mUnbindServiceBtn.setEnabled(false);
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void appendText(String text) {
        String currentText = mTextView.getText() + System.getProperty("line.separator") + text;
        mTextView.setText(currentText);
        final int scrollAmount = mTextView.getLayout().getLineTop(mTextView.getLineCount()) - mTextView.getHeight();
        // if there is no need to scroll, scrollAmount will be <=0
        if (scrollAmount > 0)
            mTextView.scrollTo(0, scrollAmount);
        else
            mTextView.scrollTo(0, 0);
    }

    public class SensorReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("HeartMessage");
            appendText(message);
        }
    }
}