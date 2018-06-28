package com.bimal.mag_positioning;

import android.os.AsyncTask;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by wmcs on 5/19/2017.
 */

public class SendMessage extends AsyncTask<String, Void, Void> {
    private Exception exception;
    private Socket socket = null;
    private PrintWriter outToServer = null;
    @Override
    protected Void doInBackground(String... params) {
        try {
            try {
                socket = new Socket("117.16.23.116",15000);
                outToServer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()),true);
                outToServer.print(params[0]);
                outToServer.flush();
                outToServer.close();
                socket.close();

            } catch (IOException e) {
                e.printStackTrace();
                outToServer.close();
                socket.close();
            }

        } catch (Exception e) {
            this.exception = e;
            return null;
        }

        return null;
    }
}
