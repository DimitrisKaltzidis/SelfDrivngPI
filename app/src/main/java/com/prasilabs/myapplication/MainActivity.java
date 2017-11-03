package com.prasilabs.myapplication;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {

    private static final String TAG = "MainActivity";

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private JavaCameraView cameraView;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            super.onManagerConnected(status);

            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    cameraView.enableView();
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        Log.i(TAG, stringFromJNI());

        cameraView = findViewById(R.id.camera_view);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    @Override
    public void onCameraViewStarted(int i, int i1) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame viewFrame) {

        //Apply transform here.

        Mat grey = doGrey(viewFrame.rgba());
        //Mat gausianBlur = doGausianBlur(grey);

        Mat edges = doCanny(grey);


        Mat coloredEdges = colorCanny(edges);
        Mat hough = applyHoughTransform(edges);

        plotTheLines(coloredEdges, hough);

        return coloredEdges;
    }

    private Mat doGrey(Mat source) {

        Mat grey = new Mat();
        Imgproc.cvtColor(source, grey, Imgproc.COLOR_BGR2GRAY);
        source.release();

        return grey;
    }

    private void plotTheLines(Mat colouredEdge, Mat hough) {

        double[] data;
        double rho, theta;
        Point pt1 = new Point();
        Point pt2 = new Point();
        double a, b;
        double x0, y0;

        for (int i = 0; i < hough.cols(); i++) {

            data = hough.get(0, i);
            if (data != null && data.length > 1) {
                rho = data[0];
                theta = data[1];

                a = Math.cos(theta);
                b = Math.sin(theta);

                x0 = a * rho;
                y0 = b * rho;

                pt1.x = Math.round(x0 + 1000 * (-b));
                pt1.y = Math.round(y0 + 1000 * (a));
                pt2.x = Math.round(x0 - 1000 * (-b));
                pt2.y = Math.round(y0 - 1000 * (a));

                Imgproc.line(colouredEdge, pt1, pt2, new Scalar(0, 0, 255), 6);
            } else {
                Log.w(TAG, "data size is null");
            }
        }
    }

    private Mat applyHoughTransform(Mat source) {

        Mat hough = new Mat();

        Imgproc.HoughLines(source, hough, 2, Math.PI / 45, 20);

        return hough;

    }

    private Mat colorCanny(Mat cannyMat) {
        Mat colouredCanny = new Mat();

        Imgproc.cvtColor(cannyMat, colouredCanny, Imgproc.COLOR_GRAY2BGR);

        return colouredCanny;
    }

    private Mat doCanny(Mat source) {

        Mat edges = new Mat();
        Imgproc.Canny(source, edges, 30, 180);

        source.release();

        return edges;
    }

    private Mat doGausianBlur(Mat source) {
        Mat blur = new Mat();

        Imgproc.GaussianBlur(source, blur, new Size(45, 45), 0);

        source.release();

        return blur;
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void disableCamera() {
        if (cameraView != null) {
            cameraView.disableView();
        }
    }
}
