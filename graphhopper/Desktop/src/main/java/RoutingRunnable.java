import com.graphhopper.PathWrapper;

/**
 * Created by zonglin on 11/3/16.
 */
class RoutingRunnable implements Runnable {
    private Trackable<RoutingRunnable> mCallback;
    private GeoPoint mStart, mEnd;
    private RoutingTest mRoutingEngine;
    private PathWrapper mOutput;
    private final long mIndex;

    public RoutingRunnable (long index, GeoPoint start, GeoPoint end, Trackable<RoutingRunnable> callBack,
                            final RoutingTest routeEngine) {
        mCallback = callBack;
        mStart = start;
        mEnd = end;
        mRoutingEngine = routeEngine;
        mIndex = index;
    }

    public void run() {
        mCallback.startCallBack(this);
        mOutput = mRoutingEngine.calcPath(mStart, mEnd);
        mCallback.doneCallBack(this);
    }

    public PathWrapper getOutput() {
        return mOutput;
    }

    public long getIndex() {
        return mIndex;
    }
}