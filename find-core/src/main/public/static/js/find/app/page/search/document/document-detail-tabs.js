/*
 * Copyright 2016 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'backbone',
    'i18n!find/nls/bundle',
    'find/app/page/search/document/tab-content-view',
    'find/app/page/search/document/authors-tab',
    'find/app/page/search/document/similar-documents-tab',
    'find/app/page/search/document/similar-dates-tab',
    'find/app/page/search/document/metadata-tab'
], function(Backbone, i18n, TabContentView, AuthorsTab, SimilarDocumentsTab, SimilarDatesTab, MetadataTab) {
    'use strict';

    return [
        {
            TabContentConstructor: TabContentView.extend({TabSubContentConstructor: MetadataTab}),

            title: i18n['search.document.detail.tabs.metadata'],

            shown: function (documentModel) {
                return true;
            }
        },

        {
            TabContentConstructor: TabContentView.extend({TabSubContentConstructor: AuthorsTab}),

            title: i18n['search.document.detail.tabs.authors'],

            shown: function (documentModel) {
                return documentModel.get('authors').length > 0;
            }
        },

        {
            TabContentConstructor: TabContentView.extend({TabSubContentConstructor: SimilarDocumentsTab}),

            title: i18n['search.document.detail.tabs.similarDocuments'],

            shown: function (documentModel) {
                return true;
            }
        },

        {
            TabContentConstructor: TabContentView.extend({TabSubContentConstructor: SimilarDatesTab}),

            title: i18n['search.document.detail.tabs.similarDates'],

            shown: function (documentModel) {
                return true;
            }
        }
    ];
});