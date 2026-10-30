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