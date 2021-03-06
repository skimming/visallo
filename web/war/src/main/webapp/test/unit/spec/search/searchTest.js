
describeComponent('search/search', function() {

    beforeEach(function(done) {
        setupComponent(this)
        var c = this.component;
        $(document).off('dataRequest').on('dataRequest', function(event, data) {
            if (data.service === 'ontology') {
                require(['util/requirejs/promise!util/service/ontologyPromise'], function(o) {
                    $(document).trigger('dataRequestCompleted', {
                        requestId: data.requestId,
                        result: data.method in o ? o[data.method] : o,
                        success: true
                    });
                })
            } else {
                _.defer(function() {
                    $(document).trigger('dataRequestCompleted', {
                        requestId: data.requestId,
                        result: { elements: [] },
                        success: true
                    });
                })
            }
        })
        switchToSearchType(c, 'visallo').then(function() {
            querySetValue(c, '');
            done();
        });
    })

    describe('search', function() {

        it('should initialize', function() {
            var c = this.component

            expect(c.select('querySelector').length).to.equal(1)
            expect(c.select('formSelector').length).to.equal(1)
        })

        it('should set visallo type to active', function() {
            this.component.$node.find('.search-type-visallo').hasClass('active').should.be.true
            this.component.$node.find('.find-visallo').hasClass('active').should.be.true
        })

        it('should set workspace type to inactive', function() {
            this.component.$node.find('.search-type-workspace').hasClass('active').should.be.false
            this.component.$node.find('.find-workspace').hasClass('active').should.be.false
        })

        it('should attach search types based on segmented control after focus', function(done) {
            var c = this.component,
                workspaceLoaded = $.Deferred();

            expect(c.$node.find('.search-type-workspace').html().length).to.equal(0)

            c.on('searchtypeloaded', workspaceLoaded.resolve);
            c.$node.find('.find-workspace').click();

            workspaceLoaded.then(function() {
                var node = c.$node.find('.search-type-workspace')
                node.hasClass('active').should.be.true
                expect(node.html().length).to.be.above(0)
                done()
            })
        })

        it('should reset the search text on type change', function(done) {
            var c = this.component,
                $query = c.select('querySelector'),
                originalQuery = 'My query';

            querySetValue(c, originalQuery);
            c.$node.find('.find-workspace').hasClass('active').should.be.false

            switchToSearchType(c, 'workspace').then(function() {
                $query.val().should.equal('')

                switchToSearchType(c, 'visallo').then(function() {
                    $query.val().should.equal(originalQuery)
                    done()
                })
            })
        })

        it('should trigger pane resized on type change', function(done) {
            var c = this.component,
                $query = c.select('querySelector'),
                originalQuery = 'My query';

            querySetValue(c, originalQuery);
            c.$node.find('.find-workspace').hasClass('active').should.be.false

            c.$node.on('paneResized', _.after(2, function() {
                done();
            }))

            switchToSearchType(c, 'workspace')
                .then(function() {
                    switchToSearchType(c, 'visallo')
                })
        })

        it('should show clear search button when text typed', function() {
            var c = this.component,
                $q = c.select('querySelector'),
                $clear = c.select('clearSearchSelector');

            querySetValue(c, 'Some text');
            $clear.css('display').should.equal('inline')
            querySetValue(c, '');
            $clear.css('display').should.equal('none')
        })

        it('should hide clear search button when type changed', function(done) {
            var c = this.component,
                $q = c.select('querySelector'),
                $clear = c.select('clearSearchSelector');

            querySetValue(c, 'Some text');
            $clear.css('display').should.equal('inline')

            switchToSearchType(c, 'workspace')
                .then(function() {
                    $clear.css('display').should.equal('none')
                    done()
                })
        })

        it('should clear search on button', function(done) {
            var c = this.component,
                $q = c.select('querySelector'),
                $clear = c.select('clearSearchSelector')

            c.$node.find('.search-type-visallo').on('clearSearch', function() {
                _.defer(function() {
                    $q.val().should.be.empty
                    done()
                })
            })
            querySetValue(c, 'Some text')
            _.defer(function() {
                $clear.css('display').should.equal('inline')
                $clear.click()
            })
        })

        it('should blur search field on escape', function() {
            var $q = this.component.select('querySelector'),
                event = $.Event('keyup');

            $q.focus()
            document.activeElement.should.equal($q[0])
            event.which = event.keyCode = $.ui.keyCode.ESCAPE;
            $q.trigger(event);
            document.activeElement.should.not.equal($q[0])
        })

        it('should clear search field on escape when value', function(done) {
            var c = this.component,
                $q = c.select('querySelector'),
                event = $.Event('keyup');

            c.$node.find('.search-type-visallo').on('clearSearch', function() {
                _.defer(function() {
                    $q.val().should.be.empty
                    done()
                })
            })

            $q.focus()
            querySetValue(c, 'something')
            document.activeElement.should.equal($q[0])
            event.which = event.keyCode = $.ui.keyCode.ESCAPE;
            $q.trigger(event);
        })

        it('should trigger submit event on enter', function(done) {
            var c = this.component;

            this.$node.on('querysubmit', function() {
                done()
            })
            querySetValue(c, 'test')
            querySubmit(c);
        })

        it('should select search text after enter', function(done) {
            var c = this.component,
                $q = c.select('querySelector'),
                event = $.Event('keyup');

            $q.focus()
            querySetValue(c, 'something')
            document.activeElement.should.equal($q[0])
            event.which = event.keyCode = $.ui.keyCode.ENTER
            $q.trigger(event)

            _.defer(function() {
                var selection = window.getSelection()
                selection.rangeCount.should.equal(1)
                selection.toString().should.equal('something')
                done()
            })
        })

        it('should show search errors', function() {
            var c = this.component,
                $error = c.$node.find('.search-query-validation')

            $error.html().should.be.empty
            c.trigger('searchRequestCompleted', { success: false, error: 'An error message' })
            $error.html().should.not.be.empty
            $error.find('button').remove()
            $.trim($error.text()).should.equal('An error message')
        })

        it('should show search error when no message is passed', function() {
            var c = this.component,
                $error = c.$node.find('.search-query-validation')

            c.trigger('searchRequestCompleted', { success: false })
            $error.html().should.not.be.empty
            $error.find('button').remove()
            $.trim($error.text()).should.equal('search.query.error')
        })

        it('should show hide errors on succesfull query', function() {
            var c = this.component,
                $error = c.$node.find('.search-query-validation')

            c.trigger('searchRequestCompleted', { success: false })
            $error.html().should.not.be.empty
            this.component.trigger('searchRequestCompleted', { success: true })
            $error.html().should.be.empty
        })

    })

    function querySubmit(component) {
        var $q = component.select('querySelector'),
            event = $.Event('keyup');

        $q.focus();
        event.which = event.keyCode = $.ui.keyCode.ENTER;
        $q.trigger(event);
    }

    function querySetValue(component, string) {
        component.select('querySelector')
            .val(string)
            .click()
            .focus()
            .change();
    }

    function switchToSearchType(component, type) {
        return new Promise(function(r) {
            component.on('searchtypeloaded', r);
            component.$node.find('.find-' + type).click();
        });
    }

});
