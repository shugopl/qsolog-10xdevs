Poprzednie poprawki nic nie dało <log>, popraw tak aby te testy udało się przejść w całości


<log>
Error:  Failures: 
Error:    AiControllerTest.generatePeriodReport_shouldCreateReportAndPersistToHistory:172 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    AiControllerTest.generatePeriodReport_shouldWorkInPolish:211 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    AiControllerTest.getReports_shouldFilterByDateRange:258 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    AiControllerTest.getReports_shouldReturnUserReports:231 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldCountByBandCorrectly:178 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldCountByDayCorrectly:238 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldCountByModeCorrectly:197 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldFilterByDateRange:218 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldReturnAllStats:159 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    StatsControllerTest.getStatsSummary_shouldReturnEmptyForUserWithNoQsos:272 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    SuggestionsControllerTest.getSuggestions_shouldReturnMostRecentData:119 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    SuggestionsControllerTest.getSuggestions_shouldReturnSuggestionsFromHistory:100 Status expected:<200 OK> but was:<400 BAD_REQUEST>
Error:    SuggestionsControllerTest.getSuggestions_shouldTruncateLongNotes:153 Status expected:<200 OK> but was:<400 BAD_REQUEST>
[INFO] 
Error:  Tests run: 71, Failures: 13, Errors: 0, Skipped: 0
[INFO] 
[INFO] ------------------------------------------------------------------------
[INFO] BUILD FAILURE
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  01:30 min
[INFO] Finished at: 2026-01-30T23:33:36Z
[INFO] ------------------------------------------------------------------------
Error:  Failed to execute goal org.apache.maven.plugins:maven-surefire-plugin:3.5.2:test (default-test) on project qsolog-backend: There are test failures.
Error:  
Error:  See /home/runner/work/qsolog-10xdevs/qsolog-10xdevs/backend/target/surefire-reports for the individual test results.
Error:  See dump files (if any exist) [date].dump, [date]-jvmRun[N].dump and [date].dumpstream.
Error:  -> [Help 1]
Error:  
Error:  To see the full stack trace of the errors, re-run Maven with the -e switch.
Error:  Re-run Maven using the -X switch to enable full debug logging.
Error:  
Error:  For more information about the errors and possible solutions, please read the following articles:
Error:  [Help 1] http://cwiki.apache.org/confluence/display/MAVEN/MojoFailureException
Error: Process completed with exit code 1.
</log> 
