package uk.gov.hmcts.reform.fpl.exceptions;

public class EncryptedPdfUploadedException extends LogAsWarningException
    // AboutToStartOrSubmitCallbackException
{
    public EncryptedPdfUploadedException(String message) {
        super("Encrypted PDF file was uploaded which cannot be processed.", message);
    }
}
