
define([
    'flight/lib/component',
    'tpl!./overlay',
    'util/formatters',
    'util/privileges',
    'util/withDataRequest',
    'configuration/plugins/registry'
], function(
    defineComponent,
    template,
    F,
    Privilege,
    withDataRequest,
    registry) {
    'use strict';

    var LAST_SAVED_UPDATE_FREQUENCY_SECONDS = 30,
        MENUBAR_WIDTH = 30,
        UPDATE_WORKSPACE_DIFF_SECONDS = 3,
        SHOW_UNPUBLUSHED_CHANGES_SECONDS = 3;

    return defineComponent(WorkspaceOverlay, withDataRequest);

    function isWorkspaceDiffPost(settings) {
        var route = ~['workspace/undo', 'workspace/publish'].indexOf(settings.url),
            isPost = (/post/i).test(settings.type);

        return !!(route && isPost);
    }

    function WorkspaceOverlay() {

        this.defaultAttrs({
            userSelector: '.user',
            nameSelector: '.name',
            toggleTimelineSelector: '.toggle-timeline'
        });

        this.after('initialize', function() {
            var self = this;

            requestAnimationFrame(function() {
                MENUBAR_WIDTH = $('.menubar-pane').width();
            })

            registry.documentExtensionPoint('org.visallo.user.account.page',
                'Add new tabs to user account modal dialog',
                function(e) {
                    return ('identifier' in e) && ('pageComponentPath' in e);
                },
                'http://docs.visallo.org/extension-points/front-end/userAccount'
            );

            this.updateDiffBadge = _.throttle(this.updateDiffBadge.bind(this), UPDATE_WORKSPACE_DIFF_SECONDS * 1000)

            this.$node.hide().html(template({}));

            this.updateUserTooltip({user: visalloData.currentUser});

            requestAnimationFrame(function() {
                self.$node.addClass('visible');
            });

            this.on(document, 'workspaceSaving', this.onWorkspaceSaving);
            this.on(document, 'workspaceSaved', this.onWorkspaceSaved);
            this.on(document, 'workspaceLoaded', this.onWorkspaceLoaded);
            this.on(document, 'workspaceUpdated', this.onWorkspaceUpdated);
            this.on(document, 'switchWorkspace', this.onSwitchWorkspace);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
            this.on(document, 'edgesLoaded', this.onEdgesLoaded);

            this.on(document, 'verticesUpdated', this.updateDiffBadge);
            this.on(document, 'verticesDeleted', this.updateDiffBadge);
            this.on(document, 'edgesUpdated', this.updateDiffBadge);
            this.on(document, 'edgesDeleted', this.updateDiffBadge);
            this.on(document, 'updateDiff', this.updateDiffBadge);

            this.on(document, 'toggleDiffPanel', this.toggleDiffPanel);
            this.on(document, 'escape', this.closeDiffPanel);

            this.on(document, 'toggleTimeline', function() {
                self.select('toggleTimelineSelector').toggleClass('expanded');
            });
            this.on('click', {
                toggleTimelineSelector: this.onToggleTimeline
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope'].map(i18n),
                shortcuts: {
                    'alt-d': { fire: 'toggleDiffPanel', desc: i18n('workspaces.help.show_diff') }
                }
            });

            this.trigger(document, 'registerKeyboardShortcuts', {
                scope: ['graph.help.scope', 'map.help.scope'].map(i18n),
                shortcuts: {
                    'alt-i': { fire: 'toggleTimeline', desc: i18n('workspaces.help.toggle_timeline') }
                }
            });
        });

        this.onToggleTimeline = function() {
            this.trigger('toggleTimeline');
        }

        this.toggleDiffPanel = function() {
            var badge = this.$node.find('.badge');
            if (badge.is(':visible')) {
                badge.popover('toggle');
            }
        };

        this.closeDiffPanel = function() {
            var badge = this.$node.find('.badge');
            if (badge.is(':visible')) {
                badge.popover('hide');
            }
        };

        this.onGraphPaddingUpdated = function(event, data) {
            this.$node.css('left', data.padding.l + MENUBAR_WIDTH);

            var width = $(window).width(),
                height = $(window).height(),
                popover = this.$node.find('.popover'),
                paddingH = 100,
                paddingV = 75,
                popoverCss = {
                    maxWidth: (width - MENUBAR_WIDTH - (data.padding.l + data.padding.r) - paddingH),
                    maxHeight: (height - (data.padding.t + data.padding.b) - paddingV)
                };

            this.popoverCss = popoverCss;
            if (popover.length) {
                this.updatePopoverSize(popover);
            }
        };

        this.updatePopoverSize = function(tip) {
            var css = {};
            if (tip.width() > this.popoverCss.maxWidth) {
                css.width = this.popoverCss.maxWidth + 'px';
            }
            if (tip.height() > this.popoverCss.maxHeight) {
                css.height = this.popoverCss.maxHeight + 'px';
            }

            tip.resizable('option', 'maxWidth', this.popoverCss.maxWidth);
            tip.resizable('option', 'maxHeight', this.popoverCss.maxHeight);
            if (_.keys(css).length) {
                tip.css(css);
            }
        }

        this.setContent = function(title, editable, commentable) {
            this.select('nameSelector').text(title);
        };

        this.updateWithNewWorkspaceData = function(workspace) {
            this.setContent(
                workspace.title,
                workspace.editable,
                workspace.commentable
            );
            clearTimeout(this.updateTimer);
            this.updateWorkspaceTooltip(workspace);
        };

        this.onSwitchWorkspace = function(event, data) {
            if (this.previousWorkspace !== data.workspaceId) {
                this.previousDiff = null;
                this.$node.find('.badge').popover('destroy').remove();
            }
        };

        this.onWorkspaceLoaded = function(event, data) {
            this.$node.show();
            this.updateWithNewWorkspaceData(data);
            this.previousWorkspace = data.workspaceId;
        };

        this.onWorkspaceUpdated = function(event, data) {
            if (visalloData.currentWorkspaceId === data.workspace.workspaceId) {
                this.updateWithNewWorkspaceData(data.workspace);
            }
        };

        this.onEdgesLoaded = function(event, data) {
            this.updateWorkspaceTooltip(data);
            this.updateDiffBadge();
        };

        this.onWorkspaceSaving = function(event, data) {
            clearTimeout(this.updateTimer);
            this.updateWorkspaceTooltip(data);
        };

        this.onWorkspaceSaved = function(event, data) {
            clearTimeout(this.updateTimer);
            this.lastSaved = F.date.utc(Date.now());

            if (data.title) {
                this.select('nameSelector').text(data.title);
            }

            this.updateWorkspaceTooltip(data);
        };

        this.onDiffBadgeMouse = function(event) {
            this.trigger(
                event.type === 'mouseenter' ? 'focusVertices' : 'defocusVertices',
                { elementIds: this.currentDiffIds || [] }
            );
        };

        this.updateDiffBadge = function(event, data) {
            var self = this,
                node = this.select('nameSelector'),
                badge = this.$node.find('.badge');

            if (!badge.length) {
                badge = $('<span class="badge"></span>')
                    .insertAfter(node)
                    .on('mouseenter mouseleave', this.onDiffBadgeMouse.bind(this))
            }

            Promise.all([
                this.dataRequest('workspace', 'diff'),
                this.dataRequest('ontology', 'properties'),
                this.dataRequest('ontology', 'concepts')
            ]).done(function(results) {
                var ontologyProperties = results[1],
                    ontologyConcepts = results[2],
                    diffs = results[0].diffs,
                    diffsWithoutVisibleProperty = _.map(diffs, function(d) {
                        return _.omit(d, 'visible');
                    });

                // Check if same
                if (self.previousDiff && _.isEqual(diffsWithoutVisibleProperty, self.previousDiff)) {
                    return;
                }
                self.previousDiff = diffsWithoutVisibleProperty;

                var vertexDiffsById = _.indexBy(diffs, function(diff) {
                        return diff.vertexId;
                    }),
                    count = 0,
                    alreadyCountedCompoundProperties = [],
                    filteredDiffs = _.filter(diffs, function(diff) {
                        if (diff.type === 'PropertyDiffItem') {
                            var ontologyProperty = ontologyProperties.byTitle[diff.name];
                            if (!ontologyProperty || !ontologyProperty.userVisible) return false;

                            var vertexDiff = vertexDiffsById[diff.elementId];
                            if (vertexDiff && diff.name === 'title') return true;

                            var compoundProperty = ontologyProperties.byDependentToCompound[diff.name],
                                ontologyConcept = ontologyConcepts.byId[diff.elementConcept];
                            if (compoundProperty && ontologyConcept && ontologyConcept.properties.indexOf(compoundProperty) >= 0) {
                                var alreadyCountedKey = diff.elementId + diff.key + compoundProperty;
                                if (_.contains(alreadyCountedCompoundProperties, alreadyCountedKey)) {
                                    return true;
                                }
                                alreadyCountedCompoundProperties.push(alreadyCountedKey);
                            }
                        }
                        count++;
                        return true;
                    });
                self.formattedCount = F.number.pretty(count);

                self.currentDiffIds = _.uniq(filteredDiffs.map(function(diff) {
                    return diff.vertexId || diff.elementId || diff.edgeId;
                }));

                require(['workspaces/diff/diff'], function(Diff) {
                    var popover = badge.data('popover'),
                        tip = popover && popover.tip();

                    if (tip && tip.is(':visible')) {
                        self.trigger(popover.tip().find('.popover-content'),
                             'diffsChanged',
                             { diffs: filteredDiffs });
                        popover.show();
                    } else {
                        badge
                            .popover('destroy')
                            .popover({
                                placement: 'top',
                                content: i18n('workspaces.diff.loading'),
                                title: i18n('workspaces.diff.header.unpublished_changes')
                            });

                        popover = badge.data('popover');
                        tip = popover.tip();

                        var left = 10;
                        tip.css({
                                width: '400px',
                                height: '250px'
                            })
                            .data('sizePreference', 'diff')
                            .find('.arrow').css({
                                left: parseInt(badge.position().left - (left / 2) + 1, 10) + 'px',
                                marginLeft: 0
                            })

                        // We fill in our own content
                        popover.setContent = function() {}
                        badge.on('shown', function() {
                            var css = {
                                top: (parseInt(tip.css('top')) - 10) + 'px'
                            };
                            tip.resizable({
                                handles: 'n, e, ne',
                                maxWidth: self.popoverCss.maxWidth,
                                maxHeight: self.popoverCss.maxHeight
                            }).css({top: top});

                            self.updatePopoverSize(tip);

                            Diff.attachTo(tip.find('.popover-content'), {
                                diffs: filteredDiffs
                            });
                        })

                        Diff.teardownAll();
                    }
                });

                badge.removePrefixedClasses('badge-').addClass('badge-info')
                    .attr('title', i18n('workspaces.diff.unpublished_change.' + (
                        self.formattedCount === 1 ?
                        'one' : 'some'), self.formattedCount))
                    .text(count > 0 ? self.formattedCount : '');

                if (count > 0) {
                    self.animateBadge(badge);
                } else if (count === 0) {
                    badge.popover('destroy');
                }
            })
        };

        var badgeReset, animateTimer;
        this.animateBadge = function(badge) {
            badge.text(this.formattedCount).css('width', 'auto');

            var self = this,
                html = '<span class="number">' + this.formattedCount + '</span>' +
                    '<span class="suffix"> ' + i18n('workspaces.diff.unpublished') + '</span>',
                previousWidth = badge.outerWidth(),
                findWidth = function() {
                    return (
                        badge.find('.number').outerWidth(true) +
                        badge.find('.suffix').outerWidth(true) +
                        parseInt(badge.css('paddingRight'), 10) * 2
                    ) + 'px';
                };

            if (animateTimer) {
                clearTimeout(animateTimer);
                animateTimer = _.delay(
                    badgeReset.bind(null, previousWidth),
                    SHOW_UNPUBLUSHED_CHANGES_SECONDS * 1000
                );
                return badge.html(html).css({ width: findWidth() })
            }

            var duration = '0.5s';
            badge.css({
                width: previousWidth + 'px',
                backgroundColor: '#0088cc',
                transition: 'all cubic-bezier(.29,.79,0,1.48) ' + duration,
                position: 'relative'
            }).html(html);

            requestAnimationFrame(function() {
                badge.css({
                    backgroundColor: '#1ab2ff',
                    width: findWidth()
                }).find('.suffix').css({
                    transition: 'opacity ease-out ' + duration
                })

                animateTimer = _.delay((badgeReset = function(previousWidth) {
                    animateTimer = null;
                    badge.on(TRANSITION_END, function(e) {
                        if (e.originalEvent.propertyName === 'width') {
                            badge.off(TRANSITION_END);
                            badge.text(self.formattedCount).css('width', 'auto');
                        }
                    }).css({
                        transition: 'all cubic-bezier(.92,-0.42,.37,1.31) ' + duration,
                        backgroundColor: '#0088cc',
                        width: previousWidth + 'px'
                    }).find('.suffix').css('opacity', 0);
                }).bind(null, previousWidth), SHOW_UNPUBLUSHED_CHANGES_SECONDS * 1000);
            })
        };

        this.updateUserTooltip = function(data) {
            if (data && data.user) {
                this.select('userSelector')
                    .text(data.user.displayName)
                    .css({ cursor: 'pointer' })
                    .on('click', function() {
                        require([
                            'hbs!workspaces/userAccount/modal',
                            'workspaces/userAccount/userAccount'
                        ], function(modalTemplate, UserAccount) {
                            var modal = $(modalTemplate({
                                userName: visalloData.currentUser.userName
                            })).appendTo(document.body);
                            UserAccount.attachTo(modal);
                            modal.modal('show');
                        });
                    })
                    .tooltip({
                        placement: 'right',
                        title: i18n('workspaces.overlay.open.useraccount'),
                        trigger: 'hover',
                        delay: { show: 250, hide: 0 }
                    })
            }
        }

        this.updateWorkspaceTooltip = function(data) {
            if (data && data.data && data.data.vertices) {
                this.verticesCount = data.data.vertices.length;
            }
            if (this.verticesCount === 0) {
                this.edgesCount = 0;
            } else if (data.edges) {
                this.edgesCount = data.edges.length;
            } else {
                this.edgesCount = 0;
            }

            var name = this.select('nameSelector'),
                tooltip = name.data('tooltip'),
                tip = tooltip && tooltip.tip(),
                text = i18n('workspaces.overlay.vertices') + ': ' +
                    F.number.pretty(this.verticesCount || 0) + ', ' +
                    i18n('workspaces.overlay.edges') + ': ' +
                    F.number.pretty(this.edgesCount || 0)

            if (tip && tip.is(':visible')) {
                tip.find('.tooltip-inner span').text(text);
            } else {
                name
                    .tooltip('destroy')
                    .tooltip({
                        placement: 'right',
                        html: true,
                        title: '<span style="white-space:nowrap">' + text + '</span>',
                        trigger: 'hover',
                        delay: { show: 500, hide: 0 }
                    });
            }

        }
    }
});
