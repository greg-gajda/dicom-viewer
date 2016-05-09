var viewerServices = angular.module('viewer.services', ['ngResource']);


viewerServices.factory('Viewer', [ '$resource', function($resource) {
	return $resource('../rest/viewer/config', {}, {
		query: {
			method : 'GET',
			isArray : false
		}
	});
} ]);

viewerServices.factory('Dicom', [ '$resource', function($resource) {
	return $resource('rest/dicom/:method', {}, {
		studies: {
			method : 'GET',
			params:{method:'studies'},
			isArray : true
		},
		series: {
			method : 'GET',
			params:{method:'series'},
			isArray : true
		},
		image: {
			method : 'GET',
			params:{method:'image'},
			isArray : false
		},
		header: {
			method : 'GET',
			params:{method:'header'},
			isArray : true
		},
		images: {
			method : 'GET',
			params:{method:'images'},
			isArray : true
		},
		mpr: {
			method : 'GET',
			params:{method:'mpr'},
			isArray : true
		},
		oblique: {
			method : 'GET',
			params:{method:'oblique'},
			isArray : false
		},
	});
} ]);

