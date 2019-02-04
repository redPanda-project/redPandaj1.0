package main.redanda.jobs;

import main.redanda.core.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Job implements Runnable {

    public static final long RERUNTIME = 500L;
    private static HashMap<Integer, Job> runningJobs = new HashMap<>(10);
    private static ReentrantLock runningJobsLock = new ReentrantLock();
    public static final Random rand = new Random();


    private int jobId = -1;
    private int runCounter = 0;
    private ScheduledFuture future;
    private boolean done = false;
    private boolean initilized = false;


    public abstract void init();

    @Override
    public void run() {

        if (getEstimatedRuntime() > 20000L) {
            //if this job takes too long, lets finish without
            System.out.println("job max time reached: " + jobId + " " + this.getClass().getName());
            done();
        }

        //lets run the init inside the run loop such that the init is runs in the threadpool
        // and not in the creating thread
        if (!initilized) {
            initilized = true;
            init();
        }

        work();
        //count after doing the work, since the first start of the job is immediately
        runCounter++;
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

        //lets set an jobId
        this.jobId = rand.nextInt();


        //run immediately
        JobScheduler.runNow(this);

        //run delayed recurrent
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
