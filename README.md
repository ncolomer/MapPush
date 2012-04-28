## MapPush demonstration
### Kesako?
This web application draws realtime events on a geomap using the Comet/WebSocket protocols. It was created to illustrate the **Atmosphere** framework's capabilities and ease of use.

User can trigger events by clicking on the map, and server can generate random events. 
A generated event (by either client or server) is delivered to each connected clients thanks to comet/websocket protocols.

Web client and server both use the [Atmosphere Framework](https://github.com/Atmosphere/atmosphere) to handle and broadcast realtime events.
In addition, the web client uses the [Google Maps API v3](http://code.google.com/intl/fr-FR/apis/maps/documentation/javascript/) and the [jQuery](http://jquery.com/) libraries.

A demonstration video is available on [Youtube](http://www.youtube.com/watch?v=1Abv88t5igc).

For more information, please read the [Come/WebSocket? Introducing the Atmosphere framework](http://www.ncolomer.net/2012/03/comewebsocket-introducing-the-atmosphere-framework/) blog post.

### Try it!
To launch this project, proceed as following:

* Clone the project `git clone git@github.com:ncolomer/MapPush.git` (you need [git](http://git-scm.com/))
* Deploy the webapp on Jetty8 using the Maven command `mvn jetty:run` (you need [Maven](http://maven.apache.org/))

You are now able to connect as many tab of your favorite browser you want to [http://localhost:8080/MapPush](http://localhost:8080/MapPush).
You may also want to use the following shell commands to play with the exposed API:

```
# Connect to the WebSocket URI
boundsHeader='48.0,49.0,2.0,3.0'
curl -v -N -X GET http://localhost:8080/MapPush/api -H "X-MAP-BOUNDS: $boundsHeader"

# Send an event
event='{"lat":48.921266,"lng":2.499390}'
curl -v -X POST http://localhost:8080/MapPush/api/event -d $event -H "Content-Type: application/json"
```
