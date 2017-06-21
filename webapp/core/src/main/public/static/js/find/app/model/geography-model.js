define([
    'backbone',
    'fieldtext/js/field-text-parser',
    'find/app/configuration'
], function(Backbone, parser, configuration) {

    return Backbone.Model.extend({
        /**
         * @typedef {Object} GeographyModelAttributes
         * @property {?Array} shapes
         */
        /**
         * @type GeographyModelAttributes
         */
        defaults: {
            shapes: []
        },

        appendFieldText: function(existingFieldText){
            const toAppend = this.toFieldText();

            if (toAppend) {
                return existingFieldText ? toAppend.AND(toAppend) : toAppend;
            }

            return existingFieldText;
        },

        /**
         * Convert this model to fieldtext queries
         */
        toFieldText: function() {
            const shapes = this.get('shapes');
            if (!shapes || !shapes.length) {
                return null;
            }

            const fieldNodes = [];

            const config = configuration();
            const fieldsInfo = config.fieldsInfo;

            let LAT, LON;

            function getFieldName(fieldName) {
                const fieldMeta = fieldsInfo[fieldName];
                if (fieldMeta) {
                    return fieldMeta.names[0];
                }
            }

            // TODO: what if they have multiple lat and lon fields?
            // TODO: what if a lat/lon field has multiple names?
            if (config.map && config.map.locationFields) {
                const firstField = config.map.locationFields[0];

                if (firstField) {
                    const latField = firstField.latitudeField;
                    const lonField = firstField.longitudeField;

                    LAT = getFieldName(latField);
                    LON = getFieldName(lonField);
                }
            }

            // TODO: read map config for the latitude field
            if (!LAT || !LON) {
                return null;
            }

            _.each(shapes, function (shape) {
                if (shape.type === 'circle') {
                    fieldNodes.push(new parser.ExpressionNode('DISTSPHERICAL', [ LAT, LON ], [
                        shape.center[0],
                        shape.center[1],
                        Math.round(shape.radius / 1000) // IDOL uses kilometers, while we use meters
                    ]));
                }
                else if (shape.type === 'polygon') {
                    var points = _.flatten(shape.points);
                    fieldNodes.push(new parser.ExpressionNode('POLYGON', [ LAT, LON ], points));
                }
            });

            if (fieldNodes.length) {
                return _.reduce(fieldNodes, parser.OR);
            } else {
                return null;
            }
        }
    });

});