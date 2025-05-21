package com.openclassrooms.tourguide.user;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * UserPreferences is a class that represents the preferences of a user in the Tour Guide application.
 * It contains settings related to attraction proximity, trip duration, ticket quantity, and number of adults and children.
 */
@Setter
@Getter
@NoArgsConstructor
public class UserPreferences {

    private int attractionProximity = Integer.MAX_VALUE;
    private int tripDuration = 1;
    private int ticketQuantity = 1;
    private int numberOfAdults = 1;
    private int numberOfChildren = 0;
}
