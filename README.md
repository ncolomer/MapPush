# MapPush demonstration
This web application draws realtime events on a map:
User can generate events by clicking on the map, and server can generate random events. 
A generated event (by either client or server) is delivered to each connected clients thanks to comet/websocket protocols.

The web client uses [Google Maps API v3](http://code.google.com/intl/fr-FR/apis/maps/documentation/javascript/) and [jQuery](http://jquery.com/).
Web client and server both use the [Atmosphere Framework](https://github.com/Atmosphere/atmosphere) to handle and broadcast realtime events.

A video is available on [Youtube](http://www.youtube.com/watch?v=1Abv88t5igc)