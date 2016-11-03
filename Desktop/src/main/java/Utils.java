
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
}

class Pair {
    public GeoPoint mDota, mDotb;
    public Pair(GeoPoint dota, GeoPoint dotb) {
        mDota = dota;
        mDotb = dotb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Pair)) return false;
        Pair other = (Pair) o;
        return other.mDota.equals(mDota) && other.mDotb.equals(mDotb);
    }

    @Override
    public int hashCode() {
        int result = (mDota.getLatitudeE6() + mDota.getLongitudeE6()) / 37 +
                (mDotb.getLatitudeE6() + mDotb.getLongitudeE6()) / 31;
        return result;
    }
}