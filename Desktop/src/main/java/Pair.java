/**
 * Created by zonglin on 11/3/16.
 */
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
