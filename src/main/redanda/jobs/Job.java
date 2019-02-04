package main.redanda.jobs;

import main.redanda.core.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Job implements Runnable {

    public static final long RERUNTIME = 2000L;
    private static HashMap<Integer, Job> runningJobs = new HashMap<>(10);
    private static ReentrantLock runningJobsLock = new ReentrantLock();
    public static final Random rand = new Random();


    private Integer jobId;
    private int runCounter = 0;
    private ScheduledFuture future;
    private boolean done = false;

    public Job() {
        this.jobId = rand.nextInt();
    }


    @Override
    public void run() {
        //count before doing the work, since the first start of the job is delayed by RERUNTIME
        runCounter++;
        work();
    }

    //call this method if some data has been updated and you dont have to wait till the next rerun by delay
    public void updated() {
        //do not rise the runCounter, because this is an additional run beside the returning rerun by delay
        JobScheduler.runNow(this);
    }

    /**
     * work to do for this job, call done() if finished!
     */
    public abstract void work();

    public void start() {
        future = JobScheduler.insert(this, RERUNTIME);
        runningJobs.put(jobId, this);
    }

    public long getEstimatedRuntime() {
        return RERUNTIME * runCounter;
    }

    public void done() {

        if (done) {
            return;
        }

        done = true;


        runningJobsLock.lock();
        try {
            Job remove = runningJobs.remove(jobId);
            if (remove != null) {
                future.cancel(false);
            } else {
                //job already done, but we should never be in this case, run exception to debug this case
                throw new RuntimeException("CODE 17dh6");
            }
        } finally {
            runningJobsLock.unlock();
        }
        System.out.println("job finished: " + jobId);
    }


    public static Job getRunningJob(int jobId) {
        runningJobsLock.lock();
        try {
            return runningJobs.get(jobId);
        } finally {
            runningJobsLock.unlock();
        }
    }

    public Integer getJobId() {
        return jobId;
    }
}
