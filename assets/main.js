( function() {
    bridge.registerListener( "requestCodeText", function( payload ) {
        var code = document.getElementById( "code" ).value;
        bridge.sendMessage( "onCodeTextResponse", { "code": code } );
    });
} )();
