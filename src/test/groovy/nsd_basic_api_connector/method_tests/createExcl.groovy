package nsd_basic_api_connector.method_tests


import nsd_basic_api_connector.ApiTestUtils

import java.text.SimpleDateFormat

String OBJECT_UUID = 'servicetime$4777701'
String DATE_PARSE_PATTERN = 'yyyy-MM-dd'
String TARGET_DATE = '2022-01-15'


ApiTestUtils.getApi().createExcl(
        OBJECT_UUID,
        new SimpleDateFormat(DATE_PARSE_PATTERN).parse(TARGET_DATE)
)

