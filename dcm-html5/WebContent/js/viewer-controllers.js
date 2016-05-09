var viewerControllers = angular.module('viewer.controllers', []);

viewerControllers.controller('StudiesCtrl', function($scope, $timeout, $location, $route, config, Upload, Dicom) {

	$scope.uploadDisabled = config.uploadDisabled || false;

	$scope.studies = [];

	$scope.show = function(modality, tag) {
		$scope.modality = modality;		
		var param = { modality: $scope.modality };
		if(tag !== undefined){
			param.tag = tag;
		}
		Dicom.studies(param, function(response) {
			$scope.studies = response;
			$scope.reload();
			$scope.tag = '';			
		});	
	};
	
	$scope.reload = function() {
		angular.element('#box-container').trigger('reload');
	};
	
	$scope.thumbnail = function(study) {
		return 'viewer?study=' + study.studyInstanceUID + '&format=PNG&size=256';
	};
	
	$scope.viewImage = function(study) {
		Dicom.image({study: study.studyInstanceUID}, function(response){		
			$location.path('/viewer/' + response.sopInstanceUID);
		});				
	};
	
	$scope.uploadFiles = function (files) {
        $scope.files = files;
        if (files && files.length) {        	
            Upload.upload({
                url: 'uploader',
                data: {
                    files: files
                }
            }).then(function (response) {
                $timeout(function () {
                    $scope.result = response.data;
                    $scope.show('ALL');
                });                
            }, function (response) {
                if (response.status > 0) {
                    $scope.errorMsg = response.status + ': ' + response.data;
                }
            }, function (evt) {
                $scope.progress = 
                    Math.min(100, parseInt(100.0 * evt.loaded / evt.total));
            });
        }
    };
    
    $scope.init = function() {
		Dicom.studies({ modality: 'ALL' }, function(response) {
			$scope.studies = response;
		});	    	
    }();
	
});

viewerControllers.controller('InfoCtrl', function($scope, $routeParams, $location, config, Dicom) {
	$scope.$routeParams = $routeParams;

	$scope.headers = [];
	$scope.init = function(){
		Dicom.header({image: $routeParams.image}, function(response){
			$scope.headers = response.sort(function(a, b){
				var ka = Object.keys(a)[0];
				var kb = Object.keys(b)[0];
				return ka > kb ? 1 : ka < kb ? -1 : 0;
			}).map(function(h){ 		
				var key = Object.keys(h)[0];
				var obj = {"key": key, "value": h[key]};
				return obj; 				
			});
		});
	}();	
});

viewerControllers.controller('ThreeDCtrl', function($scope, $routeParams, $http, config, Dicom) {
	
	$scope.renderer3d = null;
	$scope.init = function(){
		$http({
		    method: 'GET',
		    url: 'download?id='+$routeParams.study+'.3d'
		}).success(function(response){			
			$scope.renderer3d = new Renderer3D(document.getElementById("viewer"));			
			$scope.renderer3d.createCanvas();
			$scope.renderer3d.loadDicom(response);			
		});
		
	}();	
});

