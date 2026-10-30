Feature: Booking API

  @rejectBookingNoCapacity
  Scenario: Reject a booking with no capacity
    * print 'Scenario: Reject a booking with no capacity'

    # * def eventId = '0ebbca7e-87bb-4c1d-915c-676b59ffe329'
    # * def userId = java.util.UUID.randomUUID() + ''

    Given url bookingBaseUrl
    And path 'api', 'bookings'
    And request
        """
        {
          "eventId": "#(eventId)",
          "userId": "#(userId)"
        }
        """
    When method POST
    Then status 409
    And match response contains
        """
        {
         "title":"Event not available",
         "status":409,
         "code":"EVENT_NOT_AVAILABLE"
        }
        """




