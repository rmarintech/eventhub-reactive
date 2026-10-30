Feature: Booking API

  @createEventWithCapacity
  Scenario: Create event with Capacity
    * print 'Scenario: Create event with Capacity'
    # * def capacity = 999999

    Given url eventBaseUrl
    And path 'events'
    And request
        """
          {
            "name":"E2E Karate Event",
            "description":"E2E Karate Event",
            "startDate":"2026-11-15T10:00:00",
            "capacity":#(capacity),
            "price":49.99,
            "currency":"EUR"
          }
        """
    When method POST
    Then status 201
    And match response contains
        """
        {
          "name":"E2E Karate Event",
          "description":"E2E Karate Event",
          "startDate": "2026-11-15T10:00:00",
          "capacity": #(capacity),
          "available": #(capacity),
          "price": 49.99,
          "currency": "EUR",
          "status": "DRAFT"
        }
        """








