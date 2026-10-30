Feature: Booking API

  @publishEventById
  Scenario: Publish draft Event by eventId
    * print 'Scenario: Publish draft Event by eventId'

    Given url eventBaseUrl
    And path 'events', eventId, 'publish'
    When method POST
    Then status 200
    And match response contains
        """
        {
           "status": "PUBLISHED"
        }
        """








