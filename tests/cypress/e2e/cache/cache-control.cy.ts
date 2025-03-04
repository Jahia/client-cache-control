import {addNode, createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress';
import {addSimplePage} from '../../utils/Utils';

// Should be reactivated and completed when PR https://github.com/Jahia/jahia-private/pull/2353 will be merged
//   See -> https://jira.jahia.org/browse/BACKLOG-23569
describe.skip('Cache Control header tests', () => {
    const targetSiteKey = 'cacheTestSite';
    before('Create target test site', () => {
        cy.log('Create site ' + targetSiteKey + ' for cache-control tests');
        createSite(targetSiteKey, {locale: 'en', templateSet: 'client-cache-control-test-template', serverName: 'localhost'});
        addNode({parentPathOrId: `/sites/${targetSiteKey}/home`,
            primaryNodeType: 'jnt:contentList',
            name: 'pagecontent'
        }).then(() => {
            addNode({
                parentPathOrId: `/sites/${targetSiteKey}/home/pagecontent`,
                primaryNodeType: 'ccc:simpleType',
                name: 'simple-type',
                properties: [{name: 'j:view', value: 'displayParamValues'}],
                mixins: ['jmix:renderable']
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
    });

    // Test case 1 : Verify that a rendered page without any specific content contains Cache-Control header with :
    //  - a private cache-control (when accessed as authenticated user)
    //  - a public cache-control (when accessed as guest)
    it('should find cache-control header according to render client policy test case 1', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page1', 'Page test case 1', 'en', 'simple').then(() => {
            publishAndWaitJobEnding(`/sites/${targetSiteKey}/home`);
        });
        cy.log('The page should contains Cache-Control header for private content when accessed logged in');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page1.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('bodywrapper');
            const cache = response.headers['Cache-Control'];
            expect(cache).to.contains('private');
            expect(cache).to.contains('no-cache');
            expect(cache).to.contains('no-store');
            expect(cache).to.contains('max-age=0');
        });
        cy.logout();
        cy.log('The page should contains Cache-Control header for public content when not logged');
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page1.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('bodywrapper');
            const cache = response.headers['Cache-Control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('must-revalidate');
            expect(cache).to.contains('max-age=1');
            expect(cache).to.contains('s-maxage=300');
            expect(cache).to.contains('stale-while-revalidate=15');
        });
    });

    // Test case 2 : Verify that a rendered page with a private article is flagged with a private strategy when accessed as guest
    it('should find cache-control header according to render client policy test case 2', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page2', 'Page test case 2', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/page2`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/page2/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'private'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains Cache-Control header for private content when not logged');
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page2.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Private');
            const cache = response.headers['Cache-Control'];
            expect(cache).to.contains('private');
            expect(cache).to.contains('no-cache');
            expect(cache).to.contains('no-store');
            expect(cache).to.contains('max-age=0');
        });
    });

    // Test case 3 : Verify that a rendered page with an article authored (use of cache.expiration=60) is flagged with a custom strategy when accessed as guest
    it('should find cache-control header according to render client policy test case 3', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page3', 'Page test case 3', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/page3`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/page3/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'authored'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains Cache-Control header for custom content when not logged');
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page3.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Authored');
            const cache = response.headers['Cache-Control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('must-revalidate');
            expect(cache).to.contains('max-age=1');
            expect(cache).to.contains('s-maxage=60');
            expect(cache).to.contains('stale-while-revalidate=15');
        });
    });

    // Test case 4 : Verify that accessing files (like images) are flagged with a public strategy
    // Test case 5 : Verify that accessing modules resources content are flagged with a public strategy
    it('should find cache-control header in module resources test case 4', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page3', 'Page test case 3', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/page3`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/page3/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'authored'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.log('The page should contains Cache-Control header for custom content when not logged');
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page3.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Authored');
            const cache = response.headers['Cache-Control'];
            expect(cache).to.contains('public');
            expect(cache).to.contains('must-revalidate');
            expect(cache).to.contains('max-age=1');
            expect(cache).to.contains('s-maxage=60');
            expect(cache).to.contains('stale-while-revalidate=15');
        });
    });

    //   -> Use a request to simpleTemplateSet module for /javascript/js1.js
    // Test case 6 : Verify that accessing generated resources are flagged with an immutable strategy
    //   ->  use a page that contains resources, parse injected css or js and access the resource directly to check header
    it('should find cache-control header immutable for generated resources, test case 6', () => {
        cy.login();
        addSimplePage(`/sites/${targetSiteKey}/home`, 'page6', 'Page test case 6', 'en', 'simple').then(() => {
            addNode({parentPathOrId: `/sites/${targetSiteKey}/home/page6`,
                primaryNodeType: 'jnt:contentList',
                name: 'pagecontent'
            }).then(() => {
                addNode({
                    parentPathOrId: `/sites/${targetSiteKey}/home/page6/pagecontent`,
                    primaryNodeType: 'ccc:article',
                    name: 'article',
                    properties: [{name: 'j:view', value: 'authored'}],
                    mixins: ['jmix:renderable']
                });
            });
        }).then(() => {
            publishAndWaitJobEnding('/sites/' + targetSiteKey + '/home');
        });
        cy.logout();
        cy.request({
            url: '/en/sites/' + targetSiteKey + '/home/page6.html',
            followRedirect: true,
            failOnStatusCode: false
        }).then(response => {
            expect(response.status).to.eq(200);
            expect(response.body).to.contain('Article Authored');
            cy.log('The page should contains Cache-Control header for custom content when not logged');
            cy.get('link#staticAssetCSS0').invoke('attr', 'href').then(href => {
                cy.log('The css file should contains Cache-Control header for immutable content when not logged, visiting: ' + href);
                // eslint-disable-next-line max-nested-callbacks
                cy.request(href).then(response2 => {
                    expect(response2.status).to.eq(200);
                    const cache = response.headers['Cache-Control'];
                    expect(cache).to.contains('public');
                    expect(cache).to.contains('max-age=');
                    expect(cache).to.contains('s-maxage=60');
                    expect(cache).to.contains('immutable');
                });
            });
        });
    });
    // Test case 7 : Verify that accessing Csrf module resources are flagged with an immutable strategy
    // Test case 8 : Verify that accessing /tools is flagged with a private strategy
    // Test case 9 : Verify that accessing /cms/* (other than /cms/render) are flagged with a private strategy
    // Test case 10 : Verify that accessing /engines/*.jsp element are flagged with a private strategy
    // Test case 11 : Verify that accessing /administration/* JSP element are flagged with a private strategy

    after('Clean', () => {
        deleteSite(targetSiteKey);
    });
});
