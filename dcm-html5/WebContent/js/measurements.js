var measurements = measurements || { version: "1.0.0" };

measurements.objects = ['LINE', 'RECTANGLE', 'TETRAGON', 'ELLIPSE', 'ANGLE', 'OPEN-ANGLE', 'COBBS-ANGLE'];

function degToRad(deg){
	return deg * Math.PI / 180;
};

function radToDeg(rad){
	return 180 * rad / Math.PI;
};

function fabricLine(coords) {
	return new fabric.Line(coords, {
		fill: 'red',
	    stroke: 'red',
	    strokeWidth: 1,
	    selectable: false
	});
};

function fabricArc(point, radius, start, end) {
	return new fabric.Circle({
	    stroke: 'red',
	    strokeWidth: 1,
	    fill: 'rgba(0,0,0,0)',
	    selectable: false,
	    startAngle: start,
	    endAngle: end,	    
	    left: point.x - radius, 
	    top: point.y - radius, 
	    radius: radius
	});
};

function fabricCircle(point, radius) {
	return new fabric.Circle({
	    stroke: 'red',
	    strokeWidth: 1,
	    fill: 'rgba(0,0,0,0)',
	    selectable: false,
	    left: point.x - radius,
	    top: point.y - radius, 
	    radius: radius
	});
};

function fabricEllipse(p1, p2) {
	var left = 0;
	var top = 0;
	if(p1.y < p2.y){
		top = p1.y;
	}else{
		top = p2.y - (p1.y - p2.y);
	}
	if(p2.x > p1.x){
		left = p1.x - (p2.x - p1.x);
	} else {
		left = p2.x;
	}	
	return new fabric.Ellipse({
	    stroke: 'red',
	    strokeWidth: 1,
	    fill: 'rgba(0,0,0,0)',
	    selectable: false,
	    left: left,
	    top: top,
	    rx: Math.abs(p2.x - p1.x),
	    ry: Math.abs(p2.y - p1.y)
	});
};

function fabricText(point, text){	
	return new fabric.Text(text, {  
		left: point.x, 
		top: point.y,
		fill: 'red',
	    stroke: 'red',
	    fontSize: 12,
	    selectable: false
	});
};

function line(p1, p2, ray){
	
	var a1 = Math.atan2(p1.y - p2.y, p1.x - p2.x) + degToRad(90);
	var c1 = {
		x : p1.x + ray * Math.cos(a1),
		y : p1.y + ray * Math.sin(a1)
	};
	var a2 = a1 + degToRad(180);
	var c2 = {
		x : p1.x + ray * Math.cos(a2),
		y : p1.y + ray * Math.sin(a2)
	};
				
	var c3 = {
		x : p2.x + ray * Math.cos(a1),
		y : p2.y + ray * Math.sin(a1)
	};
	var c4 = {
		x : p2.x + ray * Math.cos(a2),
		y : p2.y + ray * Math.sin(a2)
	};	
	return {c1: c1, c2: c2, p1: p1, p2: p2, c3: c3, c4: c4};
};

