package jp.jaxa.iss.kibo.rpc.spacebroapk;

import android.graphics.Bitmap;
import android.util.Log;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import org.opencv.aruco.Aruco;
import org.opencv.aruco.DetectorParameters;
import org.opencv.aruco.Dictionary;
import org.opencv.core.Mat;

import java.util.ArrayList;
import java.util.List;

import jp.jaxa.iss.kibo.rpc.api.KiboRpcService;

import gov.nasa.arc.astrobee.Result;
import gov.nasa.arc.astrobee.types.Point;
import gov.nasa.arc.astrobee.types.Quaternion;


/**
 * Class meant to handle commands from the Ground Data System and execute them in Astrobee
 */

public class YourService extends KiboRpcService {
    double Value;
    @Override
    protected void runPlan1(){
        api.judgeSendStart();

        move(11.5, -4.3, 4.5, 0, 0, -0.7071068, 0.7071068);

        moveToWrapper(11.45, -5.7, 4.5, 0, 0, 0, 1); //p1-1
        readQRcode(0);
        double valueX = Value;

        moveToWrapper(11, -6, 5.55, 0, -0.7071068,0, 0.7071068); //p1-2
        readQRcode(1);
        double valueY = Value;

        moveToWrapper(11,-5.5,4.33,0,0.7071068,0,0.7071068); //p1-3
        readQRcode(2);
        double valueZ = Value;

        move(10.45, -5.5, 4.9,0,0,-0.7071068,0.7071068); //p1 end
        move(10.55, -6.8, 4.9,0,0,0,1);
        move(11.2,-6.8,4.9,0,0,-0.7071068,0.7071068);
        move(11.2,-7.5,4.9,0,0,1,0); //p2 start

        moveToWrapper(10.35, -7.5,4.7,0,0,1,0); //p2-1
        readQRcode(3);

        moveToWrapper(11.45, -8, 5,0,0,0,1); //p2-2
        readQRcode(4);

        moveToWrapper(11, -7.7, 5.55, 0, -0.7071068,0, 0.7071068); //p2-3
        readQRcode(5);

        move(11,-8,4.8,0,0,-0.7071068,0.7071068);

        move(10.95, -9.3, 4.8, 0, 0, -0.7071068, 0.7071068);

        moveToWrapper(valueX, valueY, valueZ, 0, 0, -0.7071068,0.7071068); //p3
        detectARMarker();
        moveToWrapper(valueX+0.282, valueY, valueZ+0.282, 0, 0, -0.7071068,0.7071068);
        api.laserControl(true);

        api.judgeSendFinishSimulation();
    }

    @Override
    protected void runPlan2(){
        // write here your plan 2
    }

    @Override
    protected void runPlan3(){
        // write here your plan 3
    }

    // move method
    private void move(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w){

        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float)qua_x, (float)qua_y,
                (float)qua_z, (float)qua_w);

        api.moveTo(point, quaternion, true);
    }


    // move to warapper method
    private void moveToWrapper(double pos_x, double pos_y, double pos_z,
                               double qua_x, double qua_y, double qua_z,
                               double qua_w){

        final int LOOP_MAX = 3;
        final Point point = new Point(pos_x, pos_y, pos_z);
        final Quaternion quaternion = new Quaternion((float)qua_x, (float)qua_y,
                (float)qua_z, (float)qua_w);

        Result result = api.moveTo(point, quaternion, true);

        int loopCounter = 0;
        while(!result.hasSucceeded() || loopCounter < LOOP_MAX){
            result = api.moveTo(point, quaternion, true);
            ++loopCounter;
        }
    }

    //QR code method
    private void readQRcode(int count){
        Bitmap bitmap = api.getBitmapNavCam();
        int width = 1280;
        int height = 960;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        try{
            LuminanceSource source = new RGBLuminanceSource(width, height, pixels);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));
            QRCodeReader reader = new QRCodeReader();
            com.google.zxing.Result decodeResult = reader.decode(binaryBitmap);
            String result = decodeResult.getText();
            Log.d("readQR", result);
            api.judgeSendDiscoveredQR(count, result);
            Value = Double.parseDouble(result);

        }catch (Exception e){
            Log.d("readQR", e.getLocalizedMessage());
        }
    }

    //AR marker method
    private void detectARMarker(){
        Dictionary dictionary = Aruco.getPredefinedDictionary(Aruco.DICT_5X5_250);

        Mat inputImage = api.getMatNavCam();
        List<Mat> corners = new ArrayList<>();
        Mat markerIds = new Mat();
        DetectorParameters parameters = DetectorParameters.create();
        Aruco.detectMarkers(inputImage, dictionary, corners, markerIds, parameters);
        String value = markerIds.toString();
        api.judgeSendDiscoveredAR(value);
    }

}

