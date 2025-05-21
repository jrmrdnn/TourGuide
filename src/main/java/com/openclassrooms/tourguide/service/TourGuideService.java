package com.openclassrooms.tourguide.service;

import com.openclassrooms.tourguide.dto.AttractionDto;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.tracker.Tracker;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tripPricer.Provider;
import tripPricer.TripPricer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * TourGuideService is responsible for managing user data, tracking user locations,
 * calculating rewards, and providing trip deals. It interacts with GpsUtil for GPS data
 * and TripPricer for trip pricing.
 */
@Slf4j
@Service
public class TourGuideService {
    private final GpsUtil gpsUtil;
    private final RewardsService rewardsService;
    private final TripPricer tripPricer = new TripPricer();
    public final Tracker tracker;

    // Set to true for testing purposes, false for production
    boolean testMode = true;

    /**
     * Constructor for TourGuideService.
     *
     * @param gpsUtil        the GpsUtil instance to access GPS data
     * @param rewardsService the RewardsService instance to manage rewards
     */
    public TourGuideService(GpsUtil gpsUtil, RewardsService rewardsService) {
        this.gpsUtil = gpsUtil;
        this.rewardsService = rewardsService;

        Locale.setDefault(Locale.US);

        if (testMode) {
            log.info("TestMode enabled");
            log.debug("Initializing users");
            initializeInternalUsers();
            log.debug("Finished initializing users");
        }
        tracker = new Tracker(this);
        addShutDownHook();
    }

    /**
     * Returns the list of user rewards for a given user.
     *
     * @param user the user for whom to retrieve rewards
     * @return the list of UserReward objects associated with the user
     */
    public List<UserReward> getUserRewards(User user) {
        return user.getUserRewards();
    }

    /**
     * Returns the current location of the user. If the user has no visited locations,
     * it tracks the user's location and returns it.
     *
     * @param user the user for whom to retrieve the location
     * @return the VisitedLocation object representing the user's current location
     */
    public VisitedLocation getUserLocation(User user) {
        return (!user.getVisitedLocations().isEmpty()) ? user.getLastVisitedLocation() : trackUserLocation(user);
    }

    /**
     * Returns the user object associated with the given userName.
     *
     * @param userName the name of the user to retrieve
     * @return the User object if found, null otherwise
     */
    public User getUser(String userName) {
        return internalUserMap.get(userName);
    }

    /**
     * Returns a list of all users in the system.
     *
     * @return a list of User objects
     */
    public List<User> getAllUsers() {
        return new ArrayList<>(internalUserMap.values());
    }

    /**
     * Adds a new user to the internal user map if the user does not already exist.
     *
     * @param user the User object to be added
     */
    public void addUser(User user) {
        if (!internalUserMap.containsKey(user.getUserName())) {
            internalUserMap.put(user.getUserName(), user);
        }
    }

    /**
     * Retrieves trip deals for a user based on their preferences and cumulative reward points.
     *
     * @param user the user for whom to retrieve trip deals
     * @return a list of Provider objects representing the trip deals
     */
    public List<Provider> getTripDeals(User user) {
        int cumulativeRewardPoints = user.getUserRewards().stream().mapToInt(UserReward::getRewardPoints).sum();
        List<Provider> providers = tripPricer.getPrice(tripPricerApiKey, user.getUserId(), user.getUserPreferences().getNumberOfAdults(), user.getUserPreferences().getNumberOfChildren(), user.getUserPreferences().getTripDuration(), cumulativeRewardPoints);
        user.setTripDeals(providers);
        return providers;
    }

    /**
     * Tracks the user's location by fetching it from the GPS utility and updating the user's visited locations.
     * It also calculates rewards for the user based on their new location.
     *
     * @param user the user whose location is to be tracked
     * @return the VisitedLocation object representing the user's current location
     */
    public VisitedLocation trackUserLocation(User user) {
        VisitedLocation visitedLocation = gpsUtil.getUserLocation(user.getUserId());
        user.addToVisitedLocations(visitedLocation);
        rewardsService.calculateRewards(user);
        return visitedLocation;
    }

