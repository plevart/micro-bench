package si.pele.microbench;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * A simple micro benchmark test runner
 *
 * @author peter.levart@gmail.com
 */
public class TestRunner {

    public static abstract class Test extends Thread {

        private AtomicInteger sync;
        long ops, nanos;

        final void start(AtomicInteger startSync) {
            this.sync = startSync;
            super.start();
        }

        public DevNull[] devNulls;

        @Override
        public void run() {
            final DevNull devNull1 = new DevNull();
            final DevNull devNull2 = new DevNull();
            final DevNull devNull3 = new DevNull();
            final DevNull devNull4 = new DevNull();
            final DevNull devNull5 = new DevNull();
            // make sure escape analysis finds devNulls as escaped!
            devNulls = new DevNull[]{devNull1, devNull2, devNull3, devNull4, devNull5};
            init();
            for (
                int remaining = sync.decrementAndGet();
                remaining > 0;
                remaining = sync.get()
                ) {
                doOp(devNull1, devNull2, devNull3, devNull4, devNull5);
            }
            long ops = 0L;
            long t0 = System.nanoTime();
            while (sync.get() == 0) {
                doOp(devNull1, devNull2, devNull3, devNull4, devNull5);
                ops++;
            }
            long t1 = System.nanoTime();
            for (
                int remaining = sync.decrementAndGet();
                remaining > 0;
                remaining = sync.get()
                ) {
                doOp(devNull1, devNull2, devNull3, devNull4, devNull5);
            }
            this.ops = ops;
            this.nanos = t1 - t0;
        }

        protected void init() { }

        protected abstract void doOp(DevNull devNull1, DevNull devNull2, DevNull devNull3, DevNull devNull4, DevNull devNull5);
    }

    public static final class DevNull {

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

    public static class Result {
        public final String testName;
        public final int threads;
        public final double nsPerOpAvg, nsPerOpSigma;
        private final double nsPerOps[];

        public Result(String testName, int threads, long[] opss, long[] nanoss) {
            this.testName = testName;
            this.threads = threads;

            long opsSum = 0L;
            long nanosSum = 0L;
            double nsPerOpAvg = (double) nanosSum / (double) opsSum;
            double nsPerOpVar = 0d;
            double nsPerOps[] = new double[threads];
            for (int i = 0; i < threads; i++) {
                double nsPerOp = (double) nanoss[i] / (double) opss[i];
                nsPerOps[i] = nsPerOp;
                double nsPerOpDiff = nsPerOpAvg - nsPerOp;
                nsPerOpVar += nsPerOpDiff * nsPerOpDiff;
            }
            nsPerOpVar /= (double) threads;

            this.nsPerOpAvg = nsPerOpAvg;
            this.nsPerOpSigma = Math.sqrt(nsPerOpVar);
            this.nsPerOps = nsPerOps;
        }

        @Override
        public String toString() {
            return toString(false);
        }

        public String toString(boolean dumpIndividualThreads) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%12d threads, Tavg = %,9.2f ns/op (σ = %,6.2f ns/op)", threads, nsPerOpAvg, nsPerOpSigma));
            if (dumpIndividualThreads) {
                sb.append(" [");
                for (int i = 0; i < nsPerOps.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(String.format("%,9.2f ns/op", nsPerOps[i]));
                }
                sb.append("]");
            }
            return sb.toString();
        }
    }

    protected static Result runTest(Supplier<? extends Test> testFactory, long runDurationMillis, int threads) throws InterruptedException {
        Test[] tests = new Test[threads];
        for (int i = 0; i < threads; i++)
            tests[i] = testFactory.get();
        AtomicInteger sync = new AtomicInteger(threads);
        for (Test test : tests)
            test.start(sync);
        Thread.sleep(runDurationMillis);
        while (sync.get() > 0)
            Thread.sleep(10); // in case runDurationMillis was to small
        sync.set(threads);
        long[] opss = new long[threads];
        long[] nanoss = new long[threads];
        for (int i = 0; i < threads; i++) {
            Test test = tests[i];
            test.join();
            opss[i] = test.ops;
            nanoss[i] = test.nanos;
        }
        return new Result(tests[0].getClass().getSimpleName(), threads, opss, nanoss);
    }

    protected static Result runTest(final Class<? extends Test> testClass, long runDurationMillis, int threads) throws InterruptedException {
        return runTest(
            new Supplier<Test>() {
                @Override
                public Test get() {
                    try {
                        return testClass.newInstance();
                    }
                    catch (InstantiationException | IllegalAccessException e) {
                        throw new RuntimeException(e.getMessage(), e);
                    }
                }
            },
            runDurationMillis,
            threads
        );
    }

    protected static void doTest(Class<? extends Test> testClass, long runDurationMillis, int minThreads, int maxThreads, int stepThreads) throws InterruptedException {
        System.out.println("#");
        System.out.printf("# %s: run duration: %,6d ms, #of logical CPUS: %d\n", testClass.getSimpleName(), runDurationMillis, Runtime.getRuntime().availableProcessors());
        System.out.println("#");
        System.out.println("# Warm up:");
        System.out.println(runTest(testClass, runDurationMillis, minThreads));
        System.out.println(runTest(testClass, runDurationMillis, minThreads));

        System.out.println("# Measure:");
        for (int threads = minThreads; threads <= maxThreads; threads += stepThreads) {
            System.out.println(runTest(testClass, runDurationMillis, threads));
        }
        System.out.println();
    }
}