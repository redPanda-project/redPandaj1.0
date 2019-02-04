package main.redanda.jobs;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class JobScheduler extends ScheduledThreadPoolExecutor {

//    public static ScheduledThreadPoolExecutor threadpool = new (1);


    private static JobScheduler jobScheduler;

    static {
        jobScheduler = new JobScheduler(10);
        jobScheduler.setKeepAliveTime(60, TimeUnit.SECONDS);
        jobScheduler.allowCoreThreadTimeOut(true);
    }

    public JobScheduler(int corePoolSize) {
        super(corePoolSize);
    }


    public static ScheduledFuture insert(Runnable runnable, long delayInMS) {
        return jobScheduler.scheduleWithFixedDelay(runnable, delayInMS, delayInMS, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void beforeExecute(Thread t, Runnable r) {

    }

    @Override
    protected void terminated() {

    }

    public static void runNow(Runnable command) {
        jobScheduler.execute(command);
    }
}
