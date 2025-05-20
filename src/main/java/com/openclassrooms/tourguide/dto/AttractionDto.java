package com.openclassrooms.tourguide.dto;

import lombok.Data;

@Data
public class AttractionDto {

    // Name of Tourist attraction
    private String attractionName;

    // Tourist attractions lat/long
    private double attractionLatitude;
    private double attractionLongitude;

    // The user's location lat/long
    private double userLatitude;
    private double userLongitude;

    // The distance in miles between the user's location and each of the attractions
    private double distanceInMiles;

    // The reward points for visiting each Attraction
    private int rewardPoints;
}
