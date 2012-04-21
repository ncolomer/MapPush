var apiUrl = document.location.toString() + "api";

$(document).ready(function() {
	// IE compatibility
	console = (!window.console) ? {} : window.console;
	console.log = (!window.console.log) ? function() {} : window.console.log;
	
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
			$.ajax({type: "GET", url: apiUrl + "/start"});
		} else {
			$.ajax({type: "GET", url: apiUrl + "/stop"});
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
		"getBoundsHeader": function() {
			var bounds = this.getBounds();
			return bounds.southLat + "," + bounds.northLat + "," + bounds.westLng + "," + bounds.eastLng;
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

var socket;
function connect() {
	var request = {
			url: apiUrl,
			logLevel : 'info',
			transport: 'websocket', /* websocket, jsonp, long-polling, polling, streaming */
			fallbackTransport: 'streaming',
			attachHeadersAsQueryString: true,
			headers: {"X-Map-Bounds": mapsAgent.getBoundsHeader()}
	};
	request.onOpen = function(response) {
		console.log("Connected to realtime endpoint using " + response.transport);
	};
	request.onClose = function(response) {
		console.log("Disconnected from realtime endpoint");
	};
	request.onMessage = function (response) {
		if (response.transport != 'polling' && response.state == 'messageReceived') {
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
	socket = $.atmosphere.subscribe(request);
}

function disconnect() {
	$.atmosphere.unsubscribe();
	socket = null;
}

function update(bounds) {
	console.log("### Map bounds changed:", JSON.stringify(bounds));
	if (!socket) return;
	socket.push(JSON.stringify(bounds));
}

function trigger(latLng) {
	if (!socket) return;
	var data = {"lat": latLng.lat(), "lng": latLng.lng()};
	console.log("### Trigger event:", JSON.stringify(data));
	$.ajax({
		type: "POST",
		url: apiUrl + "/event",
		contentType: "application/json",
		data: JSON.stringify(data)
	});
}


