// Copyright 2010 Google

/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Adapted by Peter Kuterna for the Devoxx conference.
 */
function Devoxx() {
	this.center_ = new google.maps.LatLng(51.246098, 4.417287);
	
	this.mapDiv_ = document.getElementById(this.MAP_ID);

	this.map_ = new google.maps.Map(this.mapDiv_, {
		zoom : 17,
		center : this.center_,
		navigationControl : true,
		navigationControlOptions : {
			style : google.maps.NavigationControlStyle.ANDROID
		},
		mapTypeControl : false,
		scaleControl : true,
		mapTypeId : google.maps.MapTypeId.HYBRID
	});
	
	this.infowindow_ = new google.maps.InfoWindow();

	that = this;
	
	this.addBuildingPolygon_();

	google.maps.event.addListenerOnce(this.map_, 'tilesloaded', function() {
		if (that.hasMapContainer_()) {
			MAP_CONTAINER.onMapReady();
		}
	});
}

/**
 * @type {string}
 */
Devoxx.prototype.MAP_ID = 'map-canvas';

/**
 * @type {boolean}
 * @private
 */
Devoxx.prototype.ready_ = false;

/**
 * Checks if a MAP_CONTAINER object exists
 * 
 * @return {boolean} Whether the object exists or not.
 * @private
 */
Devoxx.prototype.hasMapContainer_ = function() {
	return typeof (window['MAP_CONTAINER']) !== 'undefined';
};

Devoxx.prototype.addBuildingPolygon_ = function() {
	var polygonCoords = [
		new google.maps.LatLng(51.246459, 4.418073),
		new google.maps.LatLng(51.246459, 4.41777),
		new google.maps.LatLng(51.246511, 4.41777),
		new google.maps.LatLng(51.246511, 4.41737),
		new google.maps.LatLng(51.246459, 4.41737),
		new google.maps.LatLng(51.246459, 4.417067),
		new google.maps.LatLng(51.246404, 4.417067),
		new google.maps.LatLng(51.246404, 4.416815),
		new google.maps.LatLng(51.246347, 4.416815),
		new google.maps.LatLng(51.246347, 4.416606),
		new google.maps.LatLng(51.245853, 4.416606),
		new google.maps.LatLng(51.245853, 4.416815),
		new google.maps.LatLng(51.245792, 4.416815),
		new google.maps.LatLng(51.245792, 4.417067),
		new google.maps.LatLng(51.245749, 4.417067),
		new google.maps.LatLng(51.245749, 4.41737),
		new google.maps.LatLng(51.245693, 4.41737),
		new google.maps.LatLng(51.245693, 4.41777),
		new google.maps.LatLng(51.245754, 4.41777),
		new google.maps.LatLng(51.245754, 4.418073)
	];
	
	var buildingPolygon = new google.maps.Polygon({
		paths: polygonCoords,
	    strokeColor: "#FF0000",
	    strokeOpacity: 0.8,
	    strokeWeight: 2,
	    fillColor: "#FF0000",
	    fillOpacity: 0.35
	});
	
	buildingPolygon.setMap(this.map_);
	
	google.maps.event.addListener(buildingPolygon, 'click', this.showMetropolisInfo_);
};

Devoxx.prototype.showMetropolisInfo_ = function(event) {
	var contentString = "<b>MetroPolis Business Center</b><br/>Metropolis Antwerp<br/>Groenendaallaan 394<br/>2030 Antwerp<br/>Tel.: <a href=\"javascript:MAP_CONTAINER.makeCall('+3235443600');\">+32 3 544 36 00</a><br/>Fax: +32 3 544 36 06<br/>";

	that.infowindow_.setContent(contentString);
	that.infowindow_.setPosition(event.latLng);

	that.infowindow_.open(that.map_);
};

function NavigationControl(controlDiv, map) {
	controlDiv.style.padding = '5px';

	var carNavUI = document.createElement('DIV');
	carNavUI.style.cursor = 'pointer';
	carNavUI.style.textAlign = 'center';
	carNavUI.title = 'Car navigation';
	controlDiv.appendChild(carNavUI);
	var carNavText = document.createElement('DIV');
	carNavText.innerHTML = '<img width="24px" height="23px" src="images/car.png">';
	carNavUI.appendChild(carNavText);

	var walkNavUI = document.createElement('DIV');
	walkNavUI.style.cursor = 'pointer';
	walkNavUI.style.textAlign = 'center';
	walkNavUI.title = 'Walk navigation';
	controlDiv.appendChild(walkNavUI);
	var walkNavText = document.createElement('DIV');
	walkNavText.innerHTML = '<img width="24px" height="23px" src="images/walk.png">';
	walkNavUI.appendChild(walkNavText);

	google.maps.event.addDomListener(carNavUI, 'click', function() {
		MAP_CONTAINER.carNavigate();
	});
	google.maps.event.addDomListener(walkNavUI, 'click', function() {
		MAP_CONTAINER.walkNavigate();
	});
}

Devoxx.prototype.showNavigationButtons = function() {
	var navControlDiv = document.createElement('DIV');
	var navControl = new NavigationControl(navControlDiv, this.map_);

	navControlDiv.index = 1;
	this.map_.controls[google.maps.ControlPosition.TOP_LEFT].push(navControlDiv);
};

var devoxx = new Devoxx();
