
define([], function() {
    'use strict';

    var URL_TYPES = {
            FULLSCREEN: 'v',
            ADD: 'add',
            ADMIN: 'admin'
        },
        V = {
            url: function(vertices, workspaceId) {
                return window.location.href.replace(/#.*$/, '') +
                    '#v=' + _.map(vertices, function(v) {
                        if (_.isObject(v) && 'type' in v) {
                            return encodeURIComponent(v.type.substring(0, 1) + v.id);
                        }
                        return encodeURIComponent(_.isString(v) ? v : v.id);
                    }).join(',') +
                    '&w=' + encodeURIComponent(workspaceId);
            },

            fragmentUrl: function(vertices, workspaceId) {
                return V.url(vertices, workspaceId).replace(/^.*#/, '#');
            },

            isFullscreenUrl: function(url) {
                var toOpen = V.parametersInUrl(url);

                return toOpen &&
                    (toOpen.vertexIds && toOpen.vertexIds.length) ||
                    (toOpen.edgeIds && toOpen.edgeIds.length) &&
                    toOpen.type === 'FULLSCREEN';
            },

            parametersInUrl: function(url) {
                var type = _.invert(URL_TYPES),
                    match = url.match(/#(v|add|admin)=(.+?)(?:&w=(.*))?$/);

                if (match && match.length === 4) {
                    if (match[1] === URL_TYPES.ADMIN) {
                        var tool = match[2].split(':');
                        if (tool.length !== 2) {
                            return null;
                        }

                        return _.extend(_.mapObject({
                            section: tool[0],
                            name: tool[1]
                        }, function(v) {
                            return decodeURIComponent(v).replace(/\+/g, ' ');
                        }), { type: type[match[1]] });
                    }

                    var objects = _.map(match[2].split(','), function(v) {
                            return decodeURIComponent(v);
                        }),
                        data = _.chain(objects)
                            .groupBy(function(o) {
                                var match = o.match(/^(v|e).*/);
                                if (match) {
                                    if (match[1] === 'v') return 'vertexIds';
                                    if (match[1] === 'e') return 'edgeIds';
                                }
                                return 'vertexIds';
                            })
                            .mapObject(function(ids) {
                                return ids.map(function(val) {
                                    return val.substring(1);
                                });
                            })
                            .value();

                    return _.extend({ vertexIds: [], edgeIds: [] }, data, {
                        workspaceId: decodeURIComponent(match[3] || ''),
                        type: type[match[1]]
                    });
                }
                return null;
            }
    };

    return $.extend({}, { vertexUrl: V });
});
