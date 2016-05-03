# Find Path

Find path is powerful feature used to explore and discover details about distinct [entities](vertices.md) on
the graph and how they may be related to each other.

## How To Find A Path
To find a path, there must be at least two or more [entities](vertices.md) on the graph and a [relationship](edges.md)
between them. Right click on one entity and choose `FIND PATH` and then click on another entity. Options include limiting
your results  to a certain number of hops (the number of entities it goes through) and limit it
to the [relationship](edges.md) type to look at. This will open up the Activity Panel and it will show you the number of
paths found as well as highlight them on the graph.

Limiting the number of hops between entities when finding a path is important because it can quicken the search
because it doesn't have to look at so many things and the relationship you would see is much stronger and more likely
direct. For example, if you want to see a relationship between users A and D, you'd prefer one or two entities in the middle
rather than seeing A knows B and B knows C and C knows D, so thats how A and D are related.

This is useful, for example, if there are 26 Person concept [entities](vertices.md) all labeled between A-Z all
connected to each other in some way. You wish to see how Person A and Z may have a connection. You  would right click
on Person A, choose `Find Path`, limit the relationship type to `KNOWS`, and Up to 5 Hops, and execute the
search. The activity panel will open labeling “X Paths Found”. By clicking on that, all the paths found will be
highlighted in different colors between Person A and Person Z.


<img src = images/find-path.png width="700">
