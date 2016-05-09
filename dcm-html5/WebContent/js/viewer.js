var viewer = angular.module('viewer', [ 'ngCookies', 'ngRoute', 'ngResource',
		'viewer.services', 'viewer.controllers', 'viewer.directives',
		'ui.bootstrap', 'ngFileUpload' ]);

viewer.constant("config", {
	"showAbout" : true,
	"uploadDisabled" : true,
	"displayAnnotations" : true,
	"blockImageWheel" : false,
	"minZoomFactor" : 0.1,
	"maxZoomFactor" : 2,
	"imageWCWWDelay" : 100,
	"imageSlideDelay" : 250,
	"mouseWheelDelay" : 250,
	"cineModeDelay" : 500
});

viewer.config([ '$routeProvider', function($routeProvider) {
	$routeProvider.when('/about', {
		templateUrl : 'pages/about.html'
	});
	$routeProvider.when('/policy', {
		templateUrl : 'pages/policy.html'
	});
	$routeProvider.when('/studies', {
		templateUrl : 'pages/studies.html',
		controller : 'StudiesCtrl'
	});
	$routeProvider.when('/info/:image', {
		templateUrl : 'pages/info.html',
		controller : 'InfoCtrl'
	});
	$routeProvider.when('/viewer/:image', {
		templateUrl : 'pages/viewer.html',
		controller : 'ViewerCtrl'
	});
	$routeProvider.when('/mpr/:series/:image', {
		templateUrl : 'pages/mpr.html',
		controller : 'OrthoMprCtrl'
	});
	$routeProvider.when('/mpr2/:series/:image', {
		templateUrl : 'pages/mpr2.html',
		controller : 'ObliqueMprCtrl'
	});
	$routeProvider.when('/threed/:study', {
		templateUrl : 'pages/threed.html',
		controller : 'ThreeDCtrl'
	});
	$routeProvider.otherwise({
		redirectTo : '/studies'
	});
} ]);

viewer.run(function($rootScope) {
	$rootScope.title = 'DICOM html5 viewer';
});

viewer.config([
		'$httpProvider',
		function($httpProvider) {
			$httpProvider.interceptors.push(function($q, $window,
					config) {
				return {					
					response: function(response) {
						$("#spinner").hide();
				        return response;
				      },
				};
				
			});
			$httpProvider.defaults.transformRequest
					.push(function spinnerFunction(data, headersGetter) {
						$("#spinner").show();
						return data;
					});
		}]);
