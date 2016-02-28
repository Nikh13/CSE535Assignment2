package cse535.mobilecomputing.assignment2;

/**
 * Created by Nikhil on 2/27/16.
 */
public class Record {
    public int timestamp;
    public float xVal;
    public float yVal;
    public float zVal;

    public Record(int timestamp, float xVal, float yVal, float zVal){
        this.timestamp = timestamp;
        this.xVal = xVal;
        this.yVal = yVal;
        this.zVal = zVal;
    }
}
