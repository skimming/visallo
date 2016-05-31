
define([
    'flight/lib/component',
    'configuration/plugins/registry',
    './activity/activity',
    'tpl!./menubar'
], function(defineComponent, registry, Activity, template) {
    'use strict';

    // Add class name of <li> buttons here
    var BUTTONS = 'dashboard graph map search workspaces admin activity logout'.split(' '),
        TOOLTIPS = {
            activity: i18n('menubar.icons.activity.tooltip'),
            dashboard: i18n('menubar.icons.dashboard.tooltip'),
            graph: i18n('menubar.icons.graph'),
            map: i18n('menubar.icons.map.tooltip'),
            search: i18n('menubar.icons.search.tooltip'),
            workspaces: i18n('menubar.icons.workspaces.tooltip'),
            admin: i18n('menubar.icons.admin.tooltip'),
            logout: i18n('menubar.icons.logout.tooltip')
        },

        // Which cannot both be active
        MUTALLY_EXCLUSIVE_SWITCHES = [
            { names: ['dashboard', 'graph', 'map'], options: { allowCollapse: false } },
            { names: ['workspaces', 'search', 'admin'], options: { } }
        ],

        ACTION_TYPES = {
            full: MUTALLY_EXCLUSIVE_SWITCHES[0],
            pane: MUTALLY_EXCLUSIVE_SWITCHES[1],
            url: { names: [], options: {}}
        },

        // Don't change state to highlighted on click
        DISABLE_ACTIVE_SWITCH = 'logout'.split(' '),

        DISABLE_HIDE_TOOLTIP_ON_CLICK = 'logout'.split(' ');

    return defineComponent(Menubar);

    function nameToI18N(name) {
        return i18n('menubar.icons.' + name);
    }

    function menubarItemHandler(name) {
        var sel = name + 'IconSelector';

        return function(e) {
            e.preventDefault();

            var self = this,
                isSwitch = false;

            if (DISABLE_ACTIVE_SWITCH.indexOf(name) === -1) {
                MUTALLY_EXCLUSIVE_SWITCHES.forEach(function(exclusive, i) {
                    if (exclusive.names.indexOf(name) !== -1 && exclusive.options.allowCollapse === false) {
                        isSwitch = true;
                    }
                });
            }
            var icon = this.select(sel);
            if (!_.contains(DISABLE_HIDE_TOOLTIP_ON_CLICK, name)) {
                icon.tooltip('hide');
            }

            if (isSwitch && icon.hasClass('active')) {
                icon.toggleClass('toggled');
            } else {
                requestAnimationFrame(function() {
                    var data = { name: name };
                    if (name in self.extensions) {
                        data.action = self.extensions[name].action;
                    }
                    if (data.action && data.action.type === 'url') {
                        flashIcon(icon);
                        window.open(data.action.url);
                    } else {
                        self.trigger(document, 'menubarToggleDisplay', data);
                    }
                });
            }
        };
    }

    function flashIcon(icon) {
        icon.addClass('active');
        _.delay(function() {
            icon.removeClass('active');
        }, 200);
    }

    function Menubar() {

        this.activities = 0;

        var attrs = {}, events = {};
        BUTTONS.forEach(function(name) {
            var sel = name + 'IconSelector';

            attrs[sel] = '.' + name;
            events[sel] = menubarItemHandler(name);
        });

        var self = this,
            extensions = {};

        registry.documentExtensionPoint('org.visallo.menubar',
            'Add items to menubar',
            function(e) {
                return ('title' in e) &&
                    ('identifier' in e) &&
                    ('action' in e) &&
                    ('icon' in e);
            },
            'http://docs.visallo.org/extension-points/front-end/menubar'
        );
        registry.extensionsForPoint('org.visallo.menubar')
            .forEach(function(data) {
                var cls = data.identifier,
                    type = data.action.type;

                if (type in ACTION_TYPES) {
                    ACTION_TYPES[type].names.push(cls);
                }
                TOOLTIPS[cls] = data.options && data.options.tooltip || data.title;

                extensions[cls] = data;
                attrs[cls + 'IconSelector'] = '.' + cls;
                events[cls + 'IconSelector'] = menubarItemHandler(cls);
            });

        this.defaultAttrs(attrs);

        this.after('initialize', function() {
            var self = this;

            this.$node.html(template({
                isAdmin: visalloData.currentUser.privilegesHelper.ADMIN
            }));
            this.extensions = extensions;

            this.insertExtensions();

            BUTTONS.forEach(function(button) {
                self.$node.find('.' + button).attr('data-identifier', button);
            });

            Object.keys(TOOLTIPS).forEach(function(selectorClass) {
                self.$node.find('.' + selectorClass).tooltip({
                    placement: 'right',
                    html: true,
                    title: (TOOLTIPS[selectorClass].html || TOOLTIPS[selectorClass]).replace(/\s+/g, '&nbsp;'),
                    delay: { show: 250, hide: 0 }
                });
            });

            this.on('click', events);

            Activity.attachTo(this.select('activityIconSelector'));

            this.on(document, 'menubarToggleDisplay', this.onMenubarToggle);
            this.on(document, 'graphPaddingUpdated', this.onGraphPaddingUpdated);
        });

        this.insertExtensions = function() {
            var self = this,
                identifiers = _.pluck(this.extensions, 'identifier'),
                dependenciesForId = {},
                sorted = _.chain(this.extensions)
                    .each(function(e) {
                        var placementHint = e.options && (e.options.placementHintAfter || e.options.placementHintBefore || '');
                        if (placementHint && _.contains(identifiers, placementHint)) {
                            if (!dependenciesForId[placementHint]) {
                                dependenciesForId[placementHint] = [];
                            }
                            if (dependenciesForId[e.identifier] && _.contains(dependenciesForId[e.identifier], placementHint)) {
                                console.warn('Circular dependency between menubar extensions. Deleting placement hint:', placementHint, 'from:', e.identifier);
                                delete e.options.placementHintAfter;
                                delete e.options.placementHintBefore;
                            } else {
                                dependenciesForId[placementHint].push(e.identifier);
                            }
                        }
                    })
                    .values()
                    .value()
                    .sort(function(e1, e2) {
                        var deps1 = dependenciesForId[e1.identifier] || [],
                            deps2 = dependenciesForId[e2.identifier] || [];

                        if (_.contains(deps1, e2.identifier)) return -1;
                        if (_.contains(deps2, e1.identifier)) return 1;

                        var vals = _.flatten(_.values(dependenciesForId));
                        if (_.contains(vals, e1.identifier)) return 1;
                        if (_.contains(vals, e2.identifier)) return -1;
                        return 0;
                    })

            _.each(sorted, function(item) {
                var cls = item.identifier,
                    options = $.extend({
                        placementHint: 'top',
                        tooltip: item.title,
                        anchorCss: {}
                    }, item.options),
                    newItem = $('<li>')
                        .addClass(cls)
                        .attr('data-identifier', item.identifier)
                        .append(
                            $('<a>')
                            .text(item.title)
                            .css(
                                $.extend({
                                    'background-image': 'url("' + item.icon + '")'
                                }, options.anchorCss)
                            )
                        ),
                    container = self.$node.find('.menu-' + options.placementHint),
                    placementHint = options.placementHintAfter || options.placementHintBefore,
                    $placement = placementHint && container.find('.' + placementHint)

                if ($placement) {
                    if ($placement.length) {
                        if (options.placementHintAfter) {
                            return newItem.insertAfter($placement);
                        } else if (options.placementHintBefore) {
                            return newItem.insertBefore($placement);
                        }
                    } else {
                        console.warn('Unable to find menubar item placementHint:', placementHint, 'identifier:', item.identifier);
                    }
                }

                newItem.insertBefore(container.find('.divider:last-child'));
            })
        }

        this.onGraphPaddingUpdated = function(event, data) {
            var len = this.$node.find('a').length,
                approximateHeight = len * 50,
                approximateNoTextHeight = len * 40,
                windowHeight = $(window).height();
            this.$node.toggleClass('hide-icon-text', approximateHeight >= windowHeight);
            this.$node.toggleClass('hide-more', approximateNoTextHeight >= windowHeight);
        };

        this.onMenubarToggle = function(e, data) {
            var self = this,
                icon = this.select(data.name + 'IconSelector'),
                active = icon.hasClass('active');

            if (DISABLE_ACTIVE_SWITCH.indexOf(data.name) === -1) {
                var isSwitch = false;

                if (!active) {
                    MUTALLY_EXCLUSIVE_SWITCHES.forEach(function(exclusive, i) {
                        if (exclusive.names.indexOf(data.name) !== -1) {
                            isSwitch = true;
                                exclusive.names.forEach(function(name) {
                                    if (name !== data.name) {
                                        var otherIcon = self.select(name + 'IconSelector');
                                        if (otherIcon.hasClass('active')) {
                                            self.trigger(document, 'menubarToggleDisplay', {
                                                name: name,
                                                isSwitchButCollapse: true
                                            });
                                        }
                                    } else icon.addClass('active');
                                });
                        }
                    });
                }

                if (!isSwitch || data.isSwitchButCollapse) {
                    icon.toggleClass('active');
                }

            } else {

                // Just highlight briefly to show click worked
                icon.addClass('active');
                setTimeout(function() {
                    icon.removeClass('active');
                }, 200);
            }
        };
    }
});
