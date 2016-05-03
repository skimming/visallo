# Workspaces

A workspace can be found by clicking on the `Spaces` tab <img src = images/menubar-spaces.png width="30"> on the
menu bar. It is a collection consisting of a [graph](graph.md), [dashboard](dashboards.md) and a [map](map.md).
The [graph](graph.md) is a working area that includes a collection of [entities](vertices.md) and [relationships](edges.md) that can be
shared to improve [collaboration](collaboration.md) with other {{ book.productName }}  users. Note that new and changed [entities](vertices.md),
[properties](properties.md), and [relationships](edges.md) are only visible within a workspace until it is published
by a user with the `PUBLISH` [privilege](application-privileges.md).

## Naming, Creating, and Deleting Workspaces

Every workspace will always have a default workspace that is named “default - *username*”. Therefore if all workspaces are deleted
then another default workspace will be present. The name of a workspace can be edited at any time by any user with 'Edit'
 access to the workspace, however, two workspaces may not have the same name. Each
workspace is [sandboxed](sandboxing-and-publishing.md).

<img src = images/workspace-collaboration.png width="400">

## Sharing a Workspace
When a workspace is created and then shared, you can grant another user one of the following accesses:
`View`, `Comment`, or `Edit`.  Also, within a shared workspace, if [authorizations](data-access-control.md) are used, only users with the appropriate authorizations
 will be able to view the [entities](vertices.md)**/**[relationships](edges.md) assigned with the appropriate
  [visibility](data-access-control.md).

## Access on Workspaces
With `View` access,  you can only view the data on the workspace that is shared with you. You will not have the permissions to
change the layout or data.

With `Comment` access, you can only comment on the [relationships](edges.md)
and [entities](vertices.md) on the workspace that is shared with you. You will not have the permissions to
change the layout or data.

With `Edit` access, you can view the data, change the layout, and make changes by adding and deleting
[properties](properties.md)**/**[entities](vertices.md)**/**[relationships](edges.md) as if you are the original
owner of the workspace that is shared with you.

<img src = images/editing-workspace.png width="600">



The bottom left corner of the workspace includes the following information:

- User Name
    - Open the user account window to view your [privileges](application-privileges.md) and [authorizations](data-access-control.md)

<img src = images/user-account.png width="600">

- [Workspace](workspaces.md) Name
- Modification Panel
    - Clicking on the number next to the workspace name, opens the [unpublished changes](sandboxing-and-publishing.md)
 window. Given the [sandbox](sandboxing-and-publishing.md) feature in {{ book.productName }}, you have the ability to view
  changes made and then either
    [publish](sandboxing-and-publishing.md) or [undo](sandboxing-and-publishing.md) them.
- Timeline
    - Opens the [`workspace timeline`](timeline.md) panel to visualize [entities](vertices.md)**/**[relationships](edges.md)
    with date properties in chronological order