    /**
     * Tracks the locations of multiple users concurrently using a thread pool.
     * It limits the number of threads to 100 or the number of users, whichever is smaller.
     *
     * @param users the list of users whose locations are to be tracked
     */
    public void trackUserLocation(List<User> users) {
        int threads = Math.min(users.size(), 100);
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (User user : users)
            executor.execute(() -> trackUserLocation(user));

        executor.shutdown();

        try {
            if (!executor.awaitTermination(20, TimeUnit.MINUTES)) {
                log.warn("Timeout while waiting for location tracking threads to finish");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Location tracking interrupted", e);
        }

        log.debug("All users locations tracked");
    }

    /**
     * Retrieves a list of nearby attractions for a given visited location.
     * The attractions are sorted by distance from the user's location, and only the closest 5 are returned.
     *
     * @param visitedLocation the VisitedLocation object representing the user's current location
     * @return a list of Attraction objects representing nearby attractions
     */
    public List<Attraction> getNearByAttractions(VisitedLocation visitedLocation) {
        List<Attraction> allAttractions = gpsUtil.getAttractions();

        allAttractions.sort((a1, a2) -> {
            double dist1 = getDistance(visitedLocation.location, a1);
            double dist2 = getDistance(visitedLocation.location, a2);
            return Double.compare(dist1, dist2);
        });

        return allAttractions.stream().limit(5).collect(Collectors.toList());
    }

    /**
     * Retrieves a list of AttractionDto objects for a given user.
     * Each AttractionDto contains information about the attraction, the user's location, and the distance to the attraction.
     *
     * @param userName the name of the user for whom to retrieve attractions
     * @return a list of AttractionDto objects
     */
    public List<AttractionDto> getListAttractionsDto(String userName) {
        User user = getUser(userName);

        VisitedLocation visitedLocation = getUserLocation(user);

        List<Attraction> attractions = getNearByAttractions(visitedLocation);

        List<AttractionDto> listAttractionsDto = new ArrayList<>();

        for (Attraction attraction : attractions) {
            AttractionDto attractionDto = new AttractionDto();
            attractionDto.setAttractionName(attraction.attractionName);
            attractionDto.setAttractionLatitude(attraction.latitude);
            attractionDto.setAttractionLongitude(attraction.longitude);
            attractionDto.setUserLatitude(visitedLocation.location.latitude);
            attractionDto.setUserLongitude(visitedLocation.location.longitude);
            attractionDto.setDistanceInMiles(getDistance(visitedLocation.location, attraction));
            attractionDto.setRewardPoints(rewardsService.getRewardPoints(attraction, user));

            listAttractionsDto.add(attractionDto);
        }
        return listAttractionsDto;
    }

    /**
     * Adds a shutdown hook to stop the tracker when the application is terminated.
     */
    private void addShutDownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                tracker.stopTracking();
            }
        });
    }

    /**
     * Calculates the distance between a user's location and an attraction.
     *
     * @param userLocation the user's current location
     * @param attraction   the attraction to calculate the distance to
     * @return the distance in miles between the user's location and the attraction
     */
    private double getDistance(Location userLocation, Attraction attraction) {
        return rewardsService.getDistance(userLocation, new Location(attraction.latitude, attraction.longitude));
    }

    /**********************************************************************************
     * Methods Below: For Internal Testing
     **********************************************************************************/
    private static final String tripPricerApiKey = "test-server-api-key";
    // Database connection will be used for external users, but for testing purposes
    // internal users are provided and stored in memory
    private final Map<String, User> internalUserMap = new HashMap<>();

    /**
     * Initializes internal users for testing purposes.
     * Creates a specified number of internal users with random locations and adds them to the internalUserMap.
     */
    private void initializeInternalUsers() {
        IntStream.range(0, InternalTestHelper.getInternalUserNumber()).forEach(i -> {
            String userName = "internalUser" + i;
            String phone = "000";
            String email = userName + "@tourGuide.com";
            User user = new User(UUID.randomUUID(), userName, phone, email);
            generateUserLocationHistory(user);

            internalUserMap.put(userName, user);
        });
        log.debug("Created {} internal test users.", InternalTestHelper.getInternalUserNumber());
    }

    /**
     * Generates a random user location history for a given user.
     * Adds three visited locations with random latitudes, longitudes, and timestamps to the user's visited locations.
     *
     * @param user the user for whom to generate the location history
     */
    private void generateUserLocationHistory(User user) {
        IntStream.range(0, 3).forEach(i -> {
            user.addToVisitedLocations(new VisitedLocation(user.getUserId(), new Location(generateRandomLatitude(), generateRandomLongitude()), getRandomTime()));
        });
    }

    /**
     * Generates a random longitude value between -180 and 180 degrees.
     *
     * @return a random longitude value
     */
    private double generateRandomLongitude() {
        double leftLimit = -180;
        double rightLimit = 180;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    /**
     * Generates a random latitude value between -85.05112878 and 85.05112878 degrees.
     *
     * @return a random latitude value
     */
    private double generateRandomLatitude() {
        double leftLimit = -85.05112878;
        double rightLimit = 85.05112878;
        return leftLimit + new Random().nextDouble() * (rightLimit - leftLimit);
    }

    /**
     * Generates a random time within the last 30 days.
     *
     * @return a Date object representing a random time in the past 30 days
     */
    private Date getRandomTime() {
        LocalDateTime localDateTime = LocalDateTime.now().minusDays(new Random().nextInt(30));
        return Date.from(localDateTime.toInstant(ZoneOffset.UTC));
    }

}
