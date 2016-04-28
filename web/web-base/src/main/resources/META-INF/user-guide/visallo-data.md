#<center> {{ book.productName }} Concepts </center>

{{ book.productName }} has three types of data:

- [Entities](vertices.md)
- [Relationships](edges.md)
- [Properties](properties.md)

And enforces fine grained [`access control`](data-access-control.md) individually applied to each data type. [Entities](vertices.md) can be
connected to one another through [relationships](edges.md), where both have [properties](properties.md) that describe what
they are. To connect two entities, right click on one of the entities, and select the `Connect` menu option, and then
click on the other entity to create the relationship. Relationships vary depending on the concept types defined on
the ontology.


<img src = images/entity-right-click.png width="350" height="400">