viewerControllers.controller('ViewerCtrl', function($scope, $interval, $timeout, $routeParams, $location, config, Dicom) {
	
	angular.extend(this, new BaseViewerCtrl($scope, $interval, $location, config, Dicom, ['UNKNOWN']));
	$scope.canvasWidth = 720;
	$scope.canvasHeight = { 'UNKNOWN' : 720 };
		
	$scope.mpr = function() {	
		if($scope.series[$scope.projection].images){
			return $scope.series[$scope.projection].images[$scope.slider[$scope.projection]];		
		}
		return {};
	};
	
	$scope.thumbnail = function(image) {
		return 'viewer?image=' + image + '&format=PNG&size=100';
	};
   
	$scope.changeImage = function(index, image){
		$scope.series[$scope.projection] = angular.copy($scope.series[index]);
		$scope.viewImage($scope.projection, image, $scope.action.wc, $scope.action.ww);			
	};
    
	$scope.showSlider[$scope.projection] = true;
	$timeout(function(){			
		$scope.showSlider[$scope.projection] = false;
		if($scope.canvas['UNKNOWN'].showAnnotations !== undefined){
			$scope.canvas['UNKNOWN'].showAnnotations($scope.image['UNKNOWN'].sopInstanceUID);
		}
		$scope.canvas['UNKNOWN'].renderAll();
	}, 2500);
	
	$scope.measurements = function(tool){
		$scope.action.mode = tool;
	};
	
	$scope.measurement = function(tool){
		var object = measurements.objects.find(function(o){
			return o == tool;
		});
		return object;
	};
	
	$scope.viewer_onmousedown = function(options, projection){
		var point = $scope.canvas[projection].getPointer(options.e);		
		var object = $scope.measurement($scope.action.mode);
		if(object != undefined){
			var check = measurements.check(object, point);
			if(check == null){
				var ps = $scope.series[projection].images[$scope.slider[projection]].pixelSpacing;
				var scale = {sx: $scope.image[projection].scaleX, sy: $scope.image[projection].scaleY}; 
				if(object == 'LINE'){
					measurements.line($scope.canvas[projection], [point, point], ps, scale);
				}else if(object == 'RECTANGLE'){
					measurements.rectangle($scope.canvas[projection], [point, point], ps, scale);
				}else if(object == 'TETRAGON'){
					measurements.tetragon($scope.canvas[projection], [point, point], ps, scale);
				}else if(object == 'ELLIPSE'){
					measurements.ellipse($scope.canvas[projection], [point, point], ps, scale);
				}else if(object == 'ANGLE'){
					measurements.angle($scope.canvas[projection], [point, point, point], ps);
				}
				$scope.action.measurement = {object: object, index: 1};
			}else{
				$scope.action.measurement = check;
			}					
		} else {
			$scope.action.measurement = measurements.mouseOverPoints($scope.canvas[projection], point);
			if($scope.action.measurement != null){
				$scope.action.mode = $scope.action.measurement.object;
				$scope.$apply();
			}
		}
	};
	
    $scope.viewer_onmousemove = function(options, projection){
    	var point = $scope.canvas[projection].getPointer(options.e);    	
		if($scope.action.measurement !== undefined && $scope.action.measurement != null){    			
			var ps = $scope.series[projection].images[$scope.slider[projection]].pixelSpacing;
			var scale = {sx: $scope.image[projection].scaleX, sy: $scope.image[projection].scaleY};
			measurements.move($scope.canvas[projection], $scope.action.measurement.object, $scope.action.measurement.index, point, ps, scale);
		}
    };
    
    $scope.viewer_onmouseup = function(options, projection){
    	if($scope.action.measurement !== undefined && $scope.action.measurement != null){
    		measurements.done($scope.action.measurement.object);
    		delete $scope.action.measurement;
    		$scope.action.mode = '';
    	}    	
    };
    
	$scope.init = function(){	
		Dicom.series({image: $routeParams.image}, function(response){
			$scope.series[$scope.projection] = {};
			for(var i=0; i<response.length; ++i){
				$scope.series[i] = response[i];	
				$scope.series[i].index = i;
				(function( series, loadImage ) {
					Dicom.images({series: series.seriesInstanceUID}, function(response){
						series.images = response;
						if(loadImage){
							var image = series.images.find(function(i){
								return i.sopInstanceUID == $routeParams.image;
							});
							if(image === undefined){
								image = series.images[0];
							}
							var wc = $scope.split(image.windowCenter)[0] || 320;
		    		    	var ww = $scope.split(image.windowWidth)[0] || 320;
		    		    	$scope.series[$scope.projection] = angular.copy(series);
							$scope.viewImage($scope.projection, image.sopInstanceUID, wc, ww);		    				
						}
					});									
				})( $scope.series[i], i == 0 );		
			}	
		});
		
		$scope.projections.forEach(function(p){
			$scope.canvas[p] = new fabric.Canvas('viewer');
			$scope.canvas[p].selection = false;
			$scope.canvas[p].on('mouse:down', function(e) { $scope.onmousedown(e, p, $scope.viewer_onmousedown); });
			$scope.canvas[p].on('mouse:move', function(e) { $scope.onmousemove(e, p, $scope.viewer_onmousemove); });
			$scope.canvas[p].on('mouse:up', function(e) { $scope.onmouseup(e, p, $scope.viewer_onmouseup); });
			$($scope.canvas[p].wrapperEl).on('mousewheel', function(e) { $scope.onmousewheel(e, p); });
			$('#image-container').scroll(function() {
				$scope.canvas[p].renderAll();
			});
			$scope.canvas[p].showAnnotations = $scope.showAnnotations; 
		});			
	}();

});

