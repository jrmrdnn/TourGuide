package com.openclassrooms.tourguide;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rewardCentral.RewardCentral;

@ExtendWith(MockitoExtension.class)
public class TestRewardsService {

  @Mock
  private ExecutorService mockExecutor;

  @Mock
  private GpsUtil gpsUtil;

  @Mock
  private RewardCentral rewardCentral;

  @Mock
  private Logger mockLogger;

  @Test
  public void userGetRewards() {
    Attraction attraction = new Attraction(
      "Test Attraction",
      "Test City",
      "Test State",
      34.0,
      -118.0
    );
    when(gpsUtil.getAttractions()).thenReturn(
      Collections.singletonList(attraction)
    );

    Location location = new Location(attraction.latitude, attraction.longitude);

    VisitedLocation visitedLocation = new VisitedLocation(
      UUID.randomUUID(),
      location,
      new Date()
    );

    when(gpsUtil.getUserLocation(any(UUID.class))).thenReturn(visitedLocation);

    RewardsService rewardsService = new RewardsService(
      gpsUtil,
      new RewardCentral()
    );

    InternalTestHelper.setInternalUserNumber(0);

    TourGuideService tourGuideService = new TourGuideService(
      gpsUtil,
      rewardsService
    );

    User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
    user.addToVisitedLocations(
      new VisitedLocation(user.getUserId(), attraction, new Date())
    );

    tourGuideService.trackUserLocation(user);

    List<UserReward> userRewards = user.getUserRewards();

    tourGuideService.tracker.stopTracking();

    assertTrue(userRewards.size() == 1);
  }

  @Test
  public void calculateRewards_Timeout_LogsWarn() throws Exception {
    try (
      MockedStatic<Executors> mockedExecutors = mockStatic(Executors.class);
      MockedStatic<LoggerFactory> mockedLoggerFactory = mockStatic(
        LoggerFactory.class
      )
    ) {
      mockedExecutors
        .when(() -> Executors.newFixedThreadPool(anyInt()))
        .thenReturn(mockExecutor);
      mockedLoggerFactory
        .when(() -> LoggerFactory.getLogger(any(Class.class)))
        .thenReturn(mockLogger);

      doNothing().when(mockExecutor).execute(any(Runnable.class));
      doNothing().when(mockExecutor).shutdown();
      when(
        mockExecutor.awaitTermination(anyLong(), any(TimeUnit.class))
      ).thenReturn(false);

      RewardsService rewardsService = new RewardsService(
        gpsUtil,
        rewardCentral
      );

      List<User> users = Collections.singletonList(
        new User(UUID.randomUUID(), "testUser", "1234", "test@email.com")
      );

      rewardsService.calculateRewards(users);
    }
  }

  @Test
  public void calculateRewards_Interrupted_LogsError() throws Exception {
    try (
      MockedStatic<Executors> mockedExecutors = mockStatic(Executors.class);
      MockedStatic<LoggerFactory> mockedLoggerFactory = mockStatic(
        LoggerFactory.class
      )
    ) {
      mockedExecutors
        .when(() -> Executors.newFixedThreadPool(anyInt()))
        .thenReturn(mockExecutor);
      mockedLoggerFactory
        .when(() -> LoggerFactory.getLogger(RewardsService.class))
        .thenReturn(mockLogger);

      RewardsService rewardsService = new RewardsService(
        gpsUtil,
        rewardCentral
      );

      InterruptedException exception = new InterruptedException(
        "Test interruption"
      );
      when(mockExecutor.awaitTermination(20, TimeUnit.MINUTES)).thenThrow(
        exception
      );

      List<User> users = Collections.singletonList(
        new User(UUID.randomUUID(), "testUser", "1234", "test@email.com")
      );

      rewardsService.calculateRewards(users);
    }
  }

