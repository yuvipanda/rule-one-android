var express = require('express'),
    sharejs = require('share').server;

var server = express();
var options = {
    db: {type: 'redis'},
    browserChannel: {cors: '*'}
};

server.use(express.static(__dirname + '/..'));

// Attach the sharejs REST and Socket.io interfaces to the server
sharejs.attach(server, options);

server.get('/?', function(req, res, next) {
    res.writeHead(302, {location: '/index.html'});
    res.end();
});

server.listen(8000, function() {
    console.log("Started listening");
});