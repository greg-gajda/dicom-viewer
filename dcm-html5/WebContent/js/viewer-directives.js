var directives = angular.module('viewer.directives', []);

directives.directive("masonry", function($timeout) {
	return {
		restrict : 'AC',
		link : function(scope, elem, attrs) {

			$timeout(function() {
				elem.imagesLoaded().always(function(instance) {
					elem.masonry();
				}).done(function(instance) {
					console.log('all images successfully loaded');
					elem.masonry();
				}).fail(function() {
					console.log('all images loaded, at least one is broken');
					elem.masonry();
				}).progress( function(instance, image) {
					var result = image.isLoaded ? 'loaded' : 'broken';
					console.log('image is ' + result + ' for ' + image.img.src);
					elem.masonry();
				});
			}, 250);

			elem.bind('reload', function() {
				elem.imagesLoaded(function() {
					$timeout(function() {
						elem.masonry('reloadItems');
						elem.masonry('layout');
					}, 100);
				});
			});
		}
	};
});

directives.directive('onEnter', function() {
	var linkFn = function(scope, element, attrs) {
		element.bind("keypress", function(event) {
			if (event.which === 13) {
				scope.$apply(function() {
					scope.$eval(attrs.onEnter);
				});
				event.preventDefault();
			}
		});
	};
	return {
		link : linkFn
	};
});
