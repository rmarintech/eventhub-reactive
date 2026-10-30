Feature: Booking API

  @createBooking
  Scenario: Create a booking
    * print 'Scenario: Create a booking'

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
    Then status 201
    And match response contains
        """
        {
          "eventId": "#(eventId)",
          "userId": "#(userId)"
        }
        """





