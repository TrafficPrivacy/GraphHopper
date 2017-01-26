import org.mapsforge.core.model.LatLong;

/**
 * Created by zonglin on 11/3/16.
 */
class Pair {
    public LatLong mDota, mDotb;
    public Pair(LatLong dota, LatLong dotb) {
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
        int result = mDota.hashCode() + mDotb.hashCode();
        return result;
    }

    public String toString() {
        return "[ " + mDota + ", " + mDotb + " ]";
    }
}
