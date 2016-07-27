package us.biotyp.appelmannetje;

public class Chronograph {

    private long startTime;
    private long stopTime;
    private long duration;

    public Chronograph(){
        // Set startTime
        startTime = System.nanoTime();
    }
    public void reset(){
        startTime = 0;
    }
    public void start(){
        startTime = System.nanoTime();
    }
    public void stop(){
        stopTime = System.nanoTime();
        duration = stopTime - startTime;
    }
    public double getElapsedTime(){
        return (double)duration / Math.pow(10,9);
    }

    public String getElapsedTimeString(){
        return ( Double.toString( (double)duration / Math.pow(10,9) ) + "s" );
    }
}

