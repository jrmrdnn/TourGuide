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

/**
 * RewardsService is responsible for calculating rewards for users based on their visited locations
 * and the attractions they are near. It uses GpsUtil to get attraction data and RewardCentral to
 * manage reward points.
 */
@Slf4j
@Service
public class RewardsService {

    // Conversion factor from nautical miles to statute miles
    private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

    // proximity in miles
    private final int defaultProximityBuffer = 10;

    @Setter
    private int proximityBuffer = defaultProximityBuffer;

    // The range within which an attraction is considered for rewards
    private final int attractionProximityRange = 200;

    private final GpsUtil gpsUtil;
    private final RewardCentral rewardsCentral;

    /**
     * Constructor for RewardsService.
     *
     * @param gpsUtil        the GpsUtil instance to access GPS data
     * @param rewardCentral  the RewardCentral instance to manage rewards
     */
    public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
        this.gpsUtil = gpsUtil;
        this.rewardsCentral = rewardCentral;
    }

    /**
     * Sets the proximity buffer to the default value.
     */
    public void setDefaultProximityBuffer() {
        proximityBuffer = defaultProximityBuffer;
    }

    /**
     * Calculates rewards for a user based on their visited locations and nearby attractions.
     * It checks if the user is within proximity of any attractions and assigns reward points accordingly.
     *
     * @param user the user for whom to calculate rewards
     */
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

    /**
     * Calculates rewards for a list of users concurrently.
     * It uses a thread pool to process multiple users in parallel, improving performance for large user sets.
     *
     * @param users the list of users for whom to calculate rewards
     */
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

    /**
     * Checks if a user is within proximity of any attractions.
     *
     * @param attraction the attraction to check against
     * @param location   the user's current location
     * @return true if the user is near any attraction, false otherwise
     */
    public boolean isWithinAttractionProximity(Attraction attraction, Location location) {
        return getDistance(attraction, location) <= attractionProximityRange;
    }

    /**
     * Gets the reward points for a specific attraction and user.
     *
     * @param attraction the attraction for which to get reward points
     * @param user       the user for whom to get reward points
     * @return the reward points for the attraction and user
     */
    public int getRewardPoints(Attraction attraction, User user) {
        return rewardsCentral.getAttractionRewardPoints(attraction.attractionId, user.getUserId());
    }

    /**
     * Checks if a user is near an attraction based on their visited location.
     *
     * @param visitedLocation the user's visited location
     * @param attraction      the attraction to check against
     * @return true if the user is near the attraction, false otherwise
     */
    private boolean nearAttraction(VisitedLocation visitedLocation, Attraction attraction) {
        return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
    }

    /**
     * Gets the set of attraction IDs for which the user has already received rewards.
     *
     * @param user the user for whom to get rewarded attraction IDs
     * @return a set of UUIDs representing the rewarded attraction IDs
     */
    private Set<UUID> getRewardedAttractionIds(User user) {
        return user.getUserRewards().stream().map(r -> r.attraction.attractionId).collect(Collectors.toSet());
    }

    /**
     * Extracts attractions and their corresponding visited locations for a user.
     * It filters out attractions that the user has already been rewarded for.
     *
     * @param userLocations       the list of visited locations for the user
     * @param attractions         the list of all attractions
     * @param rewardedAttractionIds the set of attraction IDs that have already been rewarded
     * @return a map of attractions to their corresponding visited locations
     */
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

    /**
     * Calculates the distance between two locations using the Haversine formula.
     *
     * @param loc1 the first location
     * @param loc2 the second location
     * @return the distance in miles between the two locations
     */
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
