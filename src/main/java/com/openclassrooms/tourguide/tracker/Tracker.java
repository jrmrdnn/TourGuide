package com.openclassrooms.tourguide.tracker;

import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Tracker is a thread that periodically tracks the location of all users in the TourGuideService.
 * It runs every 5 minutes by default, but this interval can be adjusted if needed.
 */
@Slf4j
public class Tracker extends Thread {
    private static final long trackingPollingInterval = TimeUnit.MINUTES.toSeconds(5);
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final TourGuideService tourGuideService;
    private boolean stop = false;

    /**
     * Constructor for Tracker.
     *
     * @param tourGuideService the TourGuideService instance to track user locations
     */
    public Tracker(TourGuideService tourGuideService) {
        this.tourGuideService = tourGuideService;

        executorService.submit(this);
    }

    /**
     * Assures to shut down the Tracker thread
     */
    public void stopTracking() {
        stop = true;
        executorService.shutdownNow();
    }

    /**
     * The run method of the Tracker thread.
     * It continuously tracks user locations at specified intervals until interrupted or stopped.
     */
    @Override
    public void run() {
        StopWatch stopWatch = new StopWatch();
        while (true) {
            if (Thread.currentThread().isInterrupted() || stop) {
                log.debug("Tracker stopping");
                break;
            }

            List<User> users = tourGuideService.getAllUsers();
            log.debug("Begin Tracker. Tracking {} users.", users.size());
            stopWatch.start();
            users.forEach(tourGuideService::trackUserLocation);
            stopWatch.stop();
            log.debug("Tracker Time Elapsed: {} seconds.", stopWatch.getTime(TimeUnit.SECONDS));
            stopWatch.reset();
            try {
                log.debug("Tracker sleeping");
                TimeUnit.SECONDS.sleep(trackingPollingInterval);
            } catch (InterruptedException e) {
                break;
            }
        }

    }
}
