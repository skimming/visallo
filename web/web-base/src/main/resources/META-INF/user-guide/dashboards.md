# Dashboards

Each {{ book.productName }} [workspace](workspaces.md) has an associated dashboard that can be customized to display
information.

There are different Dashboard Cards that are available to you. These Dashboard Cards are just small widgets to aid
organizing information on the dashboard. The vast majority of dashboard cards include different visualizations, or
different forms of viewing the data. The different visualizations may include: Vertical Bar Chart, Horizontal Bar Chart,
Subway, Text Overview, and Pie Chart. The different Dashboard Cards that exist by default include:

**Entity Counts**
   - The Entity Counts dashboard card starts as a Pie Chart view displaying the number of [entities](vertices.md)
   that are in the system for that [workspace](workspaces.md) as per their concept types. If you select a specific
   “slice”, bar, or portion of the visualization, it will render a list of all the [entities](vertices.md) in that concept type
   and will provide an option to open the list in the [search](search.md) panel. Configurations for this dashboard card
   includes just changing the title and the visualization.

<img src = images/entity-counts.png width="400">

**Notifications**
   - The Notifications dashboard card contains a list of any system notifications that are pertinent to you or any other
   notifications that require your attention such as change in authorizations, or if a workspace is shared with you.
   Configurations for this dashboard card is the ability to edit the title.

<img src = images/system-notification-dashboard-card.png width="600">

**Relationship Counts**
   - The Relationship Counts dashboard card starts as a vertical bar chart and is similar to the entity counts
   dashboard card. It displays the number of [relationships](edges.md) that there are in the system for that
   [workspace](workspaces.md) as per their relationship types.

<img src = images/relationship-counts.png width="500">

**Welcome to {{ book.productName }}**

   - The Welcome to {{ book.productName }}  dashboard card is included on every user’s dashboard. It contains helpful
     information about the [menubar](navigation.md) and describes each item, such as the dashboard, find, spaces, admin,
     graph, map, activity and logout options depending on your privileges.

<img src = images/welcome-to-visallo.png width="725">

**Saved [Search](search.md)**
   - The Saved [Search](search.md) dashboard card provides the ability to customize the way you view a saved search using
   different visualization techniques.

   If a saved search does not already exist, you will need to conduct a [search](search.md) in Find and then
   save it. Once you have a saved search, you can add this card to your dashboard, if it already isn’t there, by clicking
   on the edit button next to the dashboard name and choosing “ADD”. Next, you will exit edit mode and go to the configuration
   settings, where you can also edit the title, but choose the saved [search](search.md) that you would like
   to view. After doing so, you can sort the [search](search.md) and/or limit the [search](search.md) results by using
   the ‘Sort’ and ‘Limit Results’ fields, and an option for Aggregation also exists. The dropdown menu for Aggregations
   includes Counts, Histograms, Geo-Coordinate Cluster and Statistics.
        - If you choose ‘Count’, then you choose the [property](properties.md) you wish to count.
        - If you choose Histogram, you will be then choosing a [property](properties.md) and add an interval.
        - If you choose Geo-coordinate Cluster, you would have to enter the [property](properties.md) as well as a size
          that is measured in kilometers. Lastly, for Statistics, you will input the [property](properties.md) to count.

<img src = images/saved-search.png width="500">

<!--
TODO:
- editing
-->
