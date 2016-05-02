#<center> Access Control </center>

Access to data in {{ book.productName }} is controlled at the individual [entity](vertices.md), [relationship](edges.md),
and [property](properties.md) level. Additionally, there are two concepts that enable data to be protected and users to
 be authorized. `Visibility` allows data to be protected and `authorization` ensures only authorized users can see specific
 sets of data; these terms can be used interchangeably, but its based on the perspective.

For example, assume users A, B and C exist, and share a [workspace](workspaces.md); users A and C have the “auth1”
authorization. If user A drags and drops an entity onto the [workspace](workspaces.md), assigning it the “auth1”
visibility, then only users A and C can see the entity since user B does not have the same authorization. The
visibility does not necessarily have to only be assigned to entities, it can be assigned to individual
[properties](properties.md) as well as [relationships](edges.md).

If a user is assigned a specific authorization, and applies the authorization on a [property](properties.md), which then
becomes the property's visibility, therefore any user with that specific authorization can see that [property](properties.md).



Taking the same example as above, if user A did not give the whole entity the “auth1” authorization, then all of the
users would be able to view it in the shared [workspace](workspaces.md). Then user A adds another [property](properties.md)
to that entity, and gives that the “auth1” visibility. Now the entity can be viewed by the users in the shared
[workspace](workspaces.md), but the [property](properties.md) that was just added can only be viewed by users A and C
because they both have the “auth1” authorization.


<img src = images/importing-entity.png width="250">

<!--
TODO:
- labels
- editing
- visibility plugins
-->
