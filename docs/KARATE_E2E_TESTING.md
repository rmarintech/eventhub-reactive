# Karate — Pruebas E2E de EventHub Reactive

## Objetivo

Los tests de Karate del módulo `booking-service` verifican el comportamiento HTTP de **Booking Service** y **Event Service** funcionando conjuntamente. Validan los contratos de respuesta y las reglas de negocio de creación de reservas y disponibilidad de plazas.

Son **pruebas E2E contra servicios en ejecución**: no sustituyen los tests unitarios ni los tests de integración que usan Testcontainers.

## Tecnologías y ejecución

- Java 21, Karate (`karate-junit6`, versión `2.1.3` en la configuración utilizada) y JUnit 6.
- Booking Service: `http://localhost:8081`.
- Event Service: `http://localhost:8080`.
- Ambos servicios y sus dependencias necesarias deben estar levantados antes de ejecutar los escenarios.
- Punto de entrada: `BookingApiTest`, que carga `booking.feature` desde el classpath.

Desde el directorio `booking-service`:

```powershell
.\mvnw.cmd test "-Dtest=BookingApiTest"
```

Los resultados y los errores de aserción se pueden consultar en el informe de Karate generado bajo `target/karate-reports/`.

## Configuración de entornos

La configuración se centraliza en:

```text
booking-service/src/test/resources/karate-config.js
```

Ejemplo de configuración local y para una red Docker donde los nombres de servicio sean resolubles:

```javascript
function fn() {
    var env = karate.env || 'local';
    var config = {
        bookingBaseUrl: 'http://localhost:8081',
        eventBaseUrl: 'http://localhost:8080'
    };

    if (env == 'docker') {
        config.bookingBaseUrl = 'http://booking-service:8081';
        config.eventBaseUrl = 'http://event-service:8080';
    }
    return config;
}
```

En los archivos `.feature` se accede directamente a las variables exportadas:

```gherkin
Given url bookingBaseUrl
# o, en los tests del servicio de eventos:
Given url eventBaseUrl
```

El archivo **debe llamarse exactamente** `karate-config.js`, sin espacios iniciales ni sufijos ocultos, y quedar disponible en la raíz del classpath de pruebas (`target/test-classes/karate-config.js`). Si Karate informa `classpath:karate-config.js not found`, verificar el nombre y la copia de recursos antes de modificar las expresiones de los escenarios.

> En los archivos `.feature` inicialmente facilitados todavía aparecen URLs literales. La configuración centralizada descrita aquí corresponde al ajuste realizado posteriormente; conviene mantener todos los helpers usando las variables anteriores.

## Organización

```text
booking-service/src/test/
├── java/com/rubenmarin/bookingservice/karate/
│   └── BookingApiTest.java
└── resources/
    ├── karate-config.js
    └── com/rubenmarin/bookingservice/karate/
        ├── booking.feature
        ├── create-event.feature
        ├── publish-event.feature
        ├── check-event-capacity.feature
        ├── create-booking.feature
        ├── reject-booking-invalid-req.feature
        └── reject-booking-no-capacity.feature
```

`booking.feature` orquesta los flujos. Los demás `.feature` encapsulan operaciones HTTP reutilizables mediante `call read()` y parámetros (`eventId`, `userId`, `capacity`). El resultado de una llamada permite acceder a `response` y `responseStatus`.

## Cobertura funcional

### 1. Solicitud inválida de reserva

- **Helper:** `reject-booking-invalid-req.feature`.
- Envía `POST /api/bookings` con un JSON vacío (`{}`).
- **Resultado esperado:** HTTP `400`.

### 2. Creación correcta y reducción de capacidad

- **Escenario:** `Check capacity logic de un event`.
- Crea un evento con `capacity = 999999`, lo publica y confirma `PUBLISHED`.
- Consulta `available`, crea una reserva (`201`) y vuelve a consultar el evento.
- **Invariante comprobada:** `finalCapacity == initialCapacity - 1`.
- **Estado del escenario en el último archivo facilitado:** marcado con `@ignore`; no se ejecuta en la suite ordinaria mientras conserve esa etiqueta.

### 3. Rechazo cuando se agotan las plazas

- **Escenario:** `Check rejection logic de un event sin plazas`.
- Crea y publica un evento con `capacity = 2`.
- Genera tres identificadores de usuario diferentes.
- Las dos primeras reservas se crean con HTTP `201`, agotando `available` hasta `0`.
- La tercera solicitud debe rechazarse con HTTP `409`.
- El helper comprueba además `title: Event not available`, `status: 409` y `code: EVENT_NOT_AVAILABLE`.
- **Invariante comprobada:** tras el rechazo, `available` sigue siendo `0`.

Los escenarios principales crean nuevos eventos e identificadores para evitar depender de un evento preexistente. Los escenarios auxiliares marcados `@ignore` sirven como pruebas manuales o de diagnóstico y no forman parte de la ejecución ordinaria.

## Qué verifica y qué no

**Verificado por las aserciones definidas:** códigos HTTP, estado de publicación, contenido parcial de determinadas respuestas y variaciones de `available` observadas por la API.

**No está cubierto expresamente por estos escenarios:** ausencia física de una reserva rechazada en la base de datos, concurrencia simultánea de reservas, reintentos, mensajería Kafka/IBM MQ, recuperación de DLT, y consistencia entre consumidores. Estas capacidades requieren tests específicos.

## Mantenimiento

- Centralizar URLs en `karate-config.js`.
- Reutilizar helpers para llamadas HTTP repetidas.
- Comprobar tanto el **código HTTP** como la **regla de negocio observable**.
- Evitar identificadores de eventos fijos en escenarios E2E automatizados.
- Revisar la fecha fija del evento (`2026-11-15T10:00:00` en el helper facilitado) cuando deje de ser válida para las reglas de publicación o reserva; preferir una fecha futura calculada dinámicamente si el dominio la exige.
