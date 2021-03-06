package de.windowsfreak.testjni.reader;

import de.windowsfreak.nistreamer.SettingsActivity;
import de.windowsfreak.testjni.Config;
import org.openni.*;
import org.openni.android.OpenNIHelper;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by lazer_000 on 25.06.2015.
 */
public class OniStreamSource implements Source, OpenNIHelper.DeviceOpenListener {
    private SettingsActivity activity;
    private OpenNIHelper helper;
    private boolean devicePending = false;
    private Device device;
    private VideoStream videoStream;
    SensorType type = SensorType.DEPTH;
    private ArrayList<VideoMode> supportedModes = new ArrayList<VideoMode>();

    public OniStreamSource(SettingsActivity activity) {
        this.activity = activity;
    }

    public boolean isInitialized() {
        return !devicePending && device != null;
    }

    public void initialize() {
        System.loadLibrary("OpenNI2");
        System.loadLibrary("OpenNI2.jni");
        helper = new OpenNIHelper(activity);
        OpenNI.initialize();

        String uri;

        List<DeviceInfo> devicesInfo = OpenNI.enumerateDevices();
        if (devicesInfo.isEmpty()) {
            new Exception("Error! There is no connected device.").printStackTrace();
            return;
        }
        uri = devicesInfo.get(0).getUri();

        devicePending = true;
        helper.requestDeviceOpen(uri, this);
        //device = Device.open(uri);
    }

    public void start(final Config config) {
        switch (config.depth) {
            case 1: type = SensorType.DEPTH; break;
            case 2: type = SensorType.COLOR; break;
            case 3: type = SensorType.IR; break;
        }
        videoStream = VideoStream.create(device, type);
        List<VideoMode> modes = videoStream.getSensorInfo().getSupportedVideoModes();
        supportedModes = new ArrayList<VideoMode>();

        // now only keep the ones that our application supports
        for (VideoMode mode : modes) {
            if (mode.getResolutionX() != config.x || mode.getResolutionY() != config.y) continue;
            if (mode.getFps() != 30) continue;
            switch (mode.getPixelFormat()) {
                case DEPTH_1_MM:
                    if (config.mode == 1) supportedModes.add(mode);
                    break;
                case DEPTH_100_UM:
                    if (config.mode == 2) supportedModes.add(mode);
                    break;
                case GRAY16:
                    if (config.mode == 3) supportedModes.add(mode);
                    break;
            }
        }

        VideoMode mode = supportedModes.get(0); // 1mm
        //VideoMode mode = supportedModes.get(supportedModes.size() - 1); // 100um
        if (mode == null) System.out.println("Hey, I could not find an appropriate stream!");

        videoStream.setVideoMode(mode);
        videoStream.start();

        config.fovX = videoStream.getHorizontalFieldOfView();
        config.fovY = videoStream.getVerticalFieldOfView();
        config.x = (short)videoStream.getVideoMode().getResolutionX();
        config.y = (short)videoStream.getVideoMode().getResolutionY();
        config.fps = (byte)videoStream.getVideoMode().getFps();
        System.out.println("Hey, I have a " + config.x + "x" + config.y + "@" + config.fps + " camera with " + config.fovX + "x" + config.fovY + " viewing range!");
    }

    public void stop() {
        if (videoStream != null) {
            videoStream.stop();
            videoStream.destroy();
            videoStream = null;
        }
    }

    public void shutDown() {
        try {
            device.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        device = null;
        helper.shutdown();
        OpenNI.shutdown();
    }

    @Override
    public boolean readImage(final Config config, final ByteBuffer out) {
        if (videoStream == null) return false;
        VideoFrameRef lastFrame = videoStream.readFrame();
        config.frameId = lastFrame.getFrameIndex();
        synchronized(out) {
            out.clear();
            out.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(lastFrame.getData().order(ByteOrder.BIG_ENDIAN).asShortBuffer());
            out.limit(lastFrame.getData().limit());
        }
        lastFrame.release();
        return true;

    }

    @Override
    public void onDeviceOpened(Device device) {
        System.out.println("Device correctly opened!");
        this.device = device;
        devicePending = false;
        synchronized(this) {
            this.notify();
        }
    }

    @Override
    public void onDeviceOpenFailed(String uri) {
        System.err.println("Failed to open device " + uri);
        devicePending = false;
    }
}
