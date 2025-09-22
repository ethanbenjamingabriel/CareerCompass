package dev.hungrymonkey.careercompass.config

object AppConfig {
    
    object Backend {
        const val RESUME_SERVICE_URL = "https://resume-service-xjnllu5s6a-uc.a.run.app/"
        const val COVER_LETTER_SERVICE_URL = "https://cover-letter-service-xjnllu5s6a-uc.a.run.app/"

        const val CONNECT_TIMEOUT_SECONDS = 30L
        const val READ_TIMEOUT_SECONDS = 60L
        const val WRITE_TIMEOUT_SECONDS = 60L
    }
    
    object Files {
        const val RESUMES_DIRECTORY = "resumes"
        const val FILE_PROVIDER_AUTHORITY_SUFFIX = ".fileprovider"
    }
    
    object Logging {
        const val ENABLE_NETWORK_LOGGING = true
        const val LOG_TAG = "CareerCompass"
    }
}
