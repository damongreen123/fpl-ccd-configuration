package uk.gov.hmcts.reform.fpl.service.casesubmission;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.document.domain.Document;
import uk.gov.hmcts.reform.fpl.enums.docmosis.RenderFormat;
import uk.gov.hmcts.reform.fpl.model.CaseData;
import uk.gov.hmcts.reform.fpl.model.common.DocmosisDocument;
import uk.gov.hmcts.reform.fpl.model.configuration.Language;
import uk.gov.hmcts.reform.fpl.model.docmosis.DocmosisCaseSubmission;
import uk.gov.hmcts.reform.fpl.service.UploadDocumentService;
import uk.gov.hmcts.reform.fpl.service.docmosis.CaseSubmissionGenerationService;
import uk.gov.hmcts.reform.fpl.service.docmosis.DocmosisDocumentGeneratorService;

import java.util.Optional;

import static uk.gov.hmcts.reform.fpl.enums.DocmosisTemplates.C110A;
import static uk.gov.hmcts.reform.fpl.utils.SubmittedFormFilenameHelper.buildFileName;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CaseSubmissionService {
    private final DocmosisDocumentGeneratorService docmosisDocumentGeneratorService;
    private final UploadDocumentService uploadDocumentService;
    private final CaseSubmissionGenerationService documentGenerationService;

    public Document generateSubmittedFormPDF(final CaseData caseData, final boolean isDraft) {
        DocmosisCaseSubmission submittedCase = documentGenerationService.getTemplateData(caseData);

        documentGenerationService.populateCaseNumber(submittedCase, caseData.getId());
        documentGenerationService.populateDraftWaterOrCourtSeal(submittedCase, isDraft,caseData.getImageLanguage());
        Language applicationLanguage = Optional.ofNullable(caseData.getC110A().getLanguageRequirementApplication())
            .orElse(Language.ENGLISH);

        DocmosisDocument document = docmosisDocumentGeneratorService.generateDocmosisDocument(submittedCase,
            C110A,
            RenderFormat.PDF,
            applicationLanguage);

        return uploadDocumentService.uploadPDF(document.getBytes(), buildFileName(caseData, isDraft));
    }

    public String getSigneeName(CaseData caseData) {
        return documentGenerationService.getSigneeName(caseData);
    }
}
