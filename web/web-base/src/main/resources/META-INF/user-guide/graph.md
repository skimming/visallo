# Graph

The [workspace](workspaces.md) graph is the primary method used to visualize and interact with the [relationships](edges.md)
between [entities](vertices.md); both [entities](vertices.md) and [relationships](edges.md) can be viewed and manipulated
 on the graph, as well as any paths that are [searched](search.md) for and found.


## Adding to the Graph
To create new entities, right click on the graph and select `New Entity`. Then choose the concept type
that you would like to create. Alternatively, entities can be imported by dragging and dropping entities onto the graph.


## Fitting, Panning, and Zooming the Graph
Viewing large amounts of data on a graph can be overwhelming. To improve the user experiences, Visallo
provides several options to navigate the contents.


Fit <img src = images/fit.png width="35">:  resets your view and arranges the graph so that all entities are
visible on the graph.

Zoom <img src = images/zoom.png width="50">: Using the '+' or '-' keys on your keyboard allow you to zoom in/out.
 Alternatively, you can use your mouse/track pad to zoom in/out.

Arrows: <img src = images/pan-arrows.png width="50">: Using the arrow keys on the graph allows you to
pan up, down, left, or right.

## Changing Display On the Graph
The hamburger menu <img src = images/hamburger-menu.png width="25"> found on the top right of the screen
provides you with the options enable/disable Snap to Grid and/or Edge Labels.
  - Snap to Grid: Promotes entity placement alignment on the graph through the use of an invisible
  grid. When enabling “Snap To Grid”, all the entities on the graph will align themselves to the nearest intersection of lines on the grid.
  - Edge Labels: Displays the [relationships](edges.md) between entities on the graph.

Placing large numbers of [entities](vertices.md), and [relationships](edges.md)on a graph to can get complicated quickly
while at the same time lose the effectiveness of the pictorial layout if not displayed in a meaningful manner.  As a result,
Visallo offers a few default layout styles to assist in rendering information on a graph.  These include  `Circle`, `Grid`,
`Hierarchy`, and `Forced Directed`.

To use these layouts, you must first select the entities you wish to apply a particular layout style to
<Shift + highlight or click 1 to many entities>.  Once the entities are selected, right click on the graph and choose
an option from `Layout Selection` to only layout those selected. If you wish to apply a style to those not selected, you
may use the `Invert` option.

You may also opt to choose `Layout` to layout everything that is on the graph.


<img src = images/full-graph.png width="600">

<!--
TODO:
- control
- operations
    - connect
    - find path
    - search/add related
    - and more
-->
