package main.redanda.jobs;

import main.redanda.kademlia.KadStoreManager;

public class KadRefreshJob extends Job {


    public KadRefreshJob() {
        permanent = true;
        reRunDelay = 1000L * 60L * 60L * 1L; // 1 hr
    }

    @Override
    public void init() {
    }

    @Override
    public void work() {

        System.out.println("refresh the KadContent");
        KadStoreManager.maintain();

    }
}
