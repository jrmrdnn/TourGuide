package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import org.apache.commons.lang3.time.StopWatch;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import rewardCentral.RewardCentral;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestPerformance {

    /*
     * A note on performance improvements:
     *
     * The number of users generated for the high-volume tests can be easily
     * adjusted via this method:
     *
     * InternalTestHelper.setInternalUserNumber(100000);
     *
     *
     * These tests can be modified to suit new solutions, just as long as the
     * performance metrics at the end of the tests remain consistent.
     *
     * These are performance metrics that we are trying to hit:
     *
     * highVolumeTrackLocation: 100,000 users within 15 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(15) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     *
     * highVolumeGetRewards: 100,000 users within 20 minutes:
     * assertTrue(TimeUnit.MINUTES.toSeconds(20) >=
     * TimeUnit.MILLISECONDS.toSeconds(stopWatch.getTime()));
     */

    @Nested
    public class TestHighVolumeTrackLocation {

        @ParameterizedTest
        @ValueSource(ints = {10, 100, 1_000, 10_000, 100_000})
        public void highVolumeTrackLocation(int numberOfUsers) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            GpsUtil gpsUtil = new GpsUtil();
            RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

            InternalTestHelper.setInternalUserNumber(numberOfUsers);

            TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

            List<User> allUsers = tourGuideService.getAllUsers();

            tourGuideService.trackUserLocation(allUsers);

            stopWatch.stop();
            tourGuideService.tracker.stopTracking();

            System.out.println("highVolumeTrackLocation: Time Elapsed: " + TimeUnit.NANOSECONDS.toSeconds(stopWatch.getNanoTime()) + " seconds.");
            assertTrue(TimeUnit.MINUTES.toSeconds(15) >= TimeUnit.NANOSECONDS.toSeconds(stopWatch.getNanoTime()));
        }
    }

    @Nested
    public class TestHighVolumeGetRewards {

        @ParameterizedTest
        @ValueSource(ints = {10, 100, 1_000, 10_000, 100_000})
        public void highVolumeGetRewards(int numberOfUsers) {
            StopWatch stopWatch = new StopWatch();
            stopWatch.start();

            GpsUtil gpsUtil = new GpsUtil();
            RewardsService rewardsService = new RewardsService(gpsUtil, new RewardCentral());

            InternalTestHelper.setInternalUserNumber(numberOfUsers);

            TourGuideService tourGuideService = new TourGuideService(gpsUtil, rewardsService);

            Attraction attraction = gpsUtil.getAttractions().get(0);

            List<User> allUsers = tourGuideService.getAllUsers();

            allUsers.forEach(u -> u.addToVisitedLocations(new VisitedLocation(u.getUserId(), attraction, new Date())));

            rewardsService.calculateRewards(allUsers);

            for (User user : allUsers) assertFalse(user.getUserRewards().isEmpty());

            stopWatch.stop();
            tourGuideService.tracker.stopTracking();

            System.out.println("highVolumeGetRewards: Time Elapsed: " + TimeUnit.NANOSECONDS.toSeconds(stopWatch.getNanoTime()) + " seconds.");
            assertTrue(TimeUnit.MINUTES.toSeconds(20) >= TimeUnit.NANOSECONDS.toSeconds(stopWatch.getNanoTime()));
        }
    }
}
