package us.biotyp.appelmannetje;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

public class MandelView extends SurfaceView implements Runnable {
    Thread t = null;
    SurfaceHolder holder;
    boolean active = false;
    Bitmap plot;
    int[] mandelPixels;
    int maxIter = 30;

    // Saving the Context for Toasting
    private Context context;

    public MandelView(Context context) {
        super(context);
        holder = getHolder();
        this.context = context.getApplicationContext();
    }

    public void run() {
        while (active) {
            if (!holder.getSurface().isValid()) {
                // skip canvas drawing
                continue;
            }

            Canvas c = holder.lockCanvas();
            Bitmap.Config bc = Bitmap.Config.ARGB_8888;



            // Calculate the image and measure the elapsed time
            final Chronograph cg = new Chronograph();
            createMandel(c);
            cg.stop();
            // ********************


            // Make a Toast with the elapsed time
            Handler handler = new Handler(Looper.getMainLooper());

            handler.post(new Runnable() {

                @Override
                public void run() {
                    Toast.makeText(getContext(), cg.getElapsedTimeString(), Toast.LENGTH_LONG).show();
                }
            });
            // ***********************************

            // Create the Bitmap and draw
            plot = Bitmap.createBitmap(mandelPixels, c.getWidth(), c.getHeight(), bc);
            c.drawBitmap(plot, new Matrix(), new Paint());
            holder.unlockCanvasAndPost(c);

            active=false;
        }

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

        // Calculate scaling factors
        // This ensures that the points will be in the "Mandelbrot-Rectangle" [-2.5,1]x[-1,1]
        double f_re = 3.5D/canvas.getWidth();
        double f_im = 2D/canvas.getHeight();
        // The screen coordinate (0,0) needs to be translated to the lower left corner
        // of the Mandelbrot-rectangle, that is: (0,0) -> (-2.5,-1)
        double t_re = -2.5;
        double t_im = -1;

        double zr, zi;
        double cr = 0;
        double ci = 0;
        double zrsq;
        double zisq;
        double q;
        double p;

        // Keeps track of number of completed iterations
        int iter;
        // Used to position the points in the bitmap color array
        int pos = -1;

        for (int j = 0; j < canvas.getHeight(); j++) {
            for (int i = 0; i < canvas.getWidth(); i++) {
                pos++;
                cr = f_re * i + t_re;
                ci = f_im * j + t_im;
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

                if( p <= (1/16) ) {
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
