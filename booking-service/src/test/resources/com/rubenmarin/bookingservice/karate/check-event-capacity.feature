Feature: Booking API

  @checkEventCapacity
  Scenario: Check event capacity
    * print 'Scenario: Check event capacity'
    # * def eventId = '0ebbca7e-87bb-4c1d-915c-676b59ffe329'
    # * def userId = java.util.UUID.randomUUID() + ''

    Given url eventBaseUrl
    And path 'events', eventId
    When method GET
    Then status 200








