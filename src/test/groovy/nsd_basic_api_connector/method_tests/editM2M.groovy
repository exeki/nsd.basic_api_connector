package nsd_basic_api_connector.method_tests

import nsd_basic_api_connector.ApiTestUtils
import ru.ekazantsev.nsd_basic_api_connector.Connector

String METACLASS = 'task$task'
String TEXT = 'testFile.txt'
String OBJECT_UUID = 'serviceCall$50175809'

Connector api = ApiTestUtils.getApi()

HashMap task = api.createM2M(
        METACLASS,
        [
                'description': TEXT,
                'serviceCall': OBJECT_UUID
        ],
        ['title', 'UUID']
)

api.editM2M(
        task.UUID as String,
        ['title' : 'drthwrthwrh'],
        ['title', 'UUID']
)