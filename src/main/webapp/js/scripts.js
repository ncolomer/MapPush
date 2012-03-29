var url = document.location.toString() + "api";

$(document).ready(function() {
	$("#dialog").dialog({
		title: 'Statistics',
		autoOpen: false,
		resizable: true,
		width: 150,
		height: 120
	});
	
	$("#button-fireworks").buttonset().change(function() {
		var selected = $("#button-fireworks input[type='radio']:checked")[0];
		if ($("#button-start").is(selected)) {
			$.ajax({type: "GET", url: url + "/start"});
		} else {
			$.ajax({type: "GET", url: url + "/stop"});
		}
	});
	$("#button-start").button({icons: {primary:'ui-icon-play'}});
	$("#button-stop").button({icons: {primary:'ui-icon-pause'}});

	$("#button-connection").buttonset().change(function() {
		var selected = $("#button-connection input[type='radio']:checked")[0];
		if ($("#button-connect").is(selected)) {
			console.log("Connecting...");
			connect();
		} else {
			console.log("Disconnecting...");
			disconnect(); 
		}
	});
	$("#button-connect").button({icons: {primary:'ui-icon-link'}});
	$("#button-disconnect").button({icons: {primary:'ui-icon-cancel'}});

	$("#button-stats").button({
		icons: {primary:'ui-icon-info'}
	}).click(function () {
		if ($('#button-stats').is(':checked')) {
			$('#map-stats').show();
			statsAgent.start();
		} else {
			$('#map-stats').hide();
			statsAgent.stop();
		}
	});

	statsAgent.totalCallback = function(total) { $(".tre").text(total); };
	statsAgent.epsCallback = function(eps) { $(".eps").text(eps); };
	googleMapInit();
});

function googleMapInit() {
	// Init the map
	var paris = new google.maps.LatLng(48.857720, 2.345581);
	var atlantica = new google.maps.LatLng(40, -40);
	var mapOptions = {
			zoom: 2,
			center: atlantica,
			mapTypeId: google.maps.MapTypeId.ROADMAP,
			panControl: false
	};
	mapsAgent.map = new google.maps.Map(document.getElementById('map-canvas'), mapOptions);
	// Add listeners
	google.maps.event.addListener(mapsAgent.map, 'click', function(event) {
		trigger(event.latLng);
	});
	google.maps.event.addListener(mapsAgent.map, 'idle', function() {
		update(mapsAgent.getBounds());
	});
}

var statsAgent = {
		frequency: 1000,
		totalEvent: 0,
		numEvent: 0,
		running: false,
		timer: null,
		totalCallback: null,
		epsCallback: null,
		"process": function() {
			var eps = (this.numEvent / this.frequency) * 1000;
			this.numEvent = 0;
			if (this.epsCallback) this.epsCallback(eps);
		},
		"start": function() {
			this.stop();
			this.timer = setInterval(function() {statsAgent.process();}, this.frequency);
			this.running = true;
			this.numEvent = 0;
		},
		"stop": function() {
			clearInterval(this.timer);
			this.running = false;
		},
		"notify": function() {
			this.numEvent++;
			this.totalEvent++;
			if (this.totalCallback) this.totalCallback(this.totalEvent);
		}
};

var mapsAgent = {
		map: null,
		"getBounds": function() {
			var bounds = this.map.getBounds();
			return {
				"southLat": bounds.getSouthWest().lat(),
				"northLat": bounds.getNorthEast().lat(),
				"westLng": bounds.getSouthWest().lng(),
				"eastLng": bounds.getNorthEast().lng()
			};
		},
		"drawEvent": function(json) {
			var marker = new google.maps.Marker({
				map: this.map,
				animation: google.maps.Animation.DROP,
				position: new google.maps.LatLng(json.lat, json.lng)
			});
			setTimeout(function() {
				marker.setMap(null);
			}, 2000);
		}
};

var endpoint;
function connect() {

	var callback = function callback(response) {
		// Websocket events.
		if (response.state == "opening") {
			console.log("Connected to realtime endpoint using " + response.transport);
		} else if (response.state == "closed") {
			console.log("Disconnected from realtime endpoint");
		} else if (response.transport != 'polling' && response.state == 'messageReceived') {
			if (response.status == 200) {
				var data = response.responseBody;
				if (data.length > 0) {
					statsAgent.notify();
					console.log("Message Received using " + response.transport + ": " + data);
					var json = JSON.parse(data);
					mapsAgent.drawEvent(json);
				}
			}
		}
	};

	var bounds = mapsAgent.getBounds();
	var header = bounds.southLat + "," + bounds.northLat + "," + bounds.westLng + "," + bounds.eastLng;
	endpoint = $.atmosphere.subscribe(url, callback, {
		transport: 'websocket', /* websocket, jsonp, long-polling, polling, streaming */
		fallbackTransport: 'streaming',
		attachHeadersAsQueryString: true,
		headers: {"X-Map-Bounds": header}
	});
}

function disconnect() {
	$.atmosphere.unsubscribe();
	endpoint = null;
}

function update(bounds) {
	console.log("### Map bounds changed:", JSON.stringify(bounds));
	if (!endpoint) return;
	endpoint.push(JSON.stringify(bounds));
}

function trigger(latLng) {
	if (!endpoint) return;
	var data = {"lat": latLng.lat(), "lng": latLng.lng()};
	console.log("### Trigger event:", JSON.stringify(data));
	$.ajax({
		type: "POST",
		url: url + "/event",
		contentType: "application/json",
		data: JSON.stringify(data)
	});
}