viewerControllers.controller('OrthoMprCtrl', function($scope, $interval, $routeParams, $location, config, Dicom) {
		
	angular.extend(this, new BaseViewerCtrl($scope, $interval, $location, config, Dicom, ['AXIAL', 'CORONAL', 'SAGITTAL']));
	
	$scope.viewerImage = $routeParams.image;
	$scope.canvasWidth = 540;
	$scope.canvasHeight = {};
	
	$scope.projections.forEach(function(p){
		$scope.canvasHeight[p] = 364;
	});	
	$scope.canvasHeight[$scope.projection] = 730;
    
    $scope.syncOrtoMpr = function(x, y, projection){
    	var ixy = $scope.pointPosition(x, y, projection);
    	var ix = ixy.x;
    	var iy = ixy.y;
    	if(projection == 'AXIAL'){
    		ix = $scope.series['SAGITTAL'].images.length - ix; 
    		$scope.viewImage('SAGITTAL', $scope.series['SAGITTAL'].images[ix].sopInstanceUID, $scope.action.wc, $scope.action.ww);
			$scope.viewImage('CORONAL', $scope.series['CORONAL'].images[iy].sopInstanceUID, $scope.action.wc, $scope.action.ww);
			$scope.slider['SAGITTAL'] = ix;
			$scope.slider['CORONAL'] = iy;
		}else if(projection == 'CORONAL'){
			$scope.viewImage('SAGITTAL', $scope.series['SAGITTAL'].images[ix].sopInstanceUID, $scope.action.wc, $scope.action.ww);
			iy = $scope.series['AXIAL'].images.length - iy;
			$scope.viewImage('AXIAL', $scope.series['AXIAL'].images[iy].sopInstanceUID, $scope.action.wc, $scope.action.ww);    	    			
			$scope.slider['SAGITTAL'] = ix;
			$scope.slider['AXIAL'] = iy;			
		}else if(projection == 'SAGITTAL'){
			$scope.viewImage('CORONAL', $scope.series['CORONAL'].images[ix].sopInstanceUID, $scope.action.wc, $scope.action.ww);
			iy = $scope.series['AXIAL'].images.length - iy;
			$scope.viewImage('AXIAL', $scope.series['AXIAL'].images[iy].sopInstanceUID, $scope.action.wc, $scope.action.ww);
			$scope.slider['CORONAL'] = ix;
			$scope.slider['AXIAL'] = iy;			
		}    	    	
    };
    	
    $scope.sync = function() {
    	if($scope.action.mode == 'SYNC'){
    		$scope.action.mode = '';
    	}else{
    		$scope.action.mode = 'SYNC';
    	}	    	
    };
    
    $scope.mpr_onmouseup = function(options, projection){
    	var obj = $scope.canvas[projection].getPointer(options.e);    
	    if($scope.action.mode == 'SYNC'){
	    	$scope.syncOrtoMpr(obj.x, obj.y, projection);
			if($scope.action.sync1 !== undefined){
				$scope.canvas[projection].remove($scope.action.sync1);
				delete $scope.action.sync1;
			}
			if($scope.action.sync2 !== undefined){
				$scope.canvas[projection].remove($scope.action.sync2);
				delete $scope.action.sync1;
			}			
	    }
    };
    
    $scope.mpr_onmousemove = function(options, projection){
    	var obj = $scope.canvas[projection].getPointer(options.e);    
    	if($scope.action.mode == 'SYNC'){
			if($scope.action.sync1 !== undefined){
				$scope.canvas[projection].remove($scope.action.sync1);	
			}
			if($scope.action.sync2 !== undefined){
				$scope.canvas[projection].remove($scope.action.sync2);	
			}
			if($scope.projection == projection){
				$scope.action.sync1 = fabricLine([obj.x, 0, obj.x, $scope.canvas[projection].getHeight()]);
				$scope.canvas[projection].add($scope.action.sync1);
				$scope.action.sync2 = fabricLine([0, obj.y, $scope.canvas[projection].getWidth(), obj.y]);
				$scope.canvas[projection].add($scope.action.sync2);				
		    	var dif = Number(Date.now()) - $scope.slideDelay;
		    	if(dif > config.imageSlideDelay){
		    		$scope.slideDelay = Date.now();
		    		$scope.syncOrtoMpr(obj.x, obj.y, projection);
		    	}				
				$scope.canvas[projection].renderAll();
			}			
		}
    };
        
	$scope.init = function(){
		
		Dicom.series({series: $routeParams.series}, function(response){	
			if(response[0].projection == 'UNKNOWN'){
				response[0].projection = 'AXIAL';
			}
			var projection = response[0].projection;
			$scope.series[projection] = response[0];
						
			(function( series ) {
				Dicom.images({series: series.seriesInstanceUID}, function(response){
					series.images = response;
					var image = series.images.find(function(i){
						return i.sopInstanceUID == $routeParams.image;
					});								
					$scope.viewImage(series.projection, image.sopInstanceUID);
					
					Dicom.mpr({series: $routeParams.series}, function(response){
						var series = {};
						response.forEach(function(i){
							if(series[i.series.projection] === undefined){
								series[i.series.projection] = i.series;
								series[i.series.projection].images = [];					
							}
							series[i.series.projection].images.push(i);				
						});				
						$scope.projections.forEach(function(p){
							if(series[p] !== undefined){
								$scope.series[p] = series[p];	
								$scope.viewImage(p, series[p].images[series[p].images.length / 2].sopInstanceUID);
								$scope.slider[p] = series[p].images.length / 2;
							}
						});
					});								
				});									
			})( $scope.series[projection] );				
		});
		
		$scope.projections.forEach(function(p){
			$scope.canvas[p] = new fabric.Canvas(p.toLowerCase() + '-viewer');
			$scope.canvas[p].selection = false;
			$scope.canvas[p].on('mouse:down', function(e) { $scope.onmousedown(e, p); });
			$scope.canvas[p].on('mouse:move', function(e) { $scope.onmousemove(e, p, $scope.mpr_onmousemove); });
			$scope.canvas[p].on('mouse:up', function(e) { $scope.onmouseup(e, p, $scope.mpr_onmouseup); });
			$($scope.canvas[p].wrapperEl).on('mousewheel', function(e) { $scope.onmousewheel(e, p); });
			$('#' + p.toLowerCase() + '-container').scroll(function() {
				$scope.canvas[p].renderAll();
			});		
			$scope.canvas[p].setWidth($scope.canvasWidth - 20);
			$scope.canvas[p].setHeight($scope.canvasHeight[p]);						
		});	
	}();
});

