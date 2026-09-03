package com.platform.application.partner;

import com.platform.application.media.storage.MediaFileSource;
import com.platform.application.partner.result.BusinessRegistrationOcrResult;

public interface BusinessRegistrationOcrClient {

	BusinessRegistrationOcrResult analyze(MediaFileSource file);
}
