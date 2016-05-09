var BaseViewerCtrl = function ($scope, $interval, $location, config, Dicom, projections, initialize) {
	$scope.projections = projections;
	$scope.projection = $scope.projections[0]; 
	$scope.cine = false;
    $scope.cineInterval = null;
    $scope.image = {};
    $scope.canvas = {};
    $scope.series = {};
	$scope.slider = {};
	$scope.showSlider = {}; 
	$scope.action = {};	
    $scope.slideDelay = 0;
    $scope.wheelDelay = 0;    
    $scope.wcwwDelay = 0;
    $scope.annotations = {};
    $scope.displayAnnotations = config.displayAnnotations;
    
    $scope.showAbout = config.showAbout || false;
    
    $scope.windowlevel = [{name: 'Brain', wc: 35, ww: 110},
                          {name: 'Lung', wc: -500, ww: 1500},
                          {name: 'Abdomen', wc: 60, ww: 360},
                          {name: 'Liver', wc: 100, ww: 200},
                          {name: 'Heart', wc: 90, ww: 750},
                          {name: 'Mediastinum', wc: 80, ww: 450},
                          {name: 'Bone', wc: 400, ww: 2000},
                          {name: 'Backbone', wc: 60, ww: 300},                          
                          {name: 'Image default'}];
    
    $scope.selectWCWW = function(wl){
    	$scope.action.wc = wl.wc;
    	$scope.action.ww = wl.ww;
		$scope.projections.forEach(function(p){
			$scope.viewImage(p, $scope.image[p].sopInstanceUID, $scope.action.wc, $scope.action.ww);
		});		    	    	
    };
    
    $scope.cinema = function(){
    	if($scope.cine == true){
    		if($scope.cineInterval != null){
    			$interval.cancel($scope.cineInterval);    		
    		}
    		$scope.cine = false;
    	}else{
    		$scope.cine = true;    		
    		$scope.cineInterval = $interval(function() {
    			$scope.nextImage($scope.projection);
            }, config.cineModeDelay);        
    	}
    };
	
	$scope.projections.forEach(function(p){
		$scope.series[p] = {};
		$scope.canvas[p] = {};
		$scope.image[p] = {};
		$scope.slider[p] = 0;
		$scope.showSlider[p] = false;
		$scope.annotations[p] = [];
	});	
	
	$scope.clearCanvas = function(projection){
		measurements.clearStorage($scope.canvas[projection]);
		if($scope.action.oblique !== undefined){
			$scope.action.oblique.fabrics.forEach(function(f){
				$scope.canvas[$scope.projection].remove(f);	
			});
			$scope.canvas[$scope.projection].renderAll();
			delete $scope.action.oblique;
    	}
	};
	
	$scope.slide = function(projection){
		$scope.projection = projection;
    	var dif = Number(Date.now()) - $scope.slideDelay;
    	if(dif > config.imageSlideDelay){
    		$scope.slideDelay = Date.now();
    		$scope.viewImage(projection, $scope.series[projection].images[$scope.slider[projection]].sopInstanceUID, $scope.action.wc, $scope.action.ww);
    	}
	};
	
    $scope.getImage = function(projection, image){    	
    	var img = $scope.series[projection].images.find(function(i){
    		return i.sopInstanceUID == image;	    			
    	});
		return img;
    };
    
    $scope.toggleAnnotations = function(){
    	$scope.displayAnnotations = !$scope.displayAnnotations;
    	config.displayAnnotations = $scope.displayAnnotations;
    	if($scope.displayAnnotations == false){
    		$scope.annotations[$scope.projection].forEach(function(a){
    			$scope.canvas[$scope.projection].remove(a);
    		});    		
    		$scope.annotations[$scope.projection] = [];    		
    	} else {
    		$scope.showAnnotations($scope.image[$scope.projection].sopInstanceUID);
    	}    	
    	$scope.canvas[$scope.projection].renderAll();    	
    };
	
    $scope.showAnnotations = function(image) {
		if($scope.displayAnnotations){
    		var img = $scope.getImage($scope.projection, image);    		
    		var base = $scope.showSlider[$scope.projection] ? 50 : 10;
    		$scope.annotations[$scope.projection].forEach(function(a){
    			$scope.canvas[$scope.projection].remove(a);
    		});    		
    		$scope.annotations[$scope.projection] = [];
    		$scope.displayText($scope.projection, img.tags['AcquisitionDate']+' '+img.tags['AcquisitionTime'], {x : 10, y: base}, 16);
    		$scope.displayText($scope.projection, img.tags['PatientName'], {x : 10, y: base+20}, 16);
    		$scope.displayText($scope.projection, img.tags['PatientID'], {x : 10, y: base+40}, 16);
    		$scope.displayText($scope.projection, img.tags['StudyDescription'], {x : 10, y: base+60}, 16);
    		$scope.displayText($scope.projection, 'Acc. No: '+img.tags['AccessionNumber'], {x : 10, y: base+80}, 16);    		
    		var wc = $scope.action.wc || $scope.split(img.windowCenter)[0] || 320;
    		var ww = $scope.action.ww || $scope.split(img.windowWidth)[0] || 320;												    		
    		$scope.displayText($scope.projection, 'Window level: '+wc+'/'+ww, {x : 10, y: base+100}, 16);
    		$scope.displayText($scope.projection, 'Frame: '+(1+Number($scope.slider[$scope.projection]))+'/'+$scope.series[$scope.projection].images.length , {x : 10, y: base+120}, 16);    		    		
		}    	
    };
    
    $scope.onmousewheel = function(options, projection, ext) {
    	var dif = Number(options.timeStamp) - $scope.wheelDelay;
    	if(dif > config.mouseWheelDelay){
    		$scope.wheelDelay = Number(options.timeStamp);
    		if(options.originalEvent.wheelDelta < 0){
    			$scope.nextImage(projection);
    		} else {
    			$scope.prevImage(projection);
    		}
    		if($scope.projection != projection){
    			$scope.projection = projection;
    			//$scope.$apply();    			
    		}
        	if(ext !== undefined){
        		ext(options, projection);
        	}    
        	$scope.$apply();
    	}
    };	
	
    $scope.onmouseup = function(options, projection, ext) {			
    	$scope.projection = projection;
    	if($scope.action.mode == 'WCWW'){
    		$scope.projections.forEach(function(p){
    			$scope.viewImage(p, $scope.image[p].sopInstanceUID, $scope.action.wc, $scope.action.ww);
    		});		
    	}
    	if(ext !== undefined){
    		ext(options, projection);
    	}
        delete $scope.action.mouse;
        $scope.$apply();
        $scope.canvas[projection].renderAll();
	};
	
	$scope.onmousedown = function(options, projection, ext) {
		$scope.projection = projection;
		$scope.$apply();
		$scope.action.mouse = $scope.canvas[projection].getPointer(options.e);		
    	if(ext !== undefined){
    		ext(options, projection);
    	}		
	};
	
	$scope.onmousemove = function(options, projection, ext) {	
		var obj = $scope.canvas[projection].getPointer(options.e);
		if($scope.action.mouse === undefined){			
			
			if($scope.projection != projection){				
				$scope.projections.forEach(function(p){
					$scope.showSlider[p] = false;
				});
				$scope.$apply();
			}
			var y = obj.y;
			if(y < 50){
				if($scope.showSlider[projection] == false){
					$scope.projection = projection;
					$scope.showSlider[projection] = true;	
					if($scope.canvas[projection].showAnnotations !== undefined){
						$scope.canvas[projection].showAnnotations($scope.image[projection].sopInstanceUID);
					}
					$scope.$apply();
				}
			}else{
				if($scope.showSlider[projection] == true){
					$scope.projection = projection;
					$scope.showSlider[projection] = false;
					if($scope.canvas[projection].showAnnotations !== undefined){
						$scope.canvas[projection].showAnnotations($scope.image[projection].sopInstanceUID);
					}
					$scope.$apply();
				}				
			}

			measurements.mouseOverPoints($scope.canvas[projection], obj);
			$scope.canvas[projection].renderAll();
			return;
		}
		if($scope.action.mode == 'MOVE'){			
	    	var x = obj.x - $scope.action.mouse.x;
			var y = obj.y - $scope.action.mouse.y;
			$scope.image[projection].set({
	            left : $scope.image[projection].left + x,
	            top : $scope.image[projection].top + y,
	        });
			$scope.image[projection].setCoords();						
			$scope.action.mouse = obj;
			$scope.clearCanvas($scope.canvas[projection]);
			$scope.canvas[projection].renderAll();
		}else if($scope.action.mode == 'WCWW'){						
	    	var dif = Number(Date.now()) - $scope.wcwwDelay;
	    	var delay = config.imageWCWWDelay;
	    	var stepx = 25;
	    	var stepy = 10;
			var image = $scope.getImage(projection, $scope.image[projection].sopInstanceUID);
			if(image.rows > 512 || image.columns > 512){
				delay *= 10;
				stepy *= 10;
				stepx *= 10;
				//java.lang.OutOfMemoryError: Direct buffer memory
			}
	    	if(dif > delay){	    		
	    		$scope.wcwwDelay = Date.now();
				if($scope.action.mouse.y - obj.y > 10){
					$scope.action.wc -= stepy;
					$scope.action.mouse = obj;
				}else if($scope.action.mouse.y - obj.y < -10){
					$scope.action.wc += stepy;
					$scope.action.mouse = obj;
				}
				if($scope.action.mouse.x - obj.x > 10){
					$scope.action.ww -= stepx;
					if($scope.action.ww < 1){
						$scope.action.ww = 1;
					}
					$scope.action.mouse = obj;
				}else if($scope.action.mouse.x - obj.x < -10){
					$scope.action.ww += stepx;
					$scope.action.mouse = obj;
				}					
				$scope.viewImage(projection, $scope.image[projection].sopInstanceUID, $scope.action.wc, $scope.action.ww);
	    	}			
		}else if($scope.action.mode == 'ZOOM'){
			var scale = $scope.image[projection].getScaleX();
			var diff = $scope.action.mouse.y - obj.y; 
			if(diff > 5){
				scale += 0.01;
				$scope.action.mouse = obj;
			}else if(diff < -5){
				scale -= 0.01;
				$scope.action.mouse = obj;
			}		    	
	    	if(scale > 2){
	    		scale = 2;
	    	}				
	    	
	    	$scope.image[projection].scale(scale);
	    	$scope.image[projection].scaleY *= $scope.image[projection].rescale;
	    	$scope.clearCanvas($scope.canvas[projection]);
	    	$scope.canvas[projection].renderAll(); 				
		}else if($scope.action.mode == 'MAGNIFIER'){
			var lp = $scope.pointPosition(obj.x, obj.y, projection);
			var cropped = new Image();
		    cropped.src = $scope.image[projection].toDataURL({
		        left: lp.x,
		        top: lp.y,
		        width: 75,
		        height: 75,
		    });
		    cropped.onload = function() {
		        image = new fabric.Image(cropped);
		        image.selectable = false;
		        image.left = lp.x + 75;
		        image.top = lp.y + 75;
		        image.scaleX = 2;
		        image.scaleY = 2;
		        image.setCoords();
				if($scope.action.magnifier !== undefined){
					$scope.canvas[projection].remove($scope.action.magnifier);
				}		        
		        $scope.action.magnifier = image;
		        $scope.canvas[projection].add($scope.action.magnifier);
		        $scope.canvas[projection].renderAll();
		    };
		}
		if(ext !== undefined){
			ext(options, projection);
		}
	};

	$scope.split = function(str){
		if(str !== undefined){
			return str.split(",").map(function(s){
				return Number(s.trim());
			});
		}
	};
		
    $scope.viewImage = function(projection, uid, wc, ww){  	

    	var ps = null;
    	for(var key in $scope.series){
    		var series = $scope.series[key];
    		if(series.images === undefined){
    			continue;
    		}
    		for(var i=0; i<series.images.length; ++i){
    			if(uid == series.images[i].sopInstanceUID){
    				$scope.slider[projection] = i;
    		    	ps = $scope.split(series.images[i].pixelSpacing) || [1, 1];    
    		    	series.images[i].rescale = ps[1] / ps[0];
    				break;    				
    			}
    		}    		
    	}
	    var url = 'viewer?image=' + uid + '&format=PNG';
	    if(wc !== undefined){
	    	url += '&wc='+wc;
	    }
	    if(ww !== undefined){
	    	url += '&ww='+ww;
	    }

    	fabric.Image.fromURL(url, function(image) {
    		
    		$scope.canvas[projection].clear().renderAll();			
			$scope.canvas[projection].backgroundColor = 'rgb(0,0,0)';			
			$scope.canvas[projection].setWidth($scope.canvasWidth - 20);
			$scope.canvas[projection].setHeight($scope.canvasHeight[projection]);

			image.left = $scope.image[projection].left || 0.5 * $scope.canvas[projection].width;
			image.top = $scope.image[projection].top || 0.5 * $scope.canvas[projection].height;
			image.sopInstanceUID = uid;
			image.centeredRotation = true;
			image.selectable = false;
			image.originX = 'center';
			image.originY = 'center';		            				
			image.angle = $scope.image[projection].angle || 0;
			image.flipX = $scope.image[projection].flipX || false;
            image.flipY = $scope.image[projection].flipY || false;
            image.minScaleLimit = config.minZoomFactor;
            image.filters = [];
            image.scaleX = $scope.image[projection].scaleX || 1;
            image.scaleY = $scope.image[projection].scaleY || 1;
            image.rescale = $scope.image[projection].rescale || (ps[1] / ps[0]); 
			if($scope.image[projection].sopInstanceUID === undefined){
				var diff = Math.min(($scope.canvas[projection].width - image.width)/$scope.canvas[projection].width , (($scope.canvas[projection].height - image.height) * image.rescale)/ $scope.canvas[projection].height* image.rescale );
				if(diff < -1){
					image.scaleX = Math.abs(1.0 / diff);
					image.scaleY = Math.abs(1.0 / diff);
				}
				image.scaleY *= image.rescale;
			}			
			$scope.canvas[projection].add(image);						
			$scope.image[projection] = image;
			measurements.clearStorage($scope.canvas[projection]);
			$scope.canvas[projection].renderAll();
			
			if($scope.canvas[projection].showAnnotations !== undefined){
				$scope.canvas[projection].showAnnotations(image.sopInstanceUID);
			}
		});
    };
    
	$scope.displayText = function(projection, text, pos, size, correction){
		var imageText = new fabric.Text(text, {  
			left: pos.x, 
			top: pos.y, 
		    /*stroke: 'white',*/
		    fill: 'white',
		    fontSize: size,
		    selectable: false
		});		
		if(correction){
			imageText.left -= 0.2 * $scope.canvas[projection].contextContainer.measureText(imageText).width;
		}
		$scope.annotations[projection].push(imageText);
		$scope.canvas[projection].add(imageText);
	};
	
    $scope.pointPosition = function(x, y, projection){
		var img = $scope.image[projection];
		var m = new Matrix().translate(img.left, img.top).rotateDeg(img.angle).scale(img.scaleX, img.scaleY);
		if(img.flipX){
			m.flipX();
		}
		if(img.flipY){
			m.flipY();
		}
		var mp = m.inverse().applyToPoint(x, y);
		return {x: Math.round(mp.x + 0.5*img.getWidth()/img.scaleX), y:Math.round(mp.y + 0.5*img.getHeight()/img.scaleY)};
    };	

    $scope.magnifierImage = function(){
    	if($scope.action.mode == 'MAGNIFIER'){
    		$scope.action.mode = '';
    		delete $scope.action.magnifier;
    	}else{
    		$scope.action.mode = 'MAGNIFIER';
    	}
    };

    $scope.moveImage = function(){
    	if($scope.action.mode == 'MOVE'){
    		$scope.action.mode = '';
    	}else{
    		$scope.action.mode = 'MOVE';
    	}
    };
    
    $scope.zoomImage = function(){
    	if($scope.action.mode == 'ZOOM'){
    		$scope.action.mode = '';
    	}else{
    		$scope.action.mode = 'ZOOM';
    	}
    };    
    
    $scope.wcwwImage = function(){
    	if($scope.action.mode == 'WCWW'){
    		$scope.action.mode = '';
        	delete $scope.action.wc;
        	delete $scope.action.ww;
    	}else{
    		$scope.action.mode = 'WCWW';    		
    		var img = $scope.getImage($scope.projection, $scope.image[$scope.projection].sopInstanceUID);
    		$scope.action.wc = $scope.split(img.windowCenter)[0] || 320;
    		$scope.action.ww = $scope.split(img.windowWidth)[0] || 320;												    		
    	}	
    };        
    
    $scope.invertImage = function(){
    	if($scope.image[$scope.projection].filters.length > 0){
    		$scope.image[$scope.projection].filters.length = 0;
    	}else{    		
        	var filter = new fabric.Image.filters.Invert();
        	$scope.image[$scope.projection].filters.push(filter);
    	}
    	$scope.image[$scope.projection].applyFilters($scope.canvas[$scope.projection].renderAll.bind($scope.canvas[$scope.projection]));    	
    };
    
    $scope.sharpenImage = function(){
    	if($scope.image[$scope.projection].filters.length > 0){
    		$scope.image[$scope.projection].filters.length = 0;
    	}else{    		
	    	var filter = new fabric.Image.filters.Convolute({
	    		  matrix: [ 0, -1,  0,
	    		           -1,  5, -1,
	    		            0, -1,  0 ]
	    	});
	    	$scope.image[$scope.projection].filters.push(filter);
    	}
    	$scope.image[$scope.projection].applyFilters($scope.canvas[$scope.projection].renderAll.bind($scope.canvas[$scope.projection]));
    };
    
    $scope.refreshImage = function(){		
		$scope.action.mode = '';
		if($scope.cineInterval != null){
			$interval.cancel($scope.cineInterval);    		
		}
		$scope.cine = false;
		if($scope.action.wc){
			delete $scope.action.wc;
		}
		if($scope.action.ww){
			delete $scope.action.ww;
		}
		$scope.projections.forEach(function(p){
			$scope.image[p] = {};
			var pos = Math.floor($scope.series[p].images.length / 2);
			$scope.viewImage(p, $scope.series[p].images[pos].sopInstanceUID);
			$scope.slider[p] = pos;
		});
		$scope.clearCanvas($scope.canvas[$scope.projection]);
    };
    
    $scope.flipXImage = function(){
    	$scope.clearCanvas($scope.canvas[$scope.projection]);
    	$scope.image[$scope.projection].flipX = !$scope.image[$scope.projection].flipX;
    	$scope.canvas[$scope.projection].renderAll();
    };

    $scope.flipYImage = function(){    	
    	$scope.clearCanvas($scope.canvas[$scope.projection]);
    	$scope.image[$scope.projection].flipY = !$scope.image[$scope.projection].flipY;
    	$scope.canvas[$scope.projection].renderAll();
    };
    
    $scope.rotateImage = function(angleOffset){
    	$scope.clearCanvas($scope.canvas[$scope.projection]);
        var angle = $scope.image[$scope.projection].getAngle() + angleOffset;
        angle = angle > 360 ? 90 : angle < 0 ? 270 : angle;
        $scope.image[$scope.projection].setAngle(angle);
    };    
    
    $scope.rotateLImage = function(){
		$scope.rotateImage(-90);
    	$scope.canvas[$scope.projection].renderAll();
    };
    
    $scope.rotateRImage = function(){
		$scope.rotateImage(90);
    	$scope.canvas[$scope.projection].renderAll();    	
    };
    
    $scope.zoomInImage = function(){
    	var scale = $scope.image[$scope.projection].getScaleX() + 0.1;
    	if(scale > config.maxZoomFactor){
    		scale = config.maxZoomFactor;
    	}
    	$scope.image[$scope.projection].scale(scale);
    	$scope.image[$scope.projection].scaleY *= $scope.image[$scope.projection].rescale;    	    	
    	$scope.clearCanvas($scope.canvas[$scope.projection]);
    	$scope.canvas[$scope.projection].renderAll(); 
    };
    
    $scope.zoomOutImage = function(){
    	var scale = $scope.image[$scope.projection].getScaleX() - 0.1;    	
    	$scope.image[$scope.projection].scale(scale); 
    	$scope.image[$scope.projection].scaleY *= $scope.image[$scope.projection].rescale;    	
    	$scope.clearCanvas($scope.canvas[$scope.projection]);
    	$scope.canvas[$scope.projection].renderAll(); 
    };
	
	$scope.nextImage = function(projection){
		if (!config.blockImageWheel){			
			if ($scope.slider[projection] >=0){
				if ($scope.slider[projection] < $scope.series[projection].images.length - 1){
					$scope.viewImage(projection, $scope.series[projection].images[$scope.slider[projection] +1].sopInstanceUID, $scope.action.wc, $scope.action.ww);
				}else{
					$scope.slider[projection] = 0;
				}
			}
		}
	};
	
	$scope.prevImage = function(projection){
		if (!config.blockImageWheel){
			if ($scope.slider[projection] > 0){
				$scope.viewImage(projection, $scope.series[projection].images[$scope.slider[projection] -1].sopInstanceUID, $scope.action.wc, $scope.action.ww);
			}
		}
	};
	
    $scope.close = function(){
    	$location.path('/studies');
    };

};
