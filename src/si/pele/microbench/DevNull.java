/*
 * Written by Peter Levart <peter.levart@gmail.com>
 * and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */
package si.pele.microbench;

/**
 * A /dev/null sink for yielding results of test methods...
 *
 * @author peter
 */
public final class DevNull {

    // booleans

    private boolean
        b00, b01, b02, b03, b04, b05, b06, b07,
        b08, b09, b0A, b0B, b0C, b0D, b0E, b0F,
        b10, b11, b12, b13, b14, b15, b16, b17,
        b18, b19, b1A, b1B, b1C, b1D, b1E, b1F,
        b20, b21, b22, b23, b24, b25, b26, b27,
        b28, b29, b2A, b2B, b2C, b2D, b2E, b2F,
        b30, b31, b32, b33, b34, b35, b36, b37,
        b38, b39, b3A, b3B, b3C, b3D, b3E, b3F;

    public boolean b;

    public void yield(boolean b) {
        this.b = b;
    }

    private boolean
        b40, b41, b42, b43, b44, b45, b46, b47,
        b48, b49, b4A, b4B, b4C, b4D, b4E, b4F,
        b50, b51, b52, b53, b54, b55, b56, b57,
        b58, b59, b5A, b5B, b5C, b5D, b5E, b5F,
        b60, b61, b62, b63, b64, b65, b66, b67,
        b68, b69, b6A, b6B, b6C, b6D, b6E, b6F,
        b70, b71, b72, b73, b74, b75, b76, b77,
        b78, b79, b7A, b7B, b7C, b7D, b7E, b7F;

    // ints

    private int
        i00, i01, i02, i03, i04, i05, i06, i07,
        i08, i09, i0A, i0B, i0C, i0D, i0E, i0F;

    public int i;

    public void yield(int i) {
        this.i = i;
    }

    private int
        i10, i11, i12, i13, i14, i15, i16, i17,
        i18, i19, i1A, i1B, i1C, i1D, i1E, i1F;

    // longs

    private long
        l00, l01, l02, l03, l04, l05, l06, l07;

    public long l;

    public void yield(long l) {
        this.l = l;
    }

    private long
        l08, l09, l0A, l0B, l0C, l0D, l0E, l0F;

    // objects

    private Object
        o00, o01, o02, o03, o04, o05, o06, o07,
        o08, o09, o0A, o0B, o0C, o0D, o0E, o0F;

    public Object o;

    public void yield(Object o) {
        this.o = o;
    }

    private Object
        o10, o11, o12, o13, o14, o15, o16, o17,
        o18, o19, o1A, o1B, o1C, o1D, o1E, o1F;
}
