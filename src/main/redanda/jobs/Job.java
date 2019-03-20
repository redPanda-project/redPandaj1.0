package main.redanda.jobs;

import main.redanda.core.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.ReentrantLock;

public abstract class Job implements Runnable {

    //    public static final long RERUNTIME = 500L;
    private static HashMap<Integer, Job> runningJobs = new HashMap<>(10);
    private static ReentrantLock runningJobsLock = new ReentrantLock();
    public static final Random rand = new Random();


    protected long reRunDelay = 500L; //default value
    protected boolean permanent = false;

    private int jobId = -1;
    private int runCounter = 0;
    private ScheduledFuture future;
    private boolean done = false;
    protected boolean initilized = false;


    @Override
    public void run() {

        if (!permanent && getEstimatedRuntime() > 20000L) {
            //if this job takes too long, lets finish
            System.out.println("job max time reached: " + jobId + " " + this.getClass().getName());
            done();
        }

        //lets run the init inside the run loop such that the init is runs in the threadpool
        // and not in the creating thread
        if (!initilized) {
            initilized = true;
            try {
                init();
            } catch (Throwable e) {
                e.printStackTrace();
                done();
                return;
            }
        }

        if (!initilized) {
            //if the init method failed, (protected var set to false),
            // we retry the init in the next run
            return;
        }

        try {
            work();
        } catch (Throwable e) {
            e.printStackTrace();
            done();
            return;
        }
        //count after doing the work, since the first start of the job is immediately
        runCounter++;
    }

    /**
     * call this method if some data has been updated for this job
     * and you do not want to wait till the next rerun occurs by delay
     */
    public void updated() {
        //do not rise the runCounter, because this is an additional run beside the returning rerun by delay
        JobScheduler.runNow(this);
    }


    /**
     * stuff to be done before the work method is called, this code only runs once at the beginning
     * put stuff here so it will run in the threadpool and not in the calling thread
     */
    public abstract void init();

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
        future = JobScheduler.insert(this, reRunDelay);
        runningJobs.put(jobId, this);
    }

    /**
     * estimated time in ms
     * @return
     */
    public long getEstimatedRuntime() {
        return reRunDelay * runCounter;
    }

    /**
     * call this method if the job is finished, does the necessary cleanup,
     * ie remove from running jobs and cancel the future (obtained from scheduleWithFixedDelay)
     */
    public void done() {

        if (done) {
            return;
        }

        done = true;

        //remove this job from the runningJobs
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


    /**
     * retrieves a running job with a given jobId, useful if we get an answer by a peer and
     * we want to obtain the associated job to update the data
     *
     * @param jobId
     * @return
     */
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
