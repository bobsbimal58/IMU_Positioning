package com.bimal.mag_positioning;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by wmcs on 5/18/2017.
 */

public class ServerClient extends Activity {
    public int PORT = 15000;
    private Button connectPhones;
    private String serverIpAddress = "117.16.23.116";
    private boolean connected = false;
    TextView text;
    EditText port;
    EditText ipAdr;
    private float x,y,z;
    private SensorManager sensorManager;
    private Sensor sensor;
    boolean mag = false;
    boolean isStreaming = false;
    PrintWriter out;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.live);

        connectPhones = (Button)findViewById(R.id.send);
        connectPhones.setOnClickListener(connectListener);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        port=(EditText)findViewById(R.id.port);
        ipAdr=(EditText)findViewById(R.id.ipadr);

        port.setText("15000");
        ipAdr.setText(serverIpAddress);
        mag =false;
    }
    private Button.OnClickListener connectListener = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            if (!connected) {
                if (!serverIpAddress.equals("")) {
                    connectPhones.setText("Stop Streaming");
                    Thread cThread = new Thread(new ClientThread());
                    cThread.start();
                }
            }
            else{
                connectPhones.setText("Start Streaming");
                connected=false;
                mag=false;
            }
        }
    };
    public class ClientThread implements Runnable {
        Socket socket;

        public void run() {
            try {
                mag = true;
                PORT = Integer.parseInt(port.getText().toString());
                serverIpAddress = ipAdr.getText().toString();
                InetAddress serverAddr = InetAddress.getByName(serverIpAddress);
                //InetAddress serverAddr = InetAddress.getByName("TURBOBEAVER");
                socket = new Socket(serverAddr, PORT);
                connected = true;
                out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);
                while (connected) {
                    out.printf("%10.2f\n", x);
                    out.flush();
                    Thread.sleep(2);
                }
            } catch (Exception e) {

            } finally {
                try {
                    mag = false;
                    connected = false;
                    connectPhones.setText("Start Streaming");
                    //out.close();
                    socket.close();
                } catch (Exception a) {
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(magnetometerListener, sensor,
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onStop() {
        sensorManager.unregisterListener(magnetometerListener);
        super.onStop();
    }

    private SensorEventListener magnetometerListener = new SensorEventListener() {

        @Override
        public void onAccuracyChanged(Sensor sensor, int acc) {
        }
        @Override
        public void onSensorChanged(SensorEvent event) {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
            refreshDisplay();
        }
    };

    private void refreshDisplay() {
        if(mag == true){
            String output = String.format("X:%3.2f   |  Y:%3.2f   |   Z:%3.2f ", x, y, z);
            text.setText(output);
        }
    }

}
