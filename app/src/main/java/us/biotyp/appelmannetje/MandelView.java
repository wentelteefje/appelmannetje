package us.biotyp.appelmannetje;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class MandelView extends SurfaceView implements Runnable {
    // TODO: clarify member accessibility
    Thread t = null;
    SurfaceHolder holder;
    boolean active = false;
    Bitmap plot;
    int[] mandelPixels;
    int maxIter = 30;

    private GestureDetector detector;

    // zoom factor
    private double z = 1.0;

    // scaling factors and translation for real and imaginary parts
    double sr;
    double si;
    double tr;
    double ti;
    double xlen = 3.5;
    double ylen = 2.0;

    // user touch coordinates
    float xf;
    float yf;


    public MandelView(Context context) {
        super(context);
        detector = new GestureDetector(context, new DoubleTabListener());
        holder = getHolder();
    }

    class DoubleTabListener extends GestureDetector.SimpleOnGestureListener {
        private static final String DEBUG_TAG = "Gestures";

        @Override
        public boolean onDown(MotionEvent event) {
            Log.d(DEBUG_TAG,"onDown: " + event.toString());
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent event) {
            Log.d(DEBUG_TAG, "onDoubleTap: " + event.toString());

            // Get coordinates of the doubleTap for the new origin
            xf = event.getX();
            yf = event.getY();

            // TODO: Set zoom factor as static member
            // Set zoom factor
            z = 2;

            // Start re-rendering
            resume();

            return true;
        }
    }


    public void run() {
        while (active) {
            if (!holder.getSurface().isValid()) {
                // skip canvas drawing
                continue;
            }

            Canvas c = holder.lockCanvas();
            Bitmap.Config bc = Bitmap.Config.ARGB_8888;


            // TODO: Fix this mess
            if(z == 1.0) {
                // Calculate scaling factors
                // This ensures that at beginning the points will be in the "Mandelbrot-Rectangle" [-2.5,1]x[-1,1]
                sr = 3.5D / ( c.getWidth() );
                si = 2D / ( c.getHeight() );
                // The screen coordinate (0,0) needs to be translated to the lower left corner
                // of the Mandelbrot-rectangle, that is: (0,0) -> (-2.5,-1)
                tr = -2.5;
                ti = -1;
            }else{
                // translate new origin to plane coordinates first
                xf = (float)(sr * xf + tr);
                yf = (float)(si * yf + ti);

                // apply zoom
                sr *= (1 / z);
                si *= (1 / z);
                xlen = xlen / z;
                ylen = ylen / z;

                // calculate translation that maps (0, 0) to the lower left point of the new interval
                tr = Math.min(xf + xlen/2, xf - xlen/2);
                ti = Math.min(yf + ylen/2, yf - ylen/2);
            }


            // Calculate the image and measure the elapsed time
            final Chronograph cg = new Chronograph();
            createMandel(c);
            cg.stop();
            // ********************


            // Make a Toast with the elapsed time
            makeToast( cg.getElapsedTimeString() );

            // Create the Bitmap and draw
            plot = Bitmap.createBitmap(mandelPixels, c.getWidth(), c.getHeight(), bc);
            c.drawBitmap(plot, new Matrix(), new Paint());
            holder.unlockCanvasAndPost(c);

            active = false;
        }

    }

    public void makeToast(final String msg){
        Handler handler = new Handler(Looper.getMainLooper());

        handler.post(new Runnable() {

            @Override
            public void run() {
                Toast.makeText(getContext(), msg, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void pause() {
        active = false;
        while (true) {
            try {
                t.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            break;
        }
        t = null;
    }

    public void resume() {
        active = true;
        t = new Thread(this);
        t.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        //mScaleDetector.onTouchEvent(event);
        detector.onTouchEvent(event);
        Log.d("test", event.toString());
        return true;
    }

    public int colorgradient(int iter){
        if(iter >= maxIter){
            return Color.BLACK;
        }else {
            // #b8b8ff
            int r1 = 184;
            int g1 = 184;
            int b1 = 255;
            // #e75a7c
            int r2 = 231;
            int g2 = 90;
            int b2 = 124;

            int r, g, b;

            double nu = (double)iter / (double)maxIter;

            // Linear interpolation and some magic
            r = (int) (r1 * nu + (1 - nu) * Math.cos( 7 * Math.PI * r2) );
            g = (int) (g1 * nu + (1 - nu) * g2);
            b = (int) (b1 * nu + (1 - nu) * b2);

            return Color.rgb(r, g, b);
        }
    }

    public void createMandel(Canvas canvas) {
        mandelPixels = new int[canvas.getWidth() * canvas.getHeight()];

        double zr, zi;
        double cr, ci;
        double zrsq, zisq;
        double q;
        double p;


        // Keeps track of number of completed iterations
        int iter;
        // Used to position the points in the bitmap color array
        int pos = -1;

        for (int j = 0; j < canvas.getHeight(); j++) {
            for (int i = 0; i < canvas.getWidth(); i++) {
                pos++;

                // Transform screen coordinates to coordinates in the complex plane
                cr = sr * i + tr;
                ci = si * j + ti;

                zr = 0;
                zi = 0;

                zrsq = 0;
                zisq = 0;

                iter = 0;

                // Cardioid check
                q = (cr - 0.25) * (cr - 0.25) + (ci * ci);
                q *= (q + (cr - 0.25));
                // period-2 bulb check
                p = (cr + 1) * (cr + 1) + (ci * ci);

                if( p <= 0.0625 ) {
                    // the point lies in within the period-2 bulb, that is in
                    // the circle with r = 1/4 centered at c = -3/4 + 0i
                    iter = maxIter;
                }else if ( q <= 0.25 * (ci * ci) ) {
                    // the point lies within the cardioid
                    iter = maxIter;
                }else {
                    while ((zrsq + zisq <= 4) && (iter <= maxIter)) {
                        // Im(z) = 2*Re(z)*Im(z) + Im(c)
                        zi = zi * zr;
                        zi += zi;
                        zi += ci;

                        // Re(z) = Re(z)^2 - Im(z)^2 + Re(c)
                        zr = zrsq - zisq + cr;

                        // Calculate new squares
                        zrsq = zr * zr;
                        zisq = zi * zi;

                        iter++;
                    }
                }
                // Set pixel color relative to completed iterations
                mandelPixels[pos] = colorgradient(iter);
            }

        }
    }

}