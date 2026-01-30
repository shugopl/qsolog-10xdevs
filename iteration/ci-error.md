Mimo poprawek wystąpły błędy z <log>, popraw je jeśli nie da się tego przejść bez dodawania klucza API do AI to zamokuj odpowiedzi

<log>
Error:  Failures: 
Error:    AiControllerTest.generatePeriodReport_shouldCreateReportAndPersistToHistory:172 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    AiControllerTest.generatePeriodReport_shouldWorkInPolish:211 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    AiControllerTest.getReports_shouldFilterByDateRange:258 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    AiControllerTest.getReports_shouldReturnUserReports:231 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    AuthControllerTest.me_shouldReturn401WithInvalidToken:236 Status expected:<401 UNAUTHORIZED> but was:<500 INTERNAL_SERVER_ERROR>
Error:    StatsControllerTest.getStatsSummary_shouldCountByBandCorrectly:178 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldCountByDayCorrectly:238 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldCountByModeCorrectly:197 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldFilterByDateRange:218 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldReturnAllStats:159 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldReturnEmptyForUserWithNoQsos:272 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    SuggestionsControllerTest.getSuggestions_shouldReturnMostRecentData:119 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    SuggestionsControllerTest.getSuggestions_shouldReturnSuggestionsFromHistory:100 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    SuggestionsControllerTest.getSuggestions_shouldTruncateLongNotes:153 Status expected:<200 OK> but was:<400 BAD_REQUEST>
</log> 
