package uk.gov.hmcts.reform.fpl.service.document.aggregator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.HearingFurtherEvidenceBundle;
import uk.gov.hmcts.reform.fpl.model.common.Element;
import uk.gov.hmcts.reform.fpl.model.documentview.DocumentContainerView;
import uk.gov.hmcts.reform.fpl.model.documentview.DocumentViewType;
import uk.gov.hmcts.reform.fpl.service.document.transformer.ApplicationDocumentBundleTransformer;
import uk.gov.hmcts.reform.fpl.service.document.transformer.FurtherEvidenceDocumentsBundlesTransformer;
import uk.gov.hmcts.reform.fpl.service.document.transformer.OtherDocumentsTransformer;
import uk.gov.hmcts.reform.fpl.service.document.transformer.RespondentStatementsTransformer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.reform.fpl.utils.ElementUtils.element;

@ExtendWith(MockitoExtension.class)
class BundleViewAggregatorTest {

    private static final DocumentViewType DOCUMENT_VIEW_TYPE = DocumentViewType.HMCTS;
    private static final List<Element<HearingFurtherEvidenceBundle>> HEARING_FURTHER_EVIDENCE_DOCUMENTS = List.of(
        element(UUID.randomUUID(), mock(HearingFurtherEvidenceBundle.class)));
    private static final CaseData CASE_DATA = CaseData.builder()
        .hearingFurtherEvidenceDocuments(HEARING_FURTHER_EVIDENCE_DOCUMENTS)
        .build();
    private static final DocumentContainerView APPLICATION_STATEMENT_BUNDLE_VIEWS = mock(DocumentContainerView.class);
    private static final List<DocumentContainerView> FURTHER_EVIDENCE_BUNDLE_VIEWS =
        List.of(mock(DocumentContainerView.class));
    private static final List<DocumentContainerView> RESPONDENT_STATEMENT_BUNDLE_VIEWS =
        List.of(mock(DocumentContainerView.class));
    private static final List<DocumentContainerView> OTHER_DOCUMENTS_BUNDLE_VIEWS =
        List.of(mock(DocumentContainerView.class));

    @Mock
    private ApplicationDocumentBundleTransformer applicationDocumentsTransformer;

    @Mock
    private FurtherEvidenceDocumentsBundlesTransformer furtherEvidenceTransformer;

    @Mock
    private RespondentStatementsTransformer respondentStatementsTransformer;

    @Mock
    private OtherDocumentsTransformer otherDocumentsTransformer;

    @InjectMocks
    private BundleViewAggregator underTest;

    @Test
    void testGetDocumentBundleViews() {
        when(applicationDocumentsTransformer.getApplicationStatementAndDocumentBundle(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(APPLICATION_STATEMENT_BUNDLE_VIEWS);

        when(furtherEvidenceTransformer.getFurtherEvidenceDocumentsBundleView(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(FURTHER_EVIDENCE_BUNDLE_VIEWS);

        when(respondentStatementsTransformer.getRespondentStatementsBundle(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(RESPONDENT_STATEMENT_BUNDLE_VIEWS);

        when(otherDocumentsTransformer.getOtherDocumentsView(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(OTHER_DOCUMENTS_BUNDLE_VIEWS);

        List<DocumentContainerView> actual = underTest.getDocumentBundleViews(CASE_DATA, DOCUMENT_VIEW_TYPE);

        assertThat(actual).isEqualTo(Stream.of(
            List.of(APPLICATION_STATEMENT_BUNDLE_VIEWS),
            FURTHER_EVIDENCE_BUNDLE_VIEWS,
            RESPONDENT_STATEMENT_BUNDLE_VIEWS,
            OTHER_DOCUMENTS_BUNDLE_VIEWS
        ).flatMap(Collection::stream).collect(toList()));
    }

    @Test
    void testGetDocumentBundleViewsIfEmpty() {
        when(applicationDocumentsTransformer.getApplicationStatementAndDocumentBundle(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(null);

        when(furtherEvidenceTransformer.getFurtherEvidenceDocumentsBundleView(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(Collections.emptyList());

        when(respondentStatementsTransformer.getRespondentStatementsBundle(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(Collections.emptyList());

        when(otherDocumentsTransformer.getOtherDocumentsView(CASE_DATA, DOCUMENT_VIEW_TYPE))
            .thenReturn(Collections.emptyList());

        List<DocumentContainerView> actual = underTest.getDocumentBundleViews(CASE_DATA, DOCUMENT_VIEW_TYPE);

        assertThat(actual).isEqualTo(Collections.emptyList());
    }
}