viewerControllers.controller('ObliqueMprCtrl', function($scope, $interval, $routeParams, $location, config, Dicom) {
	
	angular.extend(this, new BaseViewerCtrl($scope, $interval, $location, config, Dicom, ['UNKNOWN', 'OBLIQUE']));
	
	$scope.viewerImage = $routeParams.image;
	$scope.canvasWidth = 540;
	$scope.canvasHeight = {};
	
	$scope.projections.forEach(function(p){
		$scope.canvasHeight[p] = 600;
	});	
    	
    $scope.oblique = function() {
    	if($scope.action.mode == 'OBLIQUE'){
    		$scope.action.mode = '';
    		if($scope.action.oblique !== undefined){
				$scope.action.oblique.fabrics.forEach(function(f){
					$scope.canvas[$scope.projection].remove(f);	
				});
				$scope.canvas[$scope.projection].renderAll();
				delete $scope.action.oblique;
	    	}
    	}else{
    		$scope.action.mode = 'OBLIQUE';
    	}	    	
    };
    
    $scope.clearMpr  = function(options, projection){
    	$scope.action.mode = '';
		if($scope.action.oblique !== undefined){
			$scope.action.oblique.fabrics.forEach(function(f){
				$scope.canvas[$scope.projection].remove(f);	
			});
			$scope.canvas[$scope.projection].renderAll();
			delete $scope.action.oblique;
		}				    	
    };

    $scope.mpr_onmousedown = function(options, projection){
    	var obj = $scope.canvas[projection].getPointer(options.e);    
	    if($scope.action.mode == 'OBLIQUE' && projection != 'OBLIQUE'){
	    	if($scope.action.oblique === undefined){
	    		$scope.action.oblique = {index: -1, points: [$scope.action.mouse, obj], fabrics: []};
	    	} else {
	    		delete $scope.action.oblique.index;
	    		var x = obj.x;
	    		var y = obj.y;
	    		for(var i = 0; i < $scope.action.oblique.points.length; ++i){
	    			var p = $scope.action.oblique.points[i];
	    			if(x >= p.x -5 && x <= p.x +5 && y >= p.y -5 && y <= p.y +5){
	    				$scope.action.oblique.index = i == 0 ? i : i + 1;
	    			}	    			
	    		}	    		
	    		if ($scope.action.oblique.index == 0){	
					$scope.action.oblique.points.splice(0, 0, obj);					
				} else if ($scope.action.oblique.index == $scope.action.oblique.points.length){
					$scope.action.oblique.points.push(obj);
					$scope.action.oblique.index++;
				}	    					    		
	    	}
	    }
    };        
    
    $scope.mpr_onmousemove = function(options, projection){
    	var obj = $scope.canvas[projection].getPointer(options.e);    
		if($scope.projection == projection && $scope.action.mode == 'OBLIQUE' && projection != 'OBLIQUE'){		
			$scope.action.oblique.fabrics.forEach(function(f){
				$scope.canvas[projection].remove(f);	
			});
			$scope.action.oblique.fabrics = [];
			var idx = $scope.action.oblique.index;
			if(idx < 0){
				$scope.action.oblique.points[1] = obj;
			} else if (idx == 0){	
				$scope.action.oblique.points[0] = obj;
			} else if (idx == $scope.action.oblique.points.length){
				$scope.action.oblique.points[$scope.action.oblique.points.length - 1] = obj;
			} else if (idx > 0){
				$scope.action.oblique.points[idx -1] = obj;
			}
			for(var i = 0; i < $scope.action.oblique.points.length-1; ++i){
				var p1 = $scope.action.oblique.points[i];
				var p2 = $scope.action.oblique.points[i+1];
				$scope.action.oblique.fabrics.push(fabricLine([p1.x, p1.y, p2.x, p2.y]));
			}
			$scope.action.oblique.points.forEach(function(p){
				$scope.action.oblique.fabrics.push(fabricCircle(p, 6));
			});
			var l1 = line($scope.action.oblique.points[0], $scope.action.oblique.points[1] ,12);
			$scope.action.oblique.fabrics.push(fabricLine([l1.c1.x, l1.c1.y, l1.c2.x, l1.c2.y]));
			var length = $scope.action.oblique.points.length;
			var l2 = line($scope.action.oblique.points[length - 1], $scope.action.oblique.points[length - 2] ,12);
			$scope.action.oblique.fabrics.push(fabricLine([l2.c1.x, l2.c1.y, l2.c2.x, l2.c2.y]));
			$scope.action.oblique.fabrics.forEach(function(f){
				$scope.canvas[projection].add(f);
			});
			$scope.canvas[projection].renderAll();
		}
    };
        
    $scope.mpr_onmouseup = function(options, projection){    	    
	    if($scope.action.mode == 'OBLIQUE' && $scope.action.oblique.index !== undefined && projection != 'OBLIQUE'){
	    	var points = '';
	    	$scope.action.oblique.points.forEach(function(p){
	        	var ixy = $scope.pointPosition(p.x, p.y, projection);
	    		points += ixy.x+','+ixy.y+',';
	    	});
	    	Dicom.oblique({series: $routeParams.series, points: points}, function(response){
	    		$scope.series['OBLIQUE'] = response.series;
	    		$scope.series['OBLIQUE'].images = [response];
	    		$scope.viewImage('OBLIQUE', response.sopInstanceUID);	
	    	});
	    }
    };
    
	$scope.init = function(){
		
		Dicom.series({series: $routeParams.series}, function(response){	
			response[0].projection = 'UNKNOWN';
			var projection = response[0].projection;
			$scope.series[projection] = response[0];
						
			(function( series ) {
				Dicom.images({series: series.seriesInstanceUID}, function(response){
					series.images = response;
					var image = series.images.find(function(i){
						return i.sopInstanceUID == $routeParams.image;
					});								
					$scope.viewImage(series.projection, image.sopInstanceUID);
				});									
			})( $scope.series[projection] );	
		});
		
		$scope.projections.forEach(function(p){
			$scope.canvas[p] = new fabric.Canvas(p.toLowerCase() + '-viewer');
			$scope.canvas[p].selection = false;
			$scope.canvas[p].on('mouse:down', function(e) { $scope.onmousedown(e, p, $scope.mpr_onmousedown); });
			$scope.canvas[p].on('mouse:move', function(e) { $scope.onmousemove(e, p, $scope.mpr_onmousemove); });
			$scope.canvas[p].on('mouse:up', function(e) { $scope.onmouseup(e, p, $scope.mpr_onmouseup); });
			$($scope.canvas[p].wrapperEl).on('mousewheel', function(e) { $scope.onmousewheel(e, p, $scope.clearMpr); });
			$('#' + p.toLowerCase() + '-container').scroll(function() {
				$scope.canvas[p].renderAll();
			});					
			$scope.canvas[p].setWidth($scope.canvasWidth - 20);
			$scope.canvas[p].setHeight($scope.canvasHeight[p]);			
		});		
		$scope.displayText('OBLIQUE', 'No image yet', {x: $scope.canvasWidth / 2, y: $scope.canvasHeight['OBLIQUE'] / 2}, 30, true);
		$scope.canvas['OBLIQUE'].renderAll();		
	}();
});