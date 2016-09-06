/*
 * Copyright 2015 Hewlett-Packard Development Company, L.P.
 * Licensed under the MIT License (the "License"); you may not use this file except in compliance with the License.
 */

package com.hp.autonomy.frontend.find.hod.export;

import com.hp.autonomy.frontend.find.core.export.ExportFormat;
import com.hp.autonomy.frontend.find.core.export.ExportService;
import com.hp.autonomy.frontend.find.core.export.ExportStrategy;
import com.hp.autonomy.hod.client.api.resource.ResourceIdentifier;
import com.hp.autonomy.hod.client.error.HodErrorException;
import com.hp.autonomy.searchcomponents.core.config.FieldInfo;
import com.hp.autonomy.searchcomponents.core.search.DocumentsService;
import com.hp.autonomy.searchcomponents.core.search.SearchRequest;
import com.hp.autonomy.searchcomponents.hod.search.HodSearchResult;
import com.hp.autonomy.types.requests.Documents;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class HodExportService implements ExportService<ResourceIdentifier, HodErrorException> {
    private final DocumentsService<ResourceIdentifier, HodSearchResult, HodErrorException> documentsService;
    private final Map<ExportFormat, ExportStrategy> exportStrategies;

    @Autowired
    public HodExportService(final DocumentsService<ResourceIdentifier, HodSearchResult, HodErrorException> documentsService,
                            final ExportStrategy[] exportStrategies) {
        this.documentsService = documentsService;

        this.exportStrategies = new EnumMap<>(ExportFormat.class);
        for(final ExportStrategy exportStrategy : exportStrategies) {
            this.exportStrategies.put(exportStrategy.getExportFormat(), exportStrategy);
        }
    }

    @Override
    public void export(final OutputStream outputStream, final SearchRequest<ResourceIdentifier> searchRequest, final ExportFormat exportFormat, final Collection<String> selectedFieldIds) throws HodErrorException {
        final ExportStrategy exportStrategy = exportStrategies.get(exportFormat);
        final List<String> fieldIds = exportStrategy.getFieldNames(HodMetadataNode.values(), selectedFieldIds);
        searchRequest.setPrintFields(fieldIds);
        final Documents<HodSearchResult> documents = documentsService.queryTextIndex(searchRequest);

        try {
            //Caution: outputStream should not be written to before call to prependOutput().
            exportStrategy.prependOutput(outputStream);

            if(exportStrategy.writeHeader()) {
                exportStrategy.exportRecord(outputStream, fieldIds);
            }

            final List<Function<HodSearchResult, String>> exportMetadataFunctions = Arrays.stream(HodMetadataNode.values())
                    .filter(node -> selectedFieldIds.isEmpty() || selectedFieldIds.contains(node.getName()))
                    .map(node -> (Function<HodSearchResult, String>)hodSearchResult -> {
                        final Object value = node.getGetter().apply(hodSearchResult);
                        return value == null ? "" : value.toString();
                    })
                    .collect(Collectors.toList());

            for(final HodSearchResult searchResult : documents.getDocuments()) {
                final Stream<String> metadataStream = exportMetadataFunctions.stream()
                        .map(extractor -> extractor.apply(searchResult));

                final Stream<String> nonMetadataStream = exportStrategy.getConfiguredFieldsById().values().stream()
                        .filter(configuredField -> selectedFieldIds.isEmpty() || selectedFieldIds.contains(configuredField.getId()))
                        .map(configuredField -> {
                            final FieldInfo<?> fieldInfo = searchResult.getFieldMap().get(configuredField.getId());
                            return exportStrategy.combineValues(getValuesAsStrings(fieldInfo));
                        });

                exportStrategy.exportRecord(outputStream, Stream.concat(metadataStream, nonMetadataStream).collect(Collectors.toList()));
            }
        } catch(final IOException e) {
            //noinspection ProhibitedExceptionThrown
            throw new RuntimeException("Error parsing data", e);
        }
    }

    private List<String> getValuesAsStrings(final FieldInfo<?> fieldInfo) {
        final List<String> stringList;
        if(fieldInfo != null) {
            stringList = fieldInfo.getValues().stream()
                    // prevents NullPointerException if the data set contains an incorrectly formatted date
                    .filter(Objects::nonNull)
                    .map(Object::toString).collect(Collectors.toList());
        } else {
            stringList = Collections.emptyList();
        }
        return stringList;
    }
}
