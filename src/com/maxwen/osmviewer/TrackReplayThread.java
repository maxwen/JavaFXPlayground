package com.maxwen.osmviewer;

import com.fazecast.jSerialComm.SerialPort;
import com.github.cliftonlabs.json_simple.JsonException;
import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import com.maxwen.osmviewer.nmea.NMEAHandler;
import com.maxwen.osmviewer.nmea.NMEAParser;

import java.io.*;

public class TrackReplayThread extends Thread {
    private static boolean mStopThread;
    private static boolean mPauseThread;
    private static File mTrackFile;
    private static NMEAHandler mHandler;
    private static Runnable t = new Runnable() {
        @Override
        public void run() {
            LogUtils.log("TrackReplayThread started");
            BufferedReader reader = null;
            try {
                reader = new BufferedReader(new FileReader(mTrackFile));
            } catch (FileNotFoundException e) {
                LogUtils.error("TrackReplayThread stopped", e);
                return;
            }
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    try {
                        String[] parts = line.split("\\|");
                        if (parts.length == 2) {
                            String gpsPart = parts[1];
                            JsonObject gpsData = (JsonObject) Jsoner.deserialize(gpsPart);
                            mHandler.onLocation(gpsData);
                            if (mStopThread) {
                                break;
                            }
                            if (mPauseThread) {
                                while (mPauseThread) {
                                    Thread.sleep(1000);
                                }
                            } else {
                                Thread.sleep(1000);
                            }
                        }
                    } catch (Exception e) {
                        LogUtils.error("TrackReplayThread readLine", e);
                        break;
                    }
                }
                reader.close();
            } catch (Exception e) {
            }
            LogUtils.log("TrackReplayThread stopped");
        }
    };

    public TrackReplayThread() {
        super(t);
    }

    public boolean startThread() {
        if (mTrackFile == null || mHandler == null) {
            return false;
        }
        if (!mTrackFile.exists()) {
            return false;
        }
        mStopThread = false;
        mPauseThread = false;
        start();
        return true;
    }

    public boolean setupReplay(File trackFile, NMEAHandler handler) {
        mTrackFile = trackFile;
        mHandler = handler;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(mTrackFile));
        } catch (FileNotFoundException e) {
            LogUtils.error("TrackReplayThread setupReplay", e);
            return false;
        }
        try {
            String line = reader.readLine();
            if (line != null) {
                String[] parts = line.split("\\|");
                if (parts.length == 2) {
                    String gpsPart = parts[1];
                    JsonObject gpsData = (JsonObject) Jsoner.deserialize(gpsPart);
                    mHandler.onLocation(gpsData, true);
                    return true;
                }
            }
            reader.close();
        } catch (Exception e) {
            LogUtils.error("TrackReplayThread setupReplay", e);
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
            }
        }
        return false;
    }

    public void stopThread() {
        mStopThread = true;
    }

    public void pauseThread() {
        mPauseThread = !mPauseThread;
    }
}
