# {{ book.productName }} Search

One of the primary ways to start exploring data within {{ book.productName }} is via the search interface. Through the
search feature, {{ book.productName }} will allow you to search all the data in the system that you are authorized
to access; the results of this search will appear in the search results pane. Visallo also has features that will allow
 you to refine your search such as filtering by concept, sorting by [properties](properties.md),
filtering by [properties](properties.md), and entity and relation match searches.

## Entity Match
An `ENTITY MATCH SEARCH` will only look at the entities that are in the system and if a `RELATION MATCH SEARCH` is executed,
then it is only looking at the [relationships](edges.md) that are in the system.

## Filter By Concept
`FILTER BY CONCEPT` will only show search results that have the chosen concept type from a list. All other results are
filtered out.

## Sort By Properties
`SORT BY PROPERTIES` is a list of all the [properties](properties.md) that the search results
can be sorted by. After choosing the [property](properties.md) or [properties](properties.md), the results will first
display by the selected [properties](properties.md) and then display the remaining results. There is also an option to
order the results in ascending or descending order.

## Filter By Properties
`FILTER BY PROPERTIES` will restrict the search results to display only the results with the indicated
[property](properties.md) or [properties](properties.md). Filtering can vary based on properties that are defined
within a given ontology. For example, when filtering by [properties](properties.md), like `Birth Date`
or `Duration`, you have the option to filter by `BEFORE`, `AFTER`, `BETWEEN`, `EQUALS`, `HAS
PROPERTY`, `DOES NOT HAVE PROPERTY`, `SHORTER THAN`, `LONGER THAN`, `LESS THAN`, `GREATER THAN`, `CONTAINS`, and `WITHIN`
a value specified by user input.

## Saved Searches
The option to save your search can be found above the search bar. After executing a search, you can save the search by
clicking the Saved Searches drop down arrow, assigning a saved search name, and clicking `Create`.

For example, let’s say you are searching for all Documents that have Source `A`. Enter an `*` into the search bar, select `Entity` for
the type of `MATCH`, `FILTER BY CONCEPT` By `Document`, and `FILTER BY PROPERTIES` by `Source` that `EQUALS` `A`.
Click on the drop down right above the search bar, give it a name and then click `CREATE`.

To execute a saved search, open the saved search drop down and select a saved search from the list. Another option to execute
a saved search can be found on the [dashboard](dashboards.md).

## Find Related
Revealing relationships to entities provides greater insights to your data. To run this feature, select an [entity](entities.md)
on the graph, right click to display the menu options and select `Search...`. Then select `Related Items`. The search panel will
then appear with all the results of all the entities that are related.

<img src = images/filled-out-search.png width="400">

<!--
TODO:
- saved search
- text
- concept filtering
- property filtering
    - has/does not have
    - contains
    - equals
    - less than
    - greater than
    - between
    - geolocation and radius
-->
