/*
	 Initialize and render the MenuBar when its elements are ready 
	 to be scripted.
*/

YAHOO.util.Event.onContentReady("utility-links", function () {

	/*
		Instantiate a MenuBar:  The first argument passed to the constructor
		is the id for the Menu element to be created, the second is an 
		object literal of configuration properties.
	*/

	var oMenuBar = new YAHOO.widget.MenuBar("utility-links", { 
												autosubmenudisplay: true, 
												hidedelay: 750, 
												lazyload: true });

	/*
		 Call the "render" method with no arguments since the 
		 markup for this MenuBar instance is already exists in 
		 the page.
	*/

	oMenuBar.render();

});