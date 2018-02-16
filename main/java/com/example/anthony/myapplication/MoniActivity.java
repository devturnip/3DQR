package com.example.anthony.myapplication;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters;

public class MoniActivity extends AppCompatActivity {

    Bitmap bp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_moni);
        //Toast.makeText(getApplicationContext(), "MoniActivity started", Toast.LENGTH_SHORT).show();
        final ImageView jpgView = (ImageView)findViewById(R.id.imageView);

        File imageFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        final File mImageFolder = new File(imageFile, "monimix");
        final Handler handler = new Handler();

        if (mImageFolder.exists()) {
            File[] dirFiles = mImageFolder.listFiles();
            if (dirFiles.length!=0) {
                for (int i=0; i<dirFiles.length; i++) {
                    Bitmap bitmap = BitmapFactory.decodeFile(dirFiles[i].toString());
                    bp = bitmap;
                    jpgView.setImageBitmap(bitmap);
                }
            }
        }
        Bitmap emptyBitmap = Bitmap.createBitmap(bp.getWidth(), bp.getHeight(), bp.getConfig());
        Bitmap nBP = bp.copy(Bitmap.Config.ARGB_8888, true);
        if (bp.sameAs(emptyBitmap)) {
            Log.d("MONIMIXXXXX", "EMPTYYYYYYYYYYYYYYYYYYYYYYYYYYYY");
        } else {
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap nBP = bp.copy(Bitmap.Config.ARGB_8888, true); //bp is a bitmap which contains the source img
                    Mat imageMat = new Mat();
                    Utils.bitmapToMat(nBP, imageMat);
                    Mat srcImage = new Mat();
                    Utils.bitmapToMat(nBP, srcImage);

                    Imgproc.cvtColor(imageMat, imageMat, Imgproc.COLOR_BGR2GRAY);
                    Imgproc.GaussianBlur(imageMat, imageMat, new Size(5, 5), 0);
                    //Imgproc.adaptiveThreshold(imageMat, imageMat, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY_INV, 5, 4);
                    Imgproc.threshold(imageMat, imageMat, 0, 255, Imgproc.THRESH_OTSU);

                    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
                    Mat hierarchy = new Mat();
                    Imgproc.findContours(imageMat, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

                    MatOfPoint approx=new MatOfPoint();
                    List<MatOfPoint> largest_squares = new ArrayList<MatOfPoint>();
                    MatOfPoint temp_contour = contours.get(0); //the largest is at the index 0 for starting point
                    MatOfPoint2f approxCurve = new MatOfPoint2f();
                    MatOfPoint2f maxCurve = new MatOfPoint2f();
                    double maxArea = -1;
                    int maxAreaIdx = -1;

                    for (int i=0; i<contours.size(); i++) {

                        temp_contour = contours.get(i);
                        double contourarea = Imgproc.contourArea(temp_contour);
                        //approx = approxPolyDP(contours.get(i),  Imgproc.arcLength(new MatOfPoint2f(contours.get(i).toArray()), true)*0.02, true);
                        if (contourarea > maxArea) {
                            //check if this contour is a square
                            MatOfPoint2f new_mat = new MatOfPoint2f(temp_contour.toArray());
                            int contourSize = (int) temp_contour.total();
                            Imgproc.approxPolyDP(new_mat, approxCurve, contourSize * 0.05, true);
                            if (approxCurve.total() == 4) {
                                maxCurve = approxCurve;
                                maxArea = contourarea;
                                maxAreaIdx = i;
                                largest_squares.add(temp_contour);
                            }
                        }
                    }
                    hierarchy.release();

                    List<Point> src_pnt = new ArrayList<Point>();
                    double temp_double[] = maxCurve.get(0, 0);
                    Point p1 = new Point(temp_double[0], temp_double[1]);
                    src_pnt.add(p1);
                    temp_double = maxCurve.get(1,0);
                    Point p2 = new Point(temp_double[0], temp_double[1]);
                    src_pnt.add(p2);
                    temp_double = maxCurve.get(2,0);
                    Point p3 = new Point(temp_double[0], temp_double[1]);
                    src_pnt.add(p3);
                    temp_double = maxCurve.get(3,0);
                    Point p4 = new Point(temp_double[0], temp_double[1]);
                    src_pnt.add(p4);
                    Mat startM = Converters.vector_Point2f_to_Mat(src_pnt);

                    int destImgWidth = 500;
                    int destImgHeight = 500;

                    List<Point> dest_pnt = new ArrayList<Point>();
                    Point p5 = new Point(0,0);
                    dest_pnt.add(p5);
                    Point p6 = new Point(0,destImgHeight);
                    dest_pnt.add(p6);
                    Point p7 = new Point(destImgWidth,destImgHeight);
                    dest_pnt.add(p7);
                    Point p8 = new Point(destImgWidth,0);
                    dest_pnt.add(p8);
                    Mat endM = Converters.vector_Point2f_to_Mat(dest_pnt);

                    Mat outputMat = new Mat(destImgHeight, destImgWidth, CvType.CV_8UC4);

                    Mat perspectiveTransform = Imgproc.getPerspectiveTransform(startM, endM);

                    Imgproc.warpPerspective(srcImage, outputMat, perspectiveTransform, new Size(destImgWidth,destImgHeight));

                    //Imgproc.GaussianBlur(outputMat, outputMat, new Size(3, 3), 0);

                    Bitmap output = Bitmap.createBitmap(destImgWidth, destImgHeight, Bitmap.Config.ARGB_8888);
                    Bitmap output1 = Bitmap.createBitmap(destImgWidth, destImgHeight, Bitmap.Config.ARGB_8888);

                    Utils.matToBitmap(outputMat, output);
                    Utils.matToBitmap(outputMat, output1);

                    final Bitmap cropImg = output.copy(Bitmap.Config.ARGB_8888, true);

                    handler.post(new Runnable(){
                        @Override
                        public void run() {
                            jpgView.setImageBitmap(cropImg);

                        }
                    });

                    Bitmap temp = output.copy(Bitmap.Config.ARGB_8888, true);
                    Mat mat = new Mat(destImgWidth, destImgHeight, CvType.CV_8UC3);
                    Mat hsv_image = new Mat();

                    Mat hsv_image1 = new Mat();

                    Mat temp_hsv = new Mat();

                    Utils.bitmapToMat(temp, mat);
                    //Imgproc.cvtColor(mat, hsv_image, Imgproc.COLOR_RGB2HSV_FULL);
                    Imgproc.cvtColor(mat, hsv_image, Imgproc.COLOR_BGR2HSV);
                    Imgproc.cvtColor(mat, temp_hsv, Imgproc.COLOR_BGR2HSV);
                    Imgproc.cvtColor(mat, hsv_image1, Imgproc.COLOR_BGR2HSV);

                    //Mat lower_red_hue_range = new Mat();
                    //Mat upper_red_hue_range = new Mat();
                    Mat lower_green_hue_range = new Mat();
                    Mat fake_red_hue_range = new Mat();
                    Mat black_hue_range = new Mat();

                    /*Scalar lr = new Scalar (0,100,100);
                    Scalar lr1 = new Scalar (10,255,255);
                    Scalar hr = new Scalar (160,100,100);
                    Scalar hr1 = new Scalar (179,255,255);
                    */

                    Scalar fakeRed = new Scalar(65,100,100);
                    Scalar fakeRed1 = new Scalar(135,255,255);

                    Scalar lg = new Scalar (40,100,100);
                    Scalar lg1 = new Scalar (75,255,255);

                    Scalar bl = new Scalar(0,0,0);
                    Scalar bl1 = new Scalar(180,255,40);

                    //Core.inRange(hsv_image, lr, lr1, lower_red_hue_range);
                    //Core.inRange(hsv_image, hr, hr1, upper_red_hue_range);
                    Core.inRange(hsv_image, fakeRed, fakeRed1, fake_red_hue_range);
                    Core.inRange(hsv_image, lg, lg1, lower_green_hue_range);
                    Core.inRange(hsv_image, bl, bl1, black_hue_range);

                    Core.addWeighted(fake_red_hue_range, 1.0, lower_green_hue_range, 1.0, 0.0, hsv_image);
                    Core.addWeighted(fake_red_hue_range, 1.0, black_hue_range, 1.0, 0.0, hsv_image1);
                    //Core.addWeighted(hsv_image, 1.0, lower_green_hue_range, 1.0, 0.0, hsv_image);


                    Utils.matToBitmap(hsv_image, output);
                    Utils.matToBitmap(hsv_image1, output1);
                    //Utils.matToBitmap(outputMat, output);

                    output = Invert(output);
                    output1 = Invert(output1);

                    final Bitmap fOutput = output.copy(Bitmap.Config.ARGB_8888, true);
                    final Bitmap fOutput1 = output1.copy(Bitmap.Config.ARGB_8888, true);

                    writeToFile(cropImg, mImageFolder);
                    writeToFile(output, mImageFolder);
                    writeToFile(output1, mImageFolder);

                    handler.post(new Runnable(){
                        @Override
                        public void run() {
                            String data1 = getBarcodeData(fOutput);
                            String data2 = getBarcodeData(fOutput1);
                            String combinedData = "Decoded value: \n" + data1 + data2;
                            TextView text = (TextView) findViewById(R.id.textView1);
                            text.setText(combinedData);


                        }
                    });


                }

            });

        }



    }

    /**
     * Takes in a bitmap value
     * and checks for white and black pixels.
     * Invert black pixels to white and vice versa.
     * @param  bitmap Bitmap
     *
     */
    public Bitmap Invert (Bitmap bitmap){
        Bitmap output = bitmap;
        int length = output.getWidth()*output.getHeight();
        int[] array = new int[length];
        output.getPixels(array,0,output.getWidth(),0,0,output.getWidth(),output.getHeight());
        for (int i=0;i<length;i++){

            if (array[i] == 0xff000000){
                array[i] = 0xffffffff;
            } else if (array[i] == 0xffffffff){
                array[i] = 0xff000000;
            }
        }
        output.setPixels(array,0,output.getWidth(),0,0,output.getWidth(),output.getHeight());
        return output;
    }

    /**
     * Takes in a bitmap and a File folder
     * and writes bitmap to file
     * @param bitmap
     * @param mImageFolder
     */
    public void writeToFile (Bitmap bitmap, File mImageFolder) {
        FileOutputStream out = null;
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend = "IMAGE_" + timestamp + "_";
        String s="";
        try {
            File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
            String mImageFileName = imageFile.getAbsolutePath();
            s = mImageFileName;
            out = new FileOutputStream(mImageFileName);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
                Intent mediaUpdateIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                mediaUpdateIntent.setData(Uri.fromFile(new File(s)));
                sendBroadcast(mediaUpdateIntent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Checks bitmap for Barcode data
     * and returns value stored inside.
     * @param bitmap
     * @return
     */
    public String getBarcodeData(Bitmap bitmap) {
        String barcodeData = "";
        Frame frame = new Frame.Builder().setBitmap(bitmap).build();
        BarcodeDetector barcodeDetector = new BarcodeDetector.Builder(getApplicationContext())
                .build();
        if(barcodeDetector.isOperational()){
            SparseArray<Barcode> sparseArray = barcodeDetector.detect(frame);
            if(sparseArray != null && sparseArray.size() > 0){
                for (int i = 0; i < sparseArray.size(); i++){
                    barcodeData = sparseArray.valueAt(i).rawValue;
                    //Toast.makeText(getApplicationContext(), sparseArray.valueAt(i).rawValue, Toast.LENGTH_SHORT).show();

                }
            }else {
                barcodeData = "NO QR Code Found.";
            }

        }
        return barcodeData;
    }
}
