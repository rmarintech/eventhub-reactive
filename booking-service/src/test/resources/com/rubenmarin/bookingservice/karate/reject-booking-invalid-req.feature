Feature: Booking API

  @rejectBooking
  Scenario: Reject a booking with an invalid request
    * print 'Reject a booking with an invalid request'

    Given url bookingBaseUrl
    And  path 'api', 'bookings'
    And header Content-Type = 'application/json'
    And request {}
    When method POST
    Then status 400







