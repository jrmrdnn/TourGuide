package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.dto.AttractionDto;
import com.openclassrooms.tourguide.helper.InternalTestHelper;
import com.openclassrooms.tourguide.service.RewardsService;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserPreferences;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.GpsUtil;
import gpsUtil.location.Attraction;
import gpsUtil.location.Location;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import rewardCentral.RewardCentral;
import tripPricer.Provider;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TestTourGuideService {

    @Mock
    private GpsUtil gpsUtil;

    @Mock
    private RewardsService rewardsService;

    private TourGuideService tourGuideService;

    @BeforeEach
    public void setUp() {
        InternalTestHelper.setInternalUserNumber(0);
        tourGuideService = new TourGuideService(gpsUtil, rewardsService);
    }

    @Test
    public void getUserLocation_WhenUserHasVisitedLocations_ShouldReturnLastLocation() {
        User user = new User(UUID.randomUUID(), "testUser", "123", "test@email.com");
        Location location1 = new Location(10.0, 10.0);
        Location location2 = new Location(20.0, 20.0);

        VisitedLocation visitedLocation1 = new VisitedLocation(user.getUserId(), location1, new Date(System.currentTimeMillis() - 1000));
        VisitedLocation visitedLocation2 = new VisitedLocation(user.getUserId(), location2, new Date());

        user.addToVisitedLocations(visitedLocation1);
        user.addToVisitedLocations(visitedLocation2);

        VisitedLocation result = tourGuideService.getUserLocation(user);

        assertEquals(visitedLocation2, result);
        assertEquals(location2.latitude, result.location.latitude);
        assertEquals(location2.longitude, result.location.longitude);
    }

    @Test
    public void getUserLocation_WhenUserHasNoVisitedLocations_ShouldTrackUserLocation() {
        TourGuideService spyTourGuideService = spy(tourGuideService);
        User user = new User(UUID.randomUUID(), "testUser", "123", "test@email.com");

        Location expectedLocation = new Location(42.0, -73.0);
        VisitedLocation expectedVisitedLocation = new VisitedLocation(user.getUserId(), expectedLocation, new Date());

        doReturn(expectedVisitedLocation).when(spyTourGuideService).trackUserLocation(user);

        VisitedLocation result = spyTourGuideService.getUserLocation(user);

        assertEquals(expectedVisitedLocation, result);
        verify(spyTourGuideService, times(1)).trackUserLocation(user);
    }

    @Test
    public void addUser_shouldAddUserToInternalMap() {
        User user = new User(UUID.randomUUID(), "User", "0000", "user@email.com");

        tourGuideService.addUser(user);

        User retrievedUser = tourGuideService.getUser("User");
        assertNotNull(retrievedUser);
        assertEquals(user, retrievedUser);
        assertEquals(1, tourGuideService.getAllUsers().size());
    }

    @Test
    public void addUser_whenUserAlreadyExists_shouldNotAddDuplicate() {
        UUID userId = UUID.randomUUID();
        User user1 = new User(userId, "testUser", "000", "test@email.com");
        User user2 = new User(UUID.randomUUID(), "testUser", "111", "test2@email.com");

        tourGuideService.addUser(user1);
        tourGuideService.addUser(user2);

        User retrievedUser = tourGuideService.getUser("testUser");
        assertEquals(user1, retrievedUser);
        assertEquals(userId, retrievedUser.getUserId());
        assertEquals(1, tourGuideService.getAllUsers().size());
    }

    @Test
    public void getTripDeals_shouldReturnProviderList() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "testUser", "000", "test@email.com");

        UserPreferences preferences = new UserPreferences();
        preferences.setNumberOfAdults(2);
        preferences.setNumberOfChildren(1);
        preferences.setTripDuration(7);
        user.setUserPreferences(preferences);

        VisitedLocation visitedLocation = new VisitedLocation(userId, new Location(1.0, 1.0), new Date());

        UserReward reward1 = new UserReward(visitedLocation, new Attraction("Attraction1", "City1", "State1", 1.0, 1.0), 100);
        UserReward reward2 = new UserReward(visitedLocation, new Attraction("Attraction2", "City2", "State2", 2.0, 2.0), 200);

        user.addUserReward(reward1);
        user.addUserReward(reward2);

        List<Provider> actualProviders = tourGuideService.getTripDeals(user);

        assertNotNull(actualProviders);
    }

    @Test
    public void getAllUsers() {
        GpsUtil localGpsUtil = new GpsUtil();
        RewardsService localRewardsService = new RewardsService(localGpsUtil, new RewardCentral());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService localTourGuideService = new TourGuideService(localGpsUtil, localRewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        User user2 = new User(UUID.randomUUID(), "jon2", "000", "jon2@tourGuide.com");

        localTourGuideService.addUser(user);
        localTourGuideService.addUser(user2);

        List<User> allUsers = localTourGuideService.getAllUsers();

        localTourGuideService.tracker.stopTracking();

        assertTrue(allUsers.contains(user));
        assertTrue(allUsers.contains(user2));
    }

    @Test
    public void trackUserLocation_shouldGetLocationAndAddToUserAndCalculateRewards() {
        UUID userId = UUID.randomUUID();
        User user = new User(userId, "testUser", "000", "test@email.com");

        Location location = new Location(37.422, -122.084);
        VisitedLocation visitedLocation = new VisitedLocation(userId, location, new Date());

        when(gpsUtil.getUserLocation(userId)).thenReturn(visitedLocation);

        VisitedLocation result = tourGuideService.trackUserLocation(user);

        assertEquals(visitedLocation, result);
        assertEquals(1, user.getVisitedLocations().size());
        assertEquals(visitedLocation, user.getLastVisitedLocation());

        verify(rewardsService, times(1)).calculateRewards(user);
    }

    @Test
    public void trackUserLocation_Timeout_HandlesTimeout() throws Exception {
        try (MockedStatic<Executors> mockedExecutors = mockStatic(Executors.class)) {
            ExecutorService mockExecutor = mock(ExecutorService.class);

            mockedExecutors.when(() -> Executors.newFixedThreadPool(anyInt())).thenReturn(mockExecutor);

            when(mockExecutor.awaitTermination(20, TimeUnit.MINUTES)).thenReturn(false);

            List<User> users = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                users.add(new User(UUID.randomUUID(), "user" + i, "000", "user" + i + "@email.com"));
            }

            tourGuideService.trackUserLocation(users);

            verify(mockExecutor).shutdown();
            verify(mockExecutor).awaitTermination(20, TimeUnit.MINUTES);
        }
    }

    @Test
    public void trackUserLocation_Interrupted_HandlesException() throws Exception {
        try (MockedStatic<Executors> mockedExecutors = mockStatic(Executors.class)) {
            ExecutorService mockExecutor = mock(ExecutorService.class);

            mockedExecutors.when(() -> Executors.newFixedThreadPool(anyInt())).thenReturn(mockExecutor);

            InterruptedException exception = new InterruptedException("Test interruption");
            when(mockExecutor.awaitTermination(20, TimeUnit.MINUTES)).thenThrow(exception);

            List<User> users = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                users.add(new User(UUID.randomUUID(), "user" + i, "000", "user" + i + "@email.com"));
            }

            tourGuideService.trackUserLocation(users);

            verify(mockExecutor).shutdown();
            verify(mockExecutor).awaitTermination(20, TimeUnit.MINUTES);
        }
    }

    @Test
    public void getNearbyAttractions() {
        GpsUtil localGpsUtil = new GpsUtil();
        RewardsService localRewardsService = new RewardsService(localGpsUtil, new RewardCentral());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService localTourGuideService = new TourGuideService(localGpsUtil, localRewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");
        VisitedLocation visitedLocation = localTourGuideService.trackUserLocation(user);

        List<Attraction> attractions = localTourGuideService.getNearByAttractions(visitedLocation);

        localTourGuideService.tracker.stopTracking();

        assertEquals(5, attractions.size());
    }

    @Test
    public void getTripDeals() {
        GpsUtil localGpsUtil = new GpsUtil();
        RewardsService localRewardsService = new RewardsService(localGpsUtil, new RewardCentral());
        InternalTestHelper.setInternalUserNumber(0);
        TourGuideService localTourGuideService = new TourGuideService(localGpsUtil, localRewardsService);

        User user = new User(UUID.randomUUID(), "jon", "000", "jon@tourGuide.com");

        List<Provider> providers = localTourGuideService.getTripDeals(user);

        localTourGuideService.tracker.stopTracking();

        assertEquals(5, providers.size());
    }

    @Test
    public void getListAttractionsDto_shouldReturnCorrectAttractionDtoList() {
        TourGuideService spyTourGuideService = spy(tourGuideService);

        UUID userId = UUID.randomUUID();
        User user = new User(userId, "testUser", "000", "test@email.com");

        Location userLocation = new Location(34.0, -118.0);
        VisitedLocation visitedLocation = new VisitedLocation(userId, userLocation, new Date());

        List<Attraction> attractions = new ArrayList<>();
        Attraction attraction1 = new Attraction("Attraction One", "City1", "State1", 34.1, -118.1);
        Attraction attraction2 = new Attraction("Attraction Two", "City2", "State2", 34.2, -118.2);
        attractions.add(attraction1);
        attractions.add(attraction2);

        doReturn(user).when(spyTourGuideService).getUser("testUser");
        doReturn(visitedLocation).when(spyTourGuideService).getUserLocation(user);
        doReturn(attractions).when(spyTourGuideService).getNearByAttractions(visitedLocation);

        when(rewardsService.getRewardPoints(attraction1, user)).thenReturn(100);
        when(rewardsService.getRewardPoints(attraction2, user)).thenReturn(200);

        List<AttractionDto> result = spyTourGuideService.getListAttractionsDto("testUser");

        assertEquals(2, result.size());

        AttractionDto dto1 = result.get(0);
        assertEquals("Attraction One", dto1.getAttractionName());
        assertEquals(34.1, dto1.getAttractionLatitude());
        assertEquals(-118.1, dto1.getAttractionLongitude());
        assertEquals(34.0, dto1.getUserLatitude());
        assertEquals(-118.0, dto1.getUserLongitude());
        assertEquals(100, dto1.getRewardPoints());

        AttractionDto dto2 = result.get(1);
        assertEquals("Attraction Two", dto2.getAttractionName());
        assertEquals(34.2, dto2.getAttractionLatitude());
        assertEquals(-118.2, dto2.getAttractionLongitude());
        assertEquals(34.0, dto2.getUserLatitude());
        assertEquals(-118.0, dto2.getUserLongitude());
        assertEquals(200, dto2.getRewardPoints());

        verify(spyTourGuideService).getUser("testUser");
        verify(spyTourGuideService).getUserLocation(user);
        verify(spyTourGuideService).getNearByAttractions(visitedLocation);
        verify(rewardsService).getRewardPoints(attraction1, user);
        verify(rewardsService).getRewardPoints(attraction2, user);
    }
}
