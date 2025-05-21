package com.openclassrooms.tourguide.user;

import gpsUtil.location.VisitedLocation;
import lombok.Data;
import tripPricer.Provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * User is a class that represents a user in the Tour Guide application.
 * It contains user details, visited locations, rewards, preferences, and trip deals.
 */
@Data
public class User {
    private final UUID userId;
    private final String userName;
    private String phoneNumber;
    private String emailAddress;
    private Date latestLocationTimestamp;
    private final List<VisitedLocation> visitedLocations = new ArrayList<>();
    private final List<UserReward> userRewards = new ArrayList<>();
    private UserPreferences userPreferences = new UserPreferences();
    private List<Provider> tripDeals = new ArrayList<>();

    /**
     * Constructor for User.
     *
     * @param userId        the unique identifier for the user
     * @param userName      the name of the user
     * @param phoneNumber   the phone number of the user
     * @param emailAddress  the email address of the user
     */
    public User(UUID userId, String userName, String phoneNumber, String emailAddress) {
        this.userId = userId;
        this.userName = userName;
        this.phoneNumber = phoneNumber;
        this.emailAddress = emailAddress;
    }

    /**
     * Adds a visited location to the user's list of visited locations.
     *
     * @param visitedLocation the VisitedLocation object to be added
     */
    public void addToVisitedLocations(VisitedLocation visitedLocation) {
        visitedLocations.add(visitedLocation);
    }

    /**
     * Clears the user's list of visited locations.
     */
    public void clearVisitedLocations() {
        visitedLocations.clear();
    }

    /**
     * Adds a user reward to the user's list of rewards if it does not already exist.
     *
     * @param userReward the UserReward to be added
     */
    public void addUserReward(UserReward userReward) {
        if (userRewards.stream().noneMatch(r -> r.attraction.attractionName.equals(userReward.attraction.attractionName))) {
            userRewards.add(userReward);
        }
    }

    /**
     * Gets the last visited location of the user.
     *
     * @return the last VisitedLocation object from the user's visited locations
     */
    public VisitedLocation getLastVisitedLocation() {
        return visitedLocations.get(visitedLocations.size() - 1);
    }
}
