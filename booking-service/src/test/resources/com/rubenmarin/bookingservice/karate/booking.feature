Feature: Booking API

  Scenario: Check configuration
    * print 'Booking URL:', karate.get('bookingBaseUrl')
    * print 'Event URL:', karate.get('eventBaseUrl')

####### Rechazar reserva inválida ###########################################################################################
  Scenario: Reject una reserva
    * def result = call read('reject-booking-invalid-req.feature')
    * print result

####### Creación correcta de reservas ###########################################################################################
  Scenario: Check capacity logic de un event
    # 0.1 Crear evento
    * def capacity = 999999
    * def event = call read('create-event.feature') { capacity: #(capacity) }
    * print 'Capacity: ' + event.response.capacity
    * def eventId = event.response.id
    * def userId = java.util.UUID.randomUUID() + ''

    # 0.2 Publicar evento
    * def eventPublished = call read('publish-event.feature') { eventId: '#(eventId)' }
    * print eventPublished.response.status
    * match eventPublished.response.status == 'PUBLISHED'

    # 1. Consultar capacidad inicial
    * def before = call read('check-event-capacity.feature') { eventId: '#(eventId)' }
    * def initialCapacity = before.response.available
    * match initialCapacity == 999999

    # 2. Crear reserva
    * def booking = call read('create-booking.feature') { eventId: '#(eventId)', userId: '#(userId)' }
    * match booking.responseStatus == 201

    # 3. Consultar capacidad final
    * def after = call read('check-event-capacity.feature') { eventId: '#(eventId)' }
    * def finalCapacity = after.response.available

    # 4. Comprobar regla de negocio
    * match finalCapacity == initialCapacity-1

####### Rechazo cuando se agota la capacidad ###########################################################################################
  Scenario: Check rejection logic de un event sin plazas
    # 0.1 Crear evento
    * def capacity = 2
    * def event = call read('create-event.feature') { capacity: #(capacity) }
    * print 'Capacity: ' + event.response.capacity
    * def eventId = event.response.id
    * def userId1 = java.util.UUID.randomUUID() + ''
    * def userId2 = java.util.UUID.randomUUID() + ''
    * def userId3 = java.util.UUID.randomUUID() + ''


    # 0.2 Publicar evento
    * def eventPublished = call read('publish-event.feature') { eventId: '#(eventId)' }
    * print eventPublished.response.status
    * match eventPublished.response.status == 'PUBLISHED'

    # 1. Consultar capacidad inicial
    * def before = call read('check-event-capacity.feature') { eventId: '#(eventId)' }
    * def initialCapacity = before.response.available
    * match initialCapacity == 2

    # 2.1 Crear reserva 1
    * def booking = call read('create-booking.feature') { eventId: '#(eventId)', userId: '#(userId1)' }
    * match booking.responseStatus == 201

    # 2.2 Crear reserva 2
    * def booking = call read('create-booking.feature') { eventId: '#(eventId)', userId: '#(userId2)' }
    * match booking.responseStatus == 201

    # 1. Consultar capacidad full
    * def fullEvent = call read('check-event-capacity.feature') { eventId: '#(eventId)' }
    * def available = fullEvent.response.available
    * match available == 0

    # 2.3 Crear reserva 3 (No debe poder)
    * def booking = call read('reject-booking-no-capacity.feature') { eventId: '#(eventId)', userId: '#(userId3)' }
    * match booking.responseStatus == 409

    # 3. Consultar capacidad final
    * def after = call read('check-event-capacity.feature') { eventId: '#(eventId)' }
    * def finalCapacity = after.response.available

    # 4. Comprobar regla de negocio
    * match finalCapacity == 0



####### Pruebas Parciales ###########################################################################################
  @ignore
  Scenario: Create una reserva
    * def eventId = '0ebbca7e-87bb-4c1d-915c-676b59ffe329'
    * def userId = java.util.UUID.randomUUID() + ''
    * def result = call read('create-booking.feature') { eventId: '#(eventId)', userId: '#(userId)' }
    * print result

  @ignore
  Scenario: Check capacity de un event
    * def eventId = '0ebbca7e-87bb-4c1d-915c-676b59ffe329'
    * def userId = java.util.UUID.randomUUID() + ''
    * def event = call read('check-event-capacity.feature') { eventId: '#(eventId)' }
    * print event.response

  @ignore
  Scenario: Create event with Capacity and publish It
    * def capacity = 999999

    * def event = call read('create-event.feature') { capacity: #(capacity) }
    * print event.response
    * def eventId = event.response.id

    * def eventPublished = call read('publish-event.feature') { eventId: '#(eventId)' }
    * print eventPublished.response

  @ignore
  Scenario: Reject a booking with no capacity
    #* def eventId = '0ebbca7e-87bb-4c1d-915c-676b59ffe329'
    * def userId = java.util.UUID.randomUUID() + ''
    * def capacity = 1

    * def event = call read('create-event.feature') { capacity: #(capacity) }
    * print event.response
    * def eventId = event.response.id

    * def eventPublished = call read('publish-event.feature') { eventId: '#(eventId)' }
    * print eventPublished.response

    * def result = call read('create-booking.feature') { eventId: '#(eventId)', userId: '#(userId)' }

    * def eventRej = call read('reject-booking-no-capacity.feature') { eventId: '#(eventId)', userId: '#(userId)' }
    * print eventRej.response