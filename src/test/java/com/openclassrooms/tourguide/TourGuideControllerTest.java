package com.openclassrooms.tourguide;

import com.openclassrooms.tourguide.dto.AttractionDto;
import com.openclassrooms.tourguide.service.TourGuideService;
import com.openclassrooms.tourguide.user.User;
import com.openclassrooms.tourguide.user.UserReward;
import gpsUtil.location.VisitedLocation;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tripPricer.Provider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TourGuideControllerTest {

    @Test
    void index() {
        TourGuideController tourGuideController = new TourGuideController(null);
        String response = tourGuideController.index();
        assertEquals("Greetings from TourGuide!", response);
    }

    @Test
    void getLocation() {
        TourGuideService mockService = Mockito.mock(TourGuideService.class);
        User mockUser = Mockito.mock(User.class);
        VisitedLocation mockVisitedLocation = Mockito.mock(VisitedLocation.class);

        Mockito.when(mockService.getUser("testUser")).thenReturn(mockUser);
        Mockito.when(mockService.getUserLocation(mockUser)).thenReturn(mockVisitedLocation);

        TourGuideController tourGuideController = new TourGuideController(mockService);

        VisitedLocation result = tourGuideController.getLocation("testUser");

        assertEquals(mockVisitedLocation, result);
    }

    @Test
    void getNearbyAttractions() {
        TourGuideService mockService = Mockito.mock(TourGuideService.class);
        List<AttractionDto> mockAttractions = List.of(Mockito.mock(AttractionDto.class));

        Mockito.when(mockService.getListAttractionsDto("testUser")).thenReturn(mockAttractions);

        TourGuideController tourGuideController = new TourGuideController(mockService);

        List<AttractionDto> result = tourGuideController.getNearbyAttractions("testUser");

        assertEquals(mockAttractions, result);
    }

    @Test
    void getRewards() {
        TourGuideService mockService = Mockito.mock(TourGuideService.class);
        User mockUser = Mockito.mock(User.class);
        List<UserReward> mockRewards = List.of(Mockito.mock(UserReward.class));

        Mockito.when(mockService.getUser("testUser")).thenReturn(mockUser);
        Mockito.when(mockService.getUserRewards(mockUser)).thenReturn(mockRewards);

        TourGuideController tourGuideController = new TourGuideController(mockService);

        List<UserReward> result = tourGuideController.getRewards("testUser");

        assertEquals(mockRewards, result);
    }

    @Test
    void getTripDeals() {
        TourGuideService mockService = Mockito.mock(TourGuideService.class);
        User mockUser = Mockito.mock(User.class);
        List<Provider> mockProviders = List.of(Mockito.mock(Provider.class));

        Mockito.when(mockService.getUser("testUser")).thenReturn(mockUser);
        Mockito.when(mockService.getTripDeals(mockUser)).thenReturn(mockProviders);

        TourGuideController tourGuideController = new TourGuideController(mockService);

        List<Provider> result = tourGuideController.getTripDeals("testUser");

        assertEquals(mockProviders, result);
    }
}