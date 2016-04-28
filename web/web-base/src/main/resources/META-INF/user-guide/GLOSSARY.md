#Vertexium
The open source graph database API developed along with {{ book.productName }}. Vertexium manages storage and
indexing of the data {{ book.productName }}. More information is available at <a target="_new" href="http://vertexium.org/">http://vertexium.org/</a>

#entity
A node in the graph that can have properties and relationships to other entities.

#relationship
A connection between two entities in the graph.

#ontology
The valid concepts, properties, and relationships for a {{ book.productName }} installation.

#concept
A type defined in the ontology (e.g. person, place, or company). Uniquely identified by an IRI and
assigned to every entity in the graph.

#IRI
Internationalized Resource Identifier. e.g. http://visallo.org#person or http://visallo.org#worksFor

#property
A field defined in the ontology as valid for one or more concepts. Uniquely identified by an IRI
and optionally set on entities in the graph.

#relationship
A connection defined in the ontology as valid from one concept to another. Uniquely identified by
an IRI and stored as relationships between entities in the graph.

#owl
Web Ontology Language. Standard XML file format for defining ontologies.

#GPW
Acronym for graph property worker.

#graph property worker
Type of {{ book.productName }} plugin that responds to changes in the graph and often used for data enrichment and
analytics. GPWs can respond to creation or update events on entities, properties, or relationships.

#poster frame
An image that is displayed in the video player before the user clicks play.

#raw
The property on a entity used to store any imported data.

#term mention
A word or group of words found in a text property which may denote a concept. Term mentions are
typically identified by graph property workers (e.g. `opennlp-me-extractor`).

#resolved term mention
A term mention that has be resolved by a user or the system to represent a specific entity
in the graph.

#thumbnail image
An image displayed in search result and on the workspace.

#video preview
An image composed of multiple video frames supporting scrubbing withing the video before beginning playback.

#visibility
The access control applied to entities, properties, and relationships. The term 'visibility' is borrowed from Accumulo.

#visibility source
The data stored on behalf of the visibility user interface component to support displaying and editing
access control settings. This data is converted by the configured
org.visallo.core.security.VisibilityTranslator to the org.visallo.core.security.{{ book.productName }}Visibility
value used to enforce access control.

#visibility json
A JSONObject consisting of the visibility source and a list of workspace ids. This value is stored
as metadata on all entities, properties, and relationships to support access control.

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
