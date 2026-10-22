import {addNode, createSite, deleteSite, publishAndWaitJobEnding} from '@jahia/cypress';
import {addSimplePage} from '../../utils/Utils';

describe('Client cache status of a page', () => {
    const siteKey = 'cacheStatusSite';
    const home = `/sites/${siteKey}/home`;
    const module = 'client-cache-control-test-template';

    const addPageWithItem = (page: string, primaryNodeType: string, view: string) => {
        addSimplePage(home, page, page, 'en', 'simple').then(() => {
            addNode({parentPathOrId: `${home}/${page}`, primaryNodeType: 'jnt:contentList', name: 'pagecontent'}).then(() => {
                addNode({
                    parentPathOrId: `${home}/${page}/pagecontent`,
                    primaryNodeType: primaryNodeType,
                    name: 'item',
                    properties: [{name: 'j:view', value: view}],
                    mixins: ['jmix:renderable']
                });
            });
        });
    };

    const statusOf = (page: string) => cy.apollo({
        queryFile: 'clientCacheStatus.graphql',
        variables: {path: `${home}/${page}`, language: 'en'}
    }).then(result => result.data.jcr.nodeByPath.clientCacheStatus);

    const anonymousCacheControl = (page: string) => cy.request({url: `/en${home}/${page}.html`})
        .then(response => response.headers['cache-control']);

    before('Create the pages of the test site', () => {
        createSite(siteKey, {locale: 'en', templateSet: module, serverName: 'localhost'});
        addSimplePage(home, 'publicPage', 'publicPage', 'en', 'simple');
        addPageWithItem('privatePage', 'ccc:article', 'private');
        addPageWithItem('customPage', 'ccc:article', 'authored');
        addPageWithItem('notCacheablePage', 'ccc:time', 'default');
        publishAndWaitJobEnding(home);
        addSimplePage(home, 'unpublishedPage', 'unpublishedPage', 'en', 'simple');
    });

    after('Delete the test site', () => {
        deleteSite(siteKey);
    });

    it('reports a public page with no fragment, and the header of an anonymous visitor', () => {
        statusOf('publicPage').then(status => {
            expect(status.published).to.eq(true);
            expect(status.policy).to.eq('public');
            expect(status.fragments).to.have.length(0);
            anonymousCacheControl('publicPage').then(header => expect(status.header).to.eq(header));
        });
    });

    it('names the fragment whose view sets cache.private', () => {
        statusOf('privatePage').then(status => {
            expect(status.policy).to.eq('private');
            const fragment = status.fragments.find(f => f.path === `${home}/privatePage/pagecontent/item`);
            expect(fragment).to.include({view: 'private', module: module, policy: 'private', reason: 'CACHE_PRIVATE', fromCache: false});
            anonymousCacheControl('privatePage').then(header => expect(status.header).to.eq(header));
        });
    });

    it('names the fragment whose view sets cache.expiration, with its TTL', () => {
        statusOf('customPage').then(status => {
            expect(status.policy).to.eq('custom');
            expect(status.ttl).to.eq(42);
            const fragment = status.fragments.find(f => f.path === `${home}/customPage/pagecontent/item`);
            expect(fragment).to.include({view: 'authored', policy: 'custom', ttl: 42, reason: 'CACHE_EXPIRATION'});
            anonymousCacheControl('customPage').then(header => expect(status.header).to.eq(header));
        });
    });

    it('names the fragment that is not cacheable', () => {
        statusOf('notCacheablePage').then(status => {
            expect(status.policy).to.eq('private');
            const fragment = status.fragments.find(f => f.path === `${home}/notCacheablePage/pagecontent/item`);
            expect(fragment).to.include({policy: 'private', reason: 'NOT_CACHEABLE'});
            anonymousCacheControl('notCacheablePage').then(header => expect(status.header).to.eq(header));
        });
    });

    it('gives the same fragments from the HTML cache on a second check', () => {
        statusOf('privatePage').then(status => {
            const fragment = status.fragments.find(f => f.path === `${home}/privatePage/pagecontent/item`);
            expect(fragment).to.include({reason: 'CACHE_PRIVATE', fromCache: true});
        });
    });

    it('reports a page that a guest cannot read in live as not published', () => {
        statusOf('unpublishedPage').then(status => {
            expect(status.published).to.eq(false);
            expect(status.fragments).to.have.length(0);
        });
    });
});
