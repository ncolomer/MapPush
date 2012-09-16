var apiUrl = document.location.toString() + 'api';

$(document).ready(function() {
	// IE console compatibility
	console = (!window.console) ? {} : window.console;
	console.log = (!window.console.log) ? function() {} : window.console.log;

	mapAgent.init('map-canvas', [40, -40], 2);
	statsAgent.totalCallback = function(total) { $('.tre').text(total); };
	statsAgent.epsCallback = function(eps) { $('.eps').text(eps); };

	initButtons();
});

function initButtons() {
	$('body').on('click', '#btn-connection button', function() {
		if(!$(this).hasClass('active')) {
			$('#btn-connection button').removeClass('active');
			$(this).addClass('active');
			switch ($(this).attr('id')) {
			case 'btn-connection-connect':
				console.log("Connecting...");
				webSocketAgent.connect();
				break;
			case 'btn-connection-disconnect':
				console.log("Disconnecting...");
				webSocketAgent.disconnect();
				break;
			}
		}
	});

	$('body').on('click', '#btn-generation button', function() {
		if(!$(this).hasClass('active')) {
			$('#btn-generation button').removeClass('active');
			$(this).addClass('active');
			switch ($(this).attr('id')) {
			case 'btn-generation-start':
				$.ajax({type: 'GET', url: apiUrl + '/start'});
				break;
			case 'btn-generation-stop':
				$.ajax({type: 'GET', url: apiUrl + '/stop'});
				break;
			}
		}
	});

	$('#btn-stats').button().click(function () {
		if (!$(this).hasClass('active')) {
			$(this).addClass('active');
			$('#map-stats').show();
			statsAgent.start();
		} else {
			$(this).removeClass('active');
			$('#map-stats').hide();
			statsAgent.stop();
		}
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
		'process': function() {
			var eps = (this.numEvent / this.frequency) * 1000;
			this.numEvent = 0;
			if (this.epsCallback) this.epsCallback(eps);
		},
		'start': function() {
			this.stop();
			this.timer = setInterval(function() {statsAgent.process();}, this.frequency);
			this.running = true;
			this.numEvent = 0;
		},
		'stop': function() {
			clearInterval(this.timer);
			this.running = false;
		},
		'notify': function() {
			this.numEvent++;
			this.totalEvent++;
			if (this.totalCallback) this.totalCallback(this.totalEvent);
		}
};

var mapAgent = {
		map: null,
		'init': function(target, center, zoom) {
			var map = L.map(target).setView(center, zoom);
			L.tileLayer('http://{s}.mqcdn.com/tiles/1.0.0/osm/{z}/{x}/{y}.png', {
				subdomains: ['otile1','otile2','otile3','otile4'],
				maxZoom: 18,
			}).addTo(map);
			this.map = map;
			// Add listeners
			map.on('click', function(event) {
				var latLng = event.latlng;
				console.log('Trigger event:', JSON.stringify(latLng));
				webSocketAgent.trigger(latLng);
			});
			map.on('moveend', function(event) {
				var bounds = mapAgent.getBounds();
				console.log('Map bounds changed:', JSON.stringify(bounds));
				webSocketAgent.update(bounds);
			});
		},
		'getBounds': function() {
			var bounds = this.map.getBounds();
			return {
				'southLat': bounds.getSouthWest().lat,
				'northLat': bounds.getNorthEast().lat,
				'westLng': bounds.getSouthWest().lng,
				'eastLng': bounds.getNorthEast().lng
			};
		},
		'getBoundsHeader': function() {
			var bounds = this.getBounds();
			return bounds.southLat + ',' + bounds.northLat + ',' + bounds.westLng + ',' + bounds.eastLng;
		},
		'drawEvent': function(json) {
			var marker = L.marker([json.lat, json.lng]).addTo(mapAgent.map);
			setTimeout(function() {
				mapAgent.map.removeLayer(marker)
			}, 2000);
		}
};

var webSocketAgent = {
		socket: null,
		'connect': function() {
			var request = {
					url: apiUrl,
					logLevel : 'info',
					transport: 'websocket', /* websocket, jsonp, long-polling, polling, streaming */
					fallbackTransport: 'streaming',
					attachHeadersAsQueryString: true,
					headers: {'X-Map-Bounds': mapAgent.getBoundsHeader()}
			};
			request.onOpen = function(response) {
				console.log('Connected to realtime endpoint using ' + response.transport);
			};
			request.onReconnect = function(response) {
				console.log('Reconnecting to realtime endpoint');
			};
			request.onClose = function(response) {
				console.log('Disconnected from realtime endpoint');
			};
			request.onMessage = function (response) {
				var data = response.responseBody;
				if (data.length > 0) {
					statsAgent.notify();
					console.log('Message Received using ' + response.transport + ': ' + data);
					var json = JSON.parse(data);
					mapAgent.drawEvent(json);
				}
			};
			this.socket = $.atmosphere.subscribe(request);
		},
		'disconnect': function() {
			$.atmosphere.unsubscribe();
			this.socket = null;
		},
		'update': function(bounds) {
			if (!this.socket) return;
			this.socket.push(JSON.stringify(bounds));
		},
		'trigger': function(latlng) {
			if (!this.socket) return;
			var data = {'lat': latlng.lat, 'lng': latlng.lng};
			$.ajax({
				type: 'POST',
				url: apiUrl + '/event',
				contentType: 'application/json',
				data: JSON.stringify(data)
			});
		}
};