(function() {
		
	measurements.clearStorage = function(canvas) {
		if(canvas !== undefined){
			measurements.objects.forEach(function(o){
				measurements.storage[o].points.forEach(function(p){
					canvas.remove(p.mop);
				});				
				measurements.storage[o].fabrics.forEach(function(f){
					canvas.remove(f);
				});								
			});
		}
		measurements.storage = {};
		measurements.objects.forEach(function(o){
			measurements.storage[o] = {fabrics: [], points: [], status: 'NEW'};
		});
		
	};
	
	measurements._fabricPoints = function(canvas, object, points){		
		measurements.storage[object].points.forEach(function(p){
			canvas.remove(p.mop);
		});				
		measurements.storage[object].points = [];
		points.forEach(function(p){
			p.mop = fabricCircle(p, 6);	
			measurements.storage[object].points.push(p);
		});		
	};
	
	measurements._fabricClear = function(canvas, object){
		measurements.storage[object].fabrics.forEach(function(item){
			canvas.remove(item);
		});		
		measurements.storage[object].fabrics = [];		
	};
	
	measurements.units = function(object, pixelSpacing) {
		var unit = ' °';
		if(object == 'LINE'){
			unit = ' [mm]';
		}else if(object == 'RECTANGLE' || object == 'TETRAGON' || object == 'ELLIPSE'){
			unit = ' [mm²]';
		}					
		if(pixelSpacing === undefined || pixelSpacing == null){
			pixelSpacing = "1,1";
			if(object == 'LINE'){
				unit = ' [px]';
			}else if(object == 'RECTANGLE' || object == 'TETRAGON' || object == 'ELLIPSE'){
				unit = ' [px²]';
			}			
		}
		var px = Number(pixelSpacing.split(",")[0].trim());
		var py = Number(pixelSpacing.split(",")[1].trim());		
		return {unit: unit, px: px, py: py};
	};
	
	measurements.line = function(canvas, points, pixelSpacing, scale){				
		var units = measurements.units('LINE', pixelSpacing);
		var p1 = points[0];
		var p2 = points[1];
		var _line = line(p1, p2, 12);
		
		this._fabricClear(canvas, 'LINE');

		measurements.storage['LINE'].fabrics.push(fabricLine([_line.c1.x, _line.c1.y, _line.c2.x, _line.c2.y]));		
		measurements.storage['LINE'].fabrics.push(fabricLine([_line.p1.x, _line.p1.y, _line.p2.x, _line.p2.y]));
		measurements.storage['LINE'].fabrics.push(fabricLine([_line.c3.x, _line.c3.y, _line.c4.x, _line.c4.y]));
		
		var length = Math.sqrt(Math.pow((units.px * p2.x - units.px * p1.x), 2) + Math.pow((units.py * p2.y - units.py * p1.y), 2));
		var pos = {x: p1.x + 0.5 * (p2.x - p1.x), y: p1.y + 0.5 * (p2.y - p1.y)};
		if(p1.x < p2.x && p1.y < p2.y || p1.x > p2.x && p1.y > p2.y){
			pos.y -= 12;
		}
		if(scale !== undefined){
			length /= 0.5 * (scale.sx + scale.sy);
		}				
		measurements.storage['LINE'].fabrics.push(fabricText(pos, length.toFixed(2).toString() + units.unit));
		measurements.storage['LINE'].fabrics.forEach(function(item){
			canvas.add(item);
		});

		this._fabricPoints(canvas, 'LINE', [p1, p2]);		
	};
	
	measurements.rectangle = function(canvas, points, pixelSpacing, scale){				
		var units = measurements.units('RECTANGLE', pixelSpacing);
		var p1 = points[0];
		var p2 = points[1];
		var p3 = points[2] || {x: p2.x, y: p1.y};				
		var p4 = points[3] || {x: p1.x, y: p2.y};

		this._fabricClear(canvas, 'RECTANGLE');
		
		measurements.storage['RECTANGLE'].fabrics.push(fabricLine([p1.x, p1.y, p3.x, p3.y]));
		measurements.storage['RECTANGLE'].fabrics.push(fabricLine([p2.x, p2.y, p3.x, p3.y]));
		measurements.storage['RECTANGLE'].fabrics.push(fabricLine([p2.x, p2.y, p4.x, p4.y]));
		measurements.storage['RECTANGLE'].fabrics.push(fabricLine([p4.x, p4.y, p1.x, p1.y]));

		var area = (units.px * Math.abs(p2.x - p1.x) * units.py * Math.abs(p2.y - p1.y));
		var pos = {x: p1.x + 0.5 * (p2.x - p1.x), y: p1.y + 0.5 * (p2.y - p1.y)};	
		if(scale !== undefined){
			area /= (scale.sx * scale.sy);
		}						
		measurements.storage['RECTANGLE'].fabrics.push(fabricText(pos, area.toFixed(2).toString() + units.unit));

		measurements.storage['RECTANGLE'].fabrics.forEach(function(item){
			canvas.add(item);
		});		
		
		this._fabricPoints(canvas, 'RECTANGLE', [p1, p2, p3, p4]);
	};
	
	measurements.tetragon = function(canvas, points, pixelSpacing, scale){				
		var units = measurements.units('TETRAGON', pixelSpacing);
		var p1 = points[0];
		var p2 = points[1];
		var p3 = points[2] || {x: p2.x, y: p1.y};				
		var p4 = points[3] || {x: p1.x, y: p2.y};

		this._fabricClear(canvas, 'TETRAGON');
		
		measurements.storage['TETRAGON'].fabrics.push(fabricLine([p1.x, p1.y, p3.x, p3.y]));
		measurements.storage['TETRAGON'].fabrics.push(fabricLine([p2.x, p2.y, p3.x, p3.y]));
		measurements.storage['TETRAGON'].fabrics.push(fabricLine([p2.x, p2.y, p4.x, p4.y]));
		measurements.storage['TETRAGON'].fabrics.push(fabricLine([p4.x, p4.y, p1.x, p1.y]));

		var a1 = (units.px * p1.x - units.px * p3.x) * (units.py * p1.y + units.py * p3.y);
		var a2 = (units.px * p3.x - units.px * p2.x) * (units.py * p3.y + units.py * p2.y);
		var a3 = (units.px * p2.x - units.px * p4.x) * (units.py * p2.y + units.py * p4.y);
		var a4 = (units.px * p4.x - units.px * p1.x) * (units.py * p4.y + units.py * p1.y);
		
		var area = Math.abs(0.5 * (a1 + a2 + a3 + a4));
		var pos = {x: p1.x + 0.5 * (p2.x - p1.x), y: p1.y + 0.5 * (p2.y - p1.y)};	
		if(scale !== undefined){
			area /= (scale.sx * scale.sy);
		}				
		measurements.storage['TETRAGON'].fabrics.push(fabricText(pos, area.toFixed(2).toString() + units.unit));

		measurements.storage['TETRAGON'].fabrics.forEach(function(item){
			canvas.add(item);
		});		
		
		this._fabricPoints(canvas, 'TETRAGON', [p1, p2, p3, p4]);
	};

	measurements.ellipse = function(canvas, points, pixelSpacing, scale){				
		var units = measurements.units('ELLIPSE', pixelSpacing);
		var p1 = points[0];
		var p2 = points[1];
		
		this._fabricClear(canvas, 'ELLIPSE');
		
		measurements.storage['ELLIPSE'].fabrics.push(fabricEllipse(p1, p2));
		var ray = 12;
		measurements.storage['ELLIPSE'].fabrics.push(fabricLine([p1.x, p1.y - ray, p1.x, p1.y + ray]));
		measurements.storage['ELLIPSE'].fabrics.push(fabricLine([p2.x - ray, p2.y, p2.x + ray, p2.y]));
		
		var area = Math.abs(Math.PI * units.px * (p2.x - p1.x) * units.py * (p2.y - p1.y));  
		var pos = {x: p1.x, y: p2.y};	
		if(scale !== undefined){
			area /= (scale.sx * scale.sy);
		}				
		measurements.storage['ELLIPSE'].fabrics.push(fabricText(pos, area.toFixed(2).toString() + units.unit));
		measurements.storage['ELLIPSE'].fabrics.forEach(function(item){
			canvas.add(item);
		});		
		this._fabricPoints(canvas, 'ELLIPSE', [p1, p2]);
	};
	
	measurements.angle = function(canvas, points, pixelSpacing){
		var units = measurements.units('ANGLE', pixelSpacing);
		var p1 = points[0];
		var p2 = points[1];
		var p3 = points[2];
		this._fabricClear(canvas, 'ANGLE');
								
		measurements.storage['ANGLE'].fabrics.push(fabricLine([p1.x, p1.y, p2.x, p2.y]));
		measurements.storage['ANGLE'].fabrics.push(fabricLine([p1.x, p1.y, p3.x, p3.y]));		
		var line1 = line(p2, p1, 20);
		var line2 = line(p3, p1, 20);
		measurements.storage['ANGLE'].fabrics.push(fabricLine([line1.c1.x, line1.c1.y, line1.c2.x, line1.c2.y]));
		measurements.storage['ANGLE'].fabrics.push(fabricLine([line2.c1.x, line2.c1.y, line2.c2.x, line2.c2.y]));

		var d1 = Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
		var d2 = Math.sqrt((p1.x - p3.x) * (p1.x - p3.x) + (p1.y - p3.y) * (p1.y - p3.y));
		var d3 = 0.25 * (d1 + d2);
		var a1 = radToDeg(Math.atan2(p2.y - p1.y, p2.x - p1.x));
		var a2 = radToDeg(Math.atan2(p3.y - p1.y, p3.x - p1.x));
		if(a1 > a2){
			var temp = a1;
			a1 = a2;
			a2 = temp;			
		}
		var angle = Math.abs(Math.min(a1, a2) - Math.max(a1, a2));		
		measurements.storage['ANGLE'].fabrics.push(fabricArc(p1, d3, degToRad(a1), degToRad(a2)));		
		var pos = {x: p1.x, y: p1.y};	
		measurements.storage['ANGLE'].fabrics.push(fabricText(pos, angle.toFixed(2).toString() + units.unit));
		
		measurements.storage['ANGLE'].fabrics.forEach(function(item){
			canvas.add(item);
		});
						
		this._fabricPoints(canvas, 'ANGLE', [p1, p2, p3]);
	};

	measurements.move = function(canvas, object, index, point, ps, scale){
		if(index !== undefined && index != null){
			canvas.remove(measurements.storage[object].points[index].mop);
			var points = measurements.storage[object].points;
			if(object == 'LINE'){
				points[index] = point;		
				this.line(canvas, points, ps, scale);
			}else if(object == 'ELLIPSE'){
				points[index] = point;		
				this.ellipse(canvas, points, ps, scale);
			}else if(object == 'RECTANGLE'){
				points[index] = point;
				if(index == 0 || index ==1){
					points[2] = {x: points[1].x, y: points[0].y};
					points[3] = {x: points[0].x, y: points[1].y};
				}else{
					points[0] = {x: points[3].x, y: points[2].y};
					points[1] = {x: points[2].x, y: points[3].y};				
				}
				this.rectangle(canvas, points, ps, scale);
			}else if(object == 'TETRAGON'){
				if(measurements.storage[object].status == 'DONE'){
					points[index] = point;		
					this.tetragon(canvas, points, ps, scale);				
				}else{
					points[index] = point;
					if(index == 0 || index ==1){
						points[2] = {x: points[1].x, y: points[0].y};
						points[3] = {x: points[0].x, y: points[1].y};
					}else{
						points[0] = {x: points[3].x, y: points[2].y};
						points[1] = {x: points[2].x, y: points[3].y};				
					}
					this.tetragon(canvas, points, ps, scale);				
				}			
			}else if(object == 'ANGLE'){
				if(measurements.storage[object].status == 'DONE'){
					points[index] = point;		
					this.angle(canvas, points, ps);
				} else {
					points[index] = point;		
					points[index + 1] = {x: point.x, y: points[0].y - (points[1].y - points[0].y)};
					this.angle(canvas, points, ps);
				}
			}
			canvas.add(measurements.storage[object].points[index].mop);
		}
	};
	
	measurements.check = function(object, point){
		var x = point.x;
		var y = point.y;
		for(var i=0; i<measurements.storage[object].points.length; ++i){
			var p = measurements.storage[object].points[i];
			if(x >= p.x -5 && x <= p.x +5 && y >= p.y -5 && y <= p.y +5){
				return {object: object, index: i};
			}
		}
		if(measurements.storage[object].status == 'DONE'){
			return {object: object, index: null};
		}
		return null;
	};
	
	measurements.done = function(object){
		measurements.storage[object].status = 'DONE';
	};
	
	measurements.mouseOverPoints = function(canvas, point){
		var x = point.x;
		var y = point.y;
		var hit = null;
		measurements.objects.forEach(function(o){						
			for(var i=0; i<measurements.storage[o].points.length; ++i){
				var p = measurements.storage[o].points[i];
				canvas.remove(p.mop);
				if(x >= p.x -5 && x <= p.x +5 && y >= p.y -5 && y <= p.y +5){
					canvas.add(p.mop);
					hit = {object: o, index: i};
				}
			}					
		});		
		return hit;
	};		
	measurements.clearStorage();
})();