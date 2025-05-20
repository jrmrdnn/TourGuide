package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import rewardCentral.RewardCentral;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RewardsService {

    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    // proximity in miles
    private final int defaultProximityBuffer = 10;

    @Setter
    private int proximityBuffer = defaultProximityBuffer;

    private final int attractionProximityRange = 200;
    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;

    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    public void calculateRewards(User user) {
        List<VisitedLocation> userLocations = user.getVisitedLocations();
        List<Attraction> attractions = gpsUtil.getAttractions();
        Set<UUID> rewardedAttractionIds = getRewardedAttractionIds(user);

        Map<Attraction, VisitedLocation> attractionVisitedLocationMap = getExtractedAttractionsAndVisitedLocation(userLocations, attractions, rewardedAttractionIds);

        for (Map.Entry<Attraction, VisitedLocation> entry : attractionVisitedLocationMap.entrySet()) {
            Attraction attraction = entry.getKey();
            VisitedLocation visitedLocation = entry.getValue();
            int rewardPoints = getRewardPoints(attraction, user);
            user.addUserReward(new UserReward(visitedLocation, attraction, rewardPoints));
        }
    }

    public void calculateRewards(List<User> users) {
        int threads = Math.min(users.size(), 100);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (User user : users) executor.execute(() -> calculateRewards(user));

        executor.shutdown();

        try {
            if (!executor.awaitTermination(20, TimeUnit.MINUTES)) {
                log.warn("Timeout while waiting for reward calculation threads to finish");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Rewards calculation interrupted", e);
        }

        log.debug("All users rewards calculated");
    }

    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) <= attractionProximityRange;
    }

    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
    }

    private Set<UUID> getRewardedAttractionIds(User user) {
        return user.getUserRewards().stream().map(r -> r.attraction.attractionId).collect(Collectors.toSet());
    }

    private Map<Attraction, VisitedLocation> getExtractedAttractionsAndVisitedLocation(List<VisitedLocation> userLocations, List<Attraction> attractions, Set<UUID> rewardedAttractionIds) {
        Map<Attraction, VisitedLocation> attractionVisitedLocationMap = new HashMap<>();

        for (VisitedLocation visitedLocation : userLocations) {
            for (Attraction attraction : attractions) {
                if (!rewardedAttractionIds.contains(attraction.attractionId) && nearAttraction(visitedLocation, attraction)) {
                    attractionVisitedLocationMap.put(attraction, visitedLocation);
                }
            }
        }

        return attractionVisitedLocationMap;
    }

    public double getDistance(Location loc1, Location loc2) {
        double lat1 = Math.toRadians(loc1.latitude);
        double lon1 = Math.toRadians(loc1.longitude);
        double lat2 = Math.toRadians(loc2.latitude);
        double lon2 = Math.toRadians(loc2.longitude);

        double angle = Math.acos(Math.sin(lat1) * Math.sin(lat2) + Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

        double nauticalMiles = 60 * Math.toDegrees(angle);
        return STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
    }
}
