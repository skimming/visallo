# Sandboxing and Publishing

## Sandboxing
Sandboxing is a core feature of {{ book.productName }} where users can [collaborate](collaboration.md) with each other within the
safety of a [sandboxed](sandboxing-and-publishing.md) [workspace](workspaces.md). This allows them to explore added and
changed data and answer hypothetical "what-if" questions without impacting all users of the system.


## Publish
Users with the `PUBLISH` [privilege](application-privileges.md) can promote data from the [sandbox](sandboxing-and-publishing.md)
to be seen by all users (limited by [data access control](data-access-control.md)). Users with the `PUBLISH`
[privilege](application-privileges.md) are also able to `UNDO` changes that are made.

For example, if an image is on the
[workspace](workspaces.md) and a new 'title' [property](properties.md) is added to the image, you have the option to
`PUBLISH` the title and make it visible to everyone with the same authorization, or `UNDO` the title removing that change
entirely from even your [workspace](workspaces.md). Changes to the [workspace](workspaces.md) that can be published or
undone include but are not limited to: adding or deleting [properties](properties.md), [relationships](edges.md), comments,
[entities](entities.md), etc.


## Modification Panel
The Modification Panel can be found in the bottom of the graph and it displays all of the changes that exist in the
[workspace](workspaces.md); each change is listed under the name of the changed [entity](entities.md)**/**[relationship](edges.md).
These changes are not visible to any user until the `PUBLISH` option is used, which is only available to those with the
`EDIT` access to the workspace and the `PUBLISH` privilege as a user. By publishing, all changes are made throughout
the system.

If there is a new entity that was either created or added, publishing it will enable other users to view, comment, or edit it.
An option to `UNDO` exists to ‘undo’ the changes that are
made. Once a change is undone, it cannot be reverted.

<img src = images/diff-panel.png width="500">
