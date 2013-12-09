( function() {
    var editor;
    bridge.registerListener( "requestCodeText", function( payload ) {
        bridge.sendMessage( "onCodeTextResponse", { "code": editor.getValue() } );
    });

    var curOnLoad = window.onload;
    window.onload = function() {
        curOnLoad();
        editor = ace.edit("code");
        editor.setTheme("ace/theme/twilight");
        sharejs.open('hello', 'text', 'http://omgwtf.in:8000/channel', function(error, doc) {
                doc.attach_ace(editor);
            });
        };
} )();
