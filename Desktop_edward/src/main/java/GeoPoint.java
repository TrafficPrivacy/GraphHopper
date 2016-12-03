/**
 * Created by zonglin on 11/3/16.
 */
class GeoPoint {
    private final double mLatitude;
    private final double mLongitude;

    public GeoPoint(final double latitude, final double longitude) {
        mLatitude = latitude;
        mLongitude = longitude;
    }

    public double getLatitude() {
        return mLatitude;
    }

    public double getLongitude() {
        return mLongitude;
    }

    public int getLatitudeE6() {
        return (int) (mLatitude * 10000);
    }

    public int getLongitudeE6() {
        return (int) (mLongitude * 10000);
    }

    public String toString() {
        String result = "(";
        result += mLatitude + ", " + mLongitude + ")";
        return result;
    }

    public boolean equals(Object other) {
        return ((GeoPoint)other).mLatitude == mLatitude && ((GeoPoint)other).mLongitude == mLongitude;
    }
}
