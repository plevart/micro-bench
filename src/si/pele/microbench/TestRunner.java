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
            final AtomicInteger sync = this.sync;
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

    public static class Result {
        public final String testName;
        public final int threads;
        public final double nsPerOpAvg, nsPerOpSigma;

        private final long[] opss, nanoss;
        private final double nsPerOps[];

        public Result(String testName, int threads, long[] opss, long[] nanoss) {
            this.testName = testName;
            this.threads = threads;
            this.opss = opss;
            this.nanoss = nanoss;

            long opsSum = 0L;
            long nanosSum = 0L;
            for (int i = 0; i < threads; i++) {
                opsSum += opss[i];
                nanosSum += nanoss[i];
            }
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
            return toString(true);
        }

        public String toString(boolean dumpIndividualThreads) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("%12d threads, Tavg = %,9.2f ns/op (σ = %,6.2f ns/op)", threads, nsPerOpAvg, nsPerOpSigma));
            if (dumpIndividualThreads) {
                sb.append(" [");
                for (int i = 0; i < nsPerOps.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(String.format("%,9.2f", nsPerOps[i]));
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