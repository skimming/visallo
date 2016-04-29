
#entity
A node in the graph that can have properties and relationships to other entities.

#relationship
A connection between two entities in the graph.

#ontology
The valid concepts, properties, and relationships for a {{ book.productName }} installation.

#concept
A type defined in the ontology (e.g. person, place, or company). Uniquely identified by an IRI and
assigned to every entity in the graph.

#property
A field defined in the ontology as valid for one or more concepts. Uniquely identified by an IRI
and optionally set on entities in the graph.

#relationship
A connection defined in the ontology as valid from one concept to another. Uniquely identified by
an IRI and stored as relationships between entities in the graph.

#poster frame
An image that is displayed in the video player before the user clicks play.

#raw
The property on a entity used to store any imported data.

#thumbnail image
An image displayed in search result and on the workspace.

#video preview
An image composed of multiple video frames supporting scrubbing withing the video before beginning playback.

#visibility
The access control applied to entities, properties, and relationships. The term 'visibility' is borrowed from Accumulo.

#authorization
The access control rights granted to {{ book.productName }} users to control their access to entities,
properties, and relationships. The term 'authorization' is borrowed from Accumulo.

#privilege
The application rights granted to {{ book.productName }} users. (e.g. READ, EDIT, and PUBLISH)

#workspace
A named collection of entities that can be shared for collaboration with
other {{ book.productName }} users. New and changed entities, properties, and relationships
are only visible within a workspace until being published by a user with
the PUBLISH privilege.
