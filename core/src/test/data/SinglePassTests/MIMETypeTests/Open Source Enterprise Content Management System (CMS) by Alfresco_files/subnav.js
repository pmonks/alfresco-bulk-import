/*
     Initialize and render the Menu when its elements are ready 
     to be scripted.
*/

YAHOO.util.Event.onContentReady("subnav", function () {

    /*
         Instantiate a Menu:  The first argument passed to the 
         constructor is the id of the element in the page 
         representing the Menu; the second is an object literal 
         of configuration properties.
    */

    var oMenu = new YAHOO.widget.Menu(
                        "subnav", 
                        {
                            position: "static", 
                            hidedelay: 750, 
                            lazyload: true, 
                            effect: { 
                                effect: YAHOO.widget.ContainerEffect.FADE,
                                duration: 0.25
                            } 
                        }
                    );

    /*
         Call the "render" method with no arguments since the 
         markup for this Menu instance is already exists in the page.
    */

    oMenu.render();            

});