  @Test
  public void isWithinAttractionProximity() {
    Attraction attraction = new Attraction(
      "Test Attraction",
      "Test City",
      "Test State",
      34.0,
      -118.0
    );

    RewardsService rewardsService = new RewardsService(
      gpsUtil,
      new RewardCentral()
    );

    assertTrue(
      rewardsService.isWithinAttractionProximity(attraction, attraction)
    );
  }

  @Test
  public void isWithinAttractionProximity_ReturnsFalse() {
    Attraction attraction = new Attraction(
      "Test Attraction",
      "Test City",
      "Test State",
      34.0,
      -118.0
    );
    Location location = new Location(36.0, -120.0);

    RewardsService rewardsService = new RewardsService(
      gpsUtil,
      new RewardCentral()
    );
    RewardsService spyRewardsService = spy(rewardsService);

    doReturn(200.1)
      .when(spyRewardsService)
      .getDistance(any(Location.class), any(Location.class));

    assertFalse(
      spyRewardsService.isWithinAttractionProximity(attraction, location)
    );
  }

  @Test
  public void nearAllAttractions() {
    RewardsService rewardsService = new RewardsService(
      gpsUtil,
      new RewardCentral()
    );
    rewardsService.setProximityBuffer(Integer.MAX_VALUE);

    InternalTestHelper.setInternalUserNumber(1);

    TourGuideService tourGuideService = new TourGuideService(
      gpsUtil,
      rewardsService
    );

    User user = tourGuideService.getAllUsers().get(0);

    rewardsService.calculateRewards(user);

    List<UserReward> userRewards = tourGuideService.getUserRewards(user);

    tourGuideService.tracker.stopTracking();

    assertEquals(gpsUtil.getAttractions().size(), userRewards.size());
  }

  @Test
  public void setDefaultProximityBufferTest() {
    Attraction attraction = new Attraction(
      "Test Attraction",
      "Test City",
      "Test State",
      34.0,
      -118.0
    );
    when(gpsUtil.getAttractions()).thenReturn(
      Collections.singletonList(attraction)
    );

    RewardsService rewardsService = new RewardsService(
      gpsUtil,
      new RewardCentral()
    );

    rewardsService.setProximityBuffer(100);

    rewardsService.setDefaultProximityBuffer();

    User user = new User(UUID.randomUUID(), "test", "000", "test@test.com");

    double milesAway = 11.0;
    Location farLocation = new Location(
      attraction.latitude + (milesAway / 69.0),
      attraction.longitude
    );

    user.addToVisitedLocations(
      new VisitedLocation(user.getUserId(), farLocation, new Date())
    );

    rewardsService.calculateRewards(user);

    assertEquals(0, user.getUserRewards().size());
  }

  @Test
  public void getExtractedAttractionsAndVisitedLocation_AlreadyRewardedAttractions()
    throws Exception {
    RewardsService rewardsService = spy(
      new RewardsService(gpsUtil, rewardCentral)
    );

    Location nearLocation = new Location(34.01, -118.01);
    VisitedLocation visitedLocation = new VisitedLocation(
      UUID.randomUUID(),
      nearLocation,
      new Date()
    );
    List<VisitedLocation> userLocations = Collections.singletonList(
      visitedLocation
    );

    Attraction attraction = new Attraction(
      "Test",
      "City",
      "State",
      34.0,
      -118.0
    );
    List<Attraction> attractions = Collections.singletonList(attraction);

    Set<UUID> rewardedAttractionIds = Collections.singleton(
      attraction.attractionId
    );

    Method method =
      RewardsService.class.getDeclaredMethod(
          "getExtractedAttractionsAndVisitedLocation",
          List.class,
          List.class,
          Set.class
        );
    method.setAccessible(true);

    @SuppressWarnings("unchecked")
    Map<Attraction, VisitedLocation> result = (Map<
        Attraction,
        VisitedLocation
      >) method.invoke(
      rewardsService,
      userLocations,
      attractions,
      rewardedAttractionIds
    );

    assertTrue(result.isEmpty());
  }
}
