/*
 * Copyright 2014-2017 Hewlett Packard Enterprise Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

define([
    'find/app/util/range-input',
    'backbone',
    'jasmine-jquery'
], function(RangeInput, Backbone) {
    'use strict';

    const MIN = 15;
    const MAX = 23;
    const STEP = 1;
    const LEFT_LABEL = 'Less';
    const RIGHT_LABEL = 'More';

    describe('Range Input View', function() {
        describe('with an initial value within the range', function() {
            beforeEach(function() {
                this.model = new Backbone.Model({
                    value: 16
                });

                this.view = new RangeInput({
                    leftLabel: LEFT_LABEL,
                    max: MAX,
                    min: MIN,
                    model: this.model,
                    rightLabel: RIGHT_LABEL,
                    step: STEP
                });

                this.view.render();
            });

            it('should set the attributes on the slider', function() {
                expect(this.view.$('input')).toHaveAttr('min', String(MIN));
                expect(this.view.$('input')).toHaveAttr('max', String(MAX));
                expect(this.view.$('input')).toHaveAttr('step', String(STEP));
            });

            it('should report the correct value', function() {
                expect(this.view.$('input')).toHaveValue('16');
            });

            it('should update the model when the slider fires a change event', function() {
                this.view.$('input').val(20).change();
                expect(this.model.get('value')).toBe('20');
            });
        });

        describe('with an initial value below the range', function() {
            beforeEach(function() {
                this.model = new Backbone.Model({
                    value: 8
                });

                this.view = new RangeInput({
                    leftLabel: LEFT_LABEL,
                    max: MAX,
                    min: MIN,
                    model: this.model,
                    rightLabel: RIGHT_LABEL,
                    step: STEP
                });

                this.view.render();
            });

            it('should report the minimum value', function() {
                expect(this.view.$('input')).toHaveValue(String(MIN));
            })
        });

        describe('with an initial value above the range', function() {
            beforeEach(function() {
                this.model = new Backbone.Model({
                    value: 42
                });

                this.view = new RangeInput({
                    leftLabel: LEFT_LABEL,
                    max: MAX,
                    min: MIN,
                    model: this.model,
                    rightLabel: RIGHT_LABEL,
                    step: STEP
                });

                this.view.render();
            });

            it('should report the maximum value', function() {
                expect(this.view.$('input')).toHaveValue(String(MAX));
            })
        })
    })

});