package nsd_basic_api_connector.method_tests

import nsd_basic_api_connector.ApiTestUtils
import ru.kazantsev.nsd.basic_api_connector.Connector
import ru.kazantsev.nsd.basic_api_connector.NsdDto

import java.text.SimpleDateFormat

String OBJECT_UUID = 'servicetime$4777701'
String DATE_PARSE_PATTERN = 'yyyy-MM-dd'
String TARGET_DATE = '2023-01-17'

Connector api = ApiTestUtils.getApi()

NsdDto.ServiceTimeExclusionDto excl = api.createExcl(
        OBJECT_UUID,
        new SimpleDateFormat(DATE_PARSE_PATTERN).parse(TARGET_DATE)
)

api.editExcl(
        excl.uuid,
        28800000,
        53000000
)


