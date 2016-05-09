
Renderer3D = function(canvas) {
    // Declaration of global Variables...
    var width = 1148;
    var height = 758;
    var renderer = null;
    var camera = null;
    var material = null;
    var dicomData = null;
    var directionalLight = null;
    var mouseDown = false;
    var scene = new THREE.Scene();
    var geometry = new THREE.Geometry();

    this.createCanvas = function (){
        renderer = new THREE.WebGLRenderer( {canvas: canvas} );        
        renderer.setSize(width, height);
        renderer.domElement.style.backgroundColor = '#000000';        
        
        camera = new THREE.PerspectiveCamera(35, width / height, 1, 10000);
        camera.position.z = 500;
        scene.add(camera);
                
        controls = new THREE.TrackballControls(camera);
        controls.panSpeed = 1.0;
        controls.zoomSpeed = 2.0;
        controls.rotateSpeed = 3.0;
        controls.staticMoving = true;
        controls.dynamicDampingFactor = 0.3;
        
        renderer.domElement.addEventListener('mousemove', this.onMouseMove, false);        
        renderer.domElement.addEventListener('mousedown', this.onMouseDown, false);
        renderer.domElement.addEventListener('mouseup', this.onMouseUp, false);
        renderer.domElement.addEventListener('DOMMouseScroll', this.onMouseScroll, false);
        renderer.domElement.addEventListener('mousewheel', this.onMouseScroll, false);
    };

    this.loadDicom = function (data){

        for (var i = 0; i < data[0].length; i++) {
            geometry.vertices.push(new THREE.Vector3(data[0][i][0], data[0][i][1], data[0][i][2]));
        }
        for (i = 0; i < data[1].length; i++) {
            geometry.faces.push(new THREE.Face3(data[1][i][0], data[1][i][1], data[1][i][2]));
        }
        geometry.computeFaceNormals();
        geometry.center();

        material = new THREE.MeshLambertMaterial({
            color: 0xffffff
        });

        dicomData = new THREE.Mesh(geometry, material);
        scene.add(dicomData);
        
        var ambientLight = new THREE.AmbientLight(0x505050);
        scene.add(ambientLight);
        
        directionalLight = new THREE.DirectionalLight(0xa1a1a1);
        directionalLight.position.set(200, 200, 1000).normalize();
        camera.add(directionalLight);
        camera.add(directionalLight.target);

        controls.update();
        renderer.render(scene, camera);
    };
    
    this.onMouseDown = function(event) {
        event.preventDefault();
        mouseDown = true;
    };

    this.onMouseMove = function(event) {
        event.preventDefault();
        if (mouseDown){
            if(dicomData!=null) {
            	controls.noZoom = true;
                controls.update();
                renderer.render(scene, camera);
            }
        }
    };

    this.onMouseUp = function(event) {
        event.preventDefault();
        controls.enabled = true;
        if (mouseDown) {
            mouseDown = false;            
        }
    };
    
    this.onMouseScroll = function (event) {
        event.preventDefault();
        if(dicomData!=null){
            var rolled = 0;
            if (event.wheelDelta === undefined) {
                // Firefox - The measurement units of the detail and wheelDelta properties are different...
                rolled = -40 * event.detail;
            } else {
                rolled = event.wheelDelta;
            }
            if (rolled > 0) { //Up
                camera.translateZ(10);
            } else { //Down
                camera.translateZ(-10);
            }
            renderer.render(scene, camera);
        }
    };
};
