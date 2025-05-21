package com.openclassrooms.tourguide.user;

import gpsUtil.location.Attraction;
import gpsUtil.location.VisitedLocation;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * UserReward is a class that represents the reward points associated with a user's visit to an attraction.
 * It contains the visited location, the attraction details, and the reward points earned.
 */
@Getter
@Setter
@AllArgsConstructor
public class UserReward {

    public final VisitedLocation visitedLocation;
    public final Attraction attraction;

    private int rewardPoints;

    /**
     * Constructor for UserReward.
     *
     * @param visitedLocation the VisitedLocation object representing where the user visited
     * @param attraction      the Attraction object representing the attraction visited
     */
    public UserReward(VisitedLocation visitedLocation, Attraction attraction) {
        this.visitedLocation = visitedLocation;
        this.attraction = attraction;
    }
}