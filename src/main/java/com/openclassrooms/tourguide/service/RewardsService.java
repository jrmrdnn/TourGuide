package com.openclassrooms.tourguide.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;

import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import rewardCentral.RewardCentral;

@Service
public class RewardsService {

  private static final double STATUTE_MILES_PER_NAUTICAL_MILE = 1.15077945;

  // proximity in miles
  private final int defaultProximityBuffer = 10;
  private int proximityBuffer = defaultProximityBuffer;
  private final int attractionProximityRange = 200;
  private final GpsUtil gpsUtil;
  private final RewardCentral rewardsCentral;

  public RewardsService(GpsUtil gpsUtil, RewardCentral rewardCentral) {
    this.gpsUtil = gpsUtil;
    this.rewardsCentral = rewardCentral;
  }

  public void setProximityBuffer(int proximityBuffer) {
    this.proximityBuffer = proximityBuffer;
  }

  public void setDefaultProximityBuffer() {
    proximityBuffer = defaultProximityBuffer;
  }

  public void calculateRewards(User user) {
    List<VisitedLocation> userLocations = user.getVisitedLocations();
    List<Attraction> attractions = gpsUtil.getAttractions();
    Set<UUID> rewardedAttractionIds = getRewardedAttractionIds(user);

    Map<Attraction, VisitedLocation> attractionVisitedLocationMap = getExtractedAttractionsAndVisitedLocation(
        userLocations,
        attractions,
        rewardedAttractionIds);

    for (Map.Entry<Attraction, VisitedLocation> entry : attractionVisitedLocationMap.entrySet()) {
      Attraction attraction = entry.getKey();
      VisitedLocation visitedLocation = entry.getValue();
      int rewardPoints = getRewardPoints(attraction, user);
      user.addUserReward(
          new UserReward(visitedLocation, attraction, rewardPoints));
    }
  }

  public boolean isWithinAttractionProximity(
      Attraction attraction,
      Location location) {
    return getDistance(attraction, location) <= attractionProximityRange;
  }

  private boolean nearAttraction(
      VisitedLocation visitedLocation,
      Attraction attraction) {
    return getDistance(attraction, visitedLocation.location) <= proximityBuffer;
  }

  private int getRewardPoints(Attraction attraction, User user) {
    return rewardsCentral.getAttractionRewardPoints(
        attraction.attractionId,
        user.getUserId());
  }

  private Set<UUID> getRewardedAttractionIds(User user) {
    return user
        .getUserRewards()
        .stream()
        .map(r -> r.attraction.attractionId)
        .collect(Collectors.toSet());
  }

  private Map<Attraction, VisitedLocation> getExtractedAttractionsAndVisitedLocation(
      List<VisitedLocation> userLocations,
      List<Attraction> attractions,
      Set<UUID> rewardedAttractionIds) {
    Map<Attraction, VisitedLocation> attractionVisitedLocationMap = new HashMap<>();

    for (VisitedLocation visitedLocation : userLocations) {
      for (Attraction attraction : attractions) {
        if (!rewardedAttractionIds.contains(attraction.attractionId) &&
            nearAttraction(visitedLocation, attraction)) {
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

    double angle = Math.acos(
        Math.sin(lat1) * Math.sin(lat2) +
            Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon1 - lon2));

    double nauticalMiles = 60 * Math.toDegrees(angle);
    double statuteMiles = STATUTE_MILES_PER_NAUTICAL_MILE * nauticalMiles;
    return statuteMiles;
  }
